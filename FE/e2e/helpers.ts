import { APIRequestContext, expect, Page } from '@playwright/test';

const BASE_URL = 'http://localhost:8080/api';

// ──────────────────────────────────────────────
//  Types (mirror del backend, minimos necesarios)
// ──────────────────────────────────────────────

export interface User {
  id: string;
  email: string;
  displayName: string;
  playerId: string;
  token: string;
}

export interface Deck {
  id: string;
  name: string;
  valid: boolean;
}

export interface Match {
  id: string;
  status: string;
  players: { playerId: string; side: string; displayName: string }[];
}

export interface MatchState {
  matchId: string;
  publicState: {
    matchId: string;
    status: string;
    phase: string;
    turnNumber: number;
    currentPlayerId: string;
    firstPlayerId: string;
    players: unknown[];
    pendingInitialMulliganPlayers?: string[];
    mulliganDrawPending?: boolean;
  };
  privateState: {
    playerId: string;
    hand: { instanceId: string; cardId: string; name: string; supertype: string }[];
    deckCount: number;
    discardCount: number;
  };
}

// ──────────────────────────────────────────────
//  API helpers — HTTP directo al backend
// ──────────────────────────────────────────────

export async function registerUser(request: APIRequestContext): Promise<User> {
  const unique = Date.now();
  const body = {
    email: `e2e_${unique}@test.com`,
    password: 'test123456',
    displayName: `E2E Player ${unique}`,
  };

  const res = await request.post(`${BASE_URL}/users/register`, { data: body });
  expect(res.ok(), `registerUser failed: ${res.status()}`).toBeTruthy();

  const data = await res.json();
  return {
    id: data.id,
    email: body.email,
    displayName: body.displayName,
    playerId: data.playerId,
    token: data.token,
  };
}

export async function loginUser(
  page: Page,
  email: string,
  password: string,
): Promise<void> {
  await page.goto('/auth/login');
  // Esperar a que aparezca el campo Email
  const emailLabel = page.getByLabel('Email');
  try {
    await emailLabel.waitFor({ timeout: 5000 });
  } catch {
    // Si no aparece, puede ser el splash bloqueando — presionar Enter para saltarlo
    await page.keyboard.press('Enter');
    await page.waitForURL(/.*login/, { timeout: 10000 });
    await emailLabel.waitFor({ timeout: 5000 });
  }
  await emailLabel.fill(email);
  await page.getByLabel('Contraseña').fill(password);
  await page.getByRole('button', { name: /ingresar/i }).click();
  await page.waitForURL(/.*home/, { timeout: 10000 });
}

export async function createDeckViaApi(
  request: APIRequestContext,
  token: string,
  playerId: string,
): Promise<Deck> {
  // 1. Obtener mazos predefinidos
  const predefinedRes = await request.get(`${BASE_URL}/decks/predefined`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(predefinedRes.ok()).toBeTruthy();
  const predefined = await predefinedRes.json();
  const deckTemplate = predefined[0];
  expect(deckTemplate).toBeDefined();

  // 2. Copiar al jugador
  const copyRes = await request.post(
    `${BASE_URL}/decks/${deckTemplate.id}/copy?playerId=${playerId}`,
    {
      headers: { Authorization: `Bearer ${token}` },
      data: {},
    },
  );
  expect(copyRes.ok(), `copyDeck failed: ${copyRes.status()}`).toBeTruthy();
  const copied = await copyRes.json();

  // 3. Validar el mazo
  const validateRes = await request.post(
    `${BASE_URL}/decks/${copied.id}/validate`,
    {
      headers: { Authorization: `Bearer ${token}` },
      data: {},
    },
  );
  expect(validateRes.ok(), `validateDeck failed: ${validateRes.status()}`).toBeTruthy();

  return { id: copied.id, name: copied.name, valid: true };
}

export async function createMatchViaApi(
  request: APIRequestContext,
  token: string,
  playerId: string,
  playerName: string,
  deckId: string,
): Promise<Match> {
  const res = await request.post(`${BASE_URL}/matches`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      player1Id: playerId,
      player1Name: playerName,
      player1DeckId: deckId,
      quickMatch: true,
    },
  });
  expect(res.ok(), `createMatch failed: ${res.status()}`).toBeTruthy();
  return res.json();
}

export async function joinMatchViaApi(
  request: APIRequestContext,
  token: string,
  playerId: string,
  playerName: string,
  matchId: string,
  deckId: string,
): Promise<Match> {
  const res = await request.post(`${BASE_URL}/matches/${matchId}/join`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      playerId,
      playerName,
      deckId,
    },
  });
  expect(res.ok(), `joinMatch failed: ${res.status()}`).toBeTruthy();
  return res.json();
}

export async function getMatchState(
  request: APIRequestContext,
  token: string,
  matchId: string,
): Promise<MatchState> {
  const res = await request.get(`${BASE_URL}/matches/${matchId}/state`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(res.ok(), `getMatchState failed: ${res.status()}`).toBeTruthy();
  return res.json();
}

export async function sendGameAction(
  request: APIRequestContext,
  token: string,
  matchId: string,
  playerId: string,
  type: string,
  payload: Record<string, unknown> = {},
): Promise<{ success: boolean; error: unknown }> {
  const res = await request.post(`${BASE_URL}/matches/${matchId}/actions`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      type,
      playerId,
      payload,
      clientRequestId: crypto.randomUUID(),
    },
  });
  expect(res.ok(), `sendGameAction(${type}) failed: ${res.status()}`).toBeTruthy();
  const data = await res.json();
  if (!data.success) {
    throw new Error(`Action ${type} rejected: ${JSON.stringify(data.error)}`);
  }
  return data;
}

// ──────────────────────────────────────────────
//  Setup helpers — completan el setup via API
// ──────────────────────────────────────────────

export async function completeSetup(
  request: APIRequestContext,
  user: User,
  matchId: string,
): Promise<void> {
  const state = await getMatchState(request, user.token, matchId);
  const hand = state.privateState.hand;

  // Buscar cartas Pokémon en la mano
  const pokemonCards = hand.filter(
    (c) => c.supertype === 'POKEMON' || c.supertype === 'Pokémon' || c.supertype === 'Pokemon',
  );
  expect(pokemonCards.length, `${user.displayName} no tiene Pokémon en mano`).toBeGreaterThan(0);

  // Probar cada carta hasta encontrar un Basic (el backend rechaza Stage 1/2)
  let placed = false;
  for (const card of pokemonCards) {
    try {
      await sendGameAction(request, user.token, matchId, user.playerId, 'SETUP_PLACE_ACTIVE', {
        cardInstanceId: card.instanceId,
      });
      placed = true;
      break;
    } catch {
      // Esta carta no es Basic, probar la siguiente
    }
  }
  expect(placed, `${user.displayName} no tiene Pokémon Basic en mano`).toBeTruthy();

  // Confirmar setup
  await sendGameAction(request, user.token, matchId, user.playerId, 'CONFIRM_SETUP', {});
}

// ──────────────────────────────────────────────
//  Wait helpers — esperan condiciones en la UI
// ──────────────────────────────────────────────

export async function waitForMatchActive(
  request: APIRequestContext,
  token: string,
  matchId: string,
  timeoutMs = 15000,
): Promise<MatchState> {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const state = await getMatchState(request, token, matchId);
    if (state.publicState.status === 'ACTIVE') return state;
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`Match did not become ACTIVE within ${timeoutMs}ms`);
}
