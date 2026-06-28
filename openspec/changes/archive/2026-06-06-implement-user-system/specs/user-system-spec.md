# User System Spec

## 1. User Registration

### ADDED: CreateUserRequest DTO
- `record CreateUserRequest(String email, String password, String displayName)`

### ADDED: UserService.register(CreateUserRequest)
- Auto-set username = email
- Save password in plain text
- Return UserResponse with empty players list

### ADDED: POST /api/users/register
- Request: `{ email, password, displayName }`
- Response: `UserResponse` (201 Created)

## 2. User Login

### ADDED: LoginRequest DTO
- `record LoginRequest(String email, String password)`

### ADDED: UserService.login(LoginRequest)
- Find by email, compare password with equals()
- If not matched: `ResponseStatusException(401)`
- Return UserResponse with linked players

### ADDED: POST /api/users/login
- Request: `{ email, password }`
- Response: `UserResponse` (200 OK) or 401

## 3. User Query

### ADDED: GET /api/users/{id}
- Returns UserResponse with linked players
- 404 if not found

### ADDED: GET /api/users
- Returns list of all UserResponse

## 4. Guest Player

### MODIFIED: CreateGuestPlayerRequest
- Added optional `String userId` to link to an existing User

### MODIFIED: GuestPlayerResponse
- Added `String ownerUserId` field

### ADDED: POST /api/players
- Creates guest player, optionally linked to user via userId
- Returns GuestPlayerResponse (201 Created)

### ADDED: GET /api/players?userId={id}
- Filters guest players by owner user

## 5. Deck Ownership

### MODIFIED: CreateDeckRequest
- Renamed `playerId` to `userId`

### MODIFIED: DeckService.createDeck(CreateDeckRequest)
- If userId is provided: validate user exists via UserJpaRepository
- Set `entity.setOwnerUser(userJpaRepository.getReferenceById(userId))`
- If userId is null: ownerUser remains null (seed decks)

### MODIFIED: DeckMapper.toEntity()
- No longer sets owner field; delegated to DeckService

### MODIFIED: DeckMapper.toResponse()
- Navigates `entity.getOwnerUser().getId()` instead of `entity.getOwnerUserId()`

## 6. Match Player Identity

### MODIFIED: CreateMatchRequest
- Added `String player1Id` (required), `String player2Id` (optional)
- `@JsonAlias("playerId")` maps to setPlayer1Id for backward compat

### MODIFIED: JoinMatchRequest
- Added `String playerId` (required)

### MODIFIED: MatchApplicationService.createMatch(CreateMatchRequest)
- Parse `player1Id` from UUID string
- Validate player exists (check UserJpaRepository then GuestPlayerJpaRepository)
- Set `playerKind = "USER"` or `"GUEST"`
- Same for `player2Id` if provided

### MODIFIED: MatchApplicationService.joinMatch(UUID, JoinMatchRequest)
- Parse `playerId` from UUID string
- Validate existence and set `playerKind`

## 7. Entity Relationships

### MODIFIED: UserEntity
- Added `@OneToMany(mappedBy = "ownerUser") List<GuestPlayerEntity> players`
- Added `@OneToMany(mappedBy = "ownerUser") List<DeckEntity> decks`
- Renamed `passwordHash` → `password`

### MODIFIED: GuestPlayerEntity
- Added `@ManyToOne @JoinColumn(name = "owner_user_id") UserEntity ownerUser`

### MODIFIED: DeckEntity
- Replaced `UUID ownerUserId` with `@ManyToOne @JoinColumn(name = "owner_user_id") UserEntity ownerUser`

### MODIFIED: GuestPlayerJpaRepository
- Added `List<GuestPlayerEntity> findByOwnerUserId(UUID ownerUserId)`
