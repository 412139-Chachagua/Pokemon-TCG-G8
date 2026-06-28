## ADDED Requirements

### Requirement: AbilityDefinition domain model
The system SHALL define `AbilityDefinition` as a domain class in `cards/domain/` with fields: `name` (String), `text` (String), `type` (AbilityType).

#### Scenario: AbilityDefinition creation
- **WHEN** a card entity has abilities stored as JSON
- **THEN** the system SHALL deserialize each ability into an `AbilityDefinition` instance with name, text, and type fields populated

### Requirement: AbilityType enum
The system SHALL define `AbilityType` enum with values: `ABILITY`, `POKEMON_POWER`, `POKEMON_BODY`.

#### Scenario: Ability type from API
- **WHEN** an ability has `type: "Ability"` in the API response
- **THEN** the system SHALL map it to `AbilityType.ABILITY`

#### Scenario: Pokemon Power type from API
- **WHEN** an ability has `type: "Poké-Power"` in the API response
- **THEN** the system SHALL map it to `AbilityType.POKEMON_POWER`

#### Scenario: Pokemon Body type from API
- **WHEN** an ability has `type: "Poké-Body"` in the API response
- **THEN** the system SHALL map it to `AbilityType.POKEMON_BODY`

### Requirement: PokemonCardDefinition abilities field
The system SHALL add `List<AbilityDefinition> abilities` field to `PokemonCardDefinition`.

#### Scenario: Pokemon with abilities
- **WHEN** a `PokemonCardDefinition` is loaded from a card entity with abilities JSON
- **THEN** the `abilities` field SHALL contain the deserialized list of `AbilityDefinition` objects

#### Scenario: Pokemon without abilities
- **WHEN** a `PokemonCardDefinition` is loaded from a card entity with null or empty abilities JSON
- **THEN** the `abilities` field SHALL be an empty list

### Requirement: Hydration from DB to domain
The system SHALL modify `CardLookupAdapter.toPokemon()` to read `CardEntity.getAbilities()` JSON, deserialize to `AbilityDto`, and map to `List<AbilityDefinition>`.

#### Scenario: Abilities hydrated during card lookup
- **WHEN** `CardLookupPort.getCardById()` is called for a Pokemon card with abilities
- **THEN** the returned `PokemonCardDefinition` SHALL include hydrated abilities

#### Scenario: Abilities field is never null
- **WHEN** a Pokemon card is hydrated from the database
- **THEN** the `abilities` field SHALL never be null (empty list if no abilities)

### Requirement: REST exposure of abilities
The system SHALL expose abilities in `CardDetailResponse` with fields: `name`, `text`, `type`.

#### Scenario: Card detail includes abilities
- **WHEN** `GET /api/cards/{cardId}` is called for a Pokemon card with abilities
- **THEN** the response SHALL include an `abilities` array with each ability's name, text, and type

#### Scenario: Card detail without abilities
- **WHEN** `GET /api/cards/{cardId}` is called for a Pokemon card without abilities
- **THEN** the response SHALL include an empty `abilities` array

### Requirement: CardMapper ability mapping
The system SHALL add `toAbilityDefinitions(String jsonAbilities)` method to `CardMapper` that deserializes JSON to `List<AbilityDefinition>`.

#### Scenario: JSON deserialization
- **WHEN** `toAbilityDefinitions()` is called with a valid abilities JSON string
- **THEN** it SHALL return a list of `AbilityDefinition` objects

#### Scenario: Null JSON input
- **WHEN** `toAbilityDefinitions()` is called with null
- **THEN** it SHALL return an empty list
