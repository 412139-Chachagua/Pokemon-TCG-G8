## MODIFIED Requirements

### Requirement: PokemonInPlay model
`PokemonInPlay` SHALL include `Set<String> abilitiesUsedThisTurn` field for tracking ability usage per turn.

#### Scenario: New Pokemon has empty abilities set
- **WHEN** a `PokemonInPlay` instance is created
- **THEN** `abilitiesUsedThisTurn` SHALL be initialized as an empty `HashSet<String>`

#### Scenario: Serialized state includes abilities
- **WHEN** `GameState` is persisted to JSON
- **THEN** `abilitiesUsedThisTurn` SHALL be included in the serialized state

#### Scenario: Deserialized state restores abilities
- **WHEN** `GameState` is loaded from JSON
- **THEN** `abilitiesUsedThisTurn` SHALL be restored correctly
