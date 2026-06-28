## ADDED Requirements

### Requirement: StatePersisterAdapter serializes GameState to JSON

`StatePersisterAdapter` SHALL implement `StatePersisterPort` and SHALL use `ObjectMapper` to serialize `GameState` to a JSON string.

#### Scenario: saveState persists GameState

- **WHEN** calling `saveState(matchId, gameState)`
- **THEN** the system SHALL serialize `gameState` to JSON via `ObjectMapper.writeValueAsString()` and SHALL save a new `MatchStateEntity` with the match ID, serialized state, and incremented version number

#### Scenario: saveState increments version

- **WHEN** calling `saveState` twice for the same match ID
- **THEN** the second entity SHALL have `version = firstEntity.version + 1`

### Requirement: StatePersisterAdapter loads GameState from JSON

`StatePersisterAdapter.loadState(UUID matchId)` SHALL return the most recent `GameState` for the given match ID.

#### Scenario: loadState returns last saved state

- **WHEN** calling `loadState(matchId)` after `saveState` was called
- **THEN** the system SHALL query `findTopByMatchIdOrderByVersionDesc(matchId)` from `MatchStateJpaRepository`, deserialize the JSON via `ObjectMapper.readValue()`, and return the `GameState`

#### Scenario: loadState returns null for non-existent match

- **WHEN** calling `loadState(nonExistentMatchId)`
- **THEN** the system SHALL return `null`

### Requirement: MatchStateJpaRepository has version query

`MatchStateJpaRepository` SHALL define the query method `findTopByMatchIdOrderByVersionDesc(UUID matchId)` returning `Optional<MatchStateEntity>`.

#### Scenario: Query returns highest version

- **WHEN** calling `findTopByMatchIdOrderByVersionDesc(matchId)` on a match with 3 saved states (versions 1, 2, 3)
- **THEN** the result SHALL be the entity with version 3

### Requirement: GameEngineConfig provides SetupManager bean

`GameEngineConfig` SHALL define a `@Bean` method that creates `SetupManager` with `EventPublisherPort`, `CardLookupPort`, and `RandomizerPort`.

#### Scenario: SetupManager bean is available

- **WHEN** loading the Spring application context
- **THEN** a `SetupManager` bean SHALL be available for injection

### Requirement: GlobalExceptionHandler handles IllegalArgumentException

`GlobalExceptionHandler` SHALL handle `IllegalArgumentException` by returning HTTP 400 with an `ErrorApi` body.

The `ErrorApi` body SHALL include a `path` field with the request path and an optional `details` map for field-level validation errors.

#### Scenario: IllegalArgumentException returns 400

- **WHEN** a service throws `IllegalArgumentException`
- **THEN** the handler SHALL return HTTP 400 with `ErrorApi` containing status 400 and the exception message

#### Scenario: Generic exception returns 500

- **WHEN** an unhandled exception type is thrown
- **THEN** the handler SHALL return HTTP 500 with `ErrorApi`
