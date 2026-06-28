# Tasks — Persona A: Frontend Foundation

> All tasks are in `FE/src/app/` unless noted otherwise.
> Backend source references are in `BE/src/main/java/ar/edu/utn/frc/tup/piii/`.

---

## 1. Environment Setup

- [x] 1.1 Run `npm install` in `FE/` directory.
  - **Done when:** `node_modules/` exists and `npm install` exits 0.

- [x] 1.2 Install Tailwind CSS v4: `npm install -D tailwindcss @tailwindcss/postcss`.
  - **Done when:** `tailwindcss` and `@tailwindcss/postcss` appear in `devDependencies` in `package.json`.

- [x] 1.3 Create `FE/postcss.config.js`:
  ```js
  module.exports = {
    plugins: {
      '@tailwindcss/postcss': {}
    }
  };
  ```
  - **Done when:** file exists with correct content.

- [x] 1.4 Replace contents of `FE/src/styles.css` with:
  ```css
  @import "tailwindcss";
  ```
  - **Done when:** `styles.css` contains the Tailwind import directive.

- [x] 1.5 Run `ng build` in `FE/`. Verify zero errors.
  - **Done when:** build succeeds with no Tailwind-related errors.

---

## 2. Models — Card Models

**File:** `shared/models/card.models.ts`
**Source:** `BE/.../dtos/cards/CardDetailResponse.java`, `CardSummaryResponse.java`, `AttackDto.java`

- [x] 2.1 Add `TrainerSubtype` type:
  ```typescript
  export type TrainerSubtype = 'ITEM' | 'SUPPORTER' | 'STADIUM' | 'POKEMON_TOOL';
  ```
  Add `convertedEnergyCost: number` field to `AttackModel` (after `cost`).
  Add `isMega?: boolean` field to `CardModel` (after `isEx`).
  - **Depends on:** nothing
  - **Done when:** `TrainerSubtype` exists, `AttackModel` has `convertedEnergyCost`, `CardModel` has `isMega`.

- [x] 2.2 Add `CardSummaryResponse` interface to `card.models.ts`:
  ```typescript
  export interface CardSummaryResponse {
    id: string;
    name: string;
    supertype: string;
    setCode: string;
    number: string;
    imageSmallUrl: string;
  }
  ```
  Remove the duplicate `CardSummaryResponse` from `core/api/card-api.service.ts` (replace with import from models).
  - **Depends on:** nothing
  - **Done when:** `CardSummaryResponse` is exported from `card.models.ts`, no duplicate in `card-api.service.ts`.

- [x] 2.3 Add `CardDetailResponse` interface to `card.models.ts`. Exact fields matching backend Java record `CardDetailResponse.java`:
  ```typescript
  export interface CardDetailResponse {
    id: string;
    name: string;
    supertype: string;
    subtypes: string[];
    setCode: string;
    number: string;
    imageSmallUrl: string | null;
    imageLargeUrl: string | null;
    rulesText: string[];
    hp?: number;
    stage?: string;
    evolvesFrom?: string;
    types?: string[];
    attacks?: AttackModel[];
    weaknesses?: WeaknessModel[];
    resistances?: ResistanceModel[];
    retreatCost?: string[];
    isEx?: boolean;
    isMega?: boolean;
  }
  ```
  - **Depends on:** 2.1 (AttackModel with convertedEnergyCost)
  - **Done when:** interface exists, all fields match backend record, compiles without error.

- [x] 2.4 Add `PaginatedCardsResponse` interface to `card.models.ts`:
  ```typescript
  export interface PaginatedCardsResponse {
    items: CardSummaryResponse[];
    page: number;
    size: number;
    totalItems: number;
  }
  ```
  - **Depends on:** 2.2 (CardSummaryResponse)
  - **Done when:** interface exists and compiles.

---

## 3. Models — Game State Models

**File:** `shared/models/game-state.models.ts`
**Source:** `BE/.../engine/model/GameState.java`, `PlayerState.java`, `PokemonInPlay.java`, `CardInstance.java`

- [x] 3.1 Fix `PrizeSlotModel`: replace `card: CardModel | null` with `cardId: string | null`. Remove the `import { CardModel }` at the bottom of the file.
  - **Depends on:** nothing
  - **Done when:** `PrizeSlotModel` has `cardId: string | null`, no `CardModel` import in this file.

