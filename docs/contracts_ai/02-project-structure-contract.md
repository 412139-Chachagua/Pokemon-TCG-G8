# Project Structure Contract

## Rule

This file defines the canonical folder and package structure.

OpenCode must not create alternative package names, duplicated DTO folders, or parallel architectures.

## Backend root package

```
ar.edu.utn.frc.tup.piii
```

## Backend structure

```
src/main/java/ar/edu/utn/frc/tup/piii/
  Application.java
  advice/
    GlobalExceptionHandler.java
  cards/
    domain/
      CardDefinition.java                  (supertype: String)
      CardSupertype.java
      EnergyCardDefinition.java
      EnergyCardType.java
      EnergyType.java
      PokemonCardDefinition.java           (stage: String; clases anidadas AttackDefinition, WeaknessDefinition, ResistanceDefinition)
      PokemonStage.java
      TrainerCardDefinition.java
      TrainerSubtype.java
      TrainerType.java
  clients/
    PokemonTcgApiClient.java
    PokemonTcgApiResponse.java
  common/
    ids/
      CardId.java
      CardInstanceId.java
      DeckId.java
      MatchId.java
      PlayerId.java
  configs/
    CacheConfig.java                       (ConcurrentMapCacheManager "cards")
    GameEngineConfig.java
    MappersConfig.java
    RestTemplateConfig.java                (timeout 5s connect, 15s read)
    SpringDocConfig.java
    WebSocketConfig.java
  controllers/
    PingController.java
    cards/
      CardController.java
    decks/
      DeckController.java
    matches/
      GameActionController.java
      MatchController.java
    players/
      PlayerController.java
    users/
      UserController.java
  decks/
    domain/
      Deck.java
      DeckCard.java
      DeckValidationError.java
      DeckValidationResult.java
  dtos/
    cards/
      AbilityDto.java
      AttackDto.java
      CardDetailResponse.java
      CardSearchRequest.java
      CardSearchResponse.java
      CardSummaryResponse.java
      CardSyncResponse.java
      ImagesDto.java
      PokemonTcgApiCardDto.java
      ResistanceDto.java
      SetInfoDto.java
      WeaknessDto.java
    common/
      ErrorApi.java
    decks/
      CreateDeckRequest.java
      DeckCardResponse.java
      DeckResponse.java
      DeckValidationResponse.java
      UpdateDeckRequest.java
      ValidateDeckRequest.java
    matches/
      CreateMatchRequest.java
      GameActionRequest.java
      GameActionResponse.java               (record con GameEventDto, ErrorDto anidados)
      JoinMatchRequest.java
      MatchResponse.java
      MatchStateResponse.java
    players/
      PlayerResponse.java
      UpdatePlayerRequest.java
    users/
      CreateUserRequest.java
      LoginRequest.java
      UserResponse.java
  engine/
    EngineContext.java
    ErrorCode.java
    GameEngine.java
    MatchStatus.java
    PlayerSide.java
    SpecialCondition.java
    action/
      ActionResult.java
      GameAction.java                       (payload: Map<String, Object>)
      GameActionType.java                   (incluye DRAW_CARD, CHOOSE_KNOCKOUT_REPLACEMENT, TAKE_PRIZE_CARD)
      GameError.java
    attack/
      AttackResolver.java
      DamageCalculator.java                 (clase pública separada con DamageCalculatorResult record)
      EnergyRequirementValidator.java       (clase pública separada)
    ability/
      AbilityResolver.java                  (interface: resolve(EngineContext, PlayerState, PokemonInPlay, AbilityDefinition, Map<String,Object>))
      AbilityRegistry.java                  (Map<String, AbilityResolver> by ability name)
      resolvers/
        MysticalFireResolver.java
        WaterShurikenResolver.java
        FairyTransferResolver.java
        DriveOffResolver.java
        StanceChangeResolver.java
        UpsideDownEvolutionResolver.java
      hooks/
        FurCoatHook.java
        SweetVeilHook.java
        ForestsCurseHook.java
        SpikyShieldHook.java
        DestinyBurstHook.java
    event/
      GameEvent.java                        (type, matchId, turnNumber, createdAt, message, payload)
      GameEventType.java                    (14 valores: CARD_DRAWN, VICTORY_DECIDED, etc.)
    handlers/
      GameHandler.java                      (interface: handle(EngineContext, GameAction) → void)
      AttachEnergyHandler.java
      ChooseNewActiveAfterKnockoutHandler.java
      DeclareAttackHandler.java
      DrawCardHandler.java
      EndTurnHandler.java
      EvolvePokemonHandler.java
      HandlerHelper.java
      PlayTrainerHandler.java
      PutBasicOnBenchHandler.java
      RetreatActiveHandler.java
      TakePrizeCardHandler.java
    model/
      CardInstance.java
      GameState.java                        (incluye firstPlayerId, pendingPrizeOwnerPlayerId)
      PlayerState.java
      PokemonInPlay.java
      PrivatePlayerState.java               (con PrivateHandCard, PrizeSlot anidados)
      PublicGameState.java                  (con PublicPlayerState, PublicPokemonSlot anidados)
      TurnFlags.java
    ports/
      CardLookupPort.java
      DeckLoadPort.java
      EventPublisherPort.java               (publishEvents(UUID matchId, List<GameEvent> events))
      RandomizerPort.java
      StatePersisterPort.java
      impl/
        CardLookupAdapter.java
        DeckLoadAdapter.java
        RandomizerAdapter.java
        StatePersisterAdapter.java          (usa MatchJpaRepository + MatchStateJpaRepository)
    rules/
      RuleValidator.java
    setup/
      SetupManager.java
    turn/
      TurnManager.java
      TurnPhase.java
    victory/
      FinishReason.java
      VictoryConditionChecker.java
  exceptions/
    ConflictException.java
    DomainException.java
    NotFoundException.java
    ValidationException.java
  mappers/
    cards/
      ApiCardMapper.java                    (@Deprecated)
      CardMapper.java
    decks/
      DeckMapper.java
    matches/
      MatchMapper.java
  matches/
    domain/
      Match.java
      MatchStatus.java
  repositories/
    entities/
      CardAttackEntity.java
      CardEntity.java                       (entidad unificada)
      CardResistanceEntity.java
      CardWeaknessEntity.java
      DeckCardEntity.java
      DeckEntity.java
      PlayerEntity.java
      MatchEntity.java                      (con currentPhase, turnNumber, currentPlayerId, firstPlayerId)
      MatchLogEntity.java
      MatchPlayerEntity.java                (playerKind: String)
      MatchStateEntity.java                 (versionado, serializedState TEXT)
      UserEntity.java
      api_card/                             (@Deprecated)
        EnergyCardEntity.java
        PokemonCardAttackEntity.java
        PokemonCardEntity.java
        PokemonCardResistanceEntity.java
        PokemonCardWeaknessEntity.java
        TrainerCardEntity.java
    jpa/
      CardAttackJpaRepository.java
      CardJpaRepository.java
      CardResistanceJpaRepository.java
      CardWeaknessJpaRepository.java
      DeckCardJpaRepository.java
      DeckJpaRepository.java
      PlayerJpaRepository.java
      MatchJpaRepository.java
      MatchLogJpaRepository.java
      MatchPlayerJpaRepository.java
      MatchStateJpaRepository.java
      UserJpaRepository.java
      api_card/                             (@Deprecated)
        EnergyCardJpaRepository.java
        PokemonCardAttackJpaRepository.java
        PokemonCardJpaRepository.java
        PokemonCardResistanceJpaRepository.java
        PokemonCardWeaknessJpaRepository.java
        TrainerCardJpaRepository.java
  services/
    cards/
      CardCacheSyncService.java
      CardCatalogService.java
    decks/
      DeckService.java
      DeckValidator.java
      SeedDeckService.java
    matches/
      MatchApplicationService.java          (con locks por matchId)
      MatchQueryService.java
    players/
      PlayerService.java
    users/
      UserService.java
  websocket/
    MatchWebSocketController.java
    MatchWebSocketPublisher.java            (implementa EventPublisherPort)
```

