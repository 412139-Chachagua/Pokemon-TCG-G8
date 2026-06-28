## Why

The Pokémon TCG TPI project requires an initial compilable skeleton for the Java 21 + Spring Boot backend and Angular 21+ frontend. Without this foundational structure, no further implementation work can proceed. Creating the skeleton now establishes the canonical folder structure and enables the team to verify build pipelines early.

## What Changes

- Create `/backend` root folder with Maven project structure under `com.pokemontcg`
- Create `/frontend` root folder with Angular 21+ project structure
- Implement canonical package structure per `/docs/contracts_ai/02-project-structure-contract.md`
- Place enum files per `/docs/contracts_ai/03-enums-contract.md`
- Add placeholder DTO folders, module folders and README files
- Include Spring context load smoke test for backend
- Frontend must build successfully

## Capabilities

### New Capabilities
- `project-structure`: Define the canonical backend and frontend folder structure with all required packages, modules and placeholder files according to contracts.

### Modified Capabilities
None - this is a greenfield project skeleton with no existing specs to modify.

## Impact

- New folders: `/backend`, `/frontend`
- Affected packages: all backend packages (config, common, cards, decks, matches, engine) and all frontend modules (core, shared, features)
- Dependencies: Java 21, Spring Boot 3.x, Maven, Angular 21+, TypeScript, RxJS
- Non-impact: No gameplay logic, no database migrations, no auth, no WebSocket real-time features yet