- [x] 3.2 Create `MatchStateResponse` interface in `game-state.models.ts`:
  ```typescript
  export interface MatchStateResponse {
    matchId: string;
    publicState: PublicGameStateModel;
    privateState: PrivatePlayerStateModel;
  }
  ```
  Backend source: `BE/.../controllers/matches/MatchController.java` returns `GameState` + `PlayerState` directly. The frontend wraps them in this envelope.
  JSON example from backend:
  ```json
  {
    "matchId": "550e8400-e29b-41d4-a716-446655440000",
    "publicState": { "matchId": "...", "status": "ACTIVE", "phase": "MAIN", "turnNumber": 3, "currentPlayerId": "...", "firstPlayerId": "...", "players": [...] },
    "privateState": { "playerId": "...", "hand": [...], "deckCount": 7, "discardCount": 2, "prizes": [...] }
  }
  ```
  - **Depends on:** 3.1
  - **Done when:** interface exists, compiles.

- [x] 3.3 Update `PublicGameStateModel` to match backend `GameState.java` exactly. Add missing fields:
  ```typescript
  export interface PublicGameStateModel {
    matchId: string;
    status: string;          // MatchStatus enum as string
    phase: string;           // TurnPhase enum as string
    turnNumber: number;
    currentPlayerId: string;
    firstPlayerId: string;
    players: PublicPlayerStateModel[];
    stadiumCardInstanceId: string | null;
    winnerPlayerId: string | null;
    finishReason: string | null;
    createdAt: string;       // ISO-8601
    updatedAt: string;       // ISO-8601
  }
  ```
  Backend source: `BE/.../engine/model/GameState.java`
  - **Depends on:** nothing
  - **Done when:** fields match backend `GameState.java`, compiles.

- [x] 3.4 Update `PublicPlayerStateModel` to match backend `PlayerState.java`. Add missing fields:
  ```typescript
  export interface PublicPlayerStateModel {
    playerId: string;
    side: string;            // PlayerSide enum as string
    activePokemon: PublicPokemonSlotModel | null;
    bench: PublicPokemonSlotModel[];
    deckCount: number;       // count only, not full array
    discardCount: number;    // count only, not full array
    prizesCount: number;     // count only, opponent's prizes hidden
  }
  ```
  Note: `deck`, `hand`, `prizes`, `discard` are full `CardInstance[]` in backend, but the PUBLIC state should only expose counts for opponent's data. The owning player's data comes via `privateState`.
  - **Depends on:** nothing
  - **Done when:** fields match the public-facing contract, compiles.

- [x] 3.5 Update `PublicPokemonSlotModel` to match backend `PokemonInPlay.java`:
  ```typescript
  export interface PublicPokemonSlotModel {
    instanceId: string;
    cardDefinitionId: string;    // renamed from cardId to match backend
    damageCounters: number;
    specialConditions: string[]; // SpecialCondition enum as string[]
    attachedCards: string[];     // instanceIds of attached energy cards
    toolCardInstanceId: string | null;
  }
  ```
  Backend source: `BE/.../engine/model/PokemonInPlay.java`
  - **Depends on:** nothing
  - **Done when:** fields match backend `PokemonInPlay.java`, `cardId` renamed to `cardDefinitionId`, compiles.

- [x] 3.6 Update `PrivatePlayerStateModel` to match backend `PlayerState.java`:
  ```typescript
  export interface PrivatePlayerStateModel {
    playerId: string;
    side: string;
    hand: PrivateHandCardModel[];
    deckCount: number;
    discardCount: number;
    prizes: PrizeSlotModel[];
    activePokemon: PublicPokemonSlotModel | null;
    bench: PublicPokemonSlotModel[];
  }
  ```
  - **Depends on:** 3.1 (PrizeSlotModel), 3.5 (PublicPokemonSlotModel)
  - **Done when:** fields match, compiles.

- [x] 3.7 Update `PrivateHandCardModel` to match backend `CardInstance.java`:
  ```typescript
  export interface PrivateHandCardModel {
    instanceId: string;
    cardDefinitionId: string;   // renamed from cardId to match backend
    name: string;               // resolved name for display
    supertype: string;          // resolved supertype for display
  }
  ```
  Note: Backend sends `CardInstance` with `instanceId` + `cardDefinitionId`. The frontend resolves `name`/`supertype` from `CardRepositoryService` cache. This interface adds those resolved fields for display convenience.
  - **Depends on:** nothing
  - **Done when:** interface has `cardDefinitionId` (not `cardId`), compiles.

---

## 4. Models — Game Action Models

**File:** `shared/models/game-action.models.ts`
**Source:** `BE/.../engine/action/GameActionType.java`, `GameActionResponse.java`

