## 1. Base DTOs and Config (Level 0)

- [ ] 1.1 Add `path` (String) and `details` (Map<String,String>) fields to `ErrorApi` DTO
- [ ] 1.2 Create `MatchStateResponse` record with `matchId`, `publicState`, `privateState`
- [ ] 1.3 Add `findTopByMatchIdOrderByVersionDesc(UUID matchId)` query to `MatchStateJpaRepository`
- [ ] 1.4 Add `SetupManager` bean to `GameEngineConfig` (depends on `EventPublisherPort`, `CardLookupPort`, `RandomizerPort`)

## 2. Mapper and Query Service (Level 0)

- [ ] 2.1 Add `@Component` to `MatchMapper` and implement `toMatchResponse(MatchEntity, List<MatchPlayerEntity>)` with all response fields
- [ ] 2.2 Add `@Service` to `MatchQueryService` and implement `buildPublicState(GameState)` returning `PublicGameState` without hand/deck info
- [ ] 2.3 Implement `buildPrivateState(GameState, UUID)` returning `PrivatePlayerState` with player's hand cards, or null for invalid player

## 3. Global Exception Handler (Level 1)

- [ ] 3.1 Add `@ExceptionHandler(IllegalArgumentException.class)` to `GlobalExceptionHandler` returning HTTP 400 with `ErrorApi` including `path` and `details`
- [ ] 3.2 Verify existing `DomainException` and generic handlers still work correctly

## 4. State Persistence (Level 1)

- [ ] 4.1 Inject `MatchStateJpaRepository` and `ObjectMapper` into `StatePersisterAdapter`
- [ ] 4.2 Implement `saveState(UUID, GameState)`: serialize to JSON, create `MatchStateEntity` with incremented version, save
- [ ] 4.3 Implement `loadState(UUID)`: query latest version, deserialize JSON, return `GameState` (or null if not found)

## 5. Match Application Service (Level 2)

- [ ] 5.1 Add `@Service` to `MatchApplicationService` and inject `GameEngine`, `SetupManager`, `StatePersisterPort`, `DeckLoadPort`, `MatchMapper`, `MatchQueryService`, `EventPublisherPort`
- [ ] 5.2 Implement `createMatch(CreateMatchRequest)`: load decks via `DeckLoadPort`, create `MatchEntity`, persist, return `MatchResponse`
- [ ] 5.3 Implement `joinMatch(UUID, JoinMatchRequest)`: add second player, run setup via `SetupManager`, set initial turn, persist, return `MatchResponse`
- [ ] 5.4 Implement `executeAction(UUID, GameActionRequest)`: call `GameEngine.applyAction()`, persist state, publish events, return `GameActionResponse`
- [ ] 5.5 Implement `getMatchState(UUID, UUID)`: load state, build public/private views via `MatchQueryService`, return `MatchStateResponse`

## 6. REST Controllers (Level 3)

- [ ] 6.1 Implement `POST /api/matches` in `MatchController` delegating to `MatchApplicationService.createMatch()`
- [ ] 6.2 Implement `POST /api/matches/{id}/join` in `MatchController` delegating to `MatchApplicationService.joinMatch()`
- [ ] 6.3 Implement `GET /api/matches/{id}/state` in `MatchController` delegating to `MatchApplicationService.getMatchState()`
- [ ] 6.4 Implement `POST /api/matches/{id}/actions` in `GameActionController` delegating to `MatchApplicationService.executeAction()`

## 7. WebSocket (Level 3)

- [ ] 7.1 Inject `MatchApplicationService` into `MatchWebSocketController`
- [ ] 7.2 Replace hardcoded response with `MatchApplicationService.executeAction()` call

## 8. Verification

- [ ] 8.1 Run `mvn compile` and verify zero compilation errors
- [ ] 8.2 Run `mvn test` and verify all existing tests pass
- [ ] 8.3 Verify that no class inside `engine/` imports Spring or JPA packages
