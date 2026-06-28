## 1. Backend Setup

- [x] 1.1 Create /backend directory with Maven project structure (pom.xml, src/main/java, src/main/resources)
- [x] 1.2 Create PokemonTcgApplication.java main class in com.pokemontcg package
- [x] 1.3 Add Spring Boot 3.x dependencies to pom.xml (web, validation, test)
- [x] 1.4 Create config/ package with CorsConfig.java, WebSocketConfig.java, OpenApiConfig.java stubs
- [x] 1.5 Create common/error/ package with ApiErrorResponse.java, GlobalExceptionHandler.java stubs
- [x] 1.6 Create common/ids/ package with MatchId.java, PlayerId.java, CardId.java, CardInstanceId.java, DeckId.java stubs

## 2. Backend Cards Module

- [x] 2.1 Create cards/api/ package with CardController.java stub
- [x] 2.2 Create cards/api/dto/ package with CardSummaryResponse.java, CardDetailResponse.java, CardSearchRequest.java stubs
- [x] 2.3 Create cards/application/ package with CardCatalogService.java, CardCacheSyncService.java stubs
- [x] 2.4 Create cards/domain/ package with CardDefinition.java, PokemonCardDefinition.java, EnergyCardDefinition.java, TrainerCardDefinition.java stubs
- [x] 2.5 Create cards/infrastructure/ package with CardEntity.java, CardJpaRepository.java, PokemonTcgApiClient.java stubs

## 3. Backend Decks Module

- [x] 3.1 Create decks/api/ package with DeckController.java stub
- [x] 3.2 Create decks/api/dto/ package with DeckResponse.java, DeckCardResponse.java, CreateDeckRequest.java, UpdateDeckRequest.java, DeckValidationResponse.java stubs
- [x] 3.3 Create decks/application/ package with DeckService.java, DeckValidator.java, SeedDeckService.java stubs
- [x] 3.4 Create decks/domain/ package with Deck.java, DeckCard.java, DeckValidationResult.java, DeckValidationError.java stubs
- [x] 3.5 Create decks/infrastructure/ package with DeckEntity.java, DeckCardEntity.java, DeckJpaRepository.java, DeckCardJpaRepository.java stubs

## 4. Backend Matches Module

- [x] 4.1 Create matches/api/ package with MatchController.java, GameActionController.java stubs
- [x] 4.2 Create matches/api/dto/ package with CreateMatchRequest.java, JoinMatchRequest.java, MatchResponse.java, GameActionRequest.java, GameActionResponse.java stubs
- [x] 4.3 Create matches/application/ package with MatchApplicationService.java, MatchQueryService.java stubs
- [x] 4.4 Create matches/domain/ package with Match.java, MatchStatus.java stubs
- [x] 4.5 Create matches/infrastructure/ package with MatchEntity.java, MatchStateEntity.java, MatchLogEntity.java, MatchJpaRepository.java stubs
- [x] 4.6 Create matches/websocket/ package with MatchWebSocketPublisher.java, MatchWebSocketController.java stubs

## 5. Backend Engine Package

- [x] 5.1 Create engine/ package with GameEngine.java stub (plain Java, no Spring annotations)
- [x] 5.2 Create engine/model/ package with GameState.java, PlayerState.java, BoardState.java, PokemonInPlay.java, AttachedCard.java, TurnFlags.java, PublicGameState.java, PrivatePlayerState.java stubs
- [x] 5.3 Create engine/action/ package with GameAction.java, GameActionType.java, ActionResult.java, GameError.java stubs
- [x] 5.4 Create engine/event/ package with GameEvent.java, GameEventType.java stubs
- [x] 5.5 Create engine/setup/ package with SetupManager.java, MulliganService.java stubs
- [x] 5.6 Create engine/turn/ package with TurnManager.java, TurnPhase.java stub
- [x] 5.7 Create engine/rules/ package with RuleValidator.java stub
- [x] 5.8 Create engine/attack/ package with AttackResolver.java, AttackStep.java, DamageCalculator.java, EnergyRequirementValidator.java stubs
- [x] 5.9 Create engine/status/ package with StatusEffectManager.java stub
- [x] 5.10 Create engine/victory/ package with VictoryConditionChecker.java, FinishReason.java stub
- [x] 5.11 Create engine/ports/ package with CardLookupPort.java, RandomizerPort.java, StatePersisterPort.java, EventPublisherPort.java stubs