- [x] 4.1 Update `GameActionType`: remove `USE_ABILITY`, add `TAKE_PRIZE_CARD`, add `@deprecated` JSDoc to `DRAW_CARD` and `CHOOSE_KNOCKOUT_REPLACEMENT`. Final union:
  ```typescript
  export type GameActionType =
    | 'PUT_BASIC_ON_BENCH'
    | 'ATTACH_ENERGY'
    | 'EVOLVE_POKEMON'
    | 'PLAY_TRAINER'
    | 'DECLARE_ATTACK'
    | 'RETREAT_ACTIVE'
    | 'END_TURN'
    /** @deprecated Auto-handled server-side, do not send from frontend */
    | 'DRAW_CARD'
    /** @deprecated Auto-handled server-side, do not send from frontend */
    | 'CHOOSE_KNOCKOUT_REPLACEMENT'
    | 'TAKE_PRIZE_CARD';
  ```
  Backend source: `BE/.../engine/action/GameActionType.java`
  - **Depends on:** nothing
  - **Done when:** `USE_ABILITY` removed, `TAKE_PRIZE_CARD` added, deprecated JSDoc present, compiles.

- [x] 4.2 Rename `GameEventModel` to `GameEventDto`. Replace ALL references across the entire codebase with `GameEventDto`. Delete the old `GameEventModel` type after migration. Simplify fields to:
  ```typescript
  export interface GameEventDto {
    type: string;
    message: string;
    payload?: Record<string, unknown>;
  }
  ```
  Files that may reference `GameEventModel`: `game-action.models.ts`, `match-socket.service.ts`, any future component imports.
  - **Depends on:** nothing
  - **Done when:** `grep -r "GameEventModel" FE/src/` returns zero results, `GameEventDto` exists, compiles.

- [x] 4.3 Update `GameActionResponse`:
  ```typescript
  export interface GameActionResponse {
    success: boolean;
    clientRequestId: string;
    publicState: PublicGameStateModel | null;
    privateState: PrivatePlayerStateModel | null;
    events: GameEventDto[];
    error: GameErrorModel | null;
  }
  ```
  Import `PublicGameStateModel` and `PrivatePlayerStateModel` from `game-state.models.ts`. Import `GameEventDto` (renamed from step 4.2).
  Backend source: `BE/.../dtos/matches/GameActionResponse.java`
  - **Depends on:** 3.3 (PublicGameStateModel), 3.6 (PrivatePlayerStateModel), 4.2 (GameEventDto)
  - **Done when:** types are correct (not `unknown`), compiles.

- [x] 4.4 Update `GameErrorModel` to match backend:
  ```typescript
  export interface GameErrorModel {
    code: string;
    message: string;
    details?: Record<string, unknown>;
  }
  ```
  - **Depends on:** nothing
  - **Done when:** fields match backend `GameError.java`, compiles.

---

## 5. Models — Deck Models

**File:** `shared/models/deck.models.ts`
**Source:** `BE/.../dtos/decks/DeckResponse.java`, `CreateDeckRequest.java`, `UpdateDeckRequest.java`

- [x] 5.1 Rename `DeckModel` to `DeckResponse`. Update ALL imports across the codebase (`deck-api.service.ts`, `deck-builder-facade.service.ts`, etc.).
  - **Depends on:** nothing
  - **Done when:** `grep -r "DeckModel" FE/src/` returns zero results (except in comments), `DeckResponse` is used everywhere, compiles.

- [x] 5.2 Add `CreateDeckRequest` to `deck.models.ts`:
  ```typescript
  export interface CreateDeckRequest {
    name: string;
    cards: { cardId: string; quantity: number }[];
  }
  ```
  Note: NO `playerId` — backend `CreateDeckRequest.java` does not have it.
  - **Depends on:** nothing
  - **Done when:** interface exists, no `playerId` field, compiles.

- [x] 5.3 Add `UpdateDeckRequest` to `deck.models.ts`:
  ```typescript
  export interface UpdateDeckRequest {
    name: string;
    cards: { cardId: string; quantity: number }[];
  }
  ```
  - **Depends on:** nothing
  - **Done when:** interface exists, compiles.

- [x] 5.4 Add `DeckValidationResponse` to `deck.models.ts`:
  ```typescript
  export interface DeckValidationResponse {
    valid: boolean;
    errors: DeckValidationErrorModel[];
  }
  ```
  Note: `DeckValidationErrorModel` already exists in this file.
  - **Depends on:** nothing
  - **Done when:** interface exists, references existing `DeckValidationErrorModel`, compiles.

---

## 6. Models — UI State Models

**File (CREATE):** `shared/models/ui-state.models.ts`

- [x] 6.1 Create `shared/models/ui-state.models.ts`:
  ```typescript
  export type SelectionMode =
    | 'NONE'
    | 'SELECT_BENCH_SLOT'
    | 'SELECT_TARGET_POKEMON'
    | 'SELECT_ATTACK'
    | 'SELECT_RETREAT_TARGET';

  export interface SelectionState {
    mode: SelectionMode;
    selectedHandIndex: number | null;
    selectedInstanceId: string | null;
    validTargets: string[];
  }
  ```
  - **Depends on:** nothing
  - **Done when:** file exists, both types exported, compiles.

