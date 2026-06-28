# Status Effects Contract

## Goal

Define special condition rules.

The TPI requires all five special conditions, their incompatibilities, and between-turn processing order.

## Backend location

```
engine/model/PokemonInPlay.java (specialConditions field)
engine/attack/AttackResolver.java (status logic as private helper methods)
engine/turn/TurnManager.java (invoca between-turn status processing)
```

**Estado actual:** NO IMPLEMENTADO como componente separado. La lógica de status está en `AttackResolver` como métodos privados, invocados desde `TurnManager` durante `BETWEEN_TURNS`.

**Requerimiento TPI (RNF-02, RNF-03):** El TPI exige `StatusEffectManager` como componente independiente y testeable del Game Engine, con cobertura de tests ≥ 90%. Debe residir en `engine/status/` con métodos públicos:
- `processBetweenTurnEffects(GameState state): void`
- `applyCondition(PokemonInPlay target, SpecialCondition condition): void`
- `removeCondition(PokemonInPlay target, SpecialCondition condition): void`
- `isAffectedBy(PokemonInPlay pokemon, SpecialCondition condition): boolean`

Pendiente de implementar. Hasta entonces, el comportamiento actual descrito en este contrato es el vigente pero **no cumple el TPI**.

## Frontend location

```
features/match/components/active-pokemon-slot/
features/match/components/bench-zone/
shared/models/game-state.models.ts
```

## Special conditions

- ASLEEP
- BURNED
- CONFUSED
- PARALYZED
- POISONED

## Exclusive rotation conditions

Only one of these may be active at the same time:
- ASLEEP
- CONFUSED
- PARALYZED

The newest one replaces the previous one.

## Marker conditions

These may coexist:
- BURNED
- POISONED

A Pokémon may be BURNED + POISONED + PARALYZED at the same time.

## Sweet Veil hook

Sweet Veil is a passive ability that blocks special conditions on the player's Pokémon. It is checked before applying any special condition.

**Block logic:**
- When a special condition is about to be applied to a Pokémon
- Check if ANY of the player's Pokémon in play has the Sweet Veil ability AND a Fairy Energy attached
- If yes: block the condition, do not apply it
- If no: apply the condition normally

This affects ASLEEP, BURNED, CONFUSED, PARALYZED, POISONED.

The hook is integrated in `ApplySpecialConditionResolver.resolve()`.

## Between-turns order

Fixed order:
1. POISONED
2. BURNED
3. ASLEEP
4. PARALYZED
5. Abilities / other between-turn effects
6. Knockout check

## Paralizado — timing de curación

El Pokémon Paralizado no puede atacar ni retirarse. La curación sigue esta regla oficial:
"Si tu Pokémon está Paralizado desde el comienzo de tu último turno, podrás eliminar esa Condición Especial entre turno y turno."

**Timing exacto:**
1. Turno del oponente: ataque que paraliza a tu Activo
2. Tu turno: no puedes atacar/retirarte (paralizado). Solo puedes hacer acciones que no requieran ataque ni retirada.
3. BETWEEN_TURNS (después de tu turno): paso 4 (Paralizado) → se curará automáticamente
4. Tu próximo turno: puedes actuar normalmente

Esto significa que el Pokémon afectado "pierde" un turno completo (el turno del jugador que fue paralizado), y se cura al final de ese turno.

## Removal

All special conditions are removed when the Pokémon:
- retreats to Bench
- evolves

## Eventos de estado

Los eventos de estado son `GameEvent` tipados, no strings planos:

```json
{
  "type": "STATE_UPDATED",
  "message": "Froakie took 10 damage from poison.",
  "payload": { "pokemonInstanceId": "ci-60", "condition": "POISONED", "damage": 10 }
}
```

## Frontend display

Frontend may display:
- ASLEEP: rotate card left
- CONFUSED: rotate card upside down
- PARALYZED: rotate card right
- BURNED: marker
- POISONED: marker

Frontend display must not affect rules.
