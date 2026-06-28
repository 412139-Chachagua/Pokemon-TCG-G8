## 1. SetupManager skeleton and constructor

- [x] 1.1 Delete `MulliganService.java` from `engine/setup/` (contract 07: mulligan is private within SetupManager, no separate class)
- [x] 1.2 Rewrite `SetupManager.java` with fields and constructor accepting `DeckLoadPort`, `CardLookupPort`, `RandomizerPort` — no Spring annotations, pure Java 21

## 2. Deck loading and CardInstance expansion

- [x] 2.1 Load both decks via `DeckLoadPort.loadDeck(deckId)` at the start of `setup()`
- [x] 2.2 Implement private method `List<CardInstance> expandDeck(Deck deck)` that converts each `DeckCard` into `quantity` `CardInstance` objects with `UUID.randomUUID()` as `instanceId` and `cardDefinitionId = DeckCard.getCardId()`

## 3. Shuffle

- [x] 3.1 Use existing `RandomizerPort.shuffle(List<T> items)` for all deck shuffling
- [x] 3.2 Ensure all setup shuffling uses the existing `RandomizerPort.shuffle()` abstraction exclusively — no direct `Collections.shuffle`, `Random`, or `ThreadLocalRandom`
- [x] 3.3 Implement private method `List<CardInstance> shuffleDeck(List<CardInstance> deck)` in `SetupManager` that creates a mutable copy of the list and calls `RandomizerPort.shuffle()` on it

## 4. Initial hand deal

- [x] 4.1 Implement initial hand deal: take first 7 cards from shuffled deck into `hand`, remaining cards stay as `deck`

## 5. Mulligan resolution

- [x] 5.1 Implement private method `boolean hasBasicPokemon(List<CardInstance> hand)` that checks each `CardInstance` via `CardLookupPort.getCardById()` and verifies `result instanceof PokemonCardDefinition && "BASIC".equals(((PokemonCardDefinition) result).getStage())`
- [x] 5.2 Implement private mulligan loop per player: if no Basic → return hand to deck → call `shuffleDeck()` → redeal 7 → increment `mulliganCount` in `PlayerState` → repeat until Basic found
- [x] 5.3 After both players' mulligan loops complete, draw `opponentMulliganCount` extra cards for each player from the top of their remaining deck into hand

## 6. Active Pokémon selection

- [x] 6.1 Implement auto-selection of first Basic Pokémon in hand: scan hand, call `CardLookupPort.getCardById()`, find first `PokemonCardDefinition` with `stage=="BASIC"`, remove that `CardInstance` from hand
- [x] 6.2 Create `PokemonInPlay` from that `CardInstance`: set `instanceId`, `cardDefinitionId`, `ownerPlayerId`, `enteredTurnNumber = 0`, `evolvedThisTurn = false`, `damageCounters = 0`, `specialConditions = new ArrayList<>()`, `attachedEnergies = new ArrayList<>()`
- [x] 6.3 Set the resulting `PokemonInPlay` as `playerState.setActivePokemon()`, bench stays as empty list

## 7. Prize assignment

- [x] 7.1 After mulligan and Active selection, take first 6 cards from each player's remaining deck and assign to `playerState.setPrizes()`
- [x] 7.2 Verify both players end with exactly 6 prizes

## 8. Coin flip and GameState construction

- [x] 8.1 Determine first player via `RandomizerPort.nextInt(2)`: 0 → playerOneId (PLAYER_ONE side), 1 → playerTwoId (PLAYER_TWO side)
- [x] 8.2 Build `GameState` with `matchId`, `status=ACTIVE`, `phase=DRAW`, `turnNumber=1`, both `currentPlayerId` and `firstPlayerId` set to the coin flip winner, `TurnFlags` all false, both `PlayerState` fully populated, `createdAt` and `updatedAt` set to `Instant.now()`
- [x] 8.3 Return the completed `GameState` from `setup()`

## 9. Verification

- [x] 9.1 Run `mvn compile` in `BE/` and verify no compilation errors
- [x] 9.2 Run `mvn test` and verify `ApplicationTests.contextLoads` passes
- [x] 9.3 Verify grep for `org.springframework` and `jakarta.persistence` in `engine/setup/SetupManager.java` returns empty
- [x] 9.4 Verify `engine/setup/MulliganService.java` no longer exists
- [x] 9.5 Verify no model classes outside `engine/setup/` were modified
