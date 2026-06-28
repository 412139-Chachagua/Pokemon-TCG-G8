## 1. Core Service

- [x] 1.1 Create `RandomDeckService` class in `services/decks/` package
- [x] 1.2 Implement card selection algorithm: pick Basic Pokémon, evolutions, Trainers, Energies to reach 60 cards
- [x] 1.3 Integrate `DeckValidator` to validate generated deck
- [x] 1.4 Add retry logic (up to 10 attempts) when generated deck is invalid
- [x] 1.5 Handle edge case: insufficient compatible cards → throw meaningful exception

## 2. REST Endpoint

- [x] 2.1 Add `POST /api/decks/random` endpoint in `DeckController`
- [x] 2.2 Return `DeckResponse` with generated deck on success
- [x] 2.3 Return 422 with error message when valid deck cannot be generated

## 3. Tests

- [ ] 3.1 Write unit tests for `RandomDeckService` (valid generation, regeneration on invalid, failure scenario)
- [ ] 3.2 Write integration test for `POST /api/decks/random` endpoint
- [ ] 3.3 Verify all existing tests still pass with `mvn test`
