## MODIFIED Requirements

### Requirement: Card sync writes to unified cards table
The system SHALL synchronize cards from the external Pokemon TCG API into the single `cards` table via `CardJpaRepository`. The system SHALL NOT write to specialized tables (`pokemon_cards`, `trainer_cards`, `energy_cards`).

#### Scenario: Sync populates cards table
- **GIVEN** the `cards` table is empty
- **WHEN** the sync endpoint `POST /api/cards/sync` is called
- **THEN** the `cards` table SHALL contain all synchronized cards
- **AND** `GET /api/cards?query=slugma` SHALL return results

#### Scenario: CardLookupAdapter reads from cards table
- **GIVEN** cards have been synchronized to the `cards` table
- **WHEN** `CardLookupAdapter.getCardById("xy1-10")` is called
- **THEN** it SHALL return a `PokemonCardDefinition` with correct data (hp, attacks, type)
- **AND** SHALL NOT throw an exception

### Requirement: CardMapper.toCardEntity sets number correctly
`CardMapper.toCardEntity(PokemonTcgApiCardDto)` SHALL extract and set the `number` field from the API DTO's `set.id` field (e.g., `"xy1-10"` → `number = "10"`).

#### Scenario: Number extracted from set.id
- **GIVEN** an API card DTO with `set.id = "xy1-10"`
- **WHEN** `CardMapper.toCardEntity(dto)` is called
- **THEN** the resulting `CardEntity.number` SHALL be `"10"`
- **AND** `CardEntity.id` SHALL be `"xy1-10"`

### Requirement: SeedDeckService verifies cards before insert
The system SHALL verify that every `cardId` referenced by a seed deck exists in `CardJpaRepository` before inserting the deck. If any card is missing, the system SHALL log a warning and skip that seed deck.

#### Scenario: Seed deck inserted after sync
- **GIVEN** cards have been synchronized to the `cards` table
- **WHEN** `SeedDeckService` runs on application startup
- **THEN** seed decks SHALL be created with valid FK references
- **AND** no FK constraint violations SHALL occur
