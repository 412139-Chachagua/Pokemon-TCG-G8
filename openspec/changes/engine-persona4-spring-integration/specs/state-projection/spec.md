## ADDED Requirements

### Requirement: MatchQueryService builds public state

`MatchQueryService.buildPublicState(GameState state)` SHALL return a `PublicGameState` containing only the information visible to both players.

`PublicGameState` SHALL include:
- `matchId`, `phase`, `turnNumber`, `currentPlayerId`
- For each player: `playerId`, active Pokémon with HP and status, bench Pokémon (count and basic info), prize count, discard pile count, deck count
- Any events or status conditions visible to both players

The method SHALL NOT include hand cards or deck contents in the public state.

#### Scenario: buildPublicState hides hands

- **WHEN** calling `buildPublicState` on a valid `GameState` with both players having hand cards
- **THEN** the returned `PublicGameState` SHALL NOT contain any player's hand cards

#### Scenario: buildPublicState shows board

- **WHEN** calling `buildPublicState` on a valid `GameState`
- **THEN** the returned `PublicGameState` SHALL contain active Pokémon, bench counts, prize counts, and discard counts for both players

### Requirement: MatchQueryService builds private state

`MatchQueryService.buildPrivateState(GameState state, UUID playerId)` SHALL return a `PrivatePlayerState` containing the full state for the specified player, including their hand cards.

`PrivatePlayerState` SHALL include:
- All public state information
- The player's hand cards
- The player's prize cards (face-down information)

#### Scenario: buildPrivateState includes hand

- **WHEN** calling `buildPrivateState(state, playerId)` where `playerId` belongs to player 1
- **THEN** the returned `PrivatePlayerState` SHALL contain player 1's hand cards

#### Scenario: buildPrivateState returns null for invalid player

- **WHEN** calling `buildPrivateState(state, nonExistentPlayerId)`
- **THEN** the method SHALL return `null`

### Requirement: MatchStateResponse DTO

`MatchStateResponse` SHALL be a Java `record` with fields:
- `matchId`: UUID
- `publicState`: PublicGameState
- `privateState`: PrivatePlayerState

#### Scenario: MatchStateResponse is constructable

- **WHEN** creating `new MatchStateResponse(matchId, publicState, privateState)`
- **THEN** all three fields SHALL be accessible via accessor methods

### Requirement: MatchMapper maps entities to DTOs

`MatchMapper` SHALL implement the following conversions:
- `MatchEntity` + `List<MatchPlayerEntity>` → `MatchResponse`
- The `MatchResponse` SHALL contain: `id`, `status`, `currentPhase`, `turnNumber`, `currentPlayerId`, `firstPlayerId`, `winnerPlayerId`, `finishReason`, `players` (list of player summaries), `createdAt`

#### Scenario: MatchMapper creates MatchResponse

- **WHEN** calling `toMatchResponse(entity, players)` with a valid `MatchEntity` and player list
- **THEN** the returned `MatchResponse` SHALL have matching `id`, `status`, and player information

#### Scenario: MatchMapper handles null winner

- **WHEN** calling `toMatchResponse(entity, players)` where `entity.winnerPlayerId` is null
- **THEN** the returned `MatchResponse` SHALL have `winnerPlayerId` as null and `finishReason` as null
