## ADDED Requirements

### Requirement: Contract reflects real backend package root
The contract SHALL use `ar.edu.utn.frc.tup.piii` as the backend package root, matching the actual source code.
The contract SHALL name the main application class `Application.java`, not `PokemonTcgApplication.java`.

#### Scenario: Package root is ar.edu.utn.frc.tup.piii
- **WHEN** inspecting the backend structure section of the contract
- **THEN** the package root SHALL be `ar.edu.utn.frc.tup.piii`
- **AND** all Java file paths SHALL start with `ar/edu/utn/frc/tup/piii/`

#### Scenario: Main class is Application.java
- **WHEN** inspecting the first file listed under the backend structure
- **THEN** the file SHALL be `Application.java` (not `PokemonTcgApplication.java`)

### Requirement: Contract lists all real backend packages and files
The contract SHALL list every Java file that exists under `BE/src/main/java/ar/edu/utn/frc/tup/piii/`, organized by package.
The contract SHALL NOT list files or packages that do not exist in the codebase.

#### Scenario: All real packages are present
- **WHEN** traversing the contract's backend structure
- **THEN** every package that exists under `ar/edu/utn/frc/tup/piii/` SHALL be listed
- **AND** no packages that do not exist in the codebase SHALL be listed

#### Scenario: Config package matches reality
- **WHEN** inspecting the `configs/` section in the contract
- **THEN** it SHALL list: `GameEngineConfig.java`, `MappersConfig.java`, `SpringDocConfig.java`, `WebSocketConfig.java`
- **AND** it SHALL NOT list `CorsConfig.java` or `OpenApiConfig.java` (they do not exist)

#### Scenario: Exceptions are in flat package
- **WHEN** inspecting the exceptions section in the contract
- **THEN** exceptions SHALL be listed under a flat `exceptions/` package at root level
- **AND** SHALL include: `DomainException.java`, `NotFoundException.java`, `ValidationException.java`

#### Scenario: DTOs are in flat dtos/ package by feature
- **WHEN** inspecting the DTOs section in the contract
- **THEN** DTOs SHALL be organized under `dtos/cards/`, `dtos/common/`, `dtos/decks/`, `dtos/matches/`
- **AND** SHALL NOT be nested inside `cards/api/dto/`, `decks/api/dto/`, or `matches/api/dto/`

#### Scenario: Entities and repos are in repositories/
- **WHEN** inspecting the persistence section in the contract
- **THEN** entities SHALL be under `repositories/entities/`
- **AND** JPA repositories SHALL be under `repositories/jpa/`

#### Scenario: Mappers are in flat mappers/ package
- **WHEN** inspecting the mappers section in the contract
- **THEN** mappers SHALL be under `mappers/decks/` and `mappers/matches/`

#### Scenario: GlobalExceptionHandler is in advice/
- **WHEN** inspecting the contract for error handling
- **THEN** `GlobalExceptionHandler.java` SHALL be listed under `advice/`

#### Scenario: Engine structure matches reality
- **WHEN** inspecting the engine section in the contract
- **THEN** it SHALL include: `PlayerSide.java`, `SpecialCondition.java`, and `ports/impl/` with `CardLookupAdapter.java`, `RandomizerAdapter.java`, `StatePersisterAdapter.java`

### Requirement: Contract reflects real frontend structure
The contract SHALL reflect the actual frontend structure under `FE/src/app/`, using Angular standalone conventions.

#### Scenario: No .component suffix in filenames
- **WHEN** inspecting the frontend structure in the contract
- **THEN** component files SHALL use names like `card-catalog-page.ts` (not `card-catalog-page.component.ts`)

#### Scenario: routes.ts per feature
- **WHEN** inspecting each feature folder in the contract
- **THEN** each feature (cards, decks, lobby, match) SHALL include a `routes.ts` file

#### Scenario: Only existing features are listed
- **WHEN** inspecting the features section in the contract
- **THEN** only these features SHALL be listed: `cards`, `decks`, `lobby`, `match`
- **AND** `auth/` SHALL NOT be listed (it does not exist)

#### Scenario: No non-existent sub-folders
- **WHEN** inspecting the `match` feature section in the contract
- **THEN** only these sub-folders SHALL be listed: `pages/`, `services/`, `routes.ts`
- **AND** sub-folders like `board/`, `player-area/`, `opponent-area/`, `active-pokemon-slot/`, `bench-zone/`, `hand-zone/`, `prize-zone/`, `discard-zone/`, `action-panel/`, `game-log/` SHALL NOT be listed

#### Scenario: Shared only has models
- **WHEN** inspecting the `shared/` section in the contract
- **THEN** it SHALL only list `models/`
- **AND** `components/` SHALL NOT be listed

#### Scenario: Core only has api and websocket
- **WHEN** inspecting the `core/` section in the contract
- **THEN** it SHALL only list `api/` and `websocket/`
- **AND** `interceptors/` and `error/` SHALL NOT be listed
