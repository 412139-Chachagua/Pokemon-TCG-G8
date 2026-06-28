# AI Proposal Spec: engine-persona4-spring-integration

## Change name

engine-persona4-spring-integration

## Purpose

Implementar la capa Spring + persistencia + REST de Persona 4, que conecta el `GameEngine` (Persona 1-3) con la infraestructura del backend. Es la que integra los ports, persiste el estado del match, expone los endpoints REST y coordina el flujo completo desde la request del cliente hasta la respuesta.

El trabajo se basa en el detalle de `/docs/divisionEnginePersona4.md` (verificado contra el codebase real — el diagnóstico es exacto).

## Mandatory context files

OpenCode MUST read and obey:

- `BE/docs/contracts_ai/00-contract-index.md`
- `BE/docs/contracts_ai/01-project-scope-contract.md`
- `BE/docs/contracts_ai/02-project-structure-contract.md`
- `BE/docs/contracts_ai/06-game-state-contract.md`
- `BE/docs/contracts_ai/08-game-action-contract.md`
- `BE/docs/contracts_ai/13-api-rest-contract.md`
- `docs/divisionEnginePersona4.md`

## Architecture constraints

- El `engine/` package **nunca** importa anotaciones Spring/JPA. Las capas `configs/`, `controllers/`, `services/`, `repositories/` sí usan Spring normalmente.
- El `GameEngine` conoce sus ports solo por interfaces. Persona 4 implementa `StatePersisterPort` y `RandomizerPort` como adaptadores; `CardLookupPort` y `DeckLoadPort` los implementa Catálogo.
- `MatchApplicationService` es el **hub central**: orquesta `GameEngine.applyAction()`, `StatePersisterAdapter`, `MatchQueryService` y `MatchMapper`.
- Persistir solo el `GameState` actual. No hay event sourcing ni auditoría en V1.
- Los DTOs de request/response son `record` de Java 21 con validación Jakarta.

## Package root

```
ar.edu.utn.frc.tup.piii
```

## Scope — clases a implementar o modificar

Todas viven bajo `BE/src/main/java/ar/edu/utn/frc/tup/piii/`.

### engine/ports/impl/ — Adaptadores

| Clase | Estado | Acción |
|-------|--------|--------|
| `StatePersisterAdapter` | Stub (métodos vacíos) | Inyectar `MatchStateJpaRepository` + `ObjectMapper`. `saveState`: serializar `GameState` a JSON, crear `MatchStateEntity` (matchId + version + serializedState) y guardar. `loadState`: buscar último `MatchStateEntity` por matchId ordenado por version descendente |

### persistence/ o repositories/ — Capa de datos

| Clase | Estado | Acción |
|-------|--------|--------|
| `repositories/jpa/MatchStateJpaRepository` | Existe sin queries custom | Agregar `findTopByMatchIdOrderByVersionDesc(UUID matchId)` |

### configs/ — Configuración Spring

| Clase | Estado | Acción |
|-------|--------|--------|
| `GameEngineConfig` | Parcial (3 beans) | Agregar bean de `SetupManager(EventPublisherPort, CardLookupPort, RandomizerPort)` |

### advice/ — Manejo de errores

| Clase | Estado | Acción |
|-------|--------|--------|
| `GlobalExceptionHandler` | Parcial | Agregar handler para `IllegalArgumentException` → 400 con `ErrorApi` completo |

### dtos/ — Data Transfer Objects

| Clase | Estado | Acción |
|-------|--------|--------|
| `dtos/common/ErrorApi` | Existe (4 campos) | Agregar `path` (String) y `details` (Map<String,String>) |
| `dtos/matches/MatchStateResponse` | NO existe | Crear record con `matchId`, `publicState`, `privateState` |

### services/ — Lógica de aplicación

| Clase | Estado | Acción |
|-------|--------|--------|
| `services/matches/MatchApplicationService` | Vacío, sin `@Service` | Agregar `@Service`. Implementar `createMatch` (usa `DeckLoadPort` + `SetupManager` + persiste), `joinMatch`, `executeAction` (delega en `GameEngine.applyAction`), `getMatchState` |
| `services/matches/MatchQueryService` | Vacío, sin `@Service` | Agregar `@Service`. Implementar `buildPublicState` y `buildPrivateState` a partir de `GameState` usando `CardLookupPort` |

### mappers/ — Mapeo entre entidades y DTOs

| Clase | Estado | Acción |
|-------|--------|--------|
| `mappers/matches/MatchMapper` | Vacío, sin `@Component` | Agregar `@Component`. Implementar mapeos `MatchEntity` + `MatchPlayerEntity` → `MatchResponse` |

### controllers/ — Endpoints REST

| Clase | Estado | Acción |
|-------|--------|--------|
| `controllers/matches/MatchController` | `@RestController` sin endpoints | Implementar `POST /api/matches`, `POST /api/matches/{id}/join`, `GET /api/matches/{id}/state` |
| `controllers/matches/GameActionController` | `@RestController` sin endpoints | Implementar `POST /api/matches/{id}/actions` |

### websocket/ — Tiempo real

| Clase | Estado | Acción |
|-------|--------|--------|
| `websocket/MatchWebSocketController` | Placeholder (respuesta fija) | Reemplazar por delegación real a `MatchApplicationService.executeAction()` |

