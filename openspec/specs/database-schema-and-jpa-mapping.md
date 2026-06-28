# AI Proposal Spec: database-schema-and-jpa-mapping

## Change name

database-schema-and-jpa-mapping

## Purpose

Create an OpenSpec change to integrate the PostgreSQL database schema into the backend.

The change must use the SQL schema file as source of truth and create compatible JPA entities, repositories and mapping tests.

## Mandatory context files

OpenCode must read and obey:

- `/openspec/config.yaml`
- `/docs/contracts_ai/00-contract-index.md`
- `/docs/contracts_ai/01-project-scope-contract.md`
- `/docs/contracts_ai/02-project-structure-contract.md`
- `/docs/contracts_ai/03-enums-contract.md`
- `/docs/contracts_ai/04-card-model-contract.md`
- `/docs/contracts_ai/05-deck-contract.md`
- `/docs/contracts_ai/06-game-state-contract.md`
- `/docs/contracts_ai/12-persistence-log-contract.md`
- `/docs/contracts_ai/13-rest-api-contract.md`
- `/docs/contracts_ai/16-test-scenarios-contract.md`

OpenCode must also read:

- `/docs/V1__init_schema.sql`

## Database source of truth

For this change, the SQL file is the source of truth:

```txt
/docs/V1__init_schema.sql

Entities must match the SQL schema.

Do not redesign the schema silently.

If the SQL has a technical incompatibility with JPA, Hibernate, PostgreSQL, Flyway or the current backend configuration, report it clearly and propose the smallest correction.

Goal

Create a backend persistence foundation for:

users;
guest players;
card cache;
card attacks;
weaknesses;
resistances;
decks;
deck cards;
matches;
match players;
match state snapshots;
immutable match logs.
Target backend location

The implementation proposal must affect only the backend:

/backend

Expected backend folders:

/backend/src/main/resources/db/migration
/backend/src/main/java/com/pokemontcg/cards/infrastructure
/backend/src/main/java/com/pokemontcg/decks/infrastructure
/backend/src/main/java/com/pokemontcg/matches/infrastructure
/backend/src/main/java/com/pokemontcg/common/error
/backend/src/test/java/com/pokemontcg
Required migration behavior

The proposal must require:

Add Flyway support if missing.
Add PostgreSQL driver if missing.
Copy or move the schema into:
/backend/src/main/resources/db/migration/V1__init_schema.sql
The migration must be based on:
/docs/V1__init_schema.sql
Required JPA entities

The proposal must create or verify these entities:

Users
UserEntity
PlayerEntity
Cards
CardEntity
CardAttackEntity
CardWeaknessEntity
CardResistanceEntity
Decks
DeckEntity
DeckCardEntity
Matches
MatchEntity
MatchPlayerEntity
MatchStateEntity
MatchLogEntity
Required repositories

The proposal must create these Spring Data repositories:

UserJpaRepository
PlayerJpaRepository
CardJpaRepository
CardAttackJpaRepository
CardWeaknessJpaRepository
CardResistanceJpaRepository
DeckJpaRepository
DeckCardJpaRepository
MatchJpaRepository
MatchPlayerJpaRepository
MatchStateJpaRepository
MatchLogJpaRepository
Mapping requirements

The proposal must require:

UUID SQL columns map to java.util.UUID.
JSONB SQL columns are mapped safely.
TEXT[] SQL columns are mapped safely.
Entity table names match SQL table names exactly.
Entity column names match SQL column names exactly.
JPA annotations should reflect SQL constraints where practical.
Relationships should avoid eager loading by default.
Repositories must not create N+1-prone default behavior.
Full match state must remain inside match_states.serialized_state.
Match log event/action payload must remain inside match_logs.payload.
Important architecture constraints

The proposal must enforce:

No JPA entities inside the engine package.
No repositories inside the engine package.
No Flyway, PostgreSQL, Hibernate or Spring Data dependency inside the engine package.
Do not replace engine/domain models with JPA entities.
Infrastructure entities must stay in infrastructure packages.
The engine remains isolated and testable.
Explicit non-goals

Do not implement:

gameplay logic;
setup flow;
attack resolution;
status effects;
card API sync;
Deck Builder behavior;
login/register/JWT behavior;
WebSocket gameplay flow;
frontend changes;
ranking;
chat;
animations.
Required tests in the proposal

The generated change must require tests for:

Spring context load.
Flyway migration execution.
Repository beans creation.
JSONB persistence and readback for:
match_states.serialized_state;
match_logs.payload.
TEXT[] persistence and readback for:
cards.subtypes;
cards.pokemon_types;
cards.retreat_cost;
card_attacks.printed_cost.
Basic relationship compatibility:
deck to deck cards;
card to attacks;
match to match players;
match to match state snapshots;
match to match logs.
Expected OpenSpec output

Generate an OpenSpec change under:

/openspec/changes/database-schema-and-jpa-mapping/

The generated change should include:

proposal.md
tasks.md
design.md
specs/database-schema/spec.md
Scope control

The change must be implementation-ready but narrow.

This proposal is only about schema integration, JPA mapping, repositories and compatibility tests.

No business logic must be implemented.




