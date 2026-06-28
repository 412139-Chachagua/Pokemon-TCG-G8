## Why

Persona A is the foundation blocker for the entire frontend team. Models are desaligned with the real backend DTOs, API services have incorrect endpoints/params, no shared components exist, and no CSS framework is installed. Until Persona A delivers, Personas B, C, and D cannot start their features. This change aligns all models with the actual Java backend, completes the API service layer, creates shared UI infrastructure (components, pipes, directives), and installs Tailwind CSS — unblocking the full team.

## What Changes

- **BREAKING** `game-action.models.ts`: Remove `USE_ABility` from `GameActionType`, add `TAKE_PRIZE_CARD`, rename `GameEventModel` → `GameEventDto` (simplify to `type`, `message`, `payload?`), type `GameActionResponse.publicState/privateState` to proper interfaces
- **BREAKING** `game-state.models.ts`: Fix `PrizeSlotModel.card` → `cardId: string | null`, create `MatchStateResponse` type
- **BREAKING** `card.models.ts`: Add `CardSummaryResponse`, `CardDetailResponse`, `PaginatedCardsResponse`, `TrainerSubtype` type; add `convertedEnergyCost` and `baseDamage` to `AttackModel`; add `isMega` to `CardModel`
- **BREAKING** `deck.models.ts`: Rename `DeckModel` → `DeckResponse`, add `CreateDeckRequest`, `UpdateDeckRequest`, `ValidateDeckRequest`
- **BREAKING** `card-api.service.ts`: Type `getCardById()` return to `CardDetailResponse`
- **BREAKING** `deck-api.service.ts`: Complete CRUD — add `listByPlayer()`, `get()`, `update()`, `delete()`, `validate()` (POST sin body), `validateCards()`; remove `getSeedDecks()`
- **BREAKING** `match-api.service.ts`: Fix `MatchResponse` fields, type `MatchStateResponse`, type `sendAction()` return to `GameActionResponse`
- `ui-state.models.ts`: Create with `SelectionMode`, `SelectionState`
- `card-repository.service.ts`: Create — cache of `CardDetailResponse` by cardId with `resolve()`, `preload()`, `getFromCache()`
- `notification.service.ts`: Create — signal-based notification system with `show()`, `dismiss()`
- `deck-builder-facade.service.ts`: Expand `DeckCardEntry` to include `name`, `supertype`, `isBasicEnergy`; fix `removeCard()` to decrement quantity
- `match-facade.service.ts`: Adapt to corrected `MatchResponse`/`MatchStateResponse`
- `game-action-dispatcher.service.ts`: Fix payload fields per action type (use `handIndex` not `energyCardInstanceId` for ATTACH_ENERGY, etc.)
- Install Tailwind CSS v4
- Create shared components: `LoadingSpinnerComponent`, `ModalComponent`, `ButtonComponent`, `NotificationComponent`, `CardViewComponent`, `PokemonCardComponent`
- Create shared pipes: `CardImagePipe`, `EnergyIconPipe`, `ConditionIconPipe`
- Create shared directive: `ClickOutsideDirective`
- Create SVG assets for energy types (10) and conditions (5)
- Add `:id/edit` route to `deck.routes.ts`
- Add notification placeholder to `app.html`

## Capabilities

### New Capabilities

- `frontend-models-alignment`: TypeScript models aligned with backend Java DTOs (cards, decks, game-state, game-action, ui-state)
- `frontend-api-services`: Complete API service layer with correct endpoints, params, and types (card, deck, match)
- `frontend-core-services`: Core services — CardRepositoryService (cache) and NotificationService (signal-based snackbar)
- `frontend-shared-components`: Reusable UI components (LoadingSpinner, Modal, Button, Notification, CardView, PokemonCard)
- `frontend-shared-pipes-directives`: Angular pipes (CardImage, EnergyIcon, ConditionIcon) and ClickOutsideDirective
- `frontend-tailwind-setup`: Tailwind CSS v4 installation and configuration
- `frontend-svg-assets`: SVG icon assets for energy types and special conditions

### Modified Capabilities

- `card-catalog-management`: DeckBuilderFacadeService expanded with name/supertype in DeckCardEntry, removeCard decrements quantity

## Impact

- **Files modified**: `shared/models/*.ts` (5 files), `core/api/*.service.ts` (3 files), `features/decks/services/deck-builder-facade.service.ts`, `features/match/services/match-facade.service.ts`, `features/match/services/game-action-dispatcher.service.ts`, `features/decks/routes.ts`, `app.html`
- **Files created**: `shared/models/ui-state.models.ts`, `core/services/card-repository.service.ts`, `core/services/notification.service.ts`, `shared/components/**` (6 components), `shared/pipes/**` (3 pipes), `shared/directives/click-outside.directive.ts`, `src/assets/icons/` (15 SVGs)
- **Dependencies added**: `tailwindcss`, `@tailwindcss/postcss` (dev)
- **Build impact**: `ng build` must compile cleanly after changes
- **Breaking changes**: All model renames/field changes require updates to all consuming files (facades, dispatcher, pages)
- **Contracts context**: `/docs/contracts_ai/` — backend DTOs verified against actual Java source code in `BE/src/main/java/`
