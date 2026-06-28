import { test, expect } from '@playwright/test';
import { registerUser, loginUser, createDeckViaApi, createMatchViaApi, joinMatchViaApi, completeSetup, getMatchState, waitForMatchActive, sendGameAction } from './helpers';

test.describe('Smoke Test', () => {
  test('la app muestra la pantalla de login', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page).toHaveURL(/.*login/);
    await expect(page.getByRole('heading', { name: /iniciar sesión/i })).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Contraseña')).toBeVisible();
    await expect(page.getByRole('button', { name: /ingresar/i })).toBeVisible();
  });
});

test.describe('Login', () => {
  test('login exitoso redirige al home', async ({ page, request }) => {
    const user = await registerUser(request);
    await loginUser(page, user.email, 'test123456');
    await expect(page).toHaveURL(/.*home/);
  });
});

test.describe('Crear Mazo', () => {
  test('mazo predefinido copiado aparece en la lista', async ({ page, request }) => {
    const user = await registerUser(request);
    const deck = await createDeckViaApi(request, user.token, user.playerId);

    await loginUser(page, user.email, 'test123456');
    await page.goto('/decks');

    await expect(page.getByText(deck.name)).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Dos Jugadores Autenticados', () => {
  test('ambos jugadores login exitoso en contextos independientes', async ({ browser, request }) => {
    const user1 = await registerUser(request);
    const user2 = await registerUser(request);

    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    await loginUser(page1, user1.email, 'test123456');
    await expect(page1).toHaveURL(/.*home/);

    await loginUser(page2, user2.email, 'test123456');
    await expect(page2).toHaveURL(/.*home/);

    await ctx1.close();
    await ctx2.close();
  });
});

test.describe('Crear y Unirse a Partida', () => {
  test('jugador 1 crea match y jugador 2 se une', async ({ browser, request }) => {
    const user1 = await registerUser(request);
    const user2 = await registerUser(request);
    const deck1 = await createDeckViaApi(request, user1.token, user1.playerId);
    const deck2 = await createDeckViaApi(request, user2.token, user2.playerId);

    const match = await createMatchViaApi(
      request, user1.token, user1.playerId, user1.displayName, deck1.id,
    );

    await joinMatchViaApi(
      request, user2.token, user2.playerId, user2.displayName, match.id, deck2.id,
    );

    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    await loginUser(page1, user1.email, 'test123456');
    await page1.goto('/match/' + match.id);
    await expect(page1).toHaveURL(new RegExp(`.*match/${match.id}`), { timeout: 10000 });

    await loginUser(page2, user2.email, 'test123456');
    await page2.goto('/match/' + match.id);
    await expect(page2).toHaveURL(new RegExp(`.*match/${match.id}`), { timeout: 10000 });

    await ctx1.close();
    await ctx2.close();
  });
});

test.describe('Setup', () => {
  test('ambos jugadores completan el setup y el match pasa a ACTIVE', async ({ browser, request }) => {
    const user1 = await registerUser(request);
    const user2 = await registerUser(request);
    const deck1 = await createDeckViaApi(request, user1.token, user1.playerId);
    const deck2 = await createDeckViaApi(request, user2.token, user2.playerId);

    const match = await createMatchViaApi(
      request, user1.token, user1.playerId, user1.displayName, deck1.id,
    );
    await joinMatchViaApi(
      request, user2.token, user2.playerId, user2.displayName, match.id, deck2.id,
    );

    await completeSetup(request, user1, match.id);
    await completeSetup(request, user2, match.id);

    const state = await waitForMatchActive(request, user1.token, match.id);
    expect(state.publicState.status).toBe('ACTIVE');

    const ctx1 = await browser.newContext();
    const page1 = await ctx1.newPage();
    await loginUser(page1, user1.email, 'test123456');
    await page1.goto('/match/' + match.id);
    await expect(page1.locator('app-setup-overlay')).not.toBeVisible({ timeout: 10000 });

    await ctx1.close();
  });
});

test.describe('Un Turno', () => {
  test('jugador ejecuta DRAW y END_TURN, turno pasa al oponente', async ({ browser, request }) => {
    const user1 = await registerUser(request);
    const user2 = await registerUser(request);
    const deck1 = await createDeckViaApi(request, user1.token, user1.playerId);
    const deck2 = await createDeckViaApi(request, user2.token, user2.playerId);

    const match = await createMatchViaApi(
      request, user1.token, user1.playerId, user1.displayName, deck1.id,
    );
    await joinMatchViaApi(
      request, user2.token, user2.playerId, user2.displayName, match.id, deck2.id,
    );

    await completeSetup(request, user1, match.id);
    await completeSetup(request, user2, match.id);
    await waitForMatchActive(request, user1.token, match.id);

    const initial = await getMatchState(request, user1.token, match.id);
    const myTurn = initial.publicState.currentPlayerId === user1.playerId;

    if (myTurn && initial.publicState.phase === 'DRAW') {
      await sendGameAction(request, user1.token, match.id, user1.playerId, 'DRAW_CARD', {});
    }

    await sendGameAction(request, user1.token, match.id, user1.playerId, 'END_TURN', {});

    const after = await getMatchState(request, user1.token, match.id);
    expect(after.publicState.currentPlayerId).toBe(user2.playerId);

    const ctx1 = await browser.newContext();
    const page1 = await ctx1.newPage();
    await loginUser(page1, user1.email, 'test123456');
    await page1.goto('/match/' + match.id);
    await expect(page1.getByText(/esperando al oponente/i)).toBeVisible({ timeout: 10000 });

    await ctx1.close();
  });
});
