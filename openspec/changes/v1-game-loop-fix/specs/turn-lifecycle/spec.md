## ADDED Requirements

### Requirement: Turn flags reset at the start of each turn
The system SHALL reset all turn flags (`hasDrawnForTurn`, `hasAttachedEnergy`, `hasRetreated`, `hasPlayedSupporter`, `hasPlayedStadium`, `hasAttacked`) at the beginning of each player's turn, before the DRAW phase begins.

#### Scenario: Flags reset after first turn
- **GIVEN** a match is ACTIVE with Player 1 having completed their first turn (attached energy, attacked)
- **WHEN** Player 1 executes END_TURN
- **AND** the system prepares Player 2's turn
- **THEN** Player 2's `hasAttachedEnergy`, `hasRetreated`, `hasPlayedSupporter`, and `hasAttacked` SHALL be `false`

#### Scenario: Sequential turns maintain fresh flags
- **GIVEN** Player 1 is on turn 3
- **WHEN** Player 1 executes END_TURN
- **AND** the system prepares Player 2's turn
- **THEN** all turn flags for Player 2 SHALL be `false`, regardless of Player 1's previous actions

### Requirement: TurnManager.startTurn() is invoked every turn
`TurnManager.startTurn()` SHALL be invoked at the start of each player's turn, not only at match initialization.

#### Scenario: EndTurnHandler wires startTurn
- **GIVEN** `EndTurnHandler.handle(ctx)` is executing for the current player
- **WHEN** `turnManager.endTurn()` completes
- **THEN** the system SHALL call `turnManager.startTurn()` to prepare the next player's state

#### Scenario: startTurn is idempotent
- **GIVEN** `startTurn()` is called multiple times within the same turn
- **WHEN** the method executes
- **THEN** it SHALL NOT double-reset flags or cause negative side effects
