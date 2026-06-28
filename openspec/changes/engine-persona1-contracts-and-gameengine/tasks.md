## 1. Modelos del estado (engine/model/)

- [x] 1.1 Crear `CardInstance` con campos `instanceId` (UUID) y `cardDefinitionId` (String)
- [x] 1.2 Crear `GameMetadata` class en `engine/model/`
- [x] 1.3 Crear `GamePhase` enum con valores `DRAW`, `MAIN`, `ATTACK`, `BETWEEN_TURNS`
- [x] 1.4 Completar `GameState` con todos los campos del contrato 06 (matchId, status, phase, turnNumber, currentPlayerId, firstPlayerId, players, stadiumCardInstanceId, turnFlags, pendingDecision, winnerPlayerId, finishReason, createdAt, updatedAt)
- [x] 1.5 Reemplazar `List<UUID>` por `List<CardInstance>` en los campos `deck`, `hand`, `prizes` y `discard` de `PlayerState`. Campos completos: `playerId`, `side`, `deck: List<CardInstance>`, `hand: List<CardInstance>`, `prizes: List<CardInstance>`, `discard: List<CardInstance>`, `activePokemon: PokemonInPlay`, `bench: List<PokemonInPlay>`, `mulliganCount: int` 
- [x] 1.6 Completar `PokemonInPlay` agregando `enteredTurnNumber`, `evolvedThisTurn`, `attachedEnergies` (List&lt;CardInstance&gt;), y `toolCardInstanceId`
- [x] 1.7 Completar `TurnFlags` agregando `hasPlayedSupporter` y `hasPlayedStadium`

## 2. Tipos de acción y payloads (engine/action/)

- [x] 2.1 Crear `GameActionPayload` como interface marker
- [x] 2.2 Completar `GameAction` con `type`, `playerId`, `payload`, `clientRequestId`
- [x] 2.3 Verificar/ajustar `GameActionType`: mantener solo `PUT_BASIC_ON_BENCH`, `ATTACH_ENERGY`, `EVOLVE_POKEMON`, `PLAY_TRAINER`, `RETREAT_ACTIVE`, `DECLARE_ATTACK`, `END_TURN`; los demás marcar como `@Deprecated`
- [x] 2.4 Crear `PutBasicOnBenchPayload` con campo `handIndex` (int)
- [x] 2.5 Crear `AttachEnergyPayload` con campos `handIndex` (int) y `targetPokemonInstanceId` (UUID)
- [x] 2.6 Crear `DeclareAttackPayload` con campos `attackIndex` (int) y `targetPokemonInstanceId` (UUID)
- [x] 2.7 Crear `RetreatPayload` con campo `benchIndex` (int)
- [x] 2.8 Crear `EvolvePokemonPayload` con campos `handIndex` (int) y `targetPokemonInstanceId` (UUID)
- [x] 2.9 Crear `PlayTrainerPayload` con campo `handIndex` (int)
- [x] 2.10 Completar ActionResult con success, clientRequestId, publicState (Object por ahora), privateState (Object por ahora), events (List<String>), error (GameError nullable)
- [x] 2.11 Verificar `GameError` tiene `code` (String), `message` (String), `details` (Map&lt;String,Object&gt;, nullable)

## 3. EngineContext, ErrorCode y enums raíz (engine/)

- [x] 3.1 Crear `ErrorCode` enum con los 15 valores del contrato 09
- [x] 3.2 Crear `EngineContext` con `GameState`, `CardLookupPort`, `RandomizerPort`, y acumulador de eventos (`addEvent`, `getEvents`)
- [x] 3.3 Verificar `PlayerSide` tiene `PLAYER_ONE` y `PLAYER_TWO`
- [x] 3.4 Verificar `SpecialCondition` tiene `ASLEEP`, `BURNED`, `CONFUSED`, `PARALYZED`, `POISONED`

## 4. ActionHandler interface (engine/handlers/)

- [x] 4.1 Crear interface ActionHandler con método: void handle(GameAction action, EngineContext ctx)

## 5. Ports interfaces (engine/ports/)

- [x] 5.1 Crear/verificar `CardLookupPort` interface
- [x] 5.2 Crear/verificar `RandomizerPort` interface
- [x] 5.3 Crear/verificar `StatePersisterPort` con `loadState(UUID)` y `saveState(UUID, GameState)`
- [x] 5.4 Crear/verificar `EventPublisherPort` interface

## 6. Stubs para otras Personas

- [x] 6.1 Crear `RuleValidator` stub con `validate(EngineContext, GameAction)` que siempre retorna válido
- [x] 6.2 Crear `SetupManager` stub (clase vacía compilable)
- [x] 6.3 Crear `VictoryConditionChecker` stub con `check(EngineContext)` que siempre retorna `Optional.empty()`
- [x] 6.4 Crear `TurnManager` stub (clase vacía compilable)

## 7. GameEngine.applyAction()

- [x] 7.1 Implementar `GameEngine` con constructor que recibe los 4 ports
- [x] 7.2 Implementar applyAction() siguiendo exactamente este orden: (1) loadState, (2) verificar ACTIVE, (3) verificar currentPlayerId, (4) crear EngineContext, (5) RuleValidator.validate → si falla retornar error, (6) dispatcher de handler, (7) VictoryConditionChecker.check, (8) si hay ganador setear winner + FINISHED, (9) StatePersisterPort.saveState, (10) EventPublisherPort publicar, (11) retornar ActionResult con success=true
- [x] 7.3 Integrar dispatcher de handlers usando: `Map<GameActionType, ActionHandler>` inyectado por constructor. NO usar switch-case.

## 8. Verificación

- [x] 8.1 Ejecutar `mvn compile` en `BE/` y verificar que no hay errores
- [x] 8.2 Ejecutar `mvn test` y verificar que `ApplicationTests.contextLoads` pasa
- [x] 8.3 Ejecutar grep -r "org.springframework\|jakarta.persistence\|javax.persistence" BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/ y confirmar que la salida está vacía
