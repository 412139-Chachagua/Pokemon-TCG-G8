## Why

El engine de juego (Personas 1-3) ya implementa toda la lógica de reglas, handlers y victoria, pero no tiene conexión con Spring, persistencia ni REST. Sin Persona 4 no se puede iniciar una partida real, enviar acciones desde el frontend, ni persistir el estado entre requests. Este cambio implementa la capa de integración que hace funcionar el sistema completo.

## What Changes

- **StatePersisterAdapter**: implementación real con `MatchStateJpaRepository` + `ObjectMapper` para serializar/deserializar `GameState` como JSON
- **MatchStateJpaRepository**: agregar query `findTopByMatchIdOrderByVersionDesc`
- **GameEngineConfig**: agregar bean de `SetupManager`
- **GlobalExceptionHandler**: agregar handler `IllegalArgumentException` → 400, agregar campos `path` y `details` a `ErrorApi`
- **MatchApplicationService**: implementar `createMatch`, `joinMatch`, `executeAction`, `getMatchState`
- **MatchQueryService**: implementar `buildPublicState` y `buildPrivateState`
- **MatchMapper**: implementar mapeos `MatchEntity` → `MatchResponse`
- **MatchStateResponse**: crear DTO record
- **MatchController**: endpoints `POST /api/matches`, `POST /api/matches/{id}/join`, `GET /api/matches/{id}/state`
- **GameActionController**: endpoint `POST /api/matches/{id}/actions`
- **MatchWebSocketController**: reemplazar placeholder por delegación real a `MatchApplicationService.executeAction`

## Capabilities

### New Capabilities
- `match-rest-api`: Endpoints REST para crear partida, unirse y consultar estado
- `game-action-execution`: Endpoint REST + WebSocket para enviar acciones al engine
- `game-state-persistence`: Persistencia y carga del `GameState` via JPA + JSON
- `state-projection`: Proyección de `GameState` a vistas públicas/privadas para el frontend

### Modified Capabilities
<!-- No existing capabilities are changing at spec level -->

## Impact

- `services/matches/MatchApplicationService` se convierte en el hub central que coordina `GameEngine`, `SetupManager`, `StatePersisterPort` y `MatchQueryService`
- `configs/GameEngineConfig` requiere nuevo bean de `SetupManager` (depende de `EventPublisherPort`, `CardLookupPort`, `RandomizerPort`)
- `advice/GlobalExceptionHandler` cambia su respuesta para incluir `path` y `details` en `ErrorApi`
- WebSocket controller deja de ser placeholder y ejecuta acciones reales
- Ninguna clase del engine (`engine/model/`, `engine/handlers/`, `engine/rules/`, etc.) es modificada
