## Context

The project requires a foundational skeleton for the Pokémon TCG TPI. Currently, no backend or frontend code exists. The skeleton must follow canonical structures defined in contract files to avoid architectural drift. The skeleton must be compilable and buildable to verify the development environment setup.

## Goals / Non-Goals

**Goals:**
- Create `/backend` folder with Java 21 + Spring Boot 3.x Maven project
- Create `/frontend` folder with Angular 21+ project structure
- Implement folder structure per `/docs/contracts_ai/02-project-structure-contract.md`
- Create all enum files per `/docs/contracts_ai/03-enums-contract.md`
- Add placeholder classes for config, common errors, IDs, cards, decks, matches and engine
- Create placeholder DTO folders
- Include Spring context load smoke test
- Verify backend compiles and frontend builds

**Non-Goals:**
- No gameplay logic implementation
- No database migrations
- No WebSocket real-time features
- No REST API implementation
- No card API sync
- No Deck Builder behavior
- No auth/JWT implementation
- No ranking, chat or animations

## Decisions

1. **Backend package structure**: Use `com.pokemontcg` as root package with sub-packages for config, common, cards, decks, matches and engine per the contract.

2. **Frontend folder structure**: Use `core/`, `shared/`, and `features/` folders under `src/app/` per the contract.

3. **Engine isolation**: The `engine/` package will contain only plain Java classes with no Spring annotations, no JPA, no REST controllers, no WebSocket classes and no repositories. This ensures the game engine remains testable and portable.

4. **Placeholder files**: Create empty or stub classes for all DTO folders to enable compilation. Place README files where logical groupings exist.

5. **Test strategy**: Include a simple `@SpringBootTest` that verifies the application context loads successfully.

## Risks / Trade-offs

- [Risk] New developers may not have Java 21 or Angular 21+ installed → Mitigation: Document version requirements in README files
- [Risk] Maven/npm dependencies may have conflicts → Mitigation: Use stable, widely-used dependency versions
- [Risk] Placeholder classes may become stale → Mitigation: Clear documentation that these are skeleton placeholders for later implementation