## Context

The attack pipeline in `DeclareAttackHandler` → `AttackResolver` currently handles raw damage calculation (base damage + weakness ×2 + resistance -20) and applies it to the defender. Three gaps exist:

1. **Damage modifiers** (`TurnFlags.damageModifiers`) are populated by `DamageModifyResolver` (trainer effects) but never consumed by `AttackResolver.calculateDamage()`.
2. **Attack text effects** (special conditions, energy discard, bench damage, heal, draw, coin flips) defined in `CardAttackEntity.effectCode`/`effectText` are never executed post-damage.
3. **AS TÁCTICO limit** (max 1 per deck) is not validated in `DeckValidator` despite `TrainerCardDefinition.isAceSpec()` and `DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED` existing.

The trainer effects system already provides a working Strategy pattern (`TrainerEffectRegistry` + `TrainerEffectResolver`) that the attack effect system can mirror.

## Goals / Non-Goals

**Goals:**
- Modify `AttackResolver.calculateDamage()` to apply damage modifiers from `TurnFlags.damageModifiers`
- Reset `damageModifiers` in `TurnManager.startTurn()`
- Create `AttackEffectType` enum, `AttackEffect` domain class, `AttackEffectResolver` strategy interface, `AttackEffectRegistry` facade
- Add `List<AttackEffect> effects` to `PokemonCardDefinition.AttackDefinition`
- Parse `effectCode`/`effectText` in `CardLookupAdapter.toAttack()` into `AttackEffect` list
- Implement 6 concrete resolvers covering xy1 attack effects: `ApplySpecialConditionResolver`, `DiscardEnergyResolver`, `DamageBenchResolver`, `HealUserResolver`, `DrawCardsResolver`, `CoinFlipEffectResolver`
- Execute attack effects in `DeclareAttackHandler.handle()` post-damage pre-phase-advance
- Add 4 new `GameEventType` values: `STATUS_APPLIED`, `ENERGY_DISCARDED`, `BENCH_DAMAGE`, `ATTACK_EFFECT_RESOLVED`
- Add AS TÁCTICO limit validation in `DeckValidator.validate()` between size and 4-copies checks

**Non-Goals:**
- Not modifying the database schema (`effect_code` and `effect_text` already exist in `CardAttackEntity`)
- Not implementing trainer effects (already handled by `TrainerEffectRegistry`)
- Not adding frontend changes for effect targeting (resolvers receive payload from existing `GameAction`)
- Not implementing Mega Evolution or postponed features

## Decisions

### D1: Attack effect system mirrors Trainer strategy pattern
- **Choice**: Create `AttackEffectRegistry` + `AttackEffectResolver` parallel to `TrainerEffectRegistry` + `TrainerEffectResolver`
- **Rationale**: Attack effects and trainer effects have different resolution contexts (`PokemonInPlay attacker/defender` vs `PlayerState player`), different effect types, different event payloads, and different lifecycle. Coupling them would introduce conditional logic and violate Single Responsibility.
- **Alternative considered**: Reusing `TrainerEffectRegistry` by adding attack-specific resolvers — rejected because the resolver signatures differ fundamentally.

### D2: AttackEffect as flat list in AttackDefinition
- **Choice**: `List<AttackEffect> effects` as a flat list, no nested pipelines
- **Rationale**: xy1 attacks have at most 1-2 effects per attack. Nested/composite patterns add complexity without current need. Can be introduced later if the game demands multi-step effects.
- **Alternative considered**: Composite pattern with sequential/parallel execution — over-engineering for current set.

### D3: Damage modifiers keyed by Pokemon instanceId
- **Choice**: `damageModifiers` map uses `instanceId.toString()` as key, integer modifier as value
- **Rationale**: `DamageModifyResolver` already writes `{targetId: modifierValue}`. Attacker modifiers affect `baseDamage`, defender modifiers affect `finalDamage`. Simple, consistent with existing code.
- **Alternative considered**: Dedicated `DamageModifier` class — not needed for a simple +/- integer.

### D4: Effect code parsing convention
- **Choice**: `effectCode` stores a comma-separated string like `"APPLY_SPECIAL_CONDITION:PARALYZED"` or `"DISCARD_ENERGY:2"` or `"DRAW_CARDS:3"`. `effectText` stores the human-readable text. `CardLookupAdapter.toAttack()` parses both into `AttackEffect` list.
- **Rationale**: No schema changes needed. The format matches the existing data in `CardAttackEntity`.
- **Alternative considered**: JSON in `effectCode` — fragile, harder to read/debug.

## Risks / Trade-offs

- **[Risk]** Some attack effects require player targeting (DiscardEnergy, DamageBench) — `DeclareAttackHandler` receives `GameAction` payload that must include target selections. **Mitigation**: Resolvers read targeting from the existing `action.payload` map; frontend must supply `targetEnergies`/`benchTargets`.
- **[Risk]** `applyCondition()` for `CONFUSED`/`ASLEEP`/`PARALYZED` replaces existing conditions (the method already handles this) but calling code must ensure it's applied post-damage, not pre. **Mitigation**: Effects always execute after `calculateDamage()` and after KO processing.
- **[Risk]** `CoinFlipEffectResolver` must pass `RandomizerPort` to the resolver chain. **Mitigation**: `EngineContext` provides `getRandomizer()` to all handlers and resolvers.
- **[Trade-off]** 6 resolvers is moderate boilerplate but each is ~30-50 lines with clear single responsibility.
- **[Trade-off]** AS TÁCTICO validation is a 10-line addition but closes a correctness gap that blocks tournament-legal deck validation.

## Attack Effect Code Format

```
APPLY_SPECIAL_CONDITION:PARALYZED
DISCARD_ENERGY:2
DAMAGE_BENCH:30
HEAL_USER:30
DRAW_CARDS:3
COIN_FLIP_BEFORE:APPLY_SPECIAL_CONDITION:ASLEEP
COIN_FLIP_AFTER:DISCARD_ENERGY:1
```

The `toAttack()` parser splits by `:` — first token is `AttackEffectType`, subsequent tokens populate `params` map with type-specific keys (`condition`, `count`, `damage`, etc.).
