## ADDED Requirements

### Requirement: Save action reuses deck validation
The system SHALL execute the same validations when saving a deck as when validating it. Save operations SHALL block persistence if validation fails.

#### Scenario: Save in create mode
- **WHEN** the user saves a new deck
- **THEN** the system SHALL run the full deck validation flow before creating it

#### Scenario: Save in edit mode
- **WHEN** the user saves an existing deck
- **THEN** the system SHALL run the full deck validation flow before updating it

#### Scenario: Validation fails on save
- **WHEN** validation finds an error
- **THEN** the system SHALL block persistence and return the validation errors

#### Scenario: Seed deck creation
- **WHEN** the system creates seed decks on startup
- **THEN** the system SHALL run the full deck validation flow before persisting each seed deck
