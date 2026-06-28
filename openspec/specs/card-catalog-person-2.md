# AI Proposal Spec: card-catalog-person-2

## Spec name

card-catalog-person-2

## Purpose

Define the work for **Persona B** within the backend card catalog / deck module, covering:

- deck CRUD
- deck validation
- deck loading adapter for the engine
- optional seed decks for development/testing
- mapping between deck entities, DTOs, and domain models

This spec is intentionally limited to the **deck module** and must not touch the external card catalog API synchronization, the engine internals, or any future features outside the MVP.

---

## Mandatory context files

OpenSpec MUST read and obey:

- `/openspec/config.yaml`
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

---

## Architecture constraints

- Backend is the source of truth for deck validation and deck loading.
- Frontend only consumes server state and does not enforce deck rules.
- `DeckLoadAdapter` must remain isolated from Spring/JPA details except for repository access through the adapter implementation.
- `DeckValidator` must be reusable from both REST services and the engine adapter.
- No postponed features should be implemented: auth, JWT, ranking, chat, animations, multiple expansions, Mega Evolution.
- Do not modify engine interfaces; only implement against the existing `DeckLoadPort`.
- Do not introduce catalog synchronization, card lookup, or external API integration.

---

## Package root

```txt
ar.edu.utn.frc.tup.piii
```

## Scope — Classes to Implement or Verify

All live under `BE/src/main/java/ar/edu/utn/frc/tup/piii/`.

### services/decks/
| Class           | Current State   | Action                        |
|-----------------|-----------------|-------------------------------|
| DeckService     | Existing stub   | Complete                      |
| DeckValidator   | Existing stub   | Complete                      |
| SeedDeckService | Existing stub   | Complete only for dev/testing |

### controllers/decks/
| Class          | Current State   | Action   |
|----------------|-----------------|----------|
| DeckController | Existing stub   | Complete |

### mappers/
| Class      | Current State   | Action |
|------------|-----------------|--------|
| DeckMapper | Does not exist  | Create |

### engine/ports/impl/
| Class           | Current State   | Action |
|-----------------|-----------------|--------|
| DeckLoadAdapter | Does not exist  | Create |

---

## Available Domain / Persistence / DTO Context

- **DTOs already exist**
    - DeckResponse
    - DeckCardResponse
    - CreateDeckRequest
    - UpdateDeckRequest
    - DeckValidationResponse

- **Domain models already exist**
    - Deck
    - DeckCard
    - DeckValidationResult
    - DeckValidationError

- **Entities already exist**
    - DeckEntity
    - DeckCardEntity

- **Repositories already exist**
    - DeckJpaRepository
    - DeckCardJpaRepository

- **Engine interface already exists**
    - `engine/ports/DeckLoadPort`
    - Persona B must implement `DeckLoadAdapter`, but must not create or modify `DeckLoadPort`.

---

## Functional Requirements

### Requirement 1 — Shared Deck Validation
- Validation logic must be reusable by REST services and the engine adapter.
- Must live in `services/decks/DeckValidator`.

**Validation rules V1**
- Exactly 60 cards.
- No card repeated more than 4 times by cardId.
- At least one Basic Pokémon.

**Additional expectations**
- Return structured errors, not just boolean.
- Deterministic and reusable.
- Invalid persisted decks must be rejected before reaching the engine.

**Scenarios**
- Valid deck → result valid.
- <60 cards → invalid.
- >4 copies of same card → duplicate violation.
- No Basic Pokémon → missing-basic violation.

---

### Requirement 2 — DeckService CRUD
- Expose logic for create, retrieve, update, delete, list by player, and validate.

**Behaviors**
- `createDeck(CreateDeckRequest)` → validate, persist valid, reject invalid.
- `getDeck(UUID deckId)` → return deck or fail clearly.
- `updateDeck(UUID deckId, UpdateDeckRequest)` → update, revalidate, reject invalid.
- `deleteDeck(UUID deckId)` → remove deck + cards.
- `listDecksByPlayer(UUID playerId)` → return only player’s decks.
- `validateDeck(UUID deckId)` → return `DeckValidationResponse`.

**Scenarios**
- Valid create → persisted.
- Existing deckId → returned.
- Invalid update → rejected.
- PlayerId → only that player’s decks.

---

### Requirement 3 — DeckController Endpoints
Expose REST endpoints:

- `POST /api/decks`
- `GET /api/decks/{id}`
- `PUT /api/decks/{id}`
- `DELETE /api/decks/{id}`
- `GET /api/decks?playerId={id}`
- `POST /api/decks/{id}/validate`

**Behavior**
- Proper HTTP codes.
- Thin controller, delegate to service.
- Use `DeckMapper` for mapping.

**Scenarios**
- Valid POST → deck created.
- GET existing → return details.
- POST validate → return validation results.

---

### Requirement 4 — DeckLoadAdapter
Implements `DeckLoadPort.loadDeck(UUID deckId)`.

**Behavior**
- Read deck from repository.
- Map entity → domain.
- Validate before returning.
- Fail controlled if deck missing or invalid.

**Critical rule**
- Must call `DeckValidator.validate()` before returning.

**Scenarios**
- Existing deck → return domain object.
- Invalid persisted deck → controlled validation error.
- Missing deck → controlled load error.

---

### Requirement 5 — SeedDeckService (Optional)
- Only for dev/testing.
- Must not run in production.

**Scenarios**
- Dev profile active → preload decks.
- Non-dev profile → no seeding.

---

## Mapping Requirements
**DeckMapper MUST support:**
- DeckEntity → DeckResponse
- CreateDeckRequest → DeckEntity
- UpdateDeckRequest → entity update flow
- DeckEntity → Deck domain model
- DeckCardEntity → DeckCard domain model

Centralize conversion logic to keep controller/service thin.

---

## Validation & Error Handling
- Failures → structured domain results.
- No duplication of validation logic.
- Missing decks → controlled exceptions.
- Invalid decks → deterministic output.
- Engine load → never bypass validation.

---

## Non-Goals
Do not implement:
- External catalog sync.
- Pokémon TCG API client.
- Card search/lookup adapter.
- Engine logic beyond deck loading.
- GameEngine, match persistence.
- Auth/JWT, ranking, chat, animations.
- New expansions/rules outside MVP.
- Changes to `DeckLoadPort`.

---

## RFs Covered
- RF-5: save/query/delete custom decks.
- RF-6: create game with validated deck.
- RF-9: implement DeckLoadPort.
- RF-11: keep contracts decoupled from engine internals.

---

## Verification Requirements
- `mvn compile` inside BE/ → no errors.
- `mvn test` inside BE/ → no failures.
- Confirm `DeckLoadAdapter` implements `DeckLoadPort`.
- Confirm validation shared between REST and engine.

---

## Implementation Targets
- `services/decks/DeckService`
- `services/decks/DeckValidator`
- `services/decks/SeedDeckService`
- `controllers/decks/DeckController`
- `mappers/DeckMapper`
- `engine/ports/impl/DeckLoadAdapter`

---

## Notes for OpenSpec
- Persona B is responsible for deck management only.
- DeckValidator is shared, not duplicated



