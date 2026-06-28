## ADDED Requirements

### Requirement: VictoryConditionChecker detects sudden death

`VictoryConditionChecker.check()` SHALL detect when both players take their last Prize card in the same turn and return a result indicating sudden death without declaring a winner.

#### Scenario: Simultaneous last prize triggers sudden death
- **WHEN** both players take their last Prize card in the same turn
- **THEN** `VictoryConditionChecker.check()` SHALL return `VictoryCheckResult(suddenDeath = true, winner = null, gameOver = false)`
- **THEN** the game SHALL NOT end yet

### Requirement: GameEngine initiates sudden death flow

`GameEngine` SHALL detect the sudden death result from `VictoryConditionChecker` and initiate a partial game restart with 1 Prize card per player.

#### Scenario: Sudden death starts partial restart
- **WHEN** `VictoryConditionChecker` returns `suddenDeath = true`
- **THEN** `GameEngine` SHALL trigger a partial restart via `SetupManager` with `prizeCount = 1`
- **THEN** the same players SHALL use the same decks (restored to full)
- **THEN** each player SHALL draw a new 7-card hand
- **THEN** a coin flip SHALL determine who goes first
- **THEN** `SUDDEN_DEATH_STARTED` event SHALL be published to both players

### Requirement: SetupManager accepts parametrized prizeCount

`SetupManager.setupGame()` SHALL accept a `prizeCount` parameter instead of hardcoding 6.

#### Scenario: Normal game uses 6 prizes
- **WHEN** `setupGame()` is called with `prizeCount = 6`
- **THEN** each player SHALL receive 6 Prize cards

#### Scenario: Sudden death uses 1 prize
- **WHEN** `setupGame()` is called with `prizeCount = 1`
- **THEN** each player SHALL receive 1 Prize card
