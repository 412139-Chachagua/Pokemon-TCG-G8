# Game Log — Log de Eventos de la Partida

## Resumen

Cada acción ejecutada en una partida genera una secuencia de eventos de juego (`GameEvent`) que describen qué ocurrió: cartas robadas, daño aplicado, evoluciones, cambios de fase, etc. Estos eventos se publican en tiempo real por WebSocket para que los clientes puedan mostrar un log detallado de la partida.

## Modelo de evento (`GameEvent.java`)

```java
public class GameEvent {
    private String type;            // tipo de evento (ej: "CARD_DRAWN")
    private UUID matchId;           // ID de la partida
    private int turnNumber;         // número de turno
    private Instant createdAt;      // timestamp de creación
    private String message;         // mensaje legible (ej: "Jugador robó 1 carta")
    private Map<String, Object> payload;  // datos adicionales específicos del tipo
}
```

## DTO de transferencia (`GameEventDto.java`)

Para la serialización hacia el cliente se usa `GameEventDto`, que expone solo los campos relevantes:

```java
public record GameEventDto(
    String type,
    String message,
    Map<String, Object> payload,
    Integer turnNumber
) {}
```

Se excluye `matchId` y `createdAt` para reducir el tamaño de la respuesta (el cliente ya conoce la partida y puede inferir el tiempo del orden de llegada).

## Tipos de eventos (`GameEventType.java`)

El enum `GameEventType` define 59 tipos de eventos que cubren todas las fases del juego:

### Setup
`SETUP_ACTIVE_PLACED`, `SETUP_BENCH_PLACED`, `SETUP_ACTIVE_REMOVED`, `SETUP_BENCH_REMOVED`, `SETUP_CONFIRMED`, `SETUP_COMPLETED`

### Mulligan
`MULLIGAN_REVEALED`, `INITIAL_MULLIGAN_NEEDED`, `INITIAL_MULLIGAN_RESOLVED`, `MULLIGAN_DRAW_OPPORTUNITY`, `MULLIGAN_DRAW_RESOLVED`

### Jugabilidad general
`CARD_DRAWN`, `CARDS_DRAWN`, `POKEMON_PLACED_ON_BENCH`, `ENERGY_ATTACHED`, `POKEMON_EVOLVED`, `TRAINER_PLAYED`, `TRAINER_EFFECT_RESOLVED`, `STADIUM_PLAYED`, `STADIUM_REMOVED`, `TOOL_ATTACHED`, `RETREAT_EXECUTED`, `SWITCH_EXECUTED`, `POKEMON_HEALED`, `POKEMON_SEARCHED`, `ENERGY_SEARCHED`, `STATUS_APPLIED`, `STATUS_REMOVED`, `ENERGY_DISCARDED`, `POKEMON_REVIVED`, `POKEMON_RETURNED_TO_DECK`

### Combate
`ATTACK_DECLARED`, `ATTACK_CANCELED`, `DAMAGE_APPLIED`, `BENCH_DAMAGE`, `KNOCKOUT_OCCURRED`, `KO_REPLACEMENT_REQUIRED`, `KO_REPLACEMENT_DONE`, `RECOIL_OCCURRED`, `CONFUSION_SELF_HIT`, `COIN_FLIP_RESULT`, `ATTACK_EFFECT_RESOLVED`

### Estado y fase
`PHASE_CHANGED`, `STATE_UPDATED`, `PRIZE_TAKEN`, `VICTORY_DECIDED`, `SUDDEN_DEATH_STARTED`

### Habilidades
`ABILITY_USED`, `ABILITY_BLOCKED`

### Interacción con el mazo/mano del oponente
`OPPONENT_HAND_SHUFFLED`, `OPPONENT_ENERGY_DISCARDED`, `OPPONENT_RANDOM_DISCARD`, `DECK_ORDERED`, `DECK_PEEKED`

### Conexión
`PLAYER_RECONNECTED`

## Cómo se generan los eventos

Los eventos son creados por el motor de juego (`GameEngine`) durante la ejecución de una acción (`GameAction`). El `ActionResult` retornado por `gameEngine.applyAction()` contiene una lista de `GameEvent`.

En `MatchApplicationService.executeAction()` (línea 281-285), los eventos se convierten a `GameEventDto` para la respuesta:

```java
List<GameEventDto> eventDtos = result.getEvents() != null
    ? result.getEvents().stream()
        .map(e -> new GameEventDto(e.getType(), e.getMessage(), e.getPayload(), e.getTurnNumber()))
        .collect(Collectors.toList())
    : List.of();
```

Estos DTOs se incluyen en el `GameActionResponse` que se envía al cliente.

## Publicación por WebSocket

### Interfaz de puerto (`EventPublisherPort.java`)

```java
public interface EventPublisherPort {
    void publishEvents(UUID matchId, List<GameEvent> events);
}
```

### Implementación (`MatchWebSocketPublisher.java`)

`MatchWebSocketPublisher` implementa `EventPublisherPort` y publica en:

```
/topic/matches/{matchId}/events
```

```java
@Override
public void publishEvents(UUID matchId, List<GameEvent> events) {
    String destination = "/topic/matches/" + matchId + "/events";
    messagingTemplate.convertAndSend(destination, events);
}
```

### Canales de publicación

| Método | Destino | Contenido |
|--------|---------|-----------|
| `publishEvents()` | `/topic/matches/{matchId}/events` | Lista de `GameEvent` |
| `publishPublicState()` | `/topic/matches/{matchId}/events` | `GameActionResponse` (incluye `events` + `publicState`) |
| `publishPlayerConnected()` | `/topic/matches/{matchId}/events` | Lista con 1 `GameEvent` de tipo `PLAYER_RECONNECTED` |
| `publishPlayerDisconnected()` | `/topic/matches/{matchId}/events` | Lista con 1 `GameEvent` de tipo `PLAYER_DISCONNECTED` |
| `publishPrivateState()` | `/topic/matches/{matchId}/player/{playerId}` | `PrivatePlayerState` (canal privado) |

**Nota**: `publishPublicState()` y `publishEvents()` comparten el mismo destino (`/topic/matches/{matchId}/events`). El método `publishPublicState()` se usa después de ejecutar una acción e incluye tanto el estado público como los eventos en un solo mensaje.

### Logging de tamaño

El método `logSize()` registra el tamaño en bytes del payload antes de publicarlo, útil para monitorear el consumo de ancho de banda.

## Orquestación del flujo

En `MatchApplicationService.executeAction()`:

1. El motor ejecuta la acción y produce un `ActionResult` con eventos.
2. Se construye un `GameActionResponse` que incluye los eventos como `List<GameEventDto>`.
3. `MatchWebSocketPublisher.publishPublicState()` envía la respuesta completa (estado público + eventos) al tópico de eventos.
4. Adicionalmente, se publican los estados privados de cada jugador en sus canales privados.

## Caso especial: rendición (`concedeMatch`)

Cuando un jugador se rinde, se genera manualmente un evento `VICTORY_DECIDED` con el payload indicando el ganador y la razón `CONCEDE`, y se publica a través del mismo flujo.
