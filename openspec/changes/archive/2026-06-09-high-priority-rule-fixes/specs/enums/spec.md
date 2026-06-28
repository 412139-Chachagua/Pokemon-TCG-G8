## ADDED Requirements

### Requirement: GameActionType.CHOOSE_KO_REPLACEMENT

`GameActionType` SHALL include `CHOOSE_KO_REPLACEMENT` as a valid action type for selecting a replacement Pokemon after a KO.

#### Scenario: CHOOSE_KO_REPLACEMENT is valid
- **WHEN** `GameActionType` is queried for `CHOOSE_KO_REPLACEMENT`
- **THEN** it SHALL return a valid enum value

### Requirement: GameEventType for KO replacement

`GameEventType` SHALL include `KO_REPLACEMENT_REQUIRED` and `KO_REPLACEMENT_DONE` as valid event types.

#### Scenario: KO_REPLACEMENT_REQUIRED is valid
- **WHEN** `GameEventType` is queried for `KO_REPLACEMENT_REQUIRED`
- **THEN** it SHALL return a valid enum value

#### Scenario: KO_REPLACEMENT_DONE is valid
- **WHEN** `GameEventType` is queried for `KO_REPLACEMENT_DONE`
- **THEN** it SHALL return a valid enum value

### Requirement: GameEventType for sudden death

`GameEventType` SHALL include `SUDDEN_DEATH_STARTED` as a valid event type.

#### Scenario: SUDDEN_DEATH_STARTED is valid
- **WHEN** `GameEventType` is queried for `SUDDEN_DEATH_STARTED`
- **THEN** it SHALL return a valid enum value

### Requirement: GameEventType for confusion self-hit

`GameEventType` SHALL include `CONFUSION_SELF_HIT` as a valid event type.

#### Scenario: CONFUSION_SELF_HIT is valid
- **WHEN** `GameEventType` is queried for `CONFUSION_SELF_HIT`
- **THEN** it SHALL return a valid enum value

### Requirement: GameEventType for stadium

`GameEventType` SHALL include `STADIUM_PLAYED` and `STADIUM_REMOVED` as valid event types.

#### Scenario: STADIUM_PLAYED is valid
- **WHEN** `GameEventType` is queried for `STADIUM_PLAYED`
- **THEN** it SHALL return a valid enum value

#### Scenario: STADIUM_REMOVED is valid
- **WHEN** `GameEventType` is queried for `STADIUM_REMOVED`
- **THEN** it SHALL return a valid enum value
