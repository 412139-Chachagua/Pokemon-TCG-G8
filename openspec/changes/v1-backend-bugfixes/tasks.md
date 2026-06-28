## 1. Fix PutBasicOnBenchHandler compile error

- [x] 1.1 Fix undefined `payload` variable reference (use `action.getPayload()` or pass correct Map)
- [x] 1.2 Initialize `specialConditions`, `attachedEnergies`, `evolvedThisTurn` on new PokemonInPlay
- [x] 1.3 Set `enteredTurnNumber = state.getTurnNumber()` on placed Pokemon
- [x] 1.4 Run `mvn compile` to verify no errors

## 2. Populate ActionResult with game state

- [x] 2.1 Add method in `MatchQueryService` to build `PublicGameState` from `GameState`
- [x] 2.2 Add method to build `PrivatePlayerState` for a given player
- [x] 2.3 Wire `GameEngine.applyAction()` to populate `ActionResult.publicState` and `privateState`
- [x] 2.4 Run `mvn test` to verify existing tests pass

## 3. Add match-level locking

- [x] 3.1 Add `ConcurrentHashMap<UUID, ReentrantLock>` in `MatchApplicationService`
- [x] 3.2 Acquire lock in `executeAction()` before load, release in finally block
- [x] 3.3 Return HTTP 409 if lock cannot be acquired within timeout
- [x] 3.4 Run `mvn test` to verify

## 4. Add validators for TAKE_PRIZE_CARD and CHOOSE_KNOCKOUT_REPLACEMENT

- [x] 4.1 Implement `validateTakePrizeCard()` checking `pendingPrizeOwnerPlayerId != null`
- [x] 4.2 Implement `validateChooseKnockoutReplacement()` checking opponent has no active but has bench
- [x] 4.3 Add guard in `validateDrawCard()` checking `hasDrawnForTurn`
- [x] 4.4 Run `mvn test` to verify

## 5. Fix DeclareAttackHandler KO logic

- [x] 5.1 Check if defender is active or bench before nullifying `opponent.setActivePokemon(null)`
- [x] 5.2 If defender is bench Pokemon, only remove from bench (don't touch active)
- [x] 5.3 Add error return when `AttackResolver.resolve()` returns `energyValid=false`
- [x] 5.4 Run `mvn test` to verify

## 6. Fix VictoryConditionChecker for opponent deck-out

- [x] 6.1 Modify `check()` to evaluate deck size of both players, not just `currentPlayerId`
- [x] 6.2 If opponent has empty deck, declare victory for current player
- [x] 6.3 Run `mvn test` to verify

## 7. Add energy type validation for retreat

- [x] 7.1 In `RuleValidator.validateRetreat()`, validate discarded energy types match `retreatCost`
- [x] 7.2 Run `mvn test` to verify

## 8. Handle USE_ABILITY and remove dead code

- [x] 8.1 Remove `USE_ABILITY` from `GameActionType` enum (or mark as deprecated with no handler)
- [x] 8.2 Remove `GamePhase.java`, `StatusEffectManager.java`, `AttackStep.java`, `GameMetadata.java`, `VictoryResult.java`
- [x] 8.3 Remove all Payload DTOs: `AttachEnergyPayload`, `DeclareAttackPayload`, `EvolvePokemonPayload`, `GameActionPayload`, `PlayTrainerPayload`, `PutBasicOnBenchPayload`, `RetreatPayload`
- [x] 8.4 Run `mvn compile` to verify no broken references

## 9. Make card sync async and fix cache

- [x] 9.1 Replace synchronous `syncAll()` call in `ApplicationReadyEvent` with `CompletableFuture.runAsync()`
- [x] 9.2 Add `cacheManager.getCache("cards").clear()` at end of `syncAll()`
- [x] 9.3 Run `mvn test` to verify

## 10. Fix SeedDeckService card IDs

- [x] 10.1 Replace fake IDs (`"energy-fire-basic"`, etc.) with real XY1 card IDs from catalog
- [x] 10.2 Verify `CardJpaRepository.existsById()` passes for all seed deck cards
- [x] 10.3 Run `mvn test` to verify

## 11. Replace DeckMapper reflection with setters

- [x] 11.1 Replace `getDeclaredField` + `setAccessible` with public setter calls in `DeckMapper.toDomain()`
- [x] 11.2 Same for `DeckMapper.toDomainCard()`
- [x] 11.3 Run `mvn test` to verify

## 12. Fix CardMapper mapping gaps

- [ ] 12.1 Map `abilities` from `PokemonTcgApiCardDto` to `CardEntity`
- [ ] 12.2 Map `evolvesTo` field
- [ ] 12.3 Map `energyCardType`, `providesEnergyTypes`, `trainerSubtype`, `isAceSpec`
- [ ] 12.4 Run `mvn compile` to verify

## 13. Align BE response formats with FE contracts

- [ ] 13.1 Create `CardSearchResponse` DTO with `items`, `page`, `size`, `totalItems`
- [ ] 13.2 Update `CardController.searchCards()` to return `CardSearchResponse` instead of `Page<CardSummaryResponse>`
- [ ] 13.3 Add `POST /api/matches` support for `{ playerName, deckId }` payload (backward compatible)
- [ ] 13.4 Add `POST /api/decks/validate` endpoint accepting `{ cards }` array
- [ ] 13.5 Run `mvn compile` and `mvn test` to verify

## 14. Implement WebSocket /queue private publishing

- [ ] 14.1 In `MatchWebSocketPublisher`, add method to publish to `/queue/matches/{matchId}/{playerId}`
- [ ] 14.2 Filter private data from `/topic` events (hand, prizes)
- [ ] 14.3 Publish `PrivatePlayerState` to `/queue` for each player after state change
- [ ] 14.4 Run `mvn compile` to verify

## 15. Implement basic PlayTrainerHandler effects

- [ ] 15.1 Move trainer card from hand to discard pile
- [ ] 15.2 Set `hasPlayedSupporter = true` if card is SUPPORTER subtype
- [ ] 15.3 Set `hasPlayedStadium = true` if card is STADIUM subtype
- [ ] 15.4 Run `mvn test` to verify

## 16. Final verification

- [ ] 16.1 Run `mvn compile` — zero errors
- [ ] 16.2 Run `mvn test` — all 49+ tests pass (no regressions)
- [ ] 16.3 Verify backwards compatibility: existing tests for unchanged handlers still pass
