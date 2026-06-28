## ADDED Requirements

### Requirement: WebSocket publishes private player state
The system SHALL publish each player's private state to a dedicated queue destination so that hidden information (hand, prizes) is only visible to the owning player.

#### Scenario: Private state published to /queue
- WHEN an action modifies a player's hand
- THEN an event SHALL be published to `/queue/matches/{matchId}/{playerId}` with the updated `PrivatePlayerState`
- AND the event SHALL NOT be published to `/topic/matches/{matchId}/events`

#### Scenario: Public events exclude private data
- WHEN an action is executed
- THEN the event on `/topic/matches/{matchId}/events` SHALL contain only `PublicGameState`
- AND SHALL NOT include `PrivatePlayerState` or hand/prize card IDs

#### Scenario: Both players receive their own private state
- WHEN an action modifies the game state
- THEN player 1 SHALL receive their own `PrivatePlayerState` on `/queue/matches/{id}/{player1Id}`
- AND player 2 SHALL receive their own `PrivatePlayerState` on `/queue/matches/{id}/{player2Id}`
