## 1. Package Structure Verification

- [x] 1.1 Verify backend packages under `/backend/src/main/java/com/pokemontcg/` match `/docs/contracts_ai/02-project-structure-contract.md`
- [x] 1.2 Verify frontend folders under `/frontend/src/app/` match `/docs/contracts_ai/02-project-structure-contract.md`
- [x] 1.3 Document MATCH/MISMATCH/EXTRA/MISSING for each package/folder

## 2. Enum Verification

- [x] 2.1 Verify backend Java enums in `engine/` and `matches/domain/` match `/docs/contracts_ai/03-enums-contract.md`
- [x] 2.2 Verify frontend TypeScript enums in `shared/models/` match `/docs/contracts_ai/03-enums-contract.md`
- [x] 2.3 Document enum value mismatches between backend and frontend

## 3. JPA Entity Verification

- [x] 3.1 Verify JPA entities under `cards/infrastructure/`, `decks/infrastructure/`, `matches/infrastructure/` match `/docs/V1__init_schema.sql`
- [x] 3.2 Verify UUID generation strategies match SQL schema
- [x] 3.3 Verify JSONB/TEXT[] column mappings

## 4. DTO and Model Verification

- [x] 4.1 Verify backend DTOs in `*/api/dto/` match contract JSON examples in `/docs/contracts_ai/04-card-model-contract.md`, `/docs/contracts_ai/05-deck-contract.md`, `/docs/contracts_ai/06-game-state-contract.md`
- [x] 4.2 Verify frontend models in `shared/models/` match backend DTOs
- [x] 4.3 Document field type mismatches

## 5. REST API Verification

- [x] 5.1 Verify REST endpoints in `@RestController` classes match `/docs/contracts_ai/13-rest-api-contract.md` MVP endpoints
- [x] 5.2 Document missing endpoints (e.g., `/api/matches/{matchId}/actions`)
- [x] 5.3 Document EXTRA endpoints not in contract

## 6. WebSocket Verification

- [x] 6.1 Verify WebSocket topics match `/docs/contracts_ai/14-websocket-contract.md`
- [x] 6.2 Verify WebSocket handlers exist in `matches/websocket/`
- [x] 6.3 Verify frontend socket service exists

## 7. Generate Alignment Report

- [x] 7.1 Create `/docs/alignment-report.md` with all documented findings
- [x] 7.2 Categorize each item as MATCH/MISMATCH/EXTRA/MISSING
- [x] 7.3 Include recommendations for follow-up work

## 8. Verification

- [x] 8.1 Verify all documented files exist
- [x] 8.2 Verify `/docs/alignment-report.md` was created