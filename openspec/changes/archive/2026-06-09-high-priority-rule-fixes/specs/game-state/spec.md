## MODIFIED Requirements

### Requirement: GameState tracks first turn completion per player

`GameState` SHALL track whether each player has completed their first turn using `Set<UUID> playersWhoCompletedFirstTurn`.

#### Scenario: New game has empty set
- **WHEN** a `GameState` instance is created for a new game
- **THEN** `playersWhoCompletedFirstTurn` SHALL be an empty `HashSet<UUID>`

#### Scenario: Player completes first turn
- **WHEN** `markPlayerCompletedFirstTurn(playerId)` is called
- **THEN** that `playerId` SHALL be added to `playersWhoCompletedFirstTurn`

#### Scenario: hasPlayerCompletedFirstTurn returns true for done player
- **WHEN** `hasPlayerCompletedFirstTurn(playerId)` is called for a player who has completed their first turn
- **THEN** it SHALL return `true`

#### Scenario: hasPlayerCompletedFirstTurn returns false for new player
- **WHEN** `hasPlayerCompletedFirstTurn(playerId)` is called for a player who has NOT completed their first turn
- **THEN** it SHALL return `false`

### Requirement: GameState tracks pending KO replacement

`GameState` SHALL include `boolean pendingKOReplacement` and `UUID knockedOutPlayerId` to manage the KO replacement flow.

#### Scenario: Initial state has no pending replacement
- **WHEN** a `GameState` is initialized
- **THEN** `pendingKOReplacement` SHALL be `false` and `knockedOutPlayerId` SHALL be `null`

#### Scenario: KO detected sets pending flag
- **WHEN** a Pokemon Active is knocked out
- **THEN** `pendingKOReplacement` SHALL be set to `true` and `knockedOutPlayerId` SHALL be set to the affected player's UUID

#### Scenario: Replacement chosen clears pending flag
- **WHEN** the player selects a replacement Pokemon
- **THEN** `pendingKOReplacement` SHALL be set to `false` and `knockedOutPlayerId` SHALL be set to `null`

### Requirement: GameState supports sudden death mode

`GameState` SHALL include `boolean suddenDeath` and `int prizeCountPerPlayer` to support sudden death mode.

#### Scenario: Normal game has 6 prizes
- **WHEN** a new `GameState` is created for a normal game
- **THEN** `suddenDeath` SHALL be `false` and `prizeCountPerPlayer` SHALL be `6`

#### Scenario: Sudden death game has 1 prize
- **WHEN** a `GameState` is created for sudden death mode
- **THEN** `suddenDeath` SHALL be `true` and `prizeCountPerPlayer` SHALL be `1`
