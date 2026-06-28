## MODIFIED Requirements

### Requirement: RuleValidator validates evolution by player's first turn

`RuleValidator.validate(EVOLVE_POKEMON)` SHALL check `GameState.hasPlayerCompletedFirstTurn(playerId)` instead of `gameState.getTurnNumber() == 1` to determine if evolution is allowed.

#### Scenario: Player 1 cannot evolve on their first turn (turn 1)
- **WHEN** Player 1 attempts to evolve a Pokemon on turn 1 (global turnNumber = 1)
- **THEN** `RuleValidator` SHALL reject the action with `EVOLVE_NOT_ALLOWED`

#### Scenario: Player 2 cannot evolve on their first turn (turn 2)
- **WHEN** Player 2 attempts to evolve a Pokemon on their first turn (global turnNumber = 2, but Player 2 has not completed their first turn)
- **THEN** `RuleValidator` SHALL reject the action with `EVOLVE_NOT_ALLOWED`

#### Scenario: Player 2 can evolve on turn 3 (after their first turn)
- **WHEN** Player 2 attempts to evolve a Pokemon after having completed their first turn
- **THEN** `RuleValidator` SHALL allow the action

#### Scenario: Player 1 can evolve on turn 2 (after their first turn)
- **WHEN** Player 1 attempts to evolve a Pokemon in a later turn (after having completed their first turn)
- **THEN** `RuleValidator` SHALL allow the action

### Requirement: RuleValidator validates attack by player's first turn

`RuleValidator.validate(DECLARE_ATTACK)` SHALL check `GameState.hasPlayerCompletedFirstTurn(playerId)` for consistency with the evolution rule.

#### Scenario: Player cannot attack on their first turn
- **WHEN** a player attempts to declare an attack on their first turn
- **THEN** `RuleValidator` SHALL reject the action

### Requirement: RuleValidator validates CHOOSE_KO_REPLACEMENT

`RuleValidator` SHALL validate `CHOOSE_KO_REPLACEMENT` actions, allowing them only when `GameState.pendingKOReplacement == true`.

#### Scenario: KO replacement allowed when pending
- **WHEN** a player sends `CHOOSE_KO_REPLACEMENT` and `pendingKOReplacement == true`
- **THEN** `RuleValidator` SHALL allow the action

#### Scenario: KO replacement rejected when not pending
- **WHEN** a player sends `CHOOSE_KO_REPLACEMENT` and `pendingKOReplacement == false`
- **THEN** `RuleValidator` SHALL reject the action

### Requirement: TurnManager marks first turn completed

`TurnManager.endTurn()` SHALL call `GameState.markPlayerCompletedFirstTurn(currentPlayerId)` after the current player's first turn ends.

#### Scenario: After turn 1, Player 1 marked as completed
- **WHEN** `TurnManager.endTurn()` is called at the end of turn 1
- **THEN** Player 1's UUID SHALL be added to `playersWhoCompletedFirstTurn`

#### Scenario: After turn 2, Player 2 marked as completed
- **WHEN** `TurnManager.endTurn()` is called at the end of turn 2
- **THEN** Player 2's UUID SHALL be added to `playersWhoCompletedFirstTurn`

### Requirement: SetupManager accepts prizeCount parameter

`SetupManager` SHALL accept a `prizeCount` parameter to support different prize configurations (6 for normal, 1 for sudden death).

#### Scenario: Normal setup uses 6 prizes
- **WHEN** `SetupManager.setupGame(..., prizeCount = 6)` is called
- **THEN** each player SHALL receive 6 Prize cards

#### Scenario: Sudden death setup uses 1 prize
- **WHEN** `SetupManager.setupGame(..., prizeCount = 1)` is called
- **THEN** each player SHALL receive 1 Prize card