---

## 7. API Services — Card API

**File:** `core/api/card-api.service.ts`

- [x] 7.1 Change `getCardById` return type from `Observable<unknown>` to `Observable<CardDetailResponse>`. Import `CardDetailResponse` from `shared/models/card.models.ts`.
  - **Depends on:** 2.3 (CardDetailResponse)
  - **Done when:** return type is `Observable<CardDetailResponse>`, no `unknown` in this method, compiles.

- [x] 7.2 Replace local `CardSummaryResponse` in `card-api.service.ts` with import from `shared/models/card.models.ts`. Update `CardSearchResponse` to use imported type. Verify `searchCards()` params are `query`, `supertype`, `setCode`, `page`, `size`.
  - **Depends on:** 2.2 (CardSummaryResponse in models)
  - **Done when:** no local `CardSummaryResponse` definition in this file, imports from models, params correct, compiles.

---

## 8. API Services — Deck API

**File:** `core/api/deck-api.service.ts`

- [x] 8.1 Add `listByPlayer(playerId: string): Observable<DeckResponse[]>`:
  ```typescript
  listByPlayer(playerId: string): Observable<DeckResponse[]> {
    return this.apiClient.get<DeckResponse[]>(`/decks?playerId=${playerId}`);
  }
  ```
  Import `DeckResponse` from models.
  - **Depends on:** 5.1 (DeckResponse)
  - **Done when:** method exists, calls `GET /decks?playerId={id}`, compiles.

- [x] 8.2 Add `get(deckId: string): Observable<DeckResponse>`:
  ```typescript
  get(deckId: string): Observable<DeckResponse> {
    return this.apiClient.get<DeckResponse>(`/decks/${deckId}`);
  }
  ```
  - **Depends on:** 5.1 (DeckResponse)
  - **Done when:** method exists, compiles.

- [x] 8.3 Add `update(deckId: string, req: UpdateDeckRequest): Observable<DeckResponse>`:
  ```typescript
  update(deckId: string, req: UpdateDeckRequest): Observable<DeckResponse> {
    return this.apiClient.put<DeckResponse>(`/decks/${deckId}`, req);
  }
  ```
  Import `UpdateDeckRequest` from models.
  - **Depends on:** 5.3 (UpdateDeckRequest), 5.1 (DeckResponse)
  - **Done when:** method exists, calls `PUT /decks/{id}`, compiles.

- [x] 8.4 Add `delete(deckId: string): Observable<void>`:
  ```typescript
  delete(deckId: string): Observable<void> {
    return this.apiClient.delete<void>(`/decks/${deckId}`);
  }
  ```
  - **Depends on:** nothing
  - **Done when:** method exists, calls `DELETE /decks/{id}`, returns `Observable<void>`, compiles.

- [x] 8.5 Add `validate(deckId: string): Observable<DeckValidationResponse>`:
  ```typescript
  validate(deckId: string): Observable<DeckValidationResponse> {
    return this.apiClient.post<DeckValidationResponse>(`/decks/${deckId}/validate`, {});
  }
  ```
  Import `DeckValidationResponse` from models.
  - **Depends on:** 5.4 (DeckValidationResponse)
  - **Done when:** method exists, calls `POST /decks/{id}/validate`, compiles.

- [x] 8.6 Add `validateCards(req: { cardId: string; quantity: number }[]): Observable<DeckValidationResponse>`:
  ```typescript
  validateCards(req: { cardId: string; quantity: number }[]): Observable<DeckValidationResponse> {
    return this.apiClient.post<DeckValidationResponse>('/decks/validate', { cards: req });
  }
  ```
  - **Depends on:** 5.4 (DeckValidationResponse)
  - **Done when:** method exists, calls `POST /decks/validate`, compiles.

- [x] 8.7 Remove `getSeedDecks()` method and `SeedDeckResponse` interface from `deck-api.service.ts`.
  - **Depends on:** nothing
  - **Done when:** `grep -r "getSeedDecks\|SeedDeckResponse" FE/src/` returns zero results, compiles.

---

## 9. API Services — Match API

**File:** `core/api/match-api.service.ts`

- [x] 9.1 Update `MatchResponse` to match backend `MatchResponse.java` (4 fields only):
  ```typescript
  export interface MatchResponse {
    matchId: string;
    playerId: string;
    side: string;
    status: string;
  }
  ```
  Remove any extra fields (`winnerPlayerId`, `createdAt`, etc. — those are NOT in the backend `MatchResponse`).
  Backend source: `BE/.../dtos/matches/MatchResponse.java`
  - **Depends on:** nothing
  - **Done when:** interface has exactly 4 fields matching backend, compiles.

