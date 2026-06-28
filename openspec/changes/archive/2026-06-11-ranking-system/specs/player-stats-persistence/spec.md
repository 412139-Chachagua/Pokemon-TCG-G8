## ADDED Requirements

### Requirement: PlayerStatsEntity persists wins, losses and streaks per player
The system SHALL persist player statistics in a `player_stats` table with one row per player.

#### Scenario: Player wins first match
- **GIVEN** a player has no existing stats
- **WHEN** the player wins a match
- **THEN** a `player_stats` row SHALL be created with `totalWins=1`, `totalLosses=0`, `currentWinStreak=1`, `maxWinStreak=1`

#### Scenario: Player loses first match
- **GIVEN** a player has no existing stats
- **WHEN** the player loses a match
- **THEN** a `player_stats` row SHALL be created with `totalWins=0`, `totalLosses=1`, `currentWinStreak=0`, `maxWinStreak=0`

#### Scenario: Subsequent win increments streak
- **GIVEN** a player has `totalWins=3`, `currentWinStreak=2`, `maxWinStreak=2`
- **WHEN** the player wins another match
- **THEN** `totalWins` SHALL be 4, `currentWinStreak` SHALL be 3, `maxWinStreak` SHALL be 3

#### Scenario: Loss resets streak
- **GIVEN** a player has `totalWins=5`, `currentWinStreak=3`, `maxWinStreak=5`
- **WHEN** the player loses a match
- **THEN** `totalLosses` SHALL increment, `currentWinStreak` SHALL be 0, `maxWinStreak` SHALL remain 5

### Requirement: Winner is persisted to MatchEntity on match finish
When a match finishes through the game engine, the system SHALL update `MatchEntity` with the winner, finish reason, and finish timestamp.

#### Scenario: Match ends by knockout
- **GIVEN** an active match between Alice and Bob
- **WHEN** Alice knocks out Bob's last Pokemon
- **THEN** `MatchEntity` SHALL have `winner_player_id` set to Alice's ID, `finish_reason` set to "KNOCKOUT", `finished_at` set, and `status` set to "FINISHED"

#### Scenario: Sudden death does not write winner
- **GIVEN** an active match where both players meet victory conditions simultaneously
- **WHEN** sudden death is triggered
- **THEN** `MatchEntity` SHALL NOT be updated with winner or finish info; the match continues with 1 prize card
