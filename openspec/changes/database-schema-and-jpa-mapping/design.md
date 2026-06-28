## Context

The PostgreSQL database schema exists at `/docs/V1__init_schema.sql` and defines tables for users, guest players, cards, decks, matches, and match state/logs. The backend skeleton created in the previous change has placeholder JPA entities that need to be replaced with properly mapped entities compatible with this schema. Flyway needs to be added to manage migrations.

## Goals / Non-Goals

**Goals:**
- Add Flyway dependency for database migrations
- Add PostgreSQL JDBC driver
- Copy SQL schema to proper migration location
- Create complete JPA entities matching SQL tables exactly
- Create Spring Data repositories with proper fetch strategies
- Add mapping compatibility tests

**Non-Goals:**
- No gameplay logic implementation
- No setup flow implementation
- No attack resolution
- No card API sync
- No Deck Builder behavior
- No auth/JWT implementation
- No WebSocket gameplay flow
- No frontend changes

## Decisions

1. **Schema Migration Location**: Copy SQL to `/backend/src/main/resources/db/migration/V1__init_schema.sql` (Flyway default location)

2. **JSONB Mapping Strategy**: Use `@Column(columnDefinition = "jsonb")` for match_states.serialized_state and match_logs.payload

3. **TEXT[] Array Mapping**: Use Hibernate's `StringArrayType` or `String[]` with `@Column(columnDefinition = "text[]")` for subtypes, pokemon_types, retreat_cost, printed_cost

4. **UUID Mapping**: Use `@Id` with `UUID` type - PostgreSQL uuid type maps directly

5. **Repository Fetch Strategy**: Use `@Repository` with explicit `@Query` for complex queries, default to lazy loading for relationships

6. **Engine Isolation**: JPA entities will stay in infrastructure packages only, engine package remains free of any database dependencies

## Risks / Trade-offs

- [Risk] TEXT[] may need custom Hibernate type → Mitigation: Use `@Type(PostgreSqlArrayType.class)` or simpler String[] with array column definition
- [Risk] JSONB read/write performance → Mitigation: Use @Column with proper columnDefinition
- [Risk] Foreign key constraints may block development testing → Mitigation: Keep ON DELETE CASCADE but test with testcontainers
- [Risk] Large match state JSON may impact performance → Mitigation: Keep in separate table as designed, lazy load only when needed