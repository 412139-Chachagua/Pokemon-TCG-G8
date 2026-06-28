## MODIFIED Requirements

### Requirement: Mulligan rules

Si un jugador no tiene ningún Pokémon BASIC en mano:

1. **Revelar la mano al oponente**: antes de devolver las cartas al deck, se debe generar un evento `MULLIGAN_REVEALED` con los `cardDefinitionId` de las 7 cartas en mano.
2. Devolver las 7 cartas al deck.
3. Mezclar de nuevo via `RandomizerPort`.
4. Repartir nuevas 7 cartas.
5. Incrementar `mulliganCount` del jugador en `PlayerState`.
6. Repetir hasta que el jugador tenga al menos un Pokémon BASIC en mano.

Si ambos jugadores declaran mulligan, el proceso se resuelve de forma independiente para cada uno.

Al finalizar el setup, cada jugador roba tantas cartas extra como mulligans declaró el oponente. Estas cartas se agregan directamente a la mano.

Invariante: al salir del mulligan, ambos jugadores tienen garantizado al menos un Pokémon BASIC en mano. Si esto no se cumple, el setup no puede continuar.

#### Scenario: Mulligan reveals hand to opponent

- **GIVEN** a player has no Basic Pokémon in their initial 7-card hand
- **WHEN** `SetupManager.resolveMulligan()` detects no Basic Pokémon
- **THEN** an event `MULLIGAN_REVEALED` is generated with the 7 `cardDefinitionId` values before reshuffling
- **AND** the event payload includes `playerId` and `revealedCardIds`

#### Scenario: Mulligan reveals hand on each iteration

- **GIVEN** a player declares multiple mulligans in a row
- **WHEN** each mulligan occurs
- **THEN** a `MULLIGAN_REVEALED` event is generated for each failed hand

## ADDED Requirements

### Requirement: MULLIGAN_REVEALED event type

The system SHALL define a new `GameEventType` value `MULLIGAN_REVEALED`.

#### Scenario: Event type exists

- **WHEN** SetupManager emits a mulligan reveal event
- **THEN** the event type SHALL be `MULLIGAN_REVEALED`
- **AND** the payload SHALL contain `playerId` (String) and `revealedCardIds` (List of String)

### Requirement: GameEventType enum extended

The `engine/event/GameEventType` enum SHALL include `MULLIGAN_REVEALED`.

#### Scenario: Enum compiles

- **WHEN** the project is compiled with `mvn compile`
- **THEN** `GameEventType.MULLIGAN_REVEALED` exists and is accessible
