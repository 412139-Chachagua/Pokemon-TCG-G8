## Why

The Pokémon TCG backend requires deck management functionality for Player B — the deck module. This includes CRUD operations for custom decks, reusable validation logic shared between REST and the engine, a `DeckLoadAdapter` implementing the engine's `DeckLoadPort`, and optional seed decks for development/testing. The existing spec at `/openspec/specs/card-catalog-person-2.md` defines Persona B's scope.

## What Changes

- Complete `DeckService` with full CRUD + validation orchestration
- Complete `DeckValidator` with V1 validation rules (60 cards, ≤4 copies, ≥1 Basic Pokémon)
- Create `DeckMapper` for entity ↔ DTO ↔ domain conversions
- Complete `DeckController` with REST endpoints for deck operations
- Create `DeckLoadAdapter` implementing `DeckLoadPort` for engine integration
- Complete `SeedDeckService` for dev/test preloaded decks (Fire and Water)
- Ensure validation is shared between REST services and engine adapter

## Capabilities

### New Capabilities

- `deck-crud`: Full CRUD for custom decks via REST and service layer
- `deck-validation`: Reusable deck validation with structured error results
- `deck-load-adapter`: Engine adapter implementing `DeckLoadPort` with validation gate
- `seed-decks`: Pre-validated seed decks for development and testing profiles
- `deck-mapping`: Centralized mapping between entities, DTOs, and domain models

### Modified Capabilities

- Existing stubs in `services/decks/`, `controllers/decks/`, and `mappers/decks/` will be completed

## Impact

- Affected packages under `ar.edu.utn.frc.tup.piii`: services/decks, controllers/decks, mappers, engine/ports/impl
- New test coverage: validator unit tests, service CRUD tests, controller integration tests, adapter tests
- No impact on: external card catalog sync, engine internals beyond DeckLoadPort, frontend, auth/JWT
- Deck validation becomes a shared dependency — DeckValidator must be reusable by both REST and engine paths

## Mandatory Context Files

- `/docs/contracts_ai/00-contract-index.md`
- `/docs/contracts_ai/01-project-scope-contract.md`
- `/docs/contracts_ai/02-project-structure-contract.md`
- `/docs/contracts_ai/04-card-model-contract.md`
- `/docs/contracts_ai/05-deck-contract.md`
- `/docs/contracts_ai/06-game-state-contract.md`
- `/docs/contracts_ai/07-setup-flow-contract.md`
- `/docs/contracts_ai/08-game-action-contract.md`
- `/docs/contracts_ai/09-rule-validation-contract.md`
- `/docs/contracts_ai/10-attack-pipeline-contract.md`
- `/docs/contracts_ai/11-status-effects-contract.md`
- `/docs/contracts_ai/13-rest-api-contract.md`
- `/docs/contracts_ai/14-websocket-contract.md`
- `/docs/contracts_ai/15-frontend-state-contract.md`
- `/docs/contracts_ai/16-test-scenarios-contract.md`
- `/openspec/specs/engine-persona1-contracts-and-gameengine.md`
