## ADDED Requirements

### Requirement: CreateMatchRequest has Jakarta validation
The `CreateMatchRequest` DTO SHALL have `@NotBlank` and `@NotNull` validation annotations on required fields.

#### Scenario: Valid request passes
- **WHEN** a valid `CreateMatchRequest` is submitted with all required fields
- **THEN** validation SHALL pass

#### Scenario: Missing player1Name returns 400
- **WHEN** `player1Name` is blank
- **THEN** the server SHALL respond with 400 Bad Request

#### Scenario: Missing player1Id returns 400
- **WHEN** `player1Id` is null
- **THEN** the server SHALL respond with 400 Bad Request

### Requirement: JoinMatchRequest has Jakarta validation
The `JoinMatchRequest` DTO SHALL have `@NotBlank` and `@NotNull` validation annotations on required fields.

#### Scenario: Valid request passes
- **WHEN** a valid `JoinMatchRequest` is submitted with all required fields
- **THEN** validation SHALL pass

#### Scenario: Missing playerName returns 400
- **WHEN** `playerName` is blank
- **THEN** the server SHALL respond with 400 Bad Request

#### Scenario: Missing deckId returns 400
- **WHEN** `deckId` is null
- **THEN** the server SHALL respond with 400 Bad Request

### Requirement: GameActionRequest has Jakarta validation
The `GameActionRequest` DTO SHALL have `@NotBlank` and `@NotNull` validation annotations on required fields.

#### Scenario: Valid request passes
- **WHEN** a valid `GameActionRequest` is submitted with all required fields
- **THEN** validation SHALL pass

#### Scenario: Missing type returns 400
- **WHEN** `type` is blank
- **THEN** the server SHALL respond with 400 Bad Request

#### Scenario: Missing playerId returns 400
- **WHEN** `playerId` is null
- **THEN** the server SHALL respond with 400 Bad Request

### Requirement: Controllers validate with @Valid
The `MatchController` and `GameActionController` SHALL use `@Valid` on request body parameters and the `GlobalExceptionHandler` SHALL handle `MethodArgumentNotValidException` returning 400.

#### Scenario: Invalid request returns structured error
- **WHEN** a controller receives an invalid request body
- **THEN** the server SHALL respond with 400 Bad Request and a descriptive error message
