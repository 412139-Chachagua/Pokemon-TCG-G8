## ADDED Requirements

### Requirement: AttackEffectType enum

The system SHALL define an `AttackEffectType` enum with the following values:

| Type | Behavior |
|------|----------|
| `APPLY_SPECIAL_CONDITION` | Applies a special condition to the defender after damage |
| `DISCARD_ENERGY` | Forces the defender to discard N energies (attacker selects which) |
| `DAMAGE_BENCH` | Deals N damage to 1+ benched Pokemon on the rival's side |
| `HEAL_USER` | Removes N damage counters from the attacker |
| `DRAW_CARDS` | Attacker draws N cards from their deck |
| `SWITCH_AFTER_DAMAGE` | Switches the attacker with a benched Pokemon after dealing damage |
| `COIN_FLIP_BEFORE_DAMAGE` | Effect conditioned on a coin flip resolved before damage |
| `COIN_FLIP_AFTER_DAMAGE` | Effect conditioned on a coin flip resolved after damage |

#### Scenario: Enum contains all expected values

- **WHEN** `AttackEffectType.values()` is called
- **THEN** it SHALL contain `APPLY_SPECIAL_CONDITION`, `DISCARD_ENERGY`, `DAMAGE_BENCH`, `HEAL_USER`, `DRAW_CARDS`, `SWITCH_AFTER_DAMAGE`, `COIN_FLIP_BEFORE_DAMAGE`, and `COIN_FLIP_AFTER_DAMAGE`

### Requirement: AttackEffect domain class

The system SHALL define an `AttackEffect` class with:

- `AttackEffectType type`
- `Map<String, Object> params` for effect-specific parameters (e.g., `{"condition": "PARALYZED"}`, `{"count": 2}`)

#### Scenario: Effect carries type and params

- **WHEN** an `AttackEffect` is created with `type = APPLY_SPECIAL_CONDITION` and `params = {"condition": "PARALYZED"}`
- **THEN** `getType()` returns `APPLY_SPECIAL_CONDITION` and `getParams()` returns `{"condition": "PARALYZED"}`

### Requirement: AttackDefinition includes effects list

`PokemonCardDefinition.AttackDefinition` SHALL have a `List<AttackEffect> effects` field.

- Attacks with no effects SHALL have an empty list (backward compatible)

#### Scenario: Attack with no effects

- **WHEN** a `PokemonCardDefinition` is loaded from the database for an attack with `effectCode = null`
- **THEN** the attack's `effects` list SHALL be empty

### Requirement: CardLookupAdapter parses effectCode to AttackEffect

`CardLookupAdapter.toAttack()` SHALL parse `CardAttackEntity.effectCode` and `effectText` into a `List<AttackEffect>`.

- Format: `"TYPE:param1:param2"` where the first segment maps to `AttackEffectType` and subsequent segments populate `params`
- Effect text SHALL be stored as `params["text"]`

#### Scenario: Parsing APPLY_SPECIAL_CONDITION

- **WHEN** `effectCode = "APPLY_SPECIAL_CONDITION:PARALYZED"` and `effectText = "Paralyze the defending Pokemon."`
- **THEN** the parsed effect has `type = APPLY_SPECIAL_CONDITION`, `params.condition = "PARALYZED"`, `params.text = "Paralyze the defending Pokemon."`

#### Scenario: Parsing DRAW_CARDS

- **WHEN** `effectCode = "DRAW_CARDS:3"`
- **THEN** the parsed effect has `type = DRAW_CARDS`, `params.count = 3`

### Requirement: AttackEffectResolver interface

The system SHALL define `AttackEffectResolver` as a Strategy interface:

```java
public interface AttackEffectResolver {
    void resolve(EngineContext ctx, PokemonInPlay attacker, PokemonInPlay defender, AttackEffect effect, Map<String, Object> payload);
    AttackEffectType getType();
}
```

#### Scenario: Resolver registration

- **WHEN** an `AttackEffectResolver` is registered in `AttackEffectRegistry`
- **THEN** `getType()` returns the expected `AttackEffectType`

### Requirement: AttackEffectRegistry facade

The system SHALL define `AttackEffectRegistry` that maps `AttackEffectType` → `AttackEffectResolver`, analogous to `TrainerEffectRegistry`.

- SHALL have `registerResolver(AttackEffectResolver)` method
- SHALL have `resolve(EngineContext, PokemonInPlay, PokemonInPlay, List<AttackEffect>, Map<String, Object>)` that iterates effects and delegates to the appropriate resolver

#### Scenario: Registered resolver is invoked

- **WHEN** `resolve()` is called with an effect list containing `APPLY_SPECIAL_CONDITION`
- **AND** a resolver for `APPLY_SPECIAL_CONDITION` is registered
- **THEN** the resolver's `resolve()` method SHALL be called with the effect's params

### Requirement: DeclareAttackHandler executes effects post-damage

`DeclareAttackHandler.handle()` SHALL execute the `AttackEffect` list after applying damage and KO processing, before advancing the phase.

- Effects SHALL only execute if the attack successfully dealt damage (energy valid, not confused self-hit)
- Effects SHALL execute even if the defender was KO'd (KO effects are processed first)
- Attacks without effects SHALL produce no additional behavior

#### Scenario: Post-damage effect execution

