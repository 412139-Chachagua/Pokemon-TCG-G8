# AI Proposal Spec: engine-persona1-contracts-and-gameengine

## Change name

engine-persona1-contracts-and-gameengine

## Purpose

Implementar los contratos congelados del engine y el `GameEngine` principal del backend del PokémonTCG TPI.

Esta persona es **bloqueante para todo el equipo**: sus modelos y la interfaz `ActionHandler` deben compilar antes de que Personas 2, 3 y 4 arranquen.

El objetivo es definir todos los modelos del estado del juego, los payloads de acciones, los errores, el contexto de ejecución y el método central `GameEngine.applyAction()`.

## Mandatory context files

OpenCode MUST read and obey:

- `BE/docs/contracts_ai/00-contract-index.md`
- `BE/docs/contracts_ai/01-project-scope-contract.md`
- `BE/docs/contracts_ai/02-project-structure-contract.md`
- `BE/docs/contracts_ai/03-enums-contract.md`
- `BE/docs/contracts_ai/06-game-state-contract.md`
- `BE/docs/contracts_ai/08-game-action-contract.md`
- `BE/docs/contracts_ai/09-rule-validation-contract.md`
- `BE/docs/contracts_ai/12-persistence-log-contract.md`

## Architecture constraints

- El paquete `engine/` es **puro Java 21**: sin `@Service`, `@Repository`, `@Autowired`, `@Entity`, `@RestController`, ni ninguna anotación de Spring o JPA.
- El `GameEngine` conoce sus ports (`CardLookupPort`, `RandomizerPort`, `StatePersisterPort`, `EventPublisherPort`) únicamente a través de interfaces — nunca de implementaciones concretas.
- Toda mutación del `GameState` pasa por `EngineContext`.
- Ninguna clase de este paquete importa nada de `ar.edu.utn.frc.tup.piii.repositories`, `ar.edu.utn.frc.tup.piii.services`, ni `ar.edu.utn.frc.tup.piii.controllers`.

## Package root

```
ar.edu.utn.frc.tup.piii
```

## Scope — clases a implementar o verificar

Todas viven bajo `BE/src/main/java/ar/edu/utn/frc/tup/piii/`.

### engine/model/ — modelos del estado

| Clase | Estado actual | Acción |
|-------|--------------|--------|
| `CardInstance` | NO existe | Crear |
| `GameState` | Stub parcial | Completar según contrato 06 |
| `PlayerState` | Stub parcial | Completar según contrato 06 |
| `PokemonInPlay` | Stub parcial | Completar (agregar `enteredTurnNumber`, `evolvedThisTurn`, `attachedEnergies: List<CardInstance>`) |
| `TurnFlags` | Stub parcial | Completar (agregar `hasPlayedSupporter`, `hasPlayedStadium`) |
| `GameMetadata` | NO existe | Crear |
| `GamePhase` | NO existe como enum | Crear enum (alias de TurnPhase) |

### engine/action/ — acciones y payloads

| Clase | Estado actual | Acción |
|-------|--------------|--------|
| `GameAction` | Stub parcial | Completar |
| `GameActionType` | Existe | Verificar valores contra contrato 03 |
| `GameActionPayload` | NO existe | Crear (interface marker) |
| `AttachEnergyPayload` | NO existe | Crear |
| `DeclareAttackPayload` | NO existe | Crear |
| `RetreatPayload` | NO existe | Crear |
| `PlayTrainerPayload` | NO existe | Crear |
| `EvolvePokemonPayload` | NO existe | Crear |
| `PutBasicOnBenchPayload` | NO existe | Crear |
| `ActionResult` | Stub parcial | Completar con `List<String> events` (plain strings, no objetos) |
| `GameError` | Existe | Verificar |

### engine/ — raíz

| Clase | Estado actual | Acción |
|-------|--------------|--------|
| `ErrorCode` | NO existe | Crear (enum con todos los códigos de error del contrato 09) |
| `PlayerSide` | Existe | Verificar (PLAYER_ONE, PLAYER_TWO) |
| `EngineContext` | NO existe | Crear |
| `SpecialCondition` | Existe | Verificar valores |

### engine/handlers/

| Clase | Estado actual | Acción |
|-------|--------------|--------|
| `ActionHandler` | NO existe | Crear interface: `void handle(EngineContext ctx)` |

### engine/ — GameEngine

| Clase | Estado actual | Acción |
|-------|--------------|--------|
| `GameEngine` | Stub parcial | Implementar `applyAction()` con el flujo completo |

### Stubs que deben existir y compilar (sin implementar lógica real)

Estas clases las crean otras Personas pero deben compilar desde ya:

- `engine/rules/RuleValidator` — stub que siempre retorna válido
- `engine/setup/SetupManager` — stub vacío
- `engine/victory/VictoryConditionChecker` — stub que siempre retorna false
- `engine/turn/TurnManager` — stub vacío

## Diseño detallado

### CardInstance

```java
// engine/model/CardInstance.java
// Representa una copia física de una carta en una partida
public class CardInstance {
    private UUID instanceId;         // ID único de esta copia (generado al inicio del match)
    private String cardDefinitionId; // Referencia al catálogo (CardDefinition.id)
}
```

