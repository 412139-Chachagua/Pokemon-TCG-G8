## MODIFIED Requirements

### Requirement: GameActionType enum
`GameActionType` SHALL include `USE_ABILITY` as a valid action type.

#### Scenario: USE_ABILITY is valid
- **WHEN** `GameActionType` is queried for `USE_ABILITY`
- **THEN** it SHALL return a valid enum value

### Requirement: GameEventType enum
`GameEventType` SHALL include `ABILITY_USED` and `ABILITY_BLOCKED` as valid event types.

#### Scenario: ABILITY_USED is valid
- **WHEN** `GameEventType` is queried for `ABILITY_USED`
- **THEN** it SHALL return a valid enum value

#### Scenario: ABILITY_BLOCKED is valid
- **WHEN** `GameEventType` is queried for `ABILITY_BLOCKED`
- **THEN** it SHALL return a valid enum value

### Requirement: ErrorCode enum
`ErrorCode` SHALL include `ABILITY_NOT_FOUND`, `ABILITY_ALREADY_USED`, and `POKEMON_CANNOT_USE_ABILITY` as valid error codes.

#### Scenario: ABILITY_NOT_FOUND is valid
- **WHEN** `ErrorCode` is queried for `ABILITY_NOT_FOUND`
- **THEN** it SHALL return a valid enum value

#### Scenario: ABILITY_ALREADY_USED is valid
- **WHEN** `ErrorCode` is queried for `ABILITY_ALREADY_USED`
- **THEN** it SHALL return a valid enum value

#### Scenario: POKEMON_CANNOT_USE_ABILITY is valid
- **WHEN** `ErrorCode` is queried for `POKEMON_CANNOT_USE_ABILITY`
- **THEN** it SHALL return a valid enum value
