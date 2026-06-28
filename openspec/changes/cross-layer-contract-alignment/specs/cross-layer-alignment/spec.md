## ADDED Requirements

### Requirement: Cross-layer consistency verification SHALL document all discrepancies between contracts and implementation

The system SHALL generate `/docs/alignment-report.md` documenting findings from verifying:
- Backend package structure against `/docs/contracts_ai/02-project-structure-contract.md`
- Frontend folder structure against `/docs/contracts_ai/02-project-structure-contract.md`
- Backend Java enums against `/docs/contracts_ai/03-enums-contract.md`
- Frontend TypeScript enums/unions against `/docs/contracts_ai/03-enums-contract.md`
- JPA entities against `/docs/V1__init_schema.sql`
- Backend DTOs against contract JSON examples in `/docs/contracts_ai/04-card-model-contract.md`, `/docs/contracts_ai/05-deck-contract.md`, `/docs/contracts_ai/06-game-state-contract.md`
- Frontend models against backend DTOs and contract JSON examples
- REST endpoint placeholders against `/docs/contracts_ai/13-rest-api-contract.md`
- WebSocket placeholders against `/docs/contracts_ai/14-websocket-contract.md`

#### Scenario: Verify backend package structure
- **GIVEN** the contract at `/docs/contracts_ai/02-project-structure-contract.md`
- **WHEN** scanning `/backend/src/main/java/com/pokemontcg/` for packages
- **THEN** document each package and whether it matches the contract

#### Scenario: Verify backend enums
- **GIVEN** the contract at `/docs/contracts_ai/03-enums-contract.md`
- **WHEN** scanning backend `engine/` and `matches/domain/` for enum values
- **THEN** document each enum value and whether it matches the contract

#### Scenario: Verify frontend enums
- **GIVEN** the contract at `/docs/contracts_ai/03-enums-contract.md`
- **WHEN** scanning frontend `shared/models/` for TypeScript enums/unions
- **THEN** document each type and whether it matches the contract

#### Scenario: Verify JPA entities
- **GIVEN** `/docs/V1__init_schema.sql`
- **WHEN** scanning infrastructure layer entities
- **THEN** document each entity field and whether it matches SQL columns

#### Scenario: Verify REST endpoints
- **GIVEN** `/docs/contracts_ai/13-rest-api-contract.md`
- **WHEN** scanning for `@RestController` and `@RequestMapping` annotations
- **THEN** document each endpoint and whether it matches contract MVP endpoints

### Requirement: Alignment report SHALL categorize discrepancies by severity

The alignment report at `/docs/alignment-report.md` SHALL categorize each discrepancy as:
- **MATCH**: Implementation matches contract
- **MISMATCH**: Implementation differs from contract (document what differs)
- **MISSING**: Contract specifies but implementation doesn't exist
- **EXTRA**: Implementation exists but contract doesn't specify

#### Scenario: Categorize backend package mismatch
- **GIVEN** backend has `matches/api/DTO/` folder
- **AND** contract doesn't specify this folder
- **THEN** categorize as EXTRA and note in report

#### Scenario: Categorize missing REST endpoint
- **GIVEN** contract specifies `POST /api/matches/{matchId}/actions`
- **AND** this endpoint doesn't exist in code
- **THEN** categorize as MISSING and note in report

### Requirement: Tasks file SHALL list verification subtasks

The tasks file generated from this spec SHALL enumerate each verification task:
1. Verify backend package structure
2. Verify frontend folder structure  
3. Verify backend Java enums
4. Verify frontend TypeScript enums
5. Verify JPA entity mapping
6. Verify REST API endpoints
7. Verify WebSocket topics
8. Generate alignment report