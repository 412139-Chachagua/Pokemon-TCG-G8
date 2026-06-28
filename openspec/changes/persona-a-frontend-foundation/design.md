## Context

The Pokémon TCG Digital frontend (Angular 20) has a scaffolding with routing, API client, and page stubs, but is non-functional because:

1. **Models are desaligned** with the real Java backend DTOs (verified against actual source in `BE/src/main/java/`)
2. **API services** have incorrect endpoints, missing methods, and untyped returns
3. **No shared UI infrastructure** exists — zero components, pipes, directives
4. **No CSS framework** installed — Tailwind CSS is the decided stack
5. **No `node_modules`** — dependencies not installed

Persona A is the team blocker. Personas B, C, D cannot start features until models and services compile cleanly.

Backend DTOs verified (Java records):
- `MatchResponse`: only `matchId`, `playerId`, `side`, `status` (NOT full game state)
- `GameActionResponse`: `publicState`/`privateState` are `Object` (serialized `GameState`/`PlayerState` from engine)
- `AttackDto`: `cost` is `List<String>` (not `EnergyType[]`), no `baseDamage` field
- `CreateDeckRequest`: NO `playerId` field (only `name` + `cards`)
- `GameActionType`: 7 active + 3 `@Deprecated` (`DRAW_CARD`, `CHOOSE_KNOCKOUT_REPLACEMENT`, `USE_ABILITY`)

## Goals / Non-Goals

**Goals:**
- All TypeScript models match actual backend Java DTOs exactly
- API services cover all REST endpoints with correct params and types
- `ng build` compiles cleanly with zero type errors
- Shared components, pipes, and directives created for team consumption
- Tailwind CSS v4 installed and configured
- CardRepositoryService provides card caching for match rendering
- NotificationService provides global snackbar system

**Non-Goals:**
- Feature implementation (cards catalog, decks, lobby, match) — left to Personas B/C/D
- WebSocket integration (V1 uses polling)
- Auth/JWT (playerName entered manually)
- Game logic in frontend (backend is source of truth)
- Testing (separate task FE-T1)
- Animations, Mega Evolution, multiple expansions

## Decisions

### D1: Models stay in `shared/models/`

**Decision**: Keep all model files in `src/app/shared/models/`.

**Rationale**: Models are already there (5 files). Moving to `core/models/` would require updating all import paths across facades, dispatcher, and page stubs for no functional benefit. The `shared/` location is conventional for types used across multiple features.

**Alternatives considered**: Move to `core/models/` per FRONTEND_PLAN.md — rejected because it's pure churn.

### D2: `CardSummaryResponse` stays in `card-api.service.ts`

**Decision**: Keep `CardSummaryResponse` and `CardSearchResponse` defined in `card-api.service.ts`.

**Rationale**: These are API response types, not domain models. They're only consumed by `CardApiService` and its callers. Keeping them co-located with the service that produces them reduces import ceremony.

**Alternatives**: Move to `card.models.ts` — rejected per user decision.

### D3: `CreateDeckRequest` stays in `deck-api.service.ts`

**Decision**: Keep `CreateDeckRequest` in `deck-api.service.ts`.

**Rationale**: Same as D2 — it's an API request type, not a domain model. The backend `CreateDeckRequest` doesn't include `playerId`, so it's purely an API concern.

### D4: `MatchStateResponse` as separate type in `game-state.models.ts`

**Decision**: Create `MatchStateResponse` interface in `shared/models/game-state.models.ts` with `matchId: string`, `publicState: PublicGameStateModel`, `privateState: PrivatePlayerStateModel`.

**Rationale**: The backend sends `GameState` and `PlayerState` engine objects directly. The frontend needs a typed envelope to deserialize the response from `GET /matches/{id}/state`.

### D5: Frontend models mirror engine structure closely

**Decision**: `PublicGameStateModel` and `PrivatePlayerStateModel` reflect the Java `GameState` and `PlayerState` classes almost field-for-field, with type adaptations (UUID → string, enum → string union).

**Rationale**: Minimizes mapping complexity. The backend sends serialized Java objects; the frontend receives JSON. Keeping the same structure means no transformation layer is needed.

**Simplifications for V1**: Skip `stadiumCardInstanceId`, `turnFlags`, `pendingDecision`, `mulliganCount`, `evolvedThisTurn`, `enteredTurnNumber` — not needed for rendering.

### D6: `DeckBuilderFacadeService` expands `DeckCardEntry`

**Decision**: Expand `DeckCardEntry` to include `name: string`, `supertype: string`, `isBasicEnergy: boolean`.

**Rationale**: The deck builder UI needs to display card names and types without looking up from a separate cache. The backend only needs `cardId` + `quantity` for API calls, but the frontend state needs the extra fields for rendering.

### D7: `removeCard()` decrements quantity

**Decision**: `removeCard(cardId)` decrements quantity by 1. Only removes the entry when quantity reaches 0.

**Rationale**: Standard deck builder UX. Users click "remove" to take one copy out, not all copies.

### D8: GameActionDispatcherService stays Observable-based

**Decision**: Keep the service returning `Observable<GameActionResponse>`.

**Rationale**: Angular best practice. Components consume via `async` pipe or `toSignal()`. Using `firstValueFrom` in the service would break reactive composition.

### D9: Include deprecated GameActionType values

**Decision**: Include `DRAW_CARD`, `CHOOSE_KNOCKOUT_REPLACEMENT`, `USE_ABILITY` in the frontend `GameActionType` union, but mark them with JSDoc `@deprecated` comments.

**Rationale**: Type completeness. The backend enum has them (deprecated). If the backend ever sends them in an event, the frontend type system won't break. The frontend should not send them.

### D10: Tailwind CSS v4 with Vite plugin

**Decision**: Install Tailwind CSS v4 using `@tailwindcss/postcss` (PostCSS plugin approach for Angular).

**Rationale**: Angular 20 uses Vite under the hood. Tailwind v4's PostCSS plugin integrates cleanly. No `tailwind.config.js` needed (v4 uses CSS-based config).

### D11: Shared components in Phase A (not Phase B)

**Decision**: Create all shared components, pipes, and directives in Phase A alongside models and services.

**Rationale**: Personas B/C/D need these immediately. Delivering them incrementally while Phase A is "in progress" creates coordination overhead. Better to deliver them as a complete batch.

## Risks / Trade-offs

- **[Risk] Backend DTOs may change** → Mitigated by verifying against actual Java source code, not documentation. Models are easy to adjust later.
- **[Risk] Tailwind install may conflict with Angular build** → Mitigated by using the standard `@tailwindcss/postcss` plugin approach documented for Angular.
- **[Risk] `CardRepositoryService` uses `firstValueFrom` internally** → Acceptable because it's a cache layer, not a UI service. The async boundary is intentional.
- **[Trade-off] Models mirror engine structure exactly** → More fields than strictly needed for V1 UI, but zero transformation code. Can simplify later.
- **[Trade-off] Shared components created in Phase A** → Larger initial deliverable, but unblocks all teammates immediately.
