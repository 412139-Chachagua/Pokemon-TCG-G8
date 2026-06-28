# AI Proposal Spec: cross-layer-contract-alignment

## Change name

cross-layer-contract-alignment

## Purpose

Create an OpenSpec change to verify and align consistency between:

- `/docs/contracts_ai`
- `/docs/V1__init_schema.sql`
- backend package structure
- backend JPA entities
- backend repositories
- backend DTOs
- backend REST endpoint placeholders
- backend WebSocket event placeholders
- frontend TypeScript models
- frontend API services
- frontend WebSocket services

This change must detect and fix cross-layer inconsistencies before implementing gameplay logic.

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
- `/docs/contracts_ai/07-setup-flow-contract.md`
- `/docs/contracts_ai/08-game-action-contract.md`
- `/docs/contracts_ai/09-rule-validation-contract.md`
- `/docs/contracts_ai/10-attack-pipeline-contract.md`
- `/docs/contracts_ai/11-status-effects-contract.md`
- `/docs/contracts_ai/12-persistence-log-contract.md`
- `/docs/contracts_ai/13-rest-api-contract.md`
- `/docs/contracts_ai/14-websocket-contract.md`
- `/docs/contracts_ai/15-frontend-state-contract.md`
- `/docs/contracts_ai/16-test-scenarios-contract.md`

Also read:

- `/docs/V1__init_schema.sql`

## Goal

Generate an OpenSpec change that verifies and aligns the project foundation across backend, frontend and database.

The change should ensure that all existing skeleton code, entities, repositories, DTOs, frontend models and service placeholders follow the contracts.

## Scope

The proposal must include verification and correction for:

### 1. Enums

Verify that backend Java enums and frontend TypeScript union types/enums match `/docs/contracts_ai/03-enums-contract.md`.

Examples:

- `MatchStatus`
- `TurnPhase`
- `PlayerSide`
- `CardSupertype`
- `PokemonStage`
- `EnergyType`
- `EnergyCardType`
- `TrainerSubtype`
- `ZoneType`
- `SpecialCondition`
- `GameActionType`
- `GameEventType`
- `FinishReason`
- `GameErrorCode`

The names and values must match exactly.

### 2. Backend structure

Verify that backend packages follow:

- `/docs/contracts_ai/02-project-structure-contract.md`

The engine package must remain isolated from:

- Spring MVC
- Spring Data
- JPA
- Hibernate
- Flyway
- WebSocket classes
- repositories

No `@Entity`, `@Repository`, `@RestController`, `@Service` or `@Autowired` inside the `engine` package.

### 3. Frontend structure

Verify that frontend folders follow:

- `/docs/contracts_ai/02-project-structure-contract.md`

Required areas:

- `core/api`
- `core/websocket`
- `shared/models`
- `features/lobby`
- `features/match`
- `features/decks`
- `features/auth/README.md`

Auth must remain postponed.

### 4. Database to JPA alignment

Verify that SQL schema and JPA entities are aligned.

Source of truth:

```txt
/docs/V1__init_schema.sql

Check:

table names;
column names;
UUID fields;
JSONB fields;
TEXT[] fields;
timestamps;
relationships;
nullable fields;
unique constraints where practical;
indexes where relevant.

The change must not redesign the database unless a real compatibility issue exists.

5. Backend DTO to contract alignment

Verify backend DTOs against contract JSON examples.

Contracts to check:

/docs/contracts_ai/04-card-model-contract.md
/docs/contracts_ai/05-deck-contract.md
/docs/contracts_ai/06-game-state-contract.md
/docs/contracts_ai/08-game-action-contract.md
/docs/contracts_ai/13-rest-api-contract.md
/docs/contracts_ai/14-websocket-contract.md

Check:

field names;
field types;
enum values;
nested object structures;
camelCase JSON naming;
request/response consistency.

6. Frontend model to backend DTO alignment

Verify frontend TypeScript models against backend DTOs and contracts.

Check especially:

card.models.ts
deck.models.ts
game-state.models.ts
game-action.models.ts
game-event.models.ts
api-error.models.ts

Frontend model fields must match backend JSON fields.

Frontend must not introduce alternative names.

7. REST endpoint alignment

Verify REST endpoint placeholders and API services match:

/docs/contracts_ai/13-rest-api-contract.md

Expected MVP endpoints:

POST /api/matches
POST /api/matches/{matchId}/join
GET  /api/matches/{matchId}/state?playerId={playerId}
POST /api/matches/{matchId}/actions
GET  /api/cards
GET  /api/cards/{cardId}
GET  /api/decks/seed
GET  /api/decks/{deckId}
POST /api/decks/validate

Backend controllers and frontend API services must use the same paths.

8. WebSocket alignment

Verify WebSocket placeholders match:

/docs/contracts_ai/14-websocket-contract.md

Expected topics:

/topic/matches/{matchId}/events
/user/queue/matches/{matchId}/private-state
/app/matches/{matchId}/actions

Do not implement gameplay WebSocket flow yet.

Only align names, placeholders and model types.

9. Alignment report

The change must create or update:

/docs/alignment-report.md

The report must include:

checked areas;
inconsistencies found;
inconsistencies fixed;
remaining risks;
skipped items and why;
commands used to verify backend/frontend.
Explicit non-goals

Do not implement:

gameplay logic;
setup flow;
attack resolution;
status effect resolution;
card API sync;
Deck Builder behavior;
login/register/JWT;
real WebSocket gameplay flow;
ranking;
chat;
animations;
new database tables unless strictly required for compatibility.
Correction policy

The proposal must allow small alignment fixes, such as:

renaming enum values to match contracts;
renaming DTO fields to match contracts;
correcting frontend model field names;
correcting API service paths;
correcting package locations;
correcting JPA column names;
correcting placeholder class names;
adding missing placeholder DTO/model files;
adding compile-safe type definitions.

The proposal must not allow broad feature implementation.

Required tests/checks

The generated change must require:

Backend
mvn test
Spring context load test still passing.
JPA repositories still compile.
No engine dependency on infrastructure.
Frontend
npm install if needed.
npm run build
TypeScript model compilation.
API service path constants compile.
Static alignment checks

If practical, add tests or simple verification scripts for:

backend enum values;
frontend enum/union values;
REST path constants;
WebSocket topic constants.
Expected OpenSpec output

Generate an OpenSpec change under:

/openspec/changes/cross-layer-contract-alignment/

The generated change should include:

proposal.md
tasks.md
design.md
specs/cross-layer-alignment/spec.md
Scope control

This is a foundation alignment proposal.

It must prepare the project for future gameplay proposals.

It must not implement gameplay.

