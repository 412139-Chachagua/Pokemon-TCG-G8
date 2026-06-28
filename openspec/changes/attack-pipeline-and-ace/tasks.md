## 1. Ace Spec Validation (R5)

- [x] 1.1 Add ace spec count loop in `DeckValidator.validate()` after size check, before 4-copies check
- [x] 1.2 Verify `TrainerCardDefinition.isAceSpec()` is used correctly with `instanceof` check
- [x] 1.3 Sum `DeckCard.quantity` for ace spec cards and add `ACE_SPEC_LIMIT_EXCEEDED` if > 1

## 2. Damage Modifiers Pipeline (R1)

- [x] 2.1 Add `damageModifiers` reset to `TurnManager.resetTurnFlags()` (set to `null`)
- [x] 2.2 Modify `AttackResolver.calculateDamage()` to accept `Map<String, Object> damageModifiers` parameter
- [x] 2.3 Apply attacker modifiers to `baseDamage` before weakness/resistance calculation
- [x] 2.4 Apply defender modifiers to `finalDamage` after weakness/resistance calculation (with `Math.max(..., 0)`)
- [x] 2.5 Wire `TurnFlags.damageModifiers` from `DeclareAttackHandler` through `AttackResolver.resolve()` into `calculateDamage()`

## 3. Attack Effect System — Core Types (R2)

- [x] 3.1 Create `AttackEffectType` enum with all 8 types: `APPLY_SPECIAL_CONDITION`, `DISCARD_ENERGY`, `DAMAGE_BENCH`, `HEAL_USER`, `DRAW_CARDS`, `SWITCH_AFTER_DAMAGE`, `COIN_FLIP_BEFORE_DAMAGE`, `COIN_FLIP_AFTER_DAMAGE`
- [x] 3.2 Create `AttackEffect` domain class with `AttackEffectType type` and `Map<String, Object> params`
- [x] 3.3 Create `AttackEffectResolver` strategy interface with `resolve(EngineContext, PokemonInPlay, PokemonInPlay, AttackEffect, Map)` and `getType()`
- [x] 3.4 Create `AttackEffectRegistry` facade with `registerResolver()` and `resolve()` methods, analogous to `TrainerEffectRegistry`

## 4. Attack Effect System — Domain Integration (R2)

- [x] 4.1 Add `List<AttackEffect> effects` field to `PokemonCardDefinition.AttackDefinition` with getter/setter
- [x] 4.2 Modify `CardLookupAdapter.toAttack()` to parse `effectCode` + `effectText` into `List<AttackEffect>`
- [x] 4.3 Implement effect code parsing: split by `:`, map first segment to `AttackEffectType`, remaining segments to `params`
- [x] 4.4 Add 4 new `GameEventType` enum values: `STATUS_APPLIED`, `ENERGY_DISCARDED`, `BENCH_DAMAGE`, `ATTACK_EFFECT_RESOLVED`

## 5. Attack Effect System — Concrete Resolvers (R3)

- [x] 5.1 Create `ApplySpecialConditionResolver` — invokes `AttackResolver.applyCondition()`, publishes `STATUS_APPLIED`
- [x] 5.2 Create `DiscardEnergyResolver` — discards N energies from defender using `payload.targetEnergies`, publishes `ENERGY_DISCARDED`
- [x] 5.3 Create `DamageBenchResolver` — applies damage counters to benched Pokemon using `payload.benchTargets`, publishes `BENCH_DAMAGE`
- [x] 5.4 Create `HealUserResolver` — removes damage counters from attacker, publishes `POKEMON_HEALED`
- [x] 5.5 Create `DrawCardsResolver` — draws N cards from attacker's deck to hand (reuse `DrawCardHandler` logic)
- [x] 5.6 Create `CoinFlipEffectResolver` — coin flip with conditional sub-effect execution

## 6. Attack Effect System — Handler Integration (R2)

- [x] 6.1 Register all 6 resolvers in `AttackEffectRegistry`
- [x] 6.2 Modify `DeclareAttackHandler.handle()` to execute attack effects after damage/KO processing, before `turnManager.advancePhase()`
- [x] 6.3 Ensure effects only execute on successful attacks (energy valid, not confused self-hit)
- [x] 6.4 Ensure backward compatibility: attacks without effects produce no additional behavior

## 7. Verify Build

- [x] 7.1 Run `mvn compile` to verify all changes compile without errors
- [x] 7.2 Run existing tests to verify no regressions