- **WHEN** an attack with `APPLY_SPECIAL_CONDITION:ASLEEP` deals damage to the defender
- **THEN** after damage is applied, the defender SHALL have the `ASLEEP` special condition applied

#### Scenario: KO before effects

- **WHEN** an attack deals lethal damage and has a `DISCARD_ENERGY` effect
- **THEN** the KO SHALL be processed first (prize, replacement, etc.), then the energy discard effect SHALL execute on the replacement active Pokemon's energies (if any) or no-op

### Requirement: ApplySpecialConditionResolver

A resolver that applies a `SpecialCondition` to the defender using `AttackResolver.applyCondition()`.

- Reads `condition` from `effect.params`
- Publishes `STATUS_APPLIED` event with `targetPokemonInstanceId`, `condition`, `sourceAttackName`

#### Scenario: Special condition applied

- **WHEN** `ApplySpecialConditionResolver.resolve()` is called with `params.condition = "PARALYZED"`
- **THEN** `AttackResolver.applyCondition(defender, SpecialCondition.PARALYZED)` SHALL be called
- **AND** a `STATUS_APPLIED` event SHALL be published

### Requirement: DiscardEnergyResolver

A resolver that forces the defender to discard N energies selected by the attacker.

- Reads `count` from `effect.params`
- Expects `payload.targetEnergies` to contain the selected energy instance IDs
- Publishes `ENERGY_DISCARDED` event

#### Scenario: Energies discarded

- **WHEN** `DiscardEnergyResolver.resolve()` is called with `params.count = 2` and `payload.targetEnergies = [energy1, energy2]`
- **THEN** the specified energies SHALL be removed from the defender and added to the discard pile
- **AND** an `ENERGY_DISCARDED` event SHALL be published

### Requirement: DamageBenchResolver

A resolver that applies damage counters to benched Pokemon on the rival's side.

- Reads `damage` from `effect.params` (in HP, converted to counters internally)
- Expects `payload.benchTargets` to contain `[{instanceId, damageCounters}]` for each target
- Publishes `BENCH_DAMAGE` event

#### Scenario: Bench damage applied

- **WHEN** `DamageBenchResolver.resolve()` is called with `params.damage = 30` and `payload.benchTargets = [{instanceId: "pkm2", damageCounters: 3}]`
- **THEN** the target benched Pokemon SHALL receive 3 damage counters
- **AND** `BENCH_DAMAGE` event SHALL be published

### Requirement: HealUserResolver

A resolver that removes damage counters from the attacker.

- Reads `count` from `effect.params` (damage counters to remove)
- Clamps to attacker's current damage (cannot heal below 0)
- Publishes `POKEMON_HEALED` event

#### Scenario: Attacker healed

- **WHEN** `HealUserResolver.resolve()` is called with `params.count = 3` and attacker has 5 damage counters
- **THEN** attacker SHALL end with 2 damage counters
- **AND** `POKEMON_HEALED` event SHALL be published

### Requirement: DrawCardsResolver

A resolver that draws N cards from the attacker's deck to their hand.

- Reads `count` from `effect.params`
- Reuses the draw logic from `DrawCardHandler` (pops from deck front, adds to hand)
- Handles empty deck gracefully (draws what's available)

#### Scenario: Cards drawn

- **WHEN** `DrawCardsResolver.resolve()` is called with `params.count = 3` and attacker has 5+ cards in deck
- **THEN** 3 cards SHALL be moved from deck to hand

### Requirement: CoinFlipEffectResolver

A resolver that executes a coin flip and conditionally applies a sub-effect.

- Uses `EngineContext.getRandomizer()` for the coin flip
- On heads, executes the sub-effect (defined in `params.effectType` and `params.effectParams`)
- On tails, no effect
- Can be configured for `COIN_FLIP_BEFORE_DAMAGE` or `COIN_FLIP_AFTER_DAMAGE` (execution timing varies)

#### Scenario: Coin flip heads applies effect

- **WHEN** `CoinFlipEffectResolver` resolves a `COIN_FLIP_BEFORE_DAMAGE` effect
- **AND** the coin flip result is heads
- **THEN** the sub-effect (e.g., `APPLY_SPECIAL_CONDITION:ASLEEP`) SHALL be applied before damage calculation

#### Scenario: Coin flip tails skips effect

- **WHEN** `CoinFlipEffectResolver` resolves a `COIN_FLIP_AFTER_DAMAGE` effect
- **AND** the coin flip result is tails
- **THEN** no sub-effect SHALL be applied

### Requirement: New event types

The system SHALL add the following values to `GameEventType`:

#### Scenario: STATUS_APPLIED event

- **WHEN** a special condition is applied by an attack
- **THEN** the event payload SHALL contain `targetPokemonInstanceId`, `condition`, `sourceAttackName`

#### Scenario: ENERGY_DISCARDED event

- **WHEN** energies are discarded by an attack effect
- **THEN** the event payload SHALL contain `targetPokemonInstanceId`, `discardedEnergies`, `count`

#### Scenario: BENCH_DAMAGE event

- **WHEN** damage is applied to benched Pokemon
- **THEN** the event payload SHALL contain `targets: [{instanceId, damageCounters}]`

#### Scenario: ATTACK_EFFECT_RESOLVED event

- **WHEN** an attack effect is resolved
- **THEN** the event payload SHALL contain `effectType`, `attackName`, `result`
