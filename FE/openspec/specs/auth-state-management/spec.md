### Requirement: AuthService manages authentication state with signals
The system SHALL provide an `AuthService` singleton (`providedIn: 'root'`) that uses Angular signals to manage authentication state. It SHALL expose readonly signals: `user`, `player`, `playerId` (computed), and `isAuthenticated`.

#### Scenario: Initial state
- **WHEN** app starts and no localStorage data exists
- **THEN** `user()` is null, `player()` is null, `playerId()` is null, `isAuthenticated()` is false

#### Scenario: After successful registration
- **WHEN** user registers and backend returns UserResponse
- **THEN** `user()` contains the UserResponse, `isAuthenticated()` is true
- **AND** `loadPlayer()` is called with the userId to fetch and set `player()`
- **AND** `playerId()` returns the player's id

#### Scenario: playerId computed from player
- **WHEN** `player()` is set to a PlayerResponse with id "abc-123"
- **THEN** `playerId()` returns "abc-123"

#### Scenario: playerId null when no player
- **WHEN** `player()` is null
- **THEN** `playerId()` returns null

### Requirement: AuthService persists session in localStorage
The system SHALL persist `user` and `player` data to localStorage on login/register, and restore them on app initialization.

#### Scenario: Session saved to localStorage
- **WHEN** `register()` or `login()` succeeds
- **THEN** `user` and `player` objects are serialized to localStorage under keys `auth_user` and `auth_player`

#### Scenario: Session restored on startup
- **WHEN** app initializes and localStorage contains `auth_user` and `auth_player`
- **THEN** `user()` and `player()` signals are populated from stored data
- **AND** `isAuthenticated()` returns true

#### Scenario: Logout clears localStorage
- **WHEN** user calls `logout()`
- **THEN** `user()`, `player()` are set to null, `isAuthenticated()` is false
- **AND** localStorage keys `auth_user` and `auth_player` are removed

### Requirement: AuthService provides login method
The system SHALL provide a `login(email, password)` method that calls POST `/api/users/login` and updates state.

#### Scenario: Successful login
- **WHEN** `login()` is called with valid credentials
- **THEN** system calls POST `/api/users/login` with `{ email, password }`
- **AND** updates `user()` signal with the response
- **AND** calls `loadPlayer()` to fetch associated player
- **AND** persists to localStorage

#### Scenario: Failed login
- **WHEN** `login()` is called with invalid credentials
- **THEN** system returns an Observable that errors with 401 status
- **AND** state remains unchanged (user/player stay null)

### Requirement: Facade services obtain playerId from AuthService
MatchFacadeService and DeckBuilderFacadeService SHALL inject AuthService and use `authService.playerId()` to obtain the player ID. The `playerId` parameter in their methods SHALL become optional for backward compatibility.

#### Scenario: MatchFacadeService.createMatch without playerId parameter
- **WHEN** `createMatch(player1Name, player1DeckId)` is called without passing playerId
- **THEN** system uses `authService.playerId()` as `player1Id` in the CreateMatchRequest
- **AND** request is sent to POST `/api/matches`

#### Scenario: MatchFacadeService.createMatch with explicit playerId
- **WHEN** `createMatch(player1Name, player1DeckId, explicitPlayerId)` is called with a playerId
- **THEN** system uses the explicit playerId instead of AuthService value

#### Scenario: DeckBuilderFacadeService.createDeck
- **WHEN** `createDeck(name)` is called
- **THEN** system uses `authService.playerId()` as `playerId` in the CreateDeckRequest
- **AND** request is sent to POST `/api/decks`

#### Scenario: playerId is null
- **WHEN** `createMatch()` or `createDeck()` is called and `authService.playerId()` returns null
- **THEN** request is sent with `playerId: null` (backend will reject if required)
