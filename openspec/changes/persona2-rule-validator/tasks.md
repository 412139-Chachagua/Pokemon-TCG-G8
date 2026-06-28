## 1. RuleValidator implementation

- [ ] 1.1 Add dispatch logic in `validate()` to route by `GameActionType` to specific validation methods
- [ ] 1.2 Implement `validateDrawCard`, `validateTakePrizeCard`, `validateChooseKnockoutReplacement` as no-op stubs returning `true` (out of V1 scope)
- [ ] 1.3 Implement `validateEndTurn`: always valid if `playerId == currentPlayerId`
- [ ] 1.4 Implement `validateAttachEnergy`: phase `MAIN`, `playerId == currentPlayerId`, `!hasAttachedEnergy`, card in hand is `EnergyCardDefinition`, target is own Pokémon in play
- [ ] 1.5 Implement `validatePutBasicOnBench`: card in hand is `PokemonCardDefinition` with `stage == "BASIC"`, bench size < 5
- [ ] 1.6 Implement `validateEvolve`: phase `MAIN`, card in hand is `PokemonCardDefinition` with `stage != "BASIC"`, target in play, `enteredTurnNumber != turnNumber`, `!evolvedThisTurn`, `evolvesFrom` matches target name (via `CardLookupPort`)
- [ ] 1.7 Implement `validatePlayTrainer`: phase `MAIN`, card in hand is `TrainerCardDefinition`, if `trainerSubtype == TrainerSubtype.SUPPORTER` then `!hasPlayedSupporter`
- [ ] 1.8 Implement `validateRetreat`: phase `MAIN`, `!hasRetreated`, Active not `ASLEEP`/`PARALYZED`, bench not empty, enough energy attached for `retreatCost.size()`
- [ ] 1.9 Implement `validateAttack`: phase `MAIN`, `playerId == currentPlayerId`, not first player's turn 1, Active not `ASLEEP`/`PARALYZED`, `attackIndex` exists in Active's attack list (via `CardLookupPort`)

## 2. GameEngine integration

- [ ] 2.1 Inject `RuleValidator` into `GameEngine` constructor
- [ ] 2.2 Add `RuleValidator.validate(ctx, action)` call in `applyAction()` before handler dispatch — if invalid, return `ActionResult` with `success=false` and `GameError`

## 3. Spring configuration

- [ ] 3.1 Add `RuleValidator` as `@Bean` in `GameEngineConfig`
- [ ] 3.2 Wire `RuleValidator` into `GameEngine` bean constructor call

## 4. Verification

- [ ] 4.1 Run `mvn compile` — must succeed
- [ ] 4.2 Run `mvn test` — must pass, preserve `ApplicationTests.contextLoads`
- [ ] 4.3 Verify no imports from Spring, JPA, repositories, services, or controllers in `engine/rules/RuleValidator`