## 6. Backend Enums

- [x] 6.1 Create MatchStatus.java enum (WAITING, SETUP, ACTIVE, FINISHED)
- [x] 6.2 Create TurnPhase.java enum (DRAW, MAIN, ATTACK, BETWEEN_TURNS)
- [x] 6.3 Create PlayerSide.java enum (PLAYER_ONE, PLAYER_TWO)
- [x] 6.4 Create CardSupertype.java enum (POKEMON, ENERGY, TRAINER)
- [x] 6.5 Create PokemonStage.java enum (BASIC, STAGE_1, STAGE_2, MEGA, RESTORED)
- [x] 6.6 Create EnergyType.java enum (GRASS, FIRE, WATER, LIGHTNING, PSYCHIC, FIGHTING, DARKNESS, METAL, FAIRY, COLORLESS)
- [x] 6.7 Create TrainerType.java enum (ITEM, STADIUM, SUPPORTER)
- [x] 6.8 Create SpecialCondition.java enum (ASLEEP, BURNED, CONFUSED, PARALYZED, POISONED)
- [x] 6.9 Create GameActionType.java enum
- [x] 6.10 Create GameEventType.java enum
- [x] 6.11 Create FinishReason.java enum
- [x] 6.12 Create DeckValidationError.java enum

## 7. Backend Verification

- [x] 7.1 Run `mvn compile` and verify no compilation errors
- [x] 7.2 Create Spring context load test (PokemonTcgApplicationTests.java) that verifies context loads
- [x] 7.3 Run `mvn test` and verify tests pass

## 8. Frontend Setup

- [x] 8.1 Create /frontend directory with Angular 21+ project structure
- [x] 8.2 Create core/api/ folder with api-client.service.ts, match-api.service.ts, card-api.service.ts, deck-api.service.ts stubs
- [x] 8.3 Create core/websocket/ folder with match-socket.service.ts stub
- [x] 8.4 Create core/interceptors/ folder (empty or placeholder)
- [x] 8.5 Create core/error/ folder (empty or placeholder)

## 9. Frontend Shared Models

- [x] 9.1 Create shared/models/ folder with card.models.ts, deck.models.ts, game-state.models.ts, game-action.models.ts, game-event.models.ts, api-error.models.ts stubs

## 10. Frontend Shared Components

- [x] 10.1 Create shared/components/card-image/ placeholder folder
- [x] 10.2 Create shared/components/loading/ placeholder folder
- [x] 10.3 Create shared/components/error-message/ placeholder folder

## 11. Frontend Features

- [x] 11.1 Create features/lobby/ with lobby-page.component.ts, lobby-page.component.html, lobby-page.component.css stubs
- [x] 11.2 Create features/match/pages/ with match-page.component.ts, match-page.component.html, match-page.component.css stubs
- [x] 11.3 Create features/match/components/board/ placeholder folder
- [x] 11.4 Create features/match/components/player-area/ placeholder folder
- [x] 11.5 Create features/match/components/opponent-area/ placeholder folder
- [x] 11.6 Create features/match/components/active-pokemon-slot/ placeholder folder
- [x] 11.7 Create features/match/components/bench-zone/ placeholder folder
- [x] 11.8 Create features/match/components/hand-zone/ placeholder folder
- [x] 11.9 Create features/match/components/prize-zone/ placeholder folder
- [x] 11.10 Create features/match/components/discard-zone/ placeholder folder
- [x] 11.11 Create features/match/components/action-panel/ placeholder folder
- [x] 11.12 Create features/match/components/game-log/ placeholder folder
- [x] 11.13 Create features/match/services/ with match-facade.service.ts, game-action-dispatcher.service.ts stubs
- [x] 11.14 Create features/decks/pages/ with deck-list-page.component.ts, deck-builder-page.component.ts stubs
- [x] 11.15 Create features/decks/components/card-search/ placeholder folder
- [x] 11.16 Create features/decks/components/deck-card-list/ placeholder folder
- [x] 11.17 Create features/decks/components/deck-validation-panel/ placeholder folder
- [x] 11.18 Create features/decks/services/ with deck-builder-facade.service.ts stub
- [x] 11.19 Create features/auth/ folder with README.md stating auth is postponed

## 12. Frontend Verification

- [x] 12.1 Run `npm install` and verify dependencies install
- [x] 12.2 Run `npm run build` and verify build completes with no errors