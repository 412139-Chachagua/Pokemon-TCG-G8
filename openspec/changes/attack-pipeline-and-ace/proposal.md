## Why

The attack pipeline currently applies raw damage without considering damage modifiers from trainer cards/abilities, never executes post-damage attack effects (special conditions, energy discard, bench damage, healing, drawing), and does not validate the 1-per-deck limit for AS TÁCTICO cards. These gaps block correctness for the xy1 card set and violate existing contract specifications.

## What Changes

- Add damage modifier pipeline: `AttackResolver.calculateDamage()` reads `TurnFlags.damageModifiers` populated by trainer effects
- Create attack effect system: `AttackEffectType` enum, `AttackEffect` domain class, `AttackEffectResolver` strategy interface, `AttackEffectRegistry` facade
- Add `List<AttackEffect> effects` to `PokemonCardDefinition.AttackDefinition`
- Map `CardAttackEntity.effectCode`/`effectText` to `AttackEffect` in `CardLookupAdapter`
- Implement 6 concrete resolvers for xy1 set: `ApplySpecialConditionResolver`, `DiscardEnergyResolver`, `DamageBenchResolver`, `HealUserResolver`, `DrawCardsResolver`, `CoinFlipEffectResolver`
- Execute attack effects in `DeclareAttackHandler.handle()` post-damage pre-phase-advance
- Add 4 new event types: `STATUS_APPLIED`, `ENERGY_DISCARDED`, `BENCH_DAMAGE`, `ATTACK_EFFECT_RESOLVED`
- Add AS TÁCTICO validation in `DeckValidator.validate()` enforcing max 1 copy per deck

## Capabilities

### New Capabilities
- `damage-modifiers-pipeline`: Pipeline que aplica modificadores de daño (attacker/defender) desde `TurnFlags.damageModifiers` en `AttackResolver.calculateDamage()`, con reseteo en `TurnManager.startTurn()`
- `attack-effects-system`: Sistema completo de efectos post-daño con AttackEffectType enum, AttackEffect domain class, AttackEffectResolver strategy interface, AttackEffectRegistry facade, 6 resolvers concretos para xy1, y mapeo desde CardAttackEntity
- `ace-spec-validation`: Validación en `DeckValidator.validate()` que itera las cartas del mazo, detecta TrainerCardDefinition con isAceSpec() y agrega ACE_SPEC_LIMIT_EXCEEDED si total > 1

### Modified Capabilities
- `attack-pipeline-and-ace-spec.md`: Spec existente que se refina y divide en los 3 capabilities listados arriba

## Impact

- **AttackResolver.java**: `calculateDamage()` recibe y aplica `damageModifiers` Map; `resolve()` expone el Map
- **TurnFlags.java**: ya tiene `damageModifiers` (Map<String, Object>) con getter/setter
- **TurnManager.java**: agregar reseteo de `damageModifiers` en `resetTurnFlags()`
- **DeclareAttackHandler.java**: ejecutar lista de `AttackEffect` post-daño antes de avanzar fase
- **PokemonCardDefinition.java**: `AttackDefinition` agrega `List<AttackEffect> effects`
- **CardLookupAdapter.java**: mapear `effectCode`/`effectText` a `AttackEffect`
- **DeckValidator.java**: agregar loop de AS TÁCTICO entre validación de tamaño y 4-copias
- **Archivos nuevos**: 10+ clases en `engine/attack/` y `engine/event/`
