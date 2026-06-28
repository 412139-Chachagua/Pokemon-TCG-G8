## ADDED Requirements

### Requirement: Win streak increments on consecutive wins
The system SHALL increment the current win streak each time a player wins a match after a previous win.

#### Scenario: Two consecutive wins
- **GIVEN** a player wins a match (streak becomes 1)
- **WHEN** the player wins the next match
- **THEN** `currentWinStreak` SHALL be 2

#### Scenario: Maximum streak is tracked
- **GIVEN** a player has `currentWinStreak=3` and `maxWinStreak=3`
- **WHEN** the player wins a fourth consecutive match
- **THEN** `currentWinStreak` SHALL be 4 and `maxWinStreak` SHALL be 4

#### Scenario: Streak resets after a loss
- **GIVEN** a player has `currentWinStreak=5`
- **WHEN** the player loses a match
- **THEN** `currentWinStreak` SHALL be 0

#### Scenario: Max streak preserved after loss
- **GIVEN** a player has `currentWinStreak=5` and `maxWinStreak=5`
- **WHEN** the player loses and later wins again
- **THEN** `currentWinStreak` SHALL be 1 and `maxWinStreak` SHALL remain 5

### Requirement: Sudden death does not affect streaks
When a match enters sudden death (draw condition), the system SHALL NOT modify any player's statistics.

#### Scenario: Sudden death triggered
- **GIVEN** both players meet victory conditions simultaneously
- **WHEN** the game detects sudden death
- **THEN** no player stats SHALL be updated, and `recordMatchResult` SHALL return immediately