- [x] 9.2 Type `getMatchState()` return as `Observable<MatchStateResponse>`. Import `MatchStateResponse` from `shared/models/game-state.models.ts`. Update the `playerId` query param:
  ```typescript
  getMatchState(matchId: string, playerId: string): Observable<MatchStateResponse> {
    return this.apiClient.get<MatchStateResponse>(`/matches/${matchId}/state?playerId=${playerId}`);
  }
  ```
  - **Depends on:** 3.2 (MatchStateResponse)
  - **Done when:** return type is `Observable<MatchStateResponse>`, no `unknown`, compiles.

- [x] 9.3 Type `sendAction()` parameter and return:
  ```typescript
  sendAction(matchId: string, action: GameActionRequest): Observable<GameActionResponse> {
    return this.apiClient.post<GameActionResponse>(`/matches/${matchId}/actions`, action);
  }
  ```
  Import `GameActionRequest` and `GameActionResponse` from `shared/models/game-action.models.ts`.
  - **Depends on:** 4.3 (GameActionResponse)
  - **Done when:** parameter typed as `GameActionRequest`, return typed as `Observable<GameActionResponse>`, compiles.

---

## 10. Core Services

- [x] 10.1 Create `core/services/card-repository.service.ts`:
  ```typescript
  import { Injectable, signal } from '@angular/core';
  import { firstValueFrom } from 'rxjs';
  import { CardApiService } from '../api/card-api.service';
  import { CardDetailResponse } from '../../shared/models/card.models';

  @Injectable({ providedIn: 'root' })
  export class CardRepositoryService {
    private readonly cardApi = inject(CardApiService);
    private readonly _cache = signal<Map<string, CardDetailResponse>>(new Map());
    private readonly _loading = signal<Set<string>>(new Set());

    readonly cache = this._cache.asReadonly();

    async resolve(cardId: string): Promise<CardDetailResponse> {
      const cached = this._cache().get(cardId);
      if (cached) return cached;

      if (this._loading().has(cardId)) {
        await this.waitForLoad(cardId);
        return this._cache().get(cardId)!;
      }

      this._loading.update(s => new Set(s).add(cardId));
      try {
        const card = await firstValueFrom(this.cardApi.getCardById(cardId));
        this._cache.update(cache => {
          const newCache = new Map(cache);
          newCache.set(cardId, card);
          return newCache;
        });
        return card;
      } finally {
        this._loading.update(s => {
          const newSet = new Set(s);
          newSet.delete(cardId);
          return newSet;
        });
      }
    }

    async preload(cardIds: string[]): Promise<void> {
      const uncached = cardIds.filter(id => !this._cache().has(id));
      if (uncached.length === 0) return;
      await Promise.all(uncached.map(id => this.resolve(id)));
    }

    getFromCache(cardId: string): CardDetailResponse | null {
      return this._cache().get(cardId) ?? null;
    }

    private waitForLoad(cardId: string): Promise<void> {
      return new Promise((resolve) => {
        const interval = setInterval(() => {
          if (!this._loading().has(cardId)) {
            clearInterval(interval);
            resolve();
          }
        }, 50);
      });
    }
  }
  ```
  Behavior:
  - `resolve(cardId)`: Returns cached card if present. Otherwise fetches from `CardApiService.getCardById()`, stores in cache, returns it. Deduplicates concurrent requests for the same cardId.
  - `preload(cardIds[])`: Resolves all uncached cards in parallel.
  - `getFromCache(cardId)`: Returns cached card or null. No HTTP call.
  - **Depends on:** 7.1 (getCardById typed), 2.3 (CardDetailResponse)
  - **Done when:** file exists, compiles, `resolve` returns `Promise<CardDetailResponse>`.

- [x] 10.2 Create `core/services/notification.service.ts`:
  ```typescript
  import { Injectable, signal } from '@angular/core';

  export type NotificationType = 'info' | 'success' | 'warning' | 'error';

  export interface Notification {
    id: string;
    message: string;
    type: NotificationType;
  }

  @Injectable({ providedIn: 'root' })
  export class NotificationService {
    private readonly _notifications = signal<Notification[]>([]);
    readonly notifications = this._notifications.asReadonly();

    show(message: string, type: NotificationType, duration = 3000): void {
      const id = crypto.randomUUID();
      const notification: Notification = { id, message, type };
      this._notifications.update(list => [...list, notification]);
      if (duration > 0) {
        setTimeout(() => this.dismiss(id), duration);
      }
    }

    dismiss(id: string): void {
      this._notifications.update(list => list.filter(n => n.id !== id));
    }
  }
  ```
  - **Depends on:** nothing
  - **Done when:** file exists, `Notification` interface exported, compiles.

---

## 11. Facades & Dispatcher Adaptation

