## ADDED Requirements

### Requirement: Create match endpoint

The system SHALL expose `POST /api/matches` to create a new match.

The endpoint SHALL accept a JSON body with:
- `player1Id` (UUID, required): ID of the first player
- `player1DeckId` (UUID, required): Deck ID for player 1
- `player2Id` (UUID or null, optional): ID of the second player (null for open match)
- `player2DeckId` (UUID or null, optional): Deck ID for player 2

The endpoint SHALL return HTTP 201 with a `MatchResponse` body containing the created match ID and status.

#### Scenario: Create a match with two players

- **WHEN** sending `POST /api/matches` with `{ "player1Id": "...", "player1DeckId": "...", "player2Id": "...", "player2DeckId": "..." }`
- **THEN** the system SHALL return HTTP 201 with a `MatchResponse` containing a non-null match ID and status `WAITING_FOR_PLAYERS` or `ACTIVE`

#### Scenario: Create an open match with one player

- **WHEN** sending `POST /api/matches` with `{ "player1Id": "...", "player1DeckId": "..." }` (no player2)
- **THEN** the system SHALL return HTTP 201 with status `WAITING_FOR_PLAYERS`

### Requirement: Join match endpoint

The system SHALL expose `POST /api/matches/{id}/join` to join an existing match.

The endpoint SHALL accept a JSON body with:
- `playerId` (UUID, required): ID of the player joining
- `deckId` (UUID, required): Deck ID for the joining player

The endpoint SHALL return HTTP 200 with a `MatchResponse`.

#### Scenario: Join an open match successfully

- **WHEN** sending `POST /api/matches/{id}/join` with `{ "playerId": "...", "deckId": "..." }` for a match in `WAITING_FOR_PLAYERS` status
- **THEN** the system SHALL execute setup (shuffle decks, deal hands, assign prizes, resolve mulligan) and return HTTP 200 with status `ACTIVE`

#### Scenario: Join a match that is already full

- **WHEN** sending `POST /api/matches/{id}/join` for a match in `ACTIVE` status
- **THEN** the system SHALL return HTTP 400

### Requirement: Get match state endpoint

The system SHALL expose `GET /api/matches/{id}/state?playerId={playerId}` to retrieve the current match state.

The endpoint SHALL return HTTP 200 with a `MatchStateResponse` containing:
- `matchId`: UUID
- `publicState`: `PublicGameState` visible to both players
- `privateState`: `PrivatePlayerState` containing hand cards only for the requesting player

#### Scenario: Get match state as participant

- **WHEN** sending `GET /api/matches/{id}/state?playerId={playerId}` where `playerId` is a participant
- **THEN** the system SHALL return HTTP 200 with `publicState` containing board state and `privateState` containing the player's hand

#### Scenario: Get match state as non-participant

- **WHEN** sending `GET /api/matches/{id}/state?playerId={playerId}` where `playerId` is NOT a participant
- **THEN** the system SHALL return HTTP 404
