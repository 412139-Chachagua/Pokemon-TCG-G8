## ADDED Requirements

### Requirement: Flyway migration runs on startup

The system SHALL execute Flyway database migrations on application startup to create all schema tables.

#### Scenario: Flyway creates tables on startup
- **WHEN** Spring Boot application starts with Flyway enabled
- **THEN** Flyway executes V1__init_schema.sql and creates all tables: users, guest_players, cards, card_attacks, card_weaknesses, card_resistances, decks, deck_cards, matches, match_players, match_states, match_logs

#### Scenario: Flyway fails on duplicate migration
- **WHEN** Flyway detects the migration has already been applied
- **THEN** Flyway reports success without re-executing the migration

### Requirement: JPA entities match SQL tables exactly

All JPA entities SHALL have table names and column names that match the SQL schema exactly.

#### Scenario: UserEntity maps to users table
- **WHEN** JPA UserEntity is created
- **THEN** @Table(name = "users") annotation is present and columns match: id (uuid), username, email, password_hash, display_name, role, status, created_at, updated_at

#### Scenario: CardEntity maps to cards table
- **WHEN** JPA CardEntity is created
- **THEN** @Table(name = "cards") annotation is present and includes TEXT[] columns: subtypes, pokemon_types, retreat_cost, rules_text; JSONB column: raw_json

#### Scenario: DeckEntity maps to decks table
- **WHEN** JPA DeckEntity is created
- **THEN** @Table(name = "decks") annotation is present and includes JSONB column: validation_errors

#### Scenario: MatchStateEntity maps to match_states table
- **WHEN** JPA MatchStateEntity is created
- **THEN** @Table(name = "match_states") annotation is present and includes JSONB column: serialized_state

#### Scenario: MatchLogEntity maps to match_logs table
- **WHEN** JPA MatchLogEntity is created
- **THEN** @Table(name = "match_logs") annotation is present and includes JSONB column: payload

### Requirement: UUID columns map to Java UUID

SQL uuid columns SHALL map directly to java.util.UUID in JPA entities.

#### Scenario: UserEntity id is UUID
- **WHEN** UserEntity id field is defined
- **THEN** the field type is UUID and @Id annotation is present

#### Scenario: MatchEntity id is UUID
- **WHEN** MatchEntity id field is defined
- **THEN** the field type is UUID and @Id annotation is present

### Requirement: TEXT[] arrays map to String[]

SQL text[] columns SHALL map to Java String[] in JPA entities.

#### Scenario: CardEntity subtypes is String array
- **WHEN** CardEntity subtypes field is defined
- **THEN** the field type is String[] with @Column(columnDefinition = "text[]")

#### Scenario: CardAttackEntity printedCost is String array
- **WHEN** CardAttackEntity printedCost field is defined
- **THEN** the field type is String[] with @Column(columnDefinition = "text[]")

### Requirement: JSONB columns map correctly

SQL jsonb columns SHALL map to JSON-compatible Java types in JPA entities.

#### Scenario: MatchStateEntity serializedState is JSONB
- **WHEN** MatchStateEntity serializedState field is defined
- **THEN** @Column(columnDefinition = "jsonb") is present and type is String or Map

#### Scenario: MatchLogEntity payload is JSONB
- **WHEN** MatchLogEntity payload field is defined
- **THEN** @Column(columnDefinition = "jsonb") is present and type is Map or Object

### Requirement: Spring Data repositories created

Spring Data JPA repositories SHALL be created for all entities with proper fetch strategies.

#### Scenario: CardJpaRepository exists
- **WHEN** CardJpaRepository interface is created
- **THEN** it extends JpaRepository<CardEntity, String> with no N+1-prone default methods

#### Scenario: DeckJpaRepository exists
- **WHEN** DeckJpaRepository interface is created
- **THEN** it extends JpaRepository<DeckEntity, UUID>

#### Scenario: MatchJpaRepository exists
- **WHEN** MatchJpaRepository interface is created
- **THEN** it extends JpaRepository<MatchEntity, UUID>

#### Scenario: MatchStateJpaRepository exists
- **WHEN** MatchStateJpaRepository interface is created
- **THEN** it extends JpaRepository<MatchStateEntity, UUID>

### Requirement: Repositories do not create N+1 queries

Repository default methods SHALL NOT cause N+1 query problems.

#### Scenario: Lazy loading relationships
- **WHEN** Repository returns entities with relationships
- **THEN** relationships are configured as LAZY by default

#### Scenario: No eager collection fetching
- **WHEN** DeckEntity is loaded
- **THEN** deckCards relationship uses FetchType.LAZY

### Requirement: JPA entities stay in infrastructure packages

JPA entities SHALL NOT be placed in the engine package or domain package.

#### Scenario: CardEntity location
- **WHEN** CardEntity is created
- **THEN** it is located in cards/infrastructure package

#### Scenario: DeckEntity location
- **WHEN** DeckEntity is created
- **THEN** it is located in decks/infrastructure package

#### Scenario: Engine package isolation
- **WHEN** engine package is inspected
- **THEN** no JPA annotations (@Entity, @Table, @Column) or Spring Data imports are present

### Requirement: Mapping tests verify entity persistence

The system SHALL include tests that verify JPA entities can be persisted and retrieved.

#### Scenario: UserEntity persists and loads
- **WHEN** UserEntity is saved and retrieved by ID
- **THEN** all fields match the original values

#### Scenario: CardEntity with arrays persists
- **WHEN** CardEntity with subtypes, pokemon_types, retreat_cost is saved and retrieved
- **THEN** array values match exactly

#### Scenario: MatchStateEntity JSONB persists
- **WHEN** MatchStateEntity with serialized_state JSONB is saved and retrieved
- **THEN** JSON content is preserved

#### Scenario: MatchLogEntity JSONB persists
- **WHEN** MatchLogEntity with payload JSONB is saved and retrieved
- **THEN** JSON content is preserved

#### Scenario: Entity relationships load correctly
- **WHEN** Deck is loaded with deckCards relationship
- **THEN** cards are accessible via getDeckCards()

### Requirement: Flyway migration runs in tests

The system SHALL run Flyway migrations during test execution.

#### Scenario: Test context loads with migrations
- **WHEN** @SpringBootTest is executed
- **THEN** Flyway runs migrations before tests execute

### Requirement: Foreign key constraints match SQL

JPA entity relationships SHALL use @JoinColumn with foreign key names matching SQL constraints.

#### Scenario: Deck to DeckCards relationship
- **WHEN** DeckEntity has relationship to DeckCardEntity
- **THEN** @JoinColumn(name = "deck_id") is present matching SQL foreign key

#### Scenario: Card to CardAttacks relationship
- **WHEN** CardEntity has relationship to CardAttackEntity
- **THEN** @JoinColumn(name = "card_id") is present with cascade = CascadeType.ALL, orphanRemoval = true

#### Scenario: Match to MatchPlayers relationship
- **WHEN** MatchEntity has relationship to MatchPlayerEntity
- **THEN** @JoinColumn(name = "match_id") is present with cascade = CascadeType.ALL