## Frontend structure

```
frontend/
src/app/
  app.config.ts
  app.css
  app.html
  app.routes.ts
  app.spec.ts
  app.ts
  core/
    api/
      api-client.service.ts
      card-api.service.ts
      deck-api.service.ts
      match-api.service.ts
    websocket/
      match-socket.service.ts
  shared/
    models/
      api-error.models.ts
      card.models.ts
      deck.models.ts
      game-action.models.ts
      game-state.models.ts
  features/
    cards/
      routes.ts
      pages/
        card-catalog-page/
          card-catalog-page.ts
    decks/
      routes.ts
      pages/
        deck-list-page/
          deck-list-page.ts
        deck-builder-page/
          deck-builder-page.ts
      services/
        deck-builder-facade.service.ts
    lobby/
      routes.ts
      pages/
        lobby-page/
          lobby-page.ts
    match/
      routes.ts
      pages/
        match-page/
          match-page.ts
      services/
        game-action-dispatcher.service.ts
        match-facade.service.ts
```

## Dependency rules

### Backend

- controllers may depend on services and dtos
- controllers may depend on engine for action types and enums
- services may depend on domain, engine, repositories, mappers, clients, and websocket
- services may depend on dtos for request/response mapping
- repositories may depend on database/JPA/external APIs
- `common/ids/` no depende de nada, usado por domain y engine
- domain (cards/domain, decks/domain, matches/domain) must not depend on:
  - Spring annotations
  - JPA entities
  - REST controllers
  - WebSocket classes
  - repositories
  - database classes
