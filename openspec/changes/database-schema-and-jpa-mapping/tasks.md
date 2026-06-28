## 1. Dependencies and Migration Setup

- [x] 1.1 Add Flyway dependency to pom.xml (spring-boot-starter-flyway or flyway-core)
- [x] 1.2 Add PostgreSQL JDBC driver dependency to pom.xml (postgresql)
- [x] 1.3 Create /backend/src/main/resources/db/migration directory
- [x] 1.4 Copy V1__init_schema.sql to /backend/src/main/resources/db/migration/V1__init_schema.sql
- [x] 1.5 Configure application.yml to enable Flyway with PostgreSQL datasource
- [x] 1.6 Run mvn compile to verify dependencies resolve

## 2. User and Player Entities

- [x] 2.1 Update UserEntity in cards/infrastructure with all columns from users table
- [x] 2.2 Update GuestPlayerEntity in cards/infrastructure with all columns from guest_players table
- [x] 2.3 Add proper @Table, @Id, @Column annotations with exact SQL column names
- [x] 2.4 Create UserJpaRepository interface in cards/infrastructure
- [x] 2.5 Create GuestPlayerJpaRepository interface in cards/infrastructure

## 3. Card Entities

- [x] 3.1 Update CardEntity in cards/infrastructure with all columns from cards table
- [x] 3.2 Add @Column(columnDefinition = "text[]") for subtypes, pokemon_types, retreat_cost, rules_text
- [x] 3.3 Add @Column(columnDefinition = "jsonb") for raw_json
- [x] 3.4 Update CardAttackEntity in cards/infrastructure with printed_cost as String[]
- [x] 3.5 Update CardWeaknessEntity in cards/infrastructure
- [x] 3.6 Update CardResistanceEntity in cards/infrastructure
- [x] 3.7 Add @OneToMany relationships from CardEntity to attacks, weaknesses, resistances
- [x] 3.8 Create CardJpaRepository interface
- [x] 3.9 Create CardAttackJpaRepository interface
- [x] 3.10 Create CardWeaknessJpaRepository interface
- [x] 3.11 Create CardResistanceJpaRepository interface

## 4. Deck Entities

- [x] 4.1 Update DeckEntity in decks/infrastructure with all columns from decks table
- [x] 4.2 Add @Column(columnDefinition = "jsonb") for validation_errors
- [x] 4.3 Update DeckCardEntity in decks/infrastructure
- [x] 4.4 Add @ManyToOne relationship from DeckCardEntity to DeckEntity
- [x] 4.5 Add @OneToMany relationship from DeckEntity to DeckCardEntity with LAZY fetch
- [x] 4.6 Update DeckJpaRepository (already exists from skeleton)
- [x] 4.7 Update DeckCardJpaRepository (already exists from skeleton)

## 5. Match Entities

- [x] 5.1 Update MatchEntity in matches/infrastructure with all columns from matches table
- [x] 5.2 Update MatchPlayerEntity in matches/infrastructure
- [x] 5.3 Update MatchStateEntity in matches/infrastructure with serialized_state as JSONB
- [x] 5.4 Update MatchLogEntity in matches/infrastructure with payload as JSONB
- [x] 5.5 Add @OneToMany from MatchEntity to MatchPlayerEntity
- [x] 5.6 Add @OneToMany from MatchEntity to MatchStateEntity
- [x] 5.7 Add @OneToMany from MatchEntity to MatchLogEntity
- [x] 5.8 Add @ManyToOne from MatchPlayerEntity to MatchEntity
- [x] 5.9 Add @ManyToOne from MatchStateEntity to MatchEntity
- [x] 5.10 Add @ManyToOne from MatchLogEntity to MatchEntity
- [x] 5.11 Configure all relationships with FetchType.LAZY
- [x] 5.12 Update MatchJpaRepository (already exists from skeleton)
- [x] 5.13 Update MatchPlayerJpaRepository (already exists from skeleton)
- [x] 5.14 Update MatchStateJpaRepository (already exists from skeleton)
- [x] 5.15 Update MatchLogJpaRepository (already exists from skeleton)

## 6. Foreign Key Constraints

- [x] 6.1 Add @JoinColumn annotations matching SQL foreign key names
- [x] 6.2 Configure cascade and orphanRemoval for CardEntity relationships
- [x] 6.3 Configure cascade for DeckEntity relationships
- [x] 6.4 Configure cascade for MatchEntity relationships

## 7. Configuration

- [x] 7.1 Update application.yml with PostgreSQL datasource configuration
- [x] 7.2 Configure Flyway to use PostgreSQL datasource
- [x] 7.3 Configure Hibernate to use PostgreSQL dialect

## 8. Tests

- [x] 8.1 Update test application.yml with testcontainers for PostgreSQL
- [x] 8.2 Create EntityMappingTest to verify table and column names
- [x] 8.3 Create JsonbPersistenceTest for match_states.serialized_state
- [x] 8.4 Create JsonbPersistenceTest for match_logs.payload
- [x] 8.5 Create ArrayPersistenceTest for cards.subtypes
- [x] 8.6 Create ArrayPersistenceTest for cards.pokemon_types
- [x] 8.7 Create ArrayPersistenceTest for cards.retreat_cost
- [x] 8.8 Create ArrayPersistenceTest for card_attacks.printed_cost
- [x] 8.9 Create RepositoryRelationshipTest for deck to deck_cards
- [x] 8.10 Create RepositoryRelationshipTest for card to attacks
- [x] 8.11 Create RepositoryRelationshipTest for match to match_players
- [x] 8.12 Create RepositoryRelationshipTest for match to match_states
- [x] 8.13 Create RepositoryRelationshipTest for match to match_logs
- [x] 8.14 Run mvn test to verify all tests pass

## 9. Verification

- [x] 9.1 Run mvn compile to verify no compilation errors
- [x] 9.2 Run mvn test to verify all tests pass including mapping tests
- [x] 9.3 Verify engine package has no JPA annotations or Spring Data imports