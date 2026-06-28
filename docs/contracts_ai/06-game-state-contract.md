# Game State Contract

## Goal

Define the canonical game state.

Backend owns the complete state.

Frontend receives only sanitized state.

## Backend location

```
engine/model/
  GameState.java
  PlayerState.java
  PublicGameState.java
  PrivatePlayerState.java
```

## Frontend location

```
shared/models/game-state.models.ts
features/match/
```

## GameState (private backend model)

```
matchId: UUID
status: MatchStatus
phase: TurnPhase
turnNumber: int
currentPlayerId: UUID
firstPlayerId: UUID
players: PlayerState[2]
stadiumCardInstanceId: UUID | null
turnFlags: TurnFlags
pendingDecision: Object (genérico, no PendingDecision tipado)
pendingPrizeOwnerPlayerId: UUID | null
winnerPlayerId: UUID | null
finishReason: FinishReason | null
createdAt: Instant
updatedAt: Instant
```

## PlayerState (private backend model)

```
playerId: UUID
side: PlayerSide
deck: CardInstance[]
hand: CardInstance[]
prizes: CardInstance[]
discard: CardInstance[]
activePokemon: PokemonInPlay | null
bench: PokemonInPlay[]
mulliganCount: int
```

## PokemonInPlay

```
instanceId: UUID
cardDefinitionId: string
ownerPlayerId: UUID
enteredTurnNumber: int
evolvedThisTurn: boolean
damageCounters: int
specialConditions: SpecialCondition[]
attachedEnergies: CardInstance[]
toolCardInstanceId: UUID | null
abilitiesUsedThisTurn: Set<String>
```

Energies attached to a Pokémon are stored as a `List<CardInstance>` directly in `PokemonInPlay.attachedEnergies`. No separate `AttachedCard` class exists.

## TurnFlags

```
hasDrawnForTurn: boolean
hasAttachedEnergy: boolean
hasRetreated: boolean
hasPlayedSupporter: boolean
hasPlayedStadium: boolean
hasAttacked: boolean
```

## PendingDecision

No existe clase tipada `PendingDecision`. Es un `Object` genérico (Map<String, Object>) en `GameState`.

## Public view (PublicGameState)

Existe como clase separada `PublicGameState` en `engine/model/PublicGameState.java` con clases anidadas:

### PublicGameState

```
matchId: UUID
status: String
phase: String
turnNumber: int
currentPlayerId: UUID
firstPlayerId: UUID
players: PublicPlayerState[]
```

### PublicPlayerState (anidada en PublicGameState)

```
playerId: UUID
side: String
activePokemon: PublicPokemonSlot | null
bench: PublicPokemonSlot[]
prizes: String[] (todos "FACE_DOWN")
```

### PublicPokemonSlot (anidada en PublicGameState)

```
instanceId: String
cardId: String
damageCounters: int
specialConditions: String[]
attachedCards: String[] (cardDefinitionIds)
```

`MatchQueryService.buildPublicState()` construye la vista pública.

## Public view JSON example

```json
{
  "matchId": "9a747f90-b50e-49df-9d8a-456c9796aa11",
  "status": "ACTIVE",
  "phase": "MAIN",
  "turnNumber": 3,
  "currentPlayerId": "player-1",
  "firstPlayerId": "player-1",
  "players": [
    {
      "playerId": "player-1",
      "side": "PLAYER_ONE",
      "activePokemon": {
        "instanceId": "ci-30",
        "cardId": "xy1-10",
        "damageCounters": 2,
        "specialConditions": [],
        "attachedCards": ["energy-fire-basic"]
      },
      "bench": [],
      "prizes": ["FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN"]
    },
    {
      "playerId": "player-2",
      "side": "PLAYER_TWO",
      "activePokemon": {
        "instanceId": "ci-60",
        "cardId": "xy1-7",
        "damageCounters": 0,
        "specialConditions": ["POISONED"],
        "attachedCards": []
      },
      "bench": [],
      "prizes": ["FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN"]
    }
  ]
}
```

## Private view (PrivatePlayerState)

Existe como clase separada `PrivatePlayerState` en `engine/model/PrivatePlayerState.java` con clases anidadas:

### PrivatePlayerState

```
playerId: UUID
hand: List<PrivateHandCard>
deckCount: int
discardCount: int
prizes: List<PrizeSlot>
```

### PrivateHandCard (anidada en PrivatePlayerState)

```
instanceId: String
cardId: String
name: String
supertype: String
```

### PrizeSlot (anidada en PrivatePlayerState)

```
slot: int
known: boolean
cardId: String
```

`MatchQueryService.buildPrivateState()` construye la vista privada.

## Muerte Súbita (SUDDEN_DEATH)

Si ambos jugadores cumplen una condición de victoria simultáneamente (ej. ambos toman su última Prize en el mismo ataque), se debe iniciar una partida de Muerte Súbita:

- Cada jugador comienza con **1 carta de Premio** en lugar de 6.
- Por lo demás, es una partida completamente nueva: lanzar moneda, barajar, robar 7, etc.
- Si la Muerte Súbita también termina en empate, se repite hasta que haya un ganador.
- No hay cambios estructurales en `GameState` para soportar esto; el engine detecta la condición y puede reinicializar el estado.

## Privacy rules

Never send to the opponent:
- hand card identities
- deck card identities
- deck order
- prize card identities
- unrevealed setup Pokémon before reveal

The opponent may receive only counts and public board information.
