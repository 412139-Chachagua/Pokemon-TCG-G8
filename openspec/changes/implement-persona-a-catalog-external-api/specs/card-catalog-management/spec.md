## ADDED Requirements

### Requirement: Search cards by filters
The system SHALL provide a catalog service to search stored cards with filters and pagination.

#### Scenario: Search by name
- WHEN a GET request is sent to /api/cards with query=charizard
- THEN the system SHALL return cards whose name contains charizard (case-insensitive)

#### Scenario: Search by supertype
- WHEN a GET request is sent to /api/cards with supertype=POKEMON
- THEN the system SHALL return only Pokemon-type cards

#### Scenario: Combined filters
- WHEN a GET request is sent to /api/cards with query=pikachu&supertype=POKEMON
- THEN the system SHALL return cards matching all filters

#### Scenario: Paginated results
- WHEN a GET request is sent to /api/cards with page=0&size=20
- THEN the system SHALL return the first 20 cards with total count

### Requirement: Get card by ID
The system SHALL provide a detail endpoint for individual cards.

#### Scenario: Existing card
- WHEN a GET request is sent to /api/cards/xy1-1
- THEN the system SHALL return full card details with attacks, weaknesses, resistances

#### Scenario: Non-existing card
- WHEN a GET request is sent to /api/cards/nonexistent-id
- THEN the system SHALL return HTTP 404

### Requirement: Card response DTOs
The system SHALL return standard DTOs for search and detail.

#### Scenario: CardSummaryResponse format
- WHEN a search request returns results
- THEN each result SHALL contain id, name, supertype, setCode, number, imageSmallUrl

#### Scenario: CardDetailResponse format
- WHEN a detail request returns a card
- THEN the response SHALL contain id, name, supertype, subtypes, setCode, number, images, rulesText, hp, stage, evolvesFrom, types, attacks, weaknesses, resistances, retreatCost, isEx, isMega