## Diseño detallado

### StatePersisterAdapter

```java
@Component
public class StatePersisterAdapter implements StatePersisterPort {
    private final MatchStateJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveState(UUID matchId, GameState state) {
        String json = objectMapper.writeValueAsString(state);
        MatchStateEntity entity = new MatchStateEntity();
        entity.setMatchId(matchId);
        entity.setSerializedState(json);
        // version = previous max + 1
        repository.save(entity);
    }

    @Override
    public GameState loadState(UUID matchId) {
        MatchStateEntity last = repository.findTopByMatchIdOrderByVersionDesc(matchId);
        if (last == null) return null;
        return objectMapper.readValue(last.getSerializedState(), GameState.class);
    }
}
```

### MatchStateJpaRepository — query a agregar

```java
@Repository
public interface MatchStateJpaRepository extends JpaRepository<MatchStateEntity, UUID> {
    Optional<MatchStateEntity> findTopByMatchIdOrderByVersionDesc(UUID matchId);
}
```

### GameEngineConfig — bean de SetupManager

```java
@Bean
public SetupManager setupManager(EventPublisherPort eventPublisher, CardLookupPort cardLookup, RandomizerPort randomizer) {
    return new SetupManager(eventPublisher, cardLookup, randomizer);
}
```

### ErrorApi — campos a agregar

```java
private String path;
private Map<String, String> details;
```

### MatchStateResponse

```java
public record MatchStateResponse(
    UUID matchId,
    PublicGameState publicState,
    PrivatePlayerState privateState
) {}
```

### MatchApplicationService

```java
@Service
public class MatchApplicationService {
    // Inyecta: GameEngine, StatePersisterPort, SetupManager, DeckLoadPort, MatchMapper, MatchQueryService

    public MatchResponse createMatch(CreateMatchRequest request) { ... }
    public MatchResponse joinMatch(UUID matchId, JoinMatchRequest request) { ... }
    public GameActionResponse executeAction(UUID matchId, GameActionRequest request) { ... }
    public MatchStateResponse getMatchState(UUID matchId, UUID playerId) { ... }
}
```

### MatchQueryService

```java
@Service
public class MatchQueryService {
    public PublicGameState buildPublicState(GameState state) { ... }    // filtra info oculta
    public PrivatePlayerState buildPrivateState(GameState state, UUID playerId) { ... } // incluye mano
}
```

### MatchController — endpoints REST

```
POST   /api/matches              → createMatch(body)
POST   /api/matches/{id}/join    → joinMatch(id, body)
GET    /api/matches/{id}/state   → getMatchState(id, ?playerId)
```

### GameActionController — endpoints REST

```
POST   /api/matches/{id}/actions → executeAction(id, body)
```

### MatchWebSocketController — reemplazar stub

```java
@MessageMapping("/matches/{matchId}/actions")
@SendTo("/topic/matches/{matchId}/events")
public GameActionResponse handleMatchAction(@DestinationVariable String matchId, GameActionRequest request) {
    return matchApplicationService.executeAction(UUID.fromString(matchId), request);
}
```

## Orden de implementación (basado en dependencias reales)

```
Nivel 0 (paralelo):  ErrorApi (agregar path/details)
                     MatchStateResponse (crear DTO)
                     MatchMapper (implementar mapeos)
                     MatchQueryService (implementar queries)
                     GameEngineConfig (agregar bean SetupManager)
                     MatchStateJpaRepository (agregar query)

Nivel 1:             GlobalExceptionHandler (depende de ErrorApi)
                     StatePersisterAdapter (depende de MatchStateJpaRepository + ObjectMapper)

Nivel 2 (hub):       MatchApplicationService (depende de MatchMapper + MatchQueryService + StatePersisterAdapter)

Nivel 3 (transporte): MatchController (depende de MatchApplicationService)
                      GameActionController (depende de MatchApplicationService)
                      MatchWebSocketController (depende de MatchApplicationService)
```

## Explicit non-goals

No implementar en este change:

- Lógica de negocio del engine (ya está en Personas 1-3)
- Implementación de `CardLookupAdapter` o `DeckLoadAdapter` (es de Catálogo)
- Event sourcing, auditoría histórica ni logging de acciones
- Autenticación, autorización ni seguridad
- Frontend ni Web UI
- Testing de integración contra base de datos real (se prueba con mocks)

## Verification requirements

El change MUST end with:

1. `mvn compile` dentro de `BE/` sin errores
2. `mvn test` — todos los tests existentes deben pasar (incluyendo los de Persona 2 y 3)
3. Los controllers deben responder correctamente a requests HTTP simuladas

## Expected output

Generar un OpenSpec change bajo:

```
openspec/changes/engine-persona4-spring-integration/
```

El change debe incluir:

- `proposal.md`
- `design.md`
- `specs/engine-persona4-integration/spec.md`
- `tasks.md`

## Scope control

Este change es de **integración**: conecta el engine con Spring, persistencia y REST. No toca ninguna clase dentro de `engine/model/`, `engine/handlers/`, `engine/rules/`, `engine/setup/`, `engine/turn/` ni `engine/victory/`. Solo implementa adaptadores, servicios, controladores y configuración.
