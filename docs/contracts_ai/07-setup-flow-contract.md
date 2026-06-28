# Setup Flow Contract

## Goal

Define match creation and initial setup.

## Backend location

```
engine/setup/SetupManager.java
services/matches/MatchApplicationService.java
repositories/entities/MatchPlayerEntity.java
repositories/jpa/MatchPlayerJpaRepository.java
```

## Frontend location

```
features/lobby/
features/match/
```

## Match creation flow

1. Player 1 creates a match (1 player or 2 players direct)
2. If 1 player: match status is WAITING, Player 2 joins later
3. If 2 players direct: match goes directly to SETUP/ACTIVE
4. Both decks are loaded from seed or selected decks
5. Backend shuffles both decks
6. Each player draws 7 cards
7. Mulligan is resolved
8. Each player chooses Active Pokémon (auto: first Basic in hand)
9. Each player may put up to 5 Basic Pokémon on Bench (auto-fill)
10. Backend creates 6 Prize cards per player
11. Coin flip chooses first player
12. Both sides reveal Active/Bench
13. Match status changes to ACTIVE
14. First turn begins

The TPI requires initial 7-card hands, mulligan, Active/Bench setup, six Prize cards and first-player coin flip.

## MVP simplification

For first playable MVP, setup is automatic:
- choose first Basic Pokémon in hand as Active
- put up to 5 additional Basic Pokémon on Bench
- create 6 Prize cards
- randomly choose first player

Manual setup can be added later.

## CreateMatchRequest

Soporta dos modos:

**1 jugador** (espera join):
```json
{
  "player1Name": "Antonio",
  "player1DeckId": "seed-fire-deck"
}
```

**2 jugadores directo** (ACTIVE inmediato):
```json
{
  "player1Name": "Antonio",
  "player1DeckId": "seed-fire-deck",
  "player2Name": "Lucas",
  "player2DeckId": "seed-water-deck"
}
```

El campo también acepta los alias `playerName` y `deckId` (via @JsonAlias) para compatibilidad con el formato anterior.

## MatchResponse

```json
{
  "id": "9a747f90-b50e-49df-9d8a-456c9796aa11",
  "status": "WAITING",
  "currentPhase": null,
  "turnNumber": 0,
  "currentPlayerId": null,
  "firstPlayerId": null,
  "winnerPlayerId": null,
  "finishReason": null,
  "players": [
    { "playerId": "player-1", "side": "PLAYER_ONE", "displayName": "Santi" }
  ],
  "createdAt": "2026-05-06T15:30:00Z"
}
```

## JoinMatchRequest

```json
{
  "playerName": "Lucas",
  "deckId": "seed-water-deck"
}
```

## JoinMatchResponse

```json
{
  "id": "9a747f90-b50e-49df-9d8a-456c9796aa11",
  "status": "ACTIVE",
  "currentPhase": "DRAW",
  "turnNumber": 1,
  "currentPlayerId": "player-1",
  "firstPlayerId": "player-1",
  "winnerPlayerId": null,
  "finishReason": null,
  "players": [
    { "playerId": "player-1", "side": "PLAYER_ONE", "displayName": "Santi" },
    { "playerId": "player-2", "side": "PLAYER_TWO", "displayName": "Lucas" }
  ],
  "createdAt": "2026-05-06T15:30:00Z"
}
```

## Flujo de join

`MatchApplicationService.joinMatch()`:
1. Carga ambos decks via `DeckLoadPort`
2. Ejecuta `SetupManager.setup()` que maneja shuffle, deal, mulligan, auto-fill bench, prize creation, coin flip
3. Persiste el estado via `StatePersisterPort`
4. Cambia match a ACTIVE en una transacción

## MatchPlayerEntity

Los jugadores se almacenan en tabla separada `match_players`:

```
id: UUID (PK)
match: MatchEntity (FK)
playerId: UUID
playerKind: String ("HUMAN")
side: String ("PLAYER_ONE" / "PLAYER_TWO")
deckId: UUID
displayName: String
joinedAt: Instant
```

## SetupManager.setup()

`SetupManager.setup()` handles:
- deck load via `DeckLoadPort`
- shuffle via `RandomizerPort`
- deal 7 cards
- mulligan resolution
- both sides automatically choose first Basic Pokémon in hand as Active
- auto-fill bench with remaining Basic Pokémon (up to 5)
- create 6 Prize cards per player
- coin flip for first player
- mark setup complete → status becomes ACTIVE

## Mulligan resolution (según reglas oficiales)

Mulligan logic is internal to `SetupManager.setup()`. No separate `MulliganService` class exists. Mulligan is a private method within `SetupManager`.

**Paso a paso:**

1. Ambos jugadores roban 7 cartas y verifican si tienen al menos 1 Pokémon Básico.

2. **Si AMBOS no tienen Básico:**
   a. Ambos muestran su mano al rival
   b. Ambos ponen su mano en la baraja y barajan
   c. Volver al paso 1

3. **Si solo UN jugador no tiene Básico:**
   a. Ese jugador declara mulligan
   b. El otro jugador coloca su Pokémon Activo, Banca y cartas de Premio NORMALMENTE (no espera)
   c. El jugador sin Básico MUESTRA su mano al rival
   d. Pone toda su mano en la baraja y baraja
   e. Roba 7 cartas nuevas
   f. Si sigue sin Básico, repite desde 3.c
   g. Una vez que tiene Básico, coloca su setup normalmente
   h. El jugador que NO tuvo mulligan roba 1 carta por cada mulligan declarado por su rival
   i. SI alguna de esas cartas extra es un Pokémon Básico, PUEDE ponerlo en su Banca
   j. Ambos jugadores revelan sus Pokémon (Activo y Banca)

4. **Si AMBOS tienen Básico:**
   a. Proceder con setup normal (colocar Activo, Banca, Premios, revelar)

## Estado SETUP

El TPI (RF-03) define SETUP como estado del juego. Sin embargo:

- **Flujo de 2 jugadores directo** (player2Name presente): el setup se ejecuta en la misma transacción y el match pasa directamente a ACTIVE sin transición visible por SETUP. **NO IMPLEMENTADO** como estado persistido.
- **Flujo de 1 jugador + join**: después del join, el match pasa de WAITING a ACTIVE (el setup ocurre dentro de joinMatch). El estado SETUP no se persiste.
- Pendiente de implementar si se requiere visibilidad del estado SETUP en el frontend o en el log de eventos.

## Setup invariants

- Each player must have exactly 1 Active Pokémon before ACTIVE status
- Bench size must be between 0 and 5
- Prize count must be 6 unless sudden death
- Hand, deck, prize and discard zones must contain unique CardInstance IDs
- No CHOOSE_KNOCKOUT_REPLACEMENT action exists; during setup, Active is chosen automatically
