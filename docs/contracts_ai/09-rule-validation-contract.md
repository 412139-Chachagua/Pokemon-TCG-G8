# Rule Validation Contract

## Goal

Define validation behavior for game actions.

All official game rules must be validated in the backend. Frontend validation is only UX.

## Backend location

```
engine/rules/RuleValidator.java
engine/action/GameError.java
engine/rules/RuleValidator.java
engine/action/GameError.java
```

## Frontend location

```
features/match/components/action-panel/
shared/models/api-error.models.ts
```

## Validation output

```json
{
  "valid": false,
  "error": {
    "code": "INSUFFICIENT_ENERGY",
    "message": "No puedes atacar: te falta 1 Energía de Fuego para usar este ataque.",
    "details": {
      "attackName": "Flamethrower",
      "required": ["FIRE", "COLORLESS", "COLORLESS"],
      "attached": ["FIRE"]
    }
  }
}
```

## General validations

Every action must validate:
- match exists
- match is in expected status
- player belongs to match
- it is the player's turn, unless pending decision says otherwise
- current phase allows action
- referenced card instances exist
- referenced card instances are in expected zone
- targets are valid
- hidden cards are not exposed

## Phase validations

### DRAW

El draw al inicio del turno es **automático y obligatorio** (ejecutado por `TurnManager`). No existe acción DRAW_CARD para el jugador. El primer jugador no roba en su primer turno. Si el mazo está vacío al intentar robar → derrota por DECK_OUT.

Allowed:
- END_TURN (técnicamente disponible para casos edge, aunque en reglas oficiales no es necesario)

### MAIN

Allowed:
- PUT_BASIC_ON_BENCH
- ATTACH_ENERGY
- EVOLVE_POKEMON
- PLAY_TRAINER
- RETREAT_ACTIVE
- DECLARE_ATTACK
- USE_ABILITY (pendiente de implementación)
- END_TURN

### ATTACK

Normally only internal attack resolution should happen.

### BETWEEN_TURNS

Only internal status/effects processing.

## Acciones internas del engine (no validadas como acciones de frontend)

Las siguientes acciones son internas del engine. No requieren validación como acciones de juego porque son ejecutadas automáticamente:

- **DRAW_CARD**: draw automático al iniciar el turno. Si mazo vacío → derrota por DECK_OUT.
- **CHOOSE_KNOCKOUT_REPLACEMENT**: reemplazo de Activo tras KO. Validación interna: bench no vacío, Pokémon seleccionado existe en bench.
- **TAKE_PRIZE_CARD**: toma automática de Prize tras KO. Validación interna: prizes no vacío, pendingPrizeOwnerPlayerId coincide.

## Specific rule validations

### Attach Energy

Reject if:
- not MAIN phase
- card is not Energy
- energy card is not in player's hand
- target is not player's Pokémon in play
- hasAttachedEnergy is true

### Put Basic on Bench

Reject if:
- card is not Pokémon
- card is not BASIC
- card is not in player's hand
- bench already has 5 Pokémon

### Attack

Reject if:
- not player's turn
- not MAIN phase
- first player tries to attack on first turn
- attacker is not player's Active Pokémon
- attack index does not exist
- attacker is ASLEEP or PARALYZED
- attacker has insufficient Energy

### Retreat

Reject if:
- not MAIN phase
- hasRetreated is true
- Active Pokémon is ASLEEP or PARALYZED
- player has no Benched Pokémon
- insufficient Energy cards discarded for retreat cost

### Evolve Pokémon

Reject if:
- not MAIN phase
- card is not in player's hand
- card is not a Pokémon
- target Pokémon is not in play (Active or Bench)
- target Pokémon does not evolve from the specified `evolvesFrom` (el nombre debe coincidir EXACTAMENTE, incluyendo sufijos como -EX, según reglas de nombre canónico)
- target Pokémon was placed or evolved this turn (`evolvedThisTurn == true`)
- same evolution chain was already used this turn (one evolution per Pokémon per turn)
- bench slot or active slot matches the Pokémon being evolved

**Regla de nombre para evolución:** El valor de `evolvesFrom` en la carta de evolución debe coincidir exactamente con el nombre canónico del Pokémon en juego. Ej: "Mega-Venusaur-EX" evoluciona de "Venusaur-EX", no de "Venusaur".

### Play Trainer

Reject if:
- not MAIN phase
- card is not a Trainer
- card is not in player's hand
- if SUPPORTER: `hasPlayedSupporter` must be false

### Play Trainer — Herramienta Pokémon (Pokémon Tool)

Reglas adicionales cuando el Trainer es una Herramienta:
- `targetPokemonInstanceId` es obligatorio en payload
- el Pokémon target debe estar en juego (Activo o Banca)
- el Pokémon target NO debe tener ya una Herramienta equipada (`toolCardInstanceId == null`)
- cuenta como Objeto (no afecta límites de Partidario/Estadio)

### Use Ability

Reject if:
- not MAIN phase
- Pokémon no está en juego (Activo o Banca)
- la Habilidad no existe en la definición del Pokémon
- la Habilidad ya fue usada este turno
- Pokémon está ASLEEP o PARALYZED

## RuleValidator

`RuleValidator` usa `CardLookupPort` para resolver definiciones de cartas durante validación. Retorna `boolean`, no `ValidationResult`.

## Error response format

```json
{
  "code": "NOT_YOUR_TURN",
  "message": "No es tu turno.",
  "details": {
    "currentPlayerId": "player-1",
    "requestingPlayerId": "player-2"
  }
}
```

## UX rule

Messages must be descriptive and actionable.

Avoid generic messages like "Validation error."
