## 1. CardMapper Implementation

- [x] 1.1 Create CardMapper component with ModelMapper for field mappings
- [x] 1.2 Implement PokemonTcgApiCardRequest to CardEntity mapping (all supertypes)
- [x] 1.3 Implement CardEntity to CardSummaryResponse mapping
- [x] 1.4 Implement CardEntity to CardDetailResponse mapping (includes attacks, weaknesses, resistances)
- [x] 1.5 Implement subtypes comma-separated string to List mapping
- [x] 1.6 Implement hp String to int parsing with null/empty handling

## 2. CardCacheSyncService Implementation

- [x] 2.1 Create CardCacheSyncService as @Component with @Transactional support
- [x] 2.2 Implement syncAll() to fetch Pokemon, Trainer, Energy cards via PokemonTcgApiClient
- [x] 2.3 Implement upsert logic (existsById check, merge or persist via CardJpaRepository)
- [x] 2.4 Add @PostConstruct for automatic sync at application startup
- [x] 2.5 Add error handling and logging for API failures

## 3. CardCatalogService Implementation

- [x] 3.1 Create CardCatalogService as @Service
- [x] 3.2 Implement searchCards(CardSearchRequest) with dynamic Specification filters
- [x] 3.3 Support filters: query (LIKE on name), supertype, setCode
- [x] 3.4 Implement Spring Data Pageable pagination for search results
- [x] 3.5 Implement getCardById(String id) returning CardDetailResponse
- [x] 3.6 Handle not-found case with NotFoundException

## 4. CardController Implementation

- [x] 4.1 Create CardController as @RestController
- [x] 4.2 Implement GET /api/cards endpoint with query params mapped to CardSearchRequest
- [x] 4.3 Implement GET /api/cards/{id} endpoint for card detail
- [x] 4.4 Implement POST /api/cards/sync endpoint for manual sync trigger
- [x] 4.5 Add Swagger/OpenAPI documentation annotations
- [x] 4.6 Ensure proper HTTP status codes (200 for success, 404 for not found, 500 for errors)

## 5. Configuration

- [x] 5.1 Add @EnableCaching in a CacheConfig class
- [x] 5.2 Verify CardLookupAdapter @Cacheable(cards) is properly configured
- [x] 5.3 Ensure CardCacheSyncService runs after application is ready

## 6. Verification

- [x] 6.1 Build project with mvn clean compile (verify no compilation errors)
- [x] 6.2 Run existing tests to verify no regressions
- [ ] 6.3 Start application and verify automatic sync at startup
- [ ] 6.4 Test GET /api/cards with various filter parameters
- [ ] 6.5 Test GET /api/cards/{id} for existing and non-existing cards
- [ ] 6.6 Test POST /api/cards/sync for manual sync
- [ ] 6.7 Verify Swagger UI shows card endpoints
- [ ] 6.8 Verify CardLookupAdapter caching works (second lookup is faster)
