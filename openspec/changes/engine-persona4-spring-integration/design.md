## Context

El engine de juego (Personas 1-3) está completo: modelos, handlers, rules, turn manager, victory checker. Sin embargo no hay capa Spring que lo conecte con el mundo exterior. Persona 4 debe implementar los adaptadores de persistencia, los servicios de aplicación, los controladores REST y la configuración Spring que integran el engine con la infraestructura.

El estado actual del código (verificado contra `docs/divisionEnginePersona4.md`):
- `StatePersisterAdapter`: stub vacío (métodos sin implementación)
- `GameEngineConfig`: faltante bean de `SetupManager`
- `GlobalExceptionHandler`: sin handler para `IllegalArgumentException`
- `MatchApplicationService`, `MatchQueryService`, `MatchMapper`: clases vacías sin anotaciones
- `MatchController`, `GameActionController`: `@RestController` sin endpoints
- `MatchWebSocketController`: placeholder con respuesta fija
- `ErrorApi`: faltan campos `path` y `details`
- `MatchStateJpaRepository`: sin query custom
- `MatchStateResponse`, `SelectActiveRequest`, `TakePrizeRequest`: no existen

## Goals / Non-Goals

**Goals:**
- Implementar `StatePersisterAdapter` con serialización JSON via `ObjectMapper` + `MatchStateJpaRepository`
- Agregar query `findTopByMatchIdOrderByVersionDesc` a `MatchStateJpaRepository`
- Agregar bean de `SetupManager` en `GameEngineConfig`
- Agregar handler `IllegalArgumentException` → 400 + campos `path`/`details` a `ErrorApi`
- Implementar `MatchApplicationService` con `createMatch`, `joinMatch`, `executeAction`, `getMatchState`
- Implementar `MatchQueryService` con `buildPublicState` y `buildPrivateState`
- Implementar `MatchMapper` con mapeos completos
- Crear `MatchStateResponse` DTO
- Implementar endpoints REST en `MatchController` y `GameActionController`
- Reemplazar placeholder de `MatchWebSocketController` por delegación real

**Non-Goals:**
- No tocar ninguna clase dentro de `engine/model/`, `engine/handlers/`, `engine/rules/`, `engine/setup/`, `engine/turn/`, `engine/victory/`
- No implementar `CardLookupAdapter` ni `DeckLoadAdapter` (responsabilidad de Catálogo)
- No implementar autenticación, JWT, ranking, chat ni event sourcing
- No modificar `GameEngine` ni `EngineContext`

## Decisions

1. **StatePersisterAdapter usa ObjectMapper directo** en vez de `@Convert` JPA. Justificación: ya existe bean `ObjectMapper` en `MappersConfig`, y `MatchStateEntity.serializedState` es columna `TEXT` sin necesidad de converter JPA. Alternativa descartada: `GameStateConverter` como `AttributeConverter` — agregaría complejidad innecesaria.

2. **Versionado de estados**: `MatchStateEntity` almacena cada versión del estado como fila separada con contador incremental. La query `findTopByMatchIdOrderByVersionDesc` obtiene la última versión. Esto permite recovery ante corrupción y simplifica el debugging sin implementar event sourcing completo.

3. **MatchApplicationService como hub central**: toda acción de juego pasa por este service, que orquesta `GameEngine.applyAction()`, `StatePersisterPort.saveState()`, `EventPublisherPort.publishEvents()`, y arma la respuesta via `MatchQueryService`. Esto mantiene los controllers livianos y testables.

4. **WebSocket delegado al mismo service**: el `MatchWebSocketController` llama a `MatchApplicationService.executeAction()` en vez de duplicar lógica. Esto asegura que la acción se procese igual vía REST o WebSocket.

5. **ErrorApi con path y details**: se agregan campos opcionales para cumplir el contrato 13. `path` indica la URL del request, `details` permite errores de validación campo-por-campo.

## Risks / Trade-offs

- [Risk] `StatePersisterAdapter` con `ObjectMapper.writeValueAsString` puede fallar si `GameState` tiene referencias circulares → Mitigation: `GameState` es un grafo acíclico simple (players → PokemonInPlay → CardInstance). Verificar con test de serialización.
- [Risk] MatchApplicationService sin manejo de concurrencia → Mitigation: en V1 se asume que no hay requests simultáneos por match. El WebSocket y REST comparten el mismo service, Spring maneja un bean singleton.
- [Trade-off] `MatchStateEntity` guarda versiones históricas pero no hay purge policy → Aceptado para V1. En V2 se puede agregar TTL o archivo.
