## ADDED Requirements

### Requirement: Card sync runs asynchronously
The system SHALL NOT block application startup while synchronizing the card catalog from the external API.

#### Scenario: Startup completes even if API is unreachable
- WHEN the application starts
- AND the Pokemon TCG API is unreachable
- THEN the application SHALL finish starting and accept HTTP requests
- AND the sync SHALL be retried or logged as a warning

### Requirement: Card cache invalidated on resync
The system SHALL clear the card lookup cache when the catalog is re-synchronized.

#### Scenario: After sync, new cards are visible
- WHEN a manual sync is triggered via `POST /api/cards/sync`
- THEN any subsequent `getCardById()` call SHALL return the updated data
- AND stale cached entries SHALL NOT be served

### Requirement: CardMapper maps all card data
The system SHALL map abilities, evolvesTo, energyCardType, providesEnergyTypes, trainerSubtype, and isAceSpec from the API DTO to the CardEntity during sync.

#### Scenario: Pokemon card abilities mapped
- WHEN a Pokemon card with abilities is synced
- THEN the `CardEntity` SHALL store the ability data from the API DTO

#### Scenario: Energy card type mapped
- WHEN an Energy card is synced
- THEN `CardEntity.energyCardType` SHALL be populated from the API DTO
- AND `CardEntity.providesEnergyTypes` SHALL be populated

#### Scenario: Trainer card subtype mapped
- WHEN a Trainer card is synced
- THEN `CardEntity.trainerSubtype` SHALL be populated
- AND `CardEntity.isAceSpec` SHALL be set if applicable

## MODIFIED Requirements

### Requirement: Card response DTOs
The system SHALL return standard DTOs for search and detail.

#### Scenario: CardSearchResponse format
- WHEN a search request returns results
- THEN the response SHALL contain `items` (array of CardSummaryResponse), `page`, `size`, `totalItems`
- AND the response SHALL NOT be a Spring `Page` object
