## Context

This change is a verification-only effort. The project has multiple contract documents defining expected structures, but no systematic way to verify if the actual implementation matches those contracts. Implementation has evolved incrementally, creating potential drift between:

- What `/docs/contracts_ai/02-project-structure-contract.md` defines as the expected package structure vs. actual backend packages
- What `/docs/contracts_ai/03-enums-contract.md` defines as enum values vs. actual enum implementations
- What `/docs/V1__init_schema.sql` defines as database columns vs. JPA entities in infrastructure layer
- What `/docs/contracts_ai/13-rest-api-contract.md` defines as REST endpoints vs. actual `@RestController` implementations
- What `/docs/contracts_ai/14-websocket-contract.md` defines as WebSocket topics vs. actual WebSocket handlers

The change does NOT modify gameplay logic, setup flow, or features. It only verifies alignment and documents findings.

## Goals / Non-Goals

**Goals:**
1. Verify backend package structure matches `/docs/contracts_ai/02-project-structure-contract.md`
2. Verify frontend folder structure matches the contract
3. Verify backend Java enums match `/docs/contracts_ai/03-enums-contract.md` values
4. Verify frontend TypeScript enums/unions match contract values
5. Verify JPA entities match `/docs/V1__init_schema.sql` columns
6. Verify backend DTOs match contract JSON examples
7. Verify frontend models match backend DTOs and contract examples
8. Verify REST endpoints exist per `/docs/contracts_ai/13-rest-api-contract.md`
9. Verify WebSocket placeholders per `/docs/contracts_ai/14-websocket-contract.md`
10. Generate `/docs/alignment-report.md` documenting findings

**Non-Goals:**
- Do NOT implement any gameplay logic
- Do NOT implement setup flow
- Do NOT implement attack resolution or status effects
- Do NOT implement card API sync from external source
- Do NOT implement Deck Builder behavior
- Do NOT implement auth/JWT/login/register
- Do NOT implement real WebSocket gameplay
- Do NOT redesign database schema
- Do NOT create new features

## Decisions

**Decision 1: Use manual inspection with existing tools (grep, read) rather than build scripts**

Rationale: The project is small enough that manual inspection using bash commands (grep, find) can verify structure. This avoids introducing build complexity for a one-time verification.

**Decision 2: Generate `/docs/alignment-report.md` instead of failing the build**

Rationale: The goal is to understand current alignment state, not break the build. The report documents what matches and what doesn't.

**Decision 3: Treat mismatches as "documented" not "fixed automatically"**

Rationale: Fixing every mismatch requires separate implementation work. This verification change only documents findings.

## Risks / Trade-offs

**[Risk] Contract specifies old structure vs. actual code has evolved**  
→ Mitigation: Document mismatches in `/docs/alignment-report.md` with recommendation to update contract or implementation.

**[Risk] Frontend TypeScript uses different naming than backend Java**  
→ Mitigation: Document each enum value mismatch. Frontend may need updates or may be intentional (e.g., camelCase vs. UPPER_SNAKE_CASE).

**[Risk] JPA entities may differ from SQL schema intentionally**  
→ Mitigation: Note where JPA column mappings differ from SQL (e.g., UUID generation strategy).

**[Risk] API placeholders may not exist**  
→ Mitigation: Document if endpoint exists but returns placeholder vs. not existing at all.