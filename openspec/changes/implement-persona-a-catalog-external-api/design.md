## Context

The backend currently has a dual-entity structure for cards:
- **Client-side entities** (clients/entity/): 3 separate tables (PokemonCardEntity, EnergyCardEntity, TrainerCardEntity) — legacy from a previous implementation
- **Canonical entities** (repositories/entities/): Single unified CardEntity table with @OneToMany relationships to attacks, weaknesses, resistances — already used by CardLookupAdapter and the game engine

The PokemonTcgApiClient is fully implemented but maps API responses (PokemonTcgApiCardResponse) to internal request DTOs (PokemonTcgApiCardRequest), not to entities. The CardCacheSyncService is entirely commented out and was targeting the client-side (legacy) entities. CardCatalogService and CardController are empty stubs. CardMapper does not exist.

The game engine depends on CardLookupPort to retrieve card data via CardLookupAdapter, which is fully implemented and uses CardJpaRepository (canonical entities).

## Goals / Non-Goals

**Goals:**
- Re-implement CardCacheSyncService to fetch cards from Pokemon TCG API via existing PokemonTcgApiClient and persist to canonical CardEntity table
- Implement CardCatalogService for local catalog queries with filtering and pagination
- Implement CardController with endpoints for search, detail, and manual sync
- Implement CardMapper for bidirectional mapping between API DTOs, canonical entities, and domain definitions
- Confirm existing RestTemplate is sufficient (no need for WebFlux)
- Configure @EnableCaching for CardLookupAdapter
- Document endpoints in Swagger/OpenAPI

**Non-Goals:**
- Do NOT modify PokemonTcgApiClient (already working — only adjust mapper to target canonical entities)
- Do NOT modify CardLookupAdapter (already fully functional)
- Do NOT modify game engine, match persistence, deck logic, or frontend
- Do NOT clean up client-side legacy entities (PokemonCardEntity etc.) — deferred

## Decisions

1. **Use canonical entities (not the legacy 3-table schema):**
   - CardLookupAdapter already queries CardEntity from CardJpaRepository
   - repositories/entities/CardEntity is a single-table design with polymorphic fields
   - The legacy clients/entity/ classes remain untouched but no longer used
   - Rationale: Avoids duplication; keeps sync-to-cache-to-adapter pipeline on one schema

2. **Reuse existing RestTemplate (no WebFlux needed):**
   - RestTemplateConfig and RestTemplate bean already exist
   - PokemonTcgApiClient is fully implemented with RestTemplate
   - Rationale: Minimize dependency changes; RestTemplate adequate for this use case

3. **CardMapper as central mapping layer:**
   - Uses ModelMapper (already in pom.xml) for simple field mappings
   - Custom conversion logic for subtypes, attacks, weaknesses, resistances
   - Maps: PokemonTcgApiCardRequest → CardEntity, CardEntity → CardSummaryResponse, CardEntity → CardDetailResponse
   - Rationale: Avoids scattered mapping logic

4. **CardCacheSyncService:**
   - @Component with @Transactional support
   - Fetch all cards from PokemonTcgApiClient (Pokemon + Trainer + Energy)
   - Map via CardMapper to CardEntity
   - Upsert by cardId: existsById → merge, else persist
   - Run at application startup via @PostConstruct
   - Extract subtypes from API response lists, parse hp from String to int

5. **CardCatalogService:**
   - searchCards(CardSearchRequest) → Page<CardSummaryResponse>
   - Build Spring Data Specification for dynamic filters (name LIKE, supertype, setCode, types, stage)
   - getCardById(String id) → CardDetailResponse

6. **CardController:**
   - GET /api/cards — query params → CardSearchRequest → CardCatalogService.searchCards()
   - GET /api/cards/{id} → CardCatalogService.getCardById()
   - POST /api/cards/sync → CardCacheSyncService.syncAll()

7. **Caching:**
   - Add @EnableCaching in a CacheConfig class
   - CardLookupAdapter @Cacheable(cards) — ensure enabled

## Risks / Trade-offs

- Risk: API rate limiting → Retry with backoff; sync at startup only (not periodic in MVP)
- Risk: Large sync payload (XY1 set ~140 cards) → @Transactional with appropriate isolation
- Risk: Legacy client-side entities become dead code → Accept as technical debt
- Risk: Single-table CardEntity has nullable columns per supertype → Already accepted in existing design

