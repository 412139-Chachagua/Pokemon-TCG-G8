## ADDED Requirements

### Requirement: GET /api/ranking returns ordered ranking list
The system SHALL expose a `GET /api/ranking` endpoint that returns all players ordered by total wins descending, then by max win streak descending.

#### Scenario: Empty ranking
- **WHEN** no matches have been played
- **THEN** the server SHALL respond with `200 OK` and an empty JSON array `[]`

#### Scenario: Ranking with multiple players
- **GIVEN** player Alice has 10 wins and player Bob has 5 wins
- **WHEN** a client sends `GET /api/ranking`
- **THEN** the server SHALL respond with `200 OK` and a JSON array where Alice appears before Bob

#### Scenario: Ranking entry format
- **WHEN** the server returns ranking entries
- **THEN** each entry SHALL contain `rank`, `playerId`, `displayName`, `totalWins`, `totalLosses`, `winRate`, `currentWinStreak`, and `maxWinStreak`

### Requirement: GET /api/players/{id}/stats returns player statistics
The system SHALL expose a `GET /api/players/{id}/stats` endpoint that returns statistics for a specific player.

#### Scenario: Player has played matches
- **GIVEN** player Charlie has 7 wins and 3 losses
- **WHEN** a client sends `GET /api/players/{id}/stats` with Charlie's ID
- **THEN** the server SHALL respond with `200 OK` and a JSON object with `playerId`, `displayName`, `totalWins` (7), `totalLosses` (3), `currentWinStreak`, and `maxWinStreak`

#### Scenario: Player has never played
- **WHEN** a client requests stats for a player who has never played
- **THEN** the server SHALL respond with `200 OK` and all stat fields set to 0

#### Scenario: Non-existent player
- **WHEN** a client requests stats for a non-existent player ID
- **THEN** the server SHALL respond with `200 OK` and `displayName` set to "Unknown"
