## Why

The Pokemon TCG backend needs user management to support authenticated users, guest players linked to users, and match creation using real player identities instead of random UUIDs. Currently, matches generate random UUIDs for players, decks are tied to raw UUIDs, and there is no user registration or login flow.

## What Changes

- Create User DTOs: `CreateUserRequest`, `LoginRequest`, `UserResponse`
- Create GuestPlayer DTOs: `CreateGuestPlayerRequest`, `GuestPlayerResponse`
- Modify `CreateDeckRequest`: rename `playerId` to `userId`
- Modify `CreateMatchRequest`: add required `player1Id` and optional `player2Id`
- Modify `JoinMatchRequest`: add required `playerId`
- Modify `UserEntity`: add `@OneToMany` for players and decks, rename `passwordHash` to `password`
- Modify `GuestPlayerEntity`: add `@ManyToOne ownerUser`
- Modify `DeckEntity`: replace `UUID ownerUserId` with `@ManyToOne UserEntity ownerUser`
- Add `findByOwnerUserId` to `GuestPlayerJpaRepository`
- Create `UserService` with register, login, getById, listAll, existsById
- Create `UserController` with 4 REST endpoints
- Create `GuestPlayerService` with create, listByUserId, listAll, getById
- Create `GuestPlayerController` with 3 REST endpoints
- Modify `DeckMapper` to navigate `ownerUser` relationship
- Modify `DeckService` to validate user existence and set `ownerUser`
- Modify `MatchApplicationService` to use player IDs from request, validate existence, set `playerKind`

## Capabilities

### New Capabilities

- `user-registration`: Register users with email, password, displayName
- `user-login`: Login with email + plain-text password verification
- `user-crud`: List and get users by ID
- `guest-player-management`: Create guest players optionally linked to users, list by userId
- `match-player-validation`: Match creation validates player IDs exist before assigning

### Modified Capabilities

- `deck-creation`: Accepts `userId` instead of generic `playerId`, validates user exists
- `match-creation`: Uses real player IDs from request instead of `UUID.randomUUID()`

## Impact

- New packages: `services/users/`, `controllers/users/`, `dtos/users/`, `dtos/players/`, `services/players/`, `controllers/players/`
- Modified entities affect JPA schema: `users`, `guest_players`, `decks` tables gain proper FK relationships
- No impact on: Game Engine, card catalog, WebSocket, frontend, contracts
- Match responses are unchanged — MatchMapper and MatchQueryService still work with UUIDs

## Mandatory Context Files

- `/docs/contracts_ai/00-contract-index.md`
- `/docs/contracts_ai/01-project-scope-contract.md`
- `/docs/contracts_ai/02-project-structure-contract.md`
- `/docs/contracts_ai/05-deck-contract.md`
- `/docs/contracts_ai/13-rest-api-contract.md`
