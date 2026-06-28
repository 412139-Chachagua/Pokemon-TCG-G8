## 1. DeckValidator Implementation

- [x] 1.1 Implement `DeckValidator.validate(List<DeckCard> cards)` returning `DeckValidationResult`
- [x] 1.2 Add rule: exactly 60 cards total → error code `DECK_SIZE_INVALID`
- [x] 1.3 Add rule: max 4 copies per cardId → error code `MORE_THAN_4_COPIES` (contrato 03-enums)
- [x] 1.4 Add rule: at least 1 Basic Pokémon → error code `MISSING_BASIC_POKEMON`
- [x] 1.5 Ensure deterministic, reusable, no side effects

## 2. DeckMapper Creation

- [x] 2.1 Implement `DeckEntity → DeckResponse` mapping (include cards, validation, totalCards)
- [x] 2.2 Implement `CreateDeckRequest → DeckEntity` mapping
- [x] 2.3 Implement `UpdateDeckRequest → entity update` flow
- [x] 2.4 Implement `DeckEntity → Deck` domain mapping
- [x] 2.5 Implement `DeckCardEntity → DeckCard` domain mapping

## 3. DeckService Completion

- [x] 3.1 Implement `createDeck(CreateDeckRequest)` — validate, persist, return
- [x] 3.2 Implement `getDeck(UUID deckId)` — find or throw controlled exception
- [x] 3.3 Implement `updateDeck(UUID deckId, UpdateDeckRequest)` — update, revalidate, reject if invalid
- [x] 3.4 Implement `deleteDeck(UUID deckId)` — remove deck and cards
- [x] 3.5 Implement `listDecksByPlayer(UUID playerId)` — return only player's decks
- [x] 3.6 Implement `validateDeck(UUID deckId)` — return `DeckValidationResponse`

## 4. DeckController Completion

- [x] 4.1 Implement `POST /api/decks` — create deck
- [x] 4.2 Implement `GET /api/decks/{id}` — get deck by ID
- [x] 4.3 Implement `PUT /api/decks/{id}` — update deck
- [x] 4.4 Implement `DELETE /api/decks/{id}` — delete deck (204 No Content)
- [x] 4.5 Implement `GET /api/decks?playerId={id}` — list by player
- [x] 4.6 Implement `POST /api/decks/{id}/validate` — validate deck
- [x] 4.7 Ensure proper HTTP codes (200, 201, 204, 400, 404)
- [x] 4.8 Keep controller thin — delegate to DeckService, use DeckMapper

## 5. DeckLoadAdapter Creation

- [x] 5.1 Create `DeckLoadAdapter` in `engine/ports/impl/` implementing `DeckLoadPort`
- [x] 5.2 Implement `loadDeck(UUID deckId)` — read from repo, map to domain, validate
- [x] 5.3 Ensure validation is called before returning domain object
- [x] 5.4 Handle missing deck with controlled error
- [x] 5.5 Handle invalid persisted deck with controlled validation error

## 6. DeckLoadPort (if missing)

- [x] 6.1 Create `DeckLoadPort` interface in `engine/ports/` if not already present
- [x] 6.2 Define `Deck loadDeck(UUID deckId)` method signature

## 7. SeedDeckService Completion

- [x] 7.1 Annotate with `@Profile("dev")` to prevent production execution
- [x] 7.2 Implement Fire seed deck preloading with valid xy1 cards
- [x] 7.3 Implement Water seed deck preloading with valid xy1 cards
- [x] 7.4 Ensure seed decks are pre-validated and stored in DB
- [x] 7.5 Skip seeding if decks already exist (idempotent)

## 8. Tests

- [x] 8.1 Write `DeckValidatorTest` — valid deck, <60 cards, >4 copies, no Basic Pokémon
- [x] 8.2 Write `DeckServiceTest` — CRUD scenarios, validation integration
- [x] 8.3 Write `DeckControllerTest` — endpoint integration tests
- [x] 8.4 Write `DeckLoadAdapterTest` — existing deck, missing deck, invalid persisted deck
- [x] 8.5 Write `DeckMapperTest` — all mapping directions
- [x] 8.6 Write `SeedDeckServiceTest` — dev profile loads seeds, non-dev does not

## 9. Verification

- [x] 9.1 Run `mvn compile` inside BE/ → no errors
- [x] 9.2 Run `mvn test` inside BE/ → no failures (28/28)
- [x] 9.3 Confirm `DeckLoadAdapter` implements `DeckLoadPort`
- [x] 9.4 Confirm `DeckValidator` is used by both `DeckService` and `DeckLoadAdapter`
