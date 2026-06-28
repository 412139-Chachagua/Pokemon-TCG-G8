# Attack Pipeline Contract

## Goal

Define the exact attack resolution order.

The TPI explicitly evaluates attack sequence, damage calculation, weakness, resistance, modifiers, knockout and prize taking.

## Backend location

```
engine/attack/
  AttackResolver.java
  DamageCalculator.java           (clase pública separada)
  EnergyRequirementValidator.java (clase pública separada)
engine/victory/
  VictoryConditionChecker.java
```

## Frontend location

```
features/match/components/action-panel/
features/match/components/game-log/
```

## AttackResolver responsibilities

`AttackResolver` encapsulates:
- Orchestrates the attack pipeline delegando a `DamageCalculator` y `EnergyRequirementValidator`
- **Energy requirement validation**: delega a `EnergyRequirementValidator.checkEnergyRequirements()`
- **Damage calculation**: delega a `DamageCalculator.calculate()`
- **Special condition effects**: confusion self-damage, between-turn poison/burn processing
- **Status condition application**: set conditions on defender from attack effects
- **Knockout detection after damage**: if damage >= remaining HP, mark KO
- **Prize coordination**: signal how many prizes the attacker takes
- Generates `GameEvent` tipados, no strings planos

## DamageCalculator

Clase pública separada en `engine/attack/DamageCalculator.java`:

```java
public class DamageCalculator {
    public record DamageCalculatorResult(
        int baseDamage,
        int weaknessMultiplier,
        int resistanceValue,
        int finalDamage,
        int damageCountersAdded,
        boolean weaknessApplied,
        boolean resistanceApplied
    ) {}

    public static DamageCalculatorResult calculate(
        PokemonInPlay attacker,
        PokemonInPlay defender,
        CardLookupPort cardLookup,
        int attackIndex
    )
}
```

## EnergyRequirementValidator

Clase pública separada en `engine/attack/EnergyRequirementValidator.java`:

```java
public class EnergyRequirementValidator {
    public boolean checkEnergyRequirements(
        PokemonInPlay attacker,
        CardLookupPort cardLookup,
        int attackIndex
    )
}
```

Soporta COLORLESS wildcard matching.

## Attack order

The attack resolution order is fixed. Este pipeline comienza después del draw automático de inicio de turno (ejecutado por `TurnManager`).

1. Announce attack
2. If attacker is CONFUSED, flip coin (cruz → 3 damage counters, turn ends)
3. Validate Energy requirement (via `EnergyRequirementValidator`)
4. Resolve required selections
5. Resolve attack prerequisites
6. Apply effects that modify or cancel the attack
7. Calculate damage (via `DamageCalculator`):
   - base damage
   - SI daño base es 0 o "—": SKIP cálculo de weakness/resistance. Ir directo a paso 9 (solo efectos posteriores).
   - attacker modifiers
   - defender weakness
   - defender resistance
   - defender modifiers
   - **Fur Coat hook** (reduce damage by 20 if defender has Fur Coat + Grass Energy)
   - minimum 0
8. Place damage counters
9. Apply post-damage effects:
   - special conditions
   - energy discards
   - bench damage (NOTA: daño a Banca NO aplica weakness ni resistance)
   - healing
   - **Spiky Shield hook** (3 counters on attacker if defender has Spiky Shield + Darkness Energy)
10. Check knockouts
11. **Destiny Burst hook** (coin flip, heads = 5 counters on attacker if KO'd Pokémon has Destiny Burst + Fire Energy)
12. Take Prize cards automáticamente (1 para KO normal, 2 para Pokémon-EX)
13. Dueño del Pokémon KO'd elige reemplazo de su Banca (si Banca vacía → oponente gana)
14. Check victory
15. End turn

## Damage formula for MVP

```
damage = baseDamage
damage += attackerModifiers
damage *= weaknessMultiplier
damage += resistanceValue
damage += defenderModifiers
damage = max(damage, 0)
damageCounters = damage / 10
```

Resistance is usually negative, for example -20. **NOTA:** El valor actual hardcodeado (-20) debe reemplazarse por el valor dinámico leído de `ResistanceDefinition.value` en la carta (ej: "-20", "-40"). Pendiente de refactor en DamageCalculator.

## DeclareAttack request

```json
{
  "type": "DECLARE_ATTACK",
  "playerId": "player-1",
  "payload": {
    "attackIndex": 0,
    "targetPokemonInstanceId": "card-instance-300"
  },
  "clientRequestId": "client-req-attack-001"
}
```

The attacker is implicitly the Active Pokémon of the requesting player.

## Eventos generados por AttackResolver

Los eventos son `GameEvent` tipados, no strings planos:

```json
{
  "type": "DAMAGE_APPLIED",
  "message": "Slugma dealt 60 damage to Froakie.",
  "payload": { "attackerId": "ci-30", "defenderId": "ci-60", "damage": 60 }
}
```

Tipos de eventos generados: `DAMAGE_APPLIED`, `KNOCKOUT_OCCURRED`, `ATTACK_DECLARED`, special conditions aplicadas.

## MVP supported attack types

MVP may support only:
- fixed damage
- fixed damage + poison
- fixed damage + burn
- fixed damage + asleep
- fixed damage + paralysis
- fixed damage + confusion
- no-damage status attack

Unsupported complex attacks must be marked as UNIMPLEMENTED_EFFECT.