- [x] 11.1 Expand `DeckCardEntry` in `features/decks/services/deck-builder-facade.service.ts`:
  ```typescript
  export interface DeckCardEntry {
    cardId: string;
    name: string;
    supertype: string;
    isBasicEnergy: boolean;
    quantity: number;
  }
  ```
  - **Depends on:** nothing
  - **Done when:** interface has all 5 fields, compiles.

- [x] 11.2 Update `addCard()` signature to:
  ```typescript
  addCard(cardId: string, name: string, supertype: string, isBasicEnergy = false): void
  ```
  Store `name`, `supertype`, `isBasicEnergy` in the entry. If card exists, increment quantity only.
  - **Depends on:** 11.1 (expanded DeckCardEntry)
  - **Done when:** method accepts 4 params, stores all fields, compiles.

- [x] 11.3 Fix `removeCard()` to decrement quantity:
  ```typescript
  removeCard(cardId: string): void {
    this._cards.update(prev => {
      const existing = prev.find(c => c.cardId === cardId);
      if (!existing) return prev;
      if (existing.quantity <= 1) return prev.filter(c => c.cardId !== cardId);
      return prev.map(c => c.cardId === cardId ? { ...c, quantity: c.quantity - 1 } : c);
    });
  }
  ```
  - **Depends on:** nothing
  - **Done when:** `removeCard` decrements, removes at 0, compiles.

- [x] 11.4 Update `DeckBuilderFacadeService` imports: replace `DeckModel` with `DeckResponse`, use corrected `DeckValidationModel`. Update `createDeck()` to accept `playerId` as second param:
  ```typescript
  createDeck(name: string, playerId: string): Observable<unknown> {
    return this.deckApi.createDeck({ name, cards: this._cards() })
      .pipe(tap(() => this.reset()));
  }
  ```
  Note: `CreateDeckRequest` doesn't have `playerId` (backend doesn't need it). The `playerId` param is reserved for future use but not sent.
  - **Depends on:** 5.1 (DeckResponse), 5.2 (CreateDeckRequest)
  - **Done when:** imports use `DeckResponse`, no `DeckModel` references, compiles.

- [x] 11.5 Update `MatchFacadeService` imports: use corrected `MatchResponse` (4 fields) and `MatchStateResponse`. Remove `_status` signal (not in backend `MatchResponse`). Update fields stored from response.
  - **Depends on:** 9.1 (MatchResponse), 3.2 (MatchStateResponse)
  - **Done when:** imports correct, `_status` removed if not needed, compiles.

- [x] 11.6 Fix `GameActionDispatcherService` payloads to match backend:
  ```typescript
  attachEnergy(matchId, playerId, handIndex: number, targetInstanceId: string) {
    return this.dispatchAction(matchId, playerId, 'ATTACH_ENERGY', {
      handIndex,
      targetPokemonInstanceId: targetInstanceId
    });
  }

  retreatActive(matchId, playerId, benchIndex: number) {
    return this.dispatchAction(matchId, playerId, 'RETREAT_ACTIVE', { benchIndex });
  }

  declareAttack(matchId, playerId, attackIndex: number, targetInstanceId: string) {
    return this.dispatchAction(matchId, playerId, 'DECLARE_ATTACK', {
      attackIndex,
      targetPokemonInstanceId: targetInstanceId
    });
  }

  putBasicOnBench(matchId, playerId, handIndex: number) {
    return this.dispatchAction(matchId, playerId, 'PUT_BASIC_ON_BENCH', { handIndex });
  }

  evolvePokemon(matchId, playerId, handIndex: number, targetInstanceId: string) {
    return this.dispatchAction(matchId, playerId, 'EVOLVE_POKEMON', {
      handIndex,
      targetPokemonInstanceId: targetInstanceId
    });
  }

  playTrainer(matchId, playerId, handIndex: number) {
    return this.dispatchAction(matchId, playerId, 'PLAY_TRAINER', { handIndex });
  }
  ```
  Backend payloads source: `BE/.../engine/action/` (PutBasicOnBenchPayload, AttachEnergyPayload, etc.)
  - **Depends on:** 4.1 (GameActionType)
  - **Done when:** all payloads match backend payload classes, compiles.

- [x] 11.7 Remove `drawCard()` method from `GameActionDispatcherService` (deprecated action, auto-handled server-side).
  - **Depends on:** nothing
  - **Done when:** `grep -r "drawCard" FE/src/` returns zero results in this file, compiles.

---

## 12. Shared Pipes & Directives