- engine must not depend on:
  - Spring annotations
  - JPA entities
  - REST controllers
  - WebSocket classes
  - repositories
  - database classes
- engine ports/impl may depend on engine ports and infrastructure
- advice depends on exceptions and dtos
- mappers depend on domain and dtos
- websocket depends on engine/event para GameEventType y GameEvent

### Forbidden in domain and engine

Do not use:
- @RestController
- @Service
- @Repository
- @Entity
- @Autowired

The engine and domain packages must be Java-oriented, testable and isolated.

## Patrones de diseño (RNF-04)

El TPI recomienda los siguientes patrones. Estado actual de implementación:

| Patrón | Estado | Ubicación |
|--------|--------|-----------|
| **State** | IMPLEMENTADO PARCIAL | `MatchStatus`, `TurnPhase` (enums). No se usa State pattern formal con clases de estado. |
| **Strategy** | NO IMPLEMENTADO | Para efectos de cartas de Entrenador y efectos de ataques. Actualmente la lógica está en handlers individuales. |
| **Chain of Responsibility** | IMPLEMENTADO PARCIAL | `AttackResolver` pipeline secuencial (pasos 1-14). No implementa cadena formal con handlers encadenables. |
| **Observer** | IMPLEMENTADO PARCIAL | `EventPublisherPort` + `MatchWebSocketPublisher`. Publica eventos a los clientes vía WebSocket. |
| **Repository** | IMPLEMENTADO | `*JpaRepository` interfaces, Spring Data JPA. |
| **Facade** | IMPLEMENTADO PARCIAL | `GameEngine` como fachada del motor de reglas. Algunas responsabilidades están fuera (MatchApplicationService orquesta setup + engine). |

Pendiente: implementar formalmente los patrones faltantes y refactorizar los parciales para cumplir con RNF-04.

## Frontend rule

Frontend never decides game rules.

Frontend can:
- render state
- send GameActionRequest
- show available buttons
- display errors
- subscribe to WebSocket events
- lazy load feature routes

Frontend cannot:
- calculate official damage
- decide victory
- mutate match state locally
- reveal opponent hidden data
