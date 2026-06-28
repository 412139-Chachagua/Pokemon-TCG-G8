## Context

The backend has existing stubs for DeckService, DeckValidator, SeedDeckService, and DeckController. DeckMapper exists as an empty class. The engine's `DeckLoadPort` interface is defined in the engine persona 1 contracts but does not yet exist as a Java file. No `DeckLoadAdapter` exists. The deck validation logic must be implemented once and shared between REST endpoints (via DeckService) and the engine (via DeckLoadAdapter).

## Goals / Non-Goals

**Goals:**
- Implement DeckValidator with V1 rules: 60 cards, ≤4 copies per cardId, ≥1 Basic Pokémon
- Implement full DeckService CRUD (create, get, update, delete, list by player, validate)
- Create DeckMapper for all entity ↔ DTO ↔ domain conversions
- Implement DeckController with REST endpoints per contract
- Create DeckLoadAdapter implementing DeckLoadPort
- Complete SeedDeckService with Fire and Water seed decks (dev profile only)
- Write unit and integration tests

**Non-Goals:**
- No external catalog sync or Pokémon TCG API calls
- No changes to engine internals beyond DeckLoadPort implementation
- No card lookup adapter implementation
- No auth/JWT, ranking, chat, animations
- No new expansions or rules outside MVP
- No frontend changes
- Do not modify DeckLoadPort interface

## Decisions

1. **Validation Location**: `DeckValidator` lives in `services/decks/` and is injected into both `DeckService` (REST path) and `DeckLoadAdapter` (engine path). No duplication.

2. **Validation Rules V1**: Exactly 60 cards, max 4 copies per cardId, at least 1 Basic Pokémon. Structured errors returned as `DeckValidationResult` with a list of `DeckValidationError` (code, message, details).

3. **Mapper Pattern**: `DeckMapper` is a utility class (or Spring Component) with static methods. Conversions: `DeckEntity → DeckResponse`, `CreateDeckRequest → DeckEntity`, `UpdateDeckRequest → entity update`, `DeckEntity → Deck`, `DeckCardEntity → DeckCard`.

4. **Engine Isolation**: `DeckLoadAdapter` lives in `engine/ports/impl/` and only has access to repository interfaces and `DeckValidator`. It has no Spring Web or JPA dependencies beyond repository injection.

5. **Seed Decks**: `SeedDeckService` uses `@Profile("dev")` to only run in development. Preloads Fire and Water seed decks. Must not run in production.

6. **Controller Thinness**: `DeckController` delegates all logic to `DeckService`. Mapping is done via `DeckMapper`.

7. **Error Handling**: Controlled exceptions for missing decks (404). Validation failures return structured `DeckValidationResponse` with error details (400).

## Risks / Trade-offs

- [Risk] Validation rules may need expansion (ACE_SPEC limit, Basic Energy exemption) → Mitigation: V1 rules are explicitly scoped; future rules are additive and don't change the validator interface
- [Risk] DeckLoadPort may not yet exist as Java file → Mitigation: Create the port interface if missing (per engine persona 1 contracts spec), then implement against it
- [Risk] Seed decks referencing card IDs that don't exist in cache → Mitigation: Seed decks use well-known xy1 card IDs; validation ensures consistency
- [Risk] Mapper complexity grows with domain changes → Mitigation: Keep mapper focused on current entity/DTO shapes; expand only when domain changes