### PokemonInPlay — campos requeridos

```java
private UUID instanceId;
private String cardDefinitionId;
private UUID ownerPlayerId;
private int enteredTurnNumber;       // turno en que entró al juego
private boolean evolvedThisTurn;     // flag para regla de evolución
private int damageCounters;
private List<SpecialCondition> specialConditions;
private List<CardInstance> attachedEnergies; // NO AttachedCard, usar CardInstance directo
private UUID toolCardInstanceId;
```

### TurnFlags — campos requeridos

```java
private boolean hasDrawnForTurn;
private boolean hasAttachedEnergy;
private boolean hasRetreated;
private boolean hasPlayedSupporter;
private boolean hasPlayedStadium;
private boolean hasAttacked;
```

### EngineContext

`EngineContext` envuelve el `GameState` mutable y acumula eventos de texto durante la resolución de una acción:

```java
public class EngineContext {
    private final GameState state;
    private final List<String> events = new ArrayList<>();
    private final CardLookupPort cardLookup;
    private final RandomizerPort randomizer;

    public void addEvent(String message) { events.add(message); }
    public List<String> getEvents() { return Collections.unmodifiableList(events); }
    public GameState getState() { return state; }
    public CardLookupPort getCardLookup() { return cardLookup; }
    public RandomizerPort getRandomizer() { return randomizer; }
}
```

### ActionHandler interface

```java
// engine/handlers/ActionHandler.java
public interface ActionHandler {
    void handle(EngineContext ctx);
}
```

### GameEngine.applyAction() — flujo obligatorio

El método debe seguir este orden exacto:

1. Cargar `GameState` desde `StatePersisterPort.loadState(matchId)`
2. Verificar que el match esté en estado `ACTIVE`
3. Verificar que `playerId` sea el `currentPlayerId`
4. Crear `EngineContext` con el state cargado
5. Delegar validación a `RuleValidator.validate(ctx, action)` — si falla, retornar `ActionResult` con `success=false` y `GameError`
6. Resolver el handler correspondiente según `action.getType()`
7. Verificar victoria via `VictoryConditionChecker.check(ctx)`
8. Si hay ganador: setear `state.winnerPlayerId` y `state.finishReason`, cambiar status a `FINISHED`
9. Persistir state via `StatePersisterPort.saveState(matchId, state)`
10. Publicar eventos via `EventPublisherPort`
11. Retornar `ActionResult` con `success=true`, lista de eventos y vistas públicas/privadas

### ErrorCode enum (valores mínimos requeridos)

```
NOT_YOUR_TURN
WRONG_PHASE
MATCH_NOT_ACTIVE
ENERGY_ALREADY_ATTACHED
BENCH_FULL
INSUFFICIENT_ENERGY
CANNOT_ATTACK_FIRST_TURN
POKEMON_ASLEEP
POKEMON_PARALYZED
RETREAT_ALREADY_USED
SUPPORTER_ALREADY_PLAYED
EVOLVE_NOT_ALLOWED
CARD_NOT_IN_HAND
INVALID_TARGET
KNOCKOUT_REPLACEMENT_REQUIRED
```

### GameActionType — valores canónicos (contrato 03)

El enum debe tener exactamente:

```
PUT_BASIC_ON_BENCH
ATTACH_ENERGY
EVOLVE_POKEMON
PLAY_TRAINER
RETREAT_ACTIVE
DECLARE_ATTACK
END_TURN
```

> Nota: `DRAW_CARD`, `CHOOSE_KNOCKOUT_REPLACEMENT`, `USE_ABILITY` existen en el stub actual pero el contrato 08 no los incluye en V1. Deben eliminarse o marcarse como `@Deprecated` con comentario.

## Explicit non-goals

No implementar en este change:

- Lógica real de `RuleValidator` (solo stub)
- Lógica real de `SetupManager` (solo stub)
- Lógica real de `VictoryConditionChecker` (solo stub)
- Lógica real de `TurnManager` (solo stub)
- Ningún handler concreto (`PutBasicOnBenchHandler`, etc.) — esos son Persona 3
- Ningún endpoint REST — eso es Persona 4
- Ninguna clase del catálogo, mazos o frontend
- Ninguna clase con `@Entity`, `@Service`, `@Repository`

## Verification requirements

El change MUST end with:

1. `mvn compile` dentro de `BE/` sin errores
2. `mvn test` — al menos el test de contexto Spring (`ApplicationTests.contextLoads`) debe pasar
3. Ningún archivo dentro de `engine/` debe importar paquetes de Spring, JPA o repositorios

## Expected output

Generar un OpenSpec change bajo:

```
openspec/changes/engine-persona1-contracts-and-gameengine/
```

El change debe incluir:

- `proposal.md`
- `design.md`
- `tasks.md`
- `specs/engine-contracts/spec.md`

## Scope control

Este change es de **infraestructura del engine**: define los tipos, el contrato entre handlers y el flujo de `applyAction`. No implementa ninguna regla de negocio real (eso es Personas 2 y 3).

La prioridad absoluta es que **compile limpio** para desbloquear al resto del equipo.