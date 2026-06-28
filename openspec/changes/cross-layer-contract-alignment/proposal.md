## Why

The project has evolved incrementally without systematic cross-layer consistency verification. Multiple contracts define expected structures, names, and values, but there's no verification that backend packages, frontend folders, enums, DTOs, models, and APIs actually align with those contracts. This creates the risk of drift between what contracts specify and what code actually implements, leading to integration failures, unclear debugging, and difficulty maintaining consistency as the project grows.

## What Changes

This change creates a verification and alignment process for cross-layer consistency. It will:
1. Document all structural inconsistencies between contracts and actual implementation
2. Create tasks to fix or document each inconsistency
3. Generate an alignment report at `/docs/alignment-report.md`
4. NOT implement any gameplay logic, setup flow, or game features

## Capabilities

### New Capabilities

- cross-layer-consistency-check: Verify and align implementation against contract specifications

### Modified Capabilities

- None - this is a verification-only change that doesn't modify feature requirements.

## Impact

**Affected areas:**
- Backend packages under `/backend/src/main/java/com/pokemontcg/`
- Frontend structure under `/frontend/src/app/`
- Contract-derived enums in both backend and frontend
- JPA entities in infrastructure layer
- DTOs in API layer
- Frontend models in shared/models

**Contract files referenced:**
- `/docs/contracts_ai/02-project-structure-contract.md`
- `/docs/contracts_ai/03-enums-contract.md`
- `/docs/contracts_ai/13-rest-api-contract.md`
- `/docs/contracts_ai/14-websocket-contract.md`
- `/docs/V1__init_schema.sql`
- `/docs/contracts_ai/04-card-model-contract.md` (for JSON examples)
- `/docs/contracts_ai/05-deck-contract.md` (for JSON examples)
- `/docs/contracts_ai/06-game-state-contract.md` (for JSON examples)

**Non-goals (strict):**
- Do NOT implement gameplay logic
- Do NOT implement setup flow
- Do NOT implement attack resolution
- Do NOT implement status effect resolution
- Do NOT implement card API sync
- Do NOT implement Deck Builder behavior
- Do NOT implement auth/JWT/login/register
- Do NOT implement real WebSocket gameplay
- Do NOT implement ranking, chat, or animations
- Do NOT redesign the database