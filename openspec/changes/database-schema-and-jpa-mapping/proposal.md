## Why

The Pokémon TCG backend requires a PostgreSQL database persistence layer with JPA entities and Spring Data repositories to support the card cache, deck management, and match state tracking. The SQL schema file (`/docs/V1__init_schema.sql`) exists but is not yet integrated into the backend project. This change will establish the database foundation for all backend data operations.

## What Changes

- Add Flyway dependency for database migrations
- Add PostgreSQL JDBC driver dependency
- Copy schema migration to `/backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create JPA entity classes matching all SQL tables
- Create Spring Data repository interfaces
- Add compatibility tests for JSONB, TEXT[] columns and relationships

## Capabilities

### New Capabilities

- `database-schema`: Integrate PostgreSQL schema with Flyway migrations and verify schema applies correctly
- `jpa-mapping`: Create JPA entities compatible with SQL schema tables, columns and constraints
- `spring-repositories`: Create Spring Data JPA repositories for all entities without N+1-prone defaults

### Modified Capabilities

None - this is a new capability for database persistence.

## Impact

- New dependencies: Flyway, PostgreSQL JDBC driver
- Affected backend packages: cards/infrastructure, decks/infrastructure, matches/infrastructure
- New test coverage: mapping tests, repository tests, JSONB/TEXT[] compatibility
- Non-impact: Frontend, Game Engine (remains isolated), no business logic changes

## Mandatory Context Files

This proposal references the following contract files:

- `/docs/contracts_ai/02-project-structure-contract.md` - Package structure
- `/docs/contracts_ai/04-card-model-contract.md` - Card model
- `/docs/contracts_ai/05-deck-contract.md` - Deck structure
- `/docs/contracts_ai/06-game-state-contract.md` - Game state model
- `/docs/contracts_ai/12-persistence-log-contract.md` - Persistence requirements
- `/docs/contracts_ai/13-rest-api-contract.md` - REST API integration