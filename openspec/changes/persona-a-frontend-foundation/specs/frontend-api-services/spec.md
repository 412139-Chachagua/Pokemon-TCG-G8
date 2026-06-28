## ADDED Requirements

### Requirement: CardApiService typed correctly
The system SHALL type `CardApiService.getCardById()` to return `Observable<CardDetailResponse>` (not `Observable<unknown>`). `searchCards()` SHALL accept `query`, `supertype`, `setCode`, `page`, `size` params.

#### Scenario: getCardById returns typed response
- **WHEN** the frontend calls `cardApiService.getCardById('xy1-1')`
- **THEN** the observable SHALL emit `CardDetailResponse`

#### Scenario: searchCards accepts correct params
- **WHEN** the frontend calls `cardApiService.searchCards({ query: 'charizard', supertype: 'POKEMON', setCode: 'xy1', page: 0, size: 20 })`
- **THEN** the request SHALL include all params as query parameters

### Requirement: DeckApiService complete CRUD
The system SHALL implement `DeckApiService` with: `listByPlayer(playerId)`, `get(id)`, `create(req)`, `update(id, req)`, `delete(id)`, `validate(id)`, `validateCards(req)`.

#### Scenario: listByPlayer calls correct endpoint
- **WHEN** the frontend calls `deckApiService.listByPlayer('player-1')`
- **THEN** it SHALL call `GET /decks?playerId=player-1`

#### Scenario: validate calls POST without body
- **WHEN** the frontend calls `deckApiService.validate('deck-id')`
- **THEN** it SHALL call `POST /decks/deck-id/validate` with no request body

#### Scenario: delete returns void
- **WHEN** the frontend calls `deckApiService.delete('deck-id')`
- **THEN** it SHALL call `DELETE /decks/deck-id` and return `Observable<void>`

### Requirement: MatchApiService typed correctly
The system SHALL type `MatchApiService` with: `createMatch({ playerName, deckId })`, `joinMatch(id, { playerName, deckId })`, `getState(id, playerId)`, `sendAction(id, req)`.

#### Scenario: createMatch sends correct body
- **WHEN** the frontend calls `matchApiService.createMatch({ playerName: 'Santi', deckId: 'deck-1' })`
- **THEN** it SHALL POST to `/matches` with body `{ playerName: 'Santi', deckId: 'deck-1' }`

#### Scenario: getState returns MatchStateResponse
- **WHEN** the frontend calls `matchApiService.getState('match-1', 'player-1')`
- **THEN** it SHALL call `GET /matches/match-1/state?playerId=player-1` and return `Observable<MatchStateResponse>`

#### Scenario: sendAction returns GameActionResponse
- **WHEN** the frontend calls `matchApiService.sendAction('match-1', actionRequest)`
- **THEN** it SHALL return `Observable<GameActionResponse>`

### Requirement: GameActionDispatcherService adapted
The system SHALL update `GameActionDispatcherService` payload fields to match backend: `ATTACH_ENERGY` uses `handIndex` + `targetPokemonInstanceId` (not `energyCardInstanceId`), `DECLARE_ATTACK` uses `attackIndex` + `targetPokemonInstanceId`, `RETREAT_ACTIVE` uses `benchIndex`.

#### Scenario: attachEnergy payload correct
- **WHEN** the frontend dispatches `ATTACH_ENERGY`
- **THEN** the payload SHALL be `{ handIndex: number, targetPokemonInstanceId: string }`

#### Scenario: retreatActive payload correct
- **WHEN** the frontend dispatches `RETREAT_ACTIVE`
- **THEN** the payload SHALL be `{ benchIndex: number }`

### Requirement: Facades adapted to corrected models
The system SHALL update `DeckBuilderFacadeService` and `MatchFacadeService` to use the corrected model types.

#### Scenario: DeckBuilderFacadeService uses corrected types
- **WHEN** the frontend uses `DeckBuilderFacadeService`
- **THEN** it SHALL work with `DeckCardEntry` (expanded) and `DeckValidationModel`

#### Scenario: MatchFacadeService uses corrected types
- **WHEN** the frontend uses `MatchFacadeService`
- **THEN** it SHALL work with `MatchResponse` (4 fields) and `MatchStateResponse`
