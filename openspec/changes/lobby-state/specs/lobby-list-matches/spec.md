## ADDED Requirements

### Requirement: Backend provides GET /api/matches endpoint
The system SHALL expose a `GET /api/matches` endpoint that returns a list of matches filtered by status.

#### Scenario: List WAITING matches
- **WHEN** a client sends `GET /api/matches` (no query params) or `GET /api/matches?status=WAITING`
- **THEN** the server SHALL respond with `200 OK` and a JSON array of matches whose status is `WAITING`

#### Scenario: List matches by other status
- **WHEN** a client sends `GET /api/matches?status=IN_PROGRESS`
- **THEN** the server SHALL respond with `200 OK` and a JSON array of matches whose status is `IN_PROGRESS`

#### Scenario: No matches found
- **WHEN** there are no matches with the requested status
- **THEN** the server SHALL respond with `200 OK` and an empty JSON array `[]`

#### Scenario: Response format
- **WHEN** the server returns matches
- **THEN** each match SHALL include `id`, `status`, `currentPhase`, `turnNumber`, `currentPlayerId`, `firstPlayerId`, `winnerPlayerId`, `finishReason`, `players` (array with `playerId`, `side`, `displayName`), and `createdAt`, following the contract in `docs/contracts/13-rest-api-contract.md`

### Requirement: Frontend MatchListComponent loads matches from API
The `MatchListComponent` SHALL fetch available matches from the backend on initialization and on user refresh.

#### Scenario: Component loads matches on init
- **WHEN** `MatchListComponent` is initialized
- **THEN** it SHALL call `MatchApiService.listMatches()` and display the returned matches

#### Scenario: User refreshes match list
- **WHEN** the user clicks refresh
- **THEN** the component SHALL call `MatchApiService.listMatches()` again and update the displayed list

#### Scenario: API error on load
- **WHEN** the API request fails
- **THEN** the component SHALL display an empty list
