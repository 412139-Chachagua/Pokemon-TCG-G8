## ADDED Requirements

### Requirement: Backend folder structure follows contract

The backend SHALL follow the canonical folder structure defined in `/docs/contracts_ai/02-project-structure-contract.md` with root package `com.pokemontcg`.

#### Scenario: Backend root package created
- **WHEN** the backend project is initialized
- **THEN** the root package is `com.pokemontcg`

#### Scenario: Backend sub-packages created
- **WHEN** backend sub-packages are created
- **THEN** the following packages exist: config, common, cards, decks, matches, engine

### Requirement: Frontend folder structure follows contract

The frontend SHALL follow the canonical folder structure defined in `/docs/contracts_ai/02-project-structure-contract.md` under `src/app/`.

#### Scenario: Frontend root created
- **WHEN** the frontend project is initialized
- **THEN** the root folder is `/frontend` with Angular 21+ structure

#### Scenario: Frontend core modules created
- **WHEN** frontend core modules are created
- **THEN** the following folders exist: core/api, core/websocket, shared/models, shared/components, features/lobby, features/match, features/decks, features/auth

### Requirement: Backend compiles successfully

The backend Maven project SHALL compile without errors after implementation.

#### Scenario: Maven build succeeds
- **WHEN** `mvn compile` is run in the backend directory
- **THEN** the build completes with no errors

#### Scenario: Spring context loads
- **WHEN** the Spring Boot application test is run
- **THEN** the application context loads successfully

### Requirement: Frontend builds successfully

The frontend Angular project SHALL build without errors after implementation.

#### Scenario: Angular build succeeds
- **WHEN** `npm run build` is run in the frontend directory
- **THEN** the build completes with no errors

### Requirement: Enums follow contract naming

All enum types SHALL use the canonical names and values defined in `/docs/contracts_ai/03-enums-contract.md`.

#### Scenario: Backend enums present
- **WHEN** backend enum classes are created
- **THEN** the following enums exist: MatchStatus, TurnPhase, PlayerSide, CardSupertype, PokemonStage, EnergyType, TrainerType, SpecialCondition, GameActionType, GameEventType, FinishReason, DeckValidationError

### Requirement: Placeholder classes created

Placeholder classes SHALL exist for all DTOs and module folders to enable compilation.

#### Scenario: DTO placeholders exist
- **WHEN** DTO folders are checked
- **THEN** placeholder files exist in: cards/api/dto, decks/api/dto, matches/api/dto

#### Scenario: Engine placeholder exists
- **WHEN** engine package is checked
- **THEN** placeholder classes exist for: GameEngine, GameState, PlayerState, BoardState, GameAction, GameEvent

### Requirement: Frontend placeholder components created

Placeholder components SHALL exist for lobby, match and decks features as defined in contract.

#### Scenario: Lobby placeholder exists
- **WHEN** lobby feature is checked
- **THEN** lobby-page component placeholder exists in features/lobby/

#### Scenario: Match placeholder exists
- **WHEN** match feature is checked
- **THEN** match-page component placeholder exists in features/match/pages/

#### Scenario: Decks placeholder exists
- **WHEN** decks feature is checked
- **THEN** deck-list-page and deck-builder-page component placeholders exist in features/decks/pages/

### Requirement: Auth feature postponed

The auth feature SHALL NOT be implemented. A README.md placeholder SHALL exist in features/auth/.

#### Scenario: Auth README exists
- **WHEN** features/auth folder is checked
- **THEN** a README.md file exists indicating auth is postponed