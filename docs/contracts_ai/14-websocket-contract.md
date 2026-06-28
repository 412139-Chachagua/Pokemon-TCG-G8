# WebSocket Contract

## Goal

Define WebSocket topics and payloads.

The TPI requires real-time bidirectional communication, state sync after valid actions, event notifications and reconnection support.

## Backend location

```
websocket/
  MatchWebSocketController.java
  MatchWebSocketPublisher.java
engine/ports/
  EventPublisherPort.java
```

`EventPublisherPort` SÍ existe y es implementado por `MatchWebSocketPublisher`.
`GameEngine.applyAction()` publica eventos via `EngineContext` → `EventPublisherPort`.
No se llama directamente desde `MatchApplicationService`.

## Frontend location

```
core/websocket/match-socket.service.ts
features/match/services/match-facade.service.ts
```

## Topics

Public match events:
- `/topic/matches/{matchId}/events`

Private player state:
- `/queue/matches/{matchId}/{playerId}` (no `/user/queue/matches/{matchId}/private-state`)

Client action destination:
- `/app/matches/{matchId}/actions`

## MatchWebSocketController

Escucha en `/app/matches/{matchId}/actions` para acciones vía STOMP.
Delega a `MatchApplicationService.executeAction()` y publica resultados via `MatchWebSocketPublisher`.

## MatchWebSocketPublisher

Implementa `EventPublisherPort`:

```java
public interface EventPublisherPort {
    void publishEvents(UUID matchId, List<GameEvent> events);
}
```

`publishEvents()` recibe `List<GameEvent>` y:
1. Persiste los eventos como `MatchLogEntity`
2. Publica el public state update a `/topic/matches/{matchId}/events`
3. Publica el private state a `/queue/matches/{matchId}/{playerId}` via `publishPrivateState()`

## Public state update event

```json
{
  "type": "STATE_UPDATED",
  "matchId": "match-uuid",
  "publicState": { ... },
  "events": [
    {
      "type": "ENERGY_ATTACHED",
      "message": "Antonio attached Fire Energy to Slugma.",
      "payload": {}
    }
  ]
}
```

## Private state update event

```json
{
  "playerId": "player-1-uuid",
  "hand": [
    {
      "instanceId": "card-instance-501",
      "cardId": "xy1-10",
      "name": "Slugma",
      "supertype": "POKEMON"
    }
  ],
  "deckCount": 34,
  "discardCount": 1,
  "prizes": [
    { "slot": 0, "known": false, "cardId": null },
    { "slot": 1, "known": false, "cardId": null }
  ]
}
```

## Event messages

WebSocket events usan `GameActionResponse` con `events: GameEventDto[]` (tipados), no strings planos.

El public state update incluye:
- `type: "STATE_UPDATED"`
- `matchId`
- `publicState`
- `events`

## Reconnection flow

When frontend reconnects:

1. Reconnect WebSocket
2. Call: `GET /api/matches/{matchId}/state?playerId={playerId}`
3. Replace local state with server state
4. Continue listening to events

## Privacy rules

Reglas de privacidad confirmadas (implementadas y verificadas):

| Regla | Estado |
|-------|--------|
| La mano del oponente NUNCA se envía al cliente (solo count) | ✅ IMPLEMENTADO |
| El orden del mazo permanece oculto | ✅ IMPLEMENTADO |
| El contenido de las cartas de Premio permanece oculto (solo "FACE_DOWN") | ✅ IMPLEMENTADO |
| Los Pokémon en Banca no revelan sus cartas unidas al oponente | ✅ IMPLEMENTADO |
| Private state se envía solo al jugador propietario via `/queue/{matchId}/{playerId}` | ✅ IMPLEMENTADO |

Public WebSocket events must not include:
- opponent hand identities
- deck order
- unrevealed prize identities
- private selections

Private messages must be sent only to the owning player.