- [x] 12.1 Create `shared/pipes/card-image.pipe.ts`:
  ```typescript
  import { Pipe, PipeTransform } from '@angular/core';

  @Pipe({ name: 'cardImage', standalone: true })
  export class CardImagePipe implements PipeTransform {
    transform(cardId: string, size: 'small' | 'large' = 'small'): string {
      const [set, number] = cardId.split('-');
      const suffix = size === 'large' ? '_hires' : '';
      return `https://images.pokemontcg.io/${set}/${number}${suffix}.png`;
    }
  }
  ```
  - **Depends on:** nothing
  - **Done when:** file exists, pipe is `standalone: true`, transforms `xy1-1` to correct URL, compiles.

- [x] 12.2 Create `shared/pipes/energy-icon.pipe.ts`:
  ```typescript
  import { Pipe, PipeTransform } from '@angular/core';

  @Pipe({ name: 'energyIcon', standalone: true })
  export class EnergyIconPipe implements PipeTransform {
    transform(type: string): string {
      return `assets/icons/energy/energy-${type.toLowerCase()}.svg`;
    }
  }
  ```
  - **Depends on:** nothing
  - **Done when:** file exists, pipe is `standalone: true`, transforms `FIRE` to `assets/icons/energy/energy-fire.svg`, compiles.

- [x] 12.3 Create `shared/pipes/condition-icon.pipe.ts`:
  ```typescript
  import { Pipe, PipeTransform } from '@angular/core';

  @Pipe({ name: 'conditionIcon', standalone: true })
  export class ConditionIconPipe implements PipeTransform {
    transform(condition: string): string {
      return `assets/icons/conditions/condition-${condition.toLowerCase()}.svg`;
    }
  }
  ```
  - **Depends on:** nothing
  - **Done when:** file exists, pipe is `standalone: true`, transforms `BURNED` to `assets/icons/conditions/condition-burned.svg`, compiles.

- [x] 12.4 Create `shared/directives/click-outside.directive.ts`:
  ```typescript
  import { Directive, ElementRef, EventEmitter, Output, OnDestroy } from '@angular/core';

  @Directive({ selector: '[clickOutside]', standalone: true })
  export class ClickOutsideDirective implements OnDestroy {
    @Output() clickOutside = new EventEmitter<void>();
    private listener = (event: MouseEvent) => {
      if (!this.el.nativeElement.contains(event.target)) {
        this.clickOutside.emit();
      }
    };

    constructor(private el: ElementRef) {
      document.addEventListener('click', this.listener);
    }

    ngOnDestroy(): void {
      document.removeEventListener('click', this.listener);
    }
  }
  ```
  - **Depends on:** nothing
  - **Done when:** file exists, directive is `standalone: true`, emits on outside click, removes listener on destroy, compiles.

---

## 13. Shared Components

All components: `standalone: true`, `ChangeDetectionStrategy.OnPush`, inline template + styles.

- [x] 13.1 Create `shared/components/loading-spinner/loading-spinner.component.ts`:
  - Standalone Angular component
  - No inputs, no outputs
  - Template: centered spinner + "Cargando..." text
  - Files: `loading-spinner.component.ts` (inline template + styles)
  - **Depends on:** nothing
  - **Done when:** file exists, `standalone: true`, `changeDetection: OnPush`, renders spinner, compiles.

- [x] 13.2 Create `shared/components/modal/modal.component.ts`:
  - Standalone Angular component
  - Inputs: `title: string` (input()), `open: boolean` (input())
  - Output: `closed: EventEmitter<void>` (output())
  - Template: `@if (open)` renders dark overlay + centered panel + title + close button + `<ng-content>` for body
  - Close button emits `closed`. Click on overlay (not panel) emits `closed`. ESC key emits `closed`.
  - Uses `ClickOutsideDirective` on the overlay div
  - Files: `modal.component.ts` (inline template + styles)
  - **Depends on:** 12.4 (ClickOutsideDirective)
  - **Done when:** file exists, standalone, OnPush, opens/closes with `open` input, emits `closed`, ESC works, compiles.

- [x] 13.3 Create `shared/components/button/button.component.ts`:
  - Standalone Angular component
  - Inputs: `variant: 'primary' | 'secondary' | 'danger' | 'ghost'` (input with default `'primary'`), `disabled: boolean` (input with default `false`), `loading: boolean` (input with default `false`)
  - Output: native click (via `output()` from host click)
  - Template: `<button [class]="..." [disabled]="disabled || loading">` with spinner if loading
  - Files: `button.component.ts` (inline template + styles)
  - **Depends on:** 13.1 (LoadingSpinnerComponent for spinner display, or inline SVG spinner)
  - **Done when:** file exists, standalone, OnPush, variant classes apply, loading shows spinner, compiles.

- [x] 13.4 Create `shared/components/notification/notification.component.ts`:
  - Standalone Angular component
  - Injects `NotificationService`, reads `notifications` signal
  - Template: renders notifications in bottom-right corner with auto-dismiss
  - Each notification: message + type-based color + dismiss button
  - Files: `notification.component.ts` (inline template + styles)
  - **Depends on:** 10.2 (NotificationService)
  - **Done when:** file exists, standalone, OnPush, reads from NotificationService, renders notifications, compiles.

- [x] 13.5 Create `shared/components/card-view/card-view.component.ts`:
  - Standalone Angular component
  - Input: `card: CardSummaryResponse` (input())
  - Template: compact card with image (`card.id | cardImage:'small'`), name, supertype, setCode
  - Image error: fallback to gray placeholder with card name
  - Files: `card-view.component.ts` (inline template + styles)
  - **Depends on:** 2.2 (CardSummaryResponse), 12.1 (CardImagePipe)
  - **Done when:** file exists, standalone, OnPush, uses CardImagePipe, fallback on error, compiles.

- [x] 13.6 Create `shared/components/pokemon-card/pokemon-card.component.ts`:
  - Standalone Angular component
  - Input: `card: CardDetailResponse` (input())
  - Template: full detail — large image, name, supertype, stage, HP, attacks with energy cost icons (EnergyIconPipe), weaknesses, resistances, retreat cost, EX/MEGA badges
  - Files: `pokemon-card.component.ts` (inline template + styles)
  - **Depends on:** 2.3 (CardDetailResponse), 12.1 (CardImagePipe), 12.2 (EnergyIconPipe)
  - **Done when:** file exists, standalone, OnPush, uses both pipes, EX/MEGA badges render, compiles.

---

## 14. SVG Assets

All SVGs are simple monochrome placeholder icons. Visual quality is not important — only filename and existence matter. Each SVG should be a valid SVG file with `<svg viewBox="0 0 24 24">` and a simple shape inside.

- [x] 14.1 Create 10 energy SVG files in `FE/src/assets/icons/energy/`:
  - `energy-grass.svg` (green leaf or circle)
  - `energy-fire.svg` (red flame or circle)
  - `energy-water.svg` (blue droplet or circle)
  - `energy-lightning.svg` (yellow bolt or circle)
  - `energy-psychic.svg` (purple eye or circle)
  - `energy-fighting.svg` (orange fist or circle)
  - `energy-darkness.svg` (dark moon or circle)
  - `energy-metal.svg` (gray gear or circle)
  - `energy-fairy.svg` (pink wing or circle)
  - `energy-colorless.svg` (gray star or circle)
  - **Depends on:** nothing
  - **Done when:** all 10 files exist in `FE/src/assets/icons/energy/`, each is a valid SVG.

- [x] 14.2 Create 5 condition SVG files in `FE/src/assets/icons/conditions/`:
  - `condition-asleep.svg` (Zzz or moon)
  - `condition-burned.svg` (flame)
  - `condition-confused.svg` (question mark)
  - `condition-paralyzed.svg` (lightning bolt)
  - `condition-poisoned.svg` (skull or droplet)
  - **Depends on:** nothing
  - **Done when:** all 5 files exist in `FE/src/assets/icons/conditions/`, each is a valid SVG.

---

## 15. Routing & App Integration

- [x] 15.1 Add `:id/edit` route to `features/decks/routes.ts`:
  ```typescript
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/deck-builder-page/deck-builder-page').then(
        (m) => m.DeckBuilderPage,
      ),
  },
  ```
  - **Depends on:** nothing
  - **Done when:** navigating to `/decks/anything/edit` loads `DeckBuilderPage`, compiles.

- [x] 15.2 Add notification component to `app.html`:
  ```html
  <router-outlet></router-outlet>
  <app-notification></app-notification>
  ```
  Import `NotificationComponent` in `app.ts` imports array.
  - **Depends on:** 13.4 (NotificationComponent)
  - **Done when:** `app.html` has `<app-notification>`, `app.ts` imports it, compiles.

---

## 16. Build Verification

- [x] 16.1 Run `npm install` in `FE/` — verify exits 0.
  - **Done when:** `node_modules/` exists.

- [x] 16.2 Run `ng build` in `FE/` — verify zero TypeScript errors.
  - **Done when:** build succeeds, no errors in output.

- [x] 16.3 Run `grep -r "any" FE/src/app/ --include="*.ts"` — verify no `any` types introduced (existing `any` in pre-existing files is acceptable, new code must not have `any`).
  - **Done when:** no new `any` types in modified/created files.

- [x] 16.4 Verify all imports resolve: open `tsconfig.app.json`, ensure `strict` is `true`, run build.
  - **Done when:** build passes with strict mode.

- [ ] 16.5 Run `ng serve` in `FE/` — verify application starts without console errors.
  - **Done when:** `ng serve` starts, browser shows the app, no console errors.

- [ ] 16.6 Run `ng test` if existing tests exist — verify they still pass.
  - **Done when:** existing tests pass (or skip if no tests exist).
