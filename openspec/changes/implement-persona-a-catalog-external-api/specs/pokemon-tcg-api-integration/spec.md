## ADDED Requirements

### Requirement: Sync cards from Pokemon TCG API
The system SHALL fetch and store cards from the Pokemon TCG API v2 into the local database.

#### Scenario: Full sync at startup
- WHEN the application starts
- THEN the system SHALL fetch all XY1 cards via PokemonTcgApiClient
- THEN each card SHALL be mapped to CardEntity via CardMapper

#### Scenario: Manual sync via endpoint
- WHEN a POST request is sent to /api/cards/sync
- THEN the system SHALL re-fetch all XY1 cards from the API
- THEN the local catalog SHALL be updated

### Requirement: Map API response to canonical entities
The system SHALL map Pokemon TCG API v2 responses to CardEntity.

#### Scenario: Pokemon card mapping
- WHEN a Pokemon card response is received
- THEN the mapper SHALL extract id, name, supertype, subtypes, hp, types, evolvesFrom, retreatCost, attacks, weaknesses, resistances
- THEN hp SHALL be parsed from String to int

#### Scenario: Trainer card mapping
- WHEN a Trainer card response is received
- THEN the mapper SHALL extract id, name, supertype, subtypes, rules, rarity, images

#### Scenario: Energy card mapping
- WHEN an Energy card response is received
- THEN the mapper SHALL extract id, name, supertype, subtypes, rules, images, energyCardType, providesEnergyTypes
