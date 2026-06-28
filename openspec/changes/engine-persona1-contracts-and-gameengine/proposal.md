## Why

El engine del backend Pokémon TCG es **bloqueante para todo el equipo**: define los modelos de estado, los payloads de acciones, los códigos de error, el contexto de ejecución y el flujo central `GameEngine.applyAction()`. Sin estos tipos compilando, las Personas 2 (reglas), 3 (handlers) y 4 (REST/WebSocket) no pueden arrancar. Este change establece la infraestructua base del paquete `engine/` como Java 21 puro, sin dependencias Spring ni JPA.

## What Changes

- Crear `CardInstance`, `GameMetadata`, `GamePhase` en `engine/model/`
- Completar `GameState`, `PlayerState`, `PokemonInPlay`, `TurnFlags` según contrato 06
- Crear `GameActionPayload` (interface marker) y todos los payloads concretos (`AttachEnergyPayload`, `DeclareAttackPayload`, `RetreatPayload`, `PlayTrainerPayload`, `EvolvePokemonPayload`, `PutBasicOnBenchPayload`)
- Completar `GameAction`, `ActionResult`, `GameError` según contrato 08
- Verificar/ajustar `GameActionType` contra contrato 03 (eliminar valores V0 no contemplados)
- Crear `ErrorCode` enum con todos los códigos del contrato 09
- Crear `EngineContext` que envuelve `GameState` mutable + ports + acumulador de eventos
- Crear interface `ActionHandler` con método `void handle(EngineContext ctx)`
- Implementar `GameEngine.applyAction()` con el flujo de 11 pasos del spec
- Crear stubs compilables para `RuleValidator`, `SetupManager`, `VictoryConditionChecker`, `TurnManager`
- Verificar `PlayerSide` y `SpecialCondition` contra contratos

## Capabilities

### New Capabilities
- `engine-contracts`: Modelos del estado (`GameState`, `PlayerState`, `PokemonInPlay`, `TurnFlags`, `CardInstance`, `GameMetadata`, `GamePhase`), tipos de acción (`GameAction`, `GameActionType`, `GameActionPayload`, payloads concretos, `ActionResult`, `GameError`), enum `ErrorCode`, `EngineContext`, interface `ActionHandler`, y el flujo central `GameEngine.applyAction()` con stubs de las dependencias

### Modified Capabilities
<!-- No existing specs are modified; this is the first engine capability -->

## Impact

- `BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/` — paquete completo (model, action, handlers, ports, rules, setup, turn, victory)
- `BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/ports/` — interfaces `CardLookupPort`, `RandomizerPort`, `StatePersisterPort`, `EventPublisherPort`
- Las clases stub (`RuleValidator`, `SetupManager`, `VictoryConditionChecker`, `TurnManager`) serán implementadas por otras Personas más adelante
- Los contratos obligatorios (00, 01, 02, 03, 06, 08, 09, 12) determinan la forma exacta de cada clase
