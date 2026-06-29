# Sala de Espera — Backend

## Resumen

El backend de la sala de espera (lobby) expone APIs REST y WebSockets para crear,
listar y unirse a partidas. La lógica de negocio centraliza en
`MatchApplicationService`, mientras que la capa de persistencia usa JPA con
`MatchEntity` y `MatchPlayerEntity`.

---

## MatchController

**Ruta base:** `/api/matches`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/matches?status=WAITING` | Lista partidas disponibles (status por defecto `WAITING`) |
| `GET` | `/api/matches/active` | Partidas activas del jugador autenticado |
| `POST` | `/api/matches` | Crear una nueva partida |
| `POST` | `/api/matches/{id}/join` | Unirse a una partida existente |
| `GET` | `/api/matches/{id}/state` | Obtener el estado completo de la partida |
| `DELETE` | `/api/matches/{id}` | Cancelar una partida (solo el creador) |
| `POST` | `/api/matches/{id}/concede` | Rendirse en una partida |
| `GET` | `/api/matches/{id}/chat` | Historial de chat de la partida |

Todas las rutas que requieren autenticación extraen el `playerId` del token JWT
mediante el método `getPlayerId()`.

---

## MatchApplicationService

Servicio transaccional que orquesta todo el ciclo de vida de una partida.

### Crear partida (`createMatch`)

1. Valida `player1Id` obligatorio.
2. Si se provee `player2Id`, la partida arranca con ambos jugadores (partida directa).
3. Si `quickMatch=true`, asigna `handSize=30` (modalidad relámpago con 30 cartas iniciales).
4. Persiste `MatchEntity` con status `WAITING`.
5. Persiste `MatchPlayerEntity` para `PLAYER_ONE` con su `deckId` y `displayName`.
6. Si hay segundo jugador, ejecuta `setupManager.setup()` para inicializar el
   motor de juego, guarda el `GameState`, y actualiza la entidad con
   `ACTIVE`, `currentPlayerId`, `firstPlayerId` y `turnNumber`.

### Unirse a partida (`joinMatch`)

1. Busca la partida por ID; lanza `NotFoundException` si no existe.
2. Valida que el status sea `WAITING`.
3. Persiste `MatchPlayerEntity` para `PLAYER_TWO`.
4. Carga ambos mazos via `DeckLoadPort`.
5. Ejecuta `setupManager.setup()` para inicializar el estado de juego.
6. Persiste el `GameState` y actualiza la entidad a `ACTIVE`.
7. Retorna el `MatchResponse` completo.

### Ejecutar acción (`executeAction`)

Usa un `ReentrantLock` por partida para evitar condiciones de carrera. Delega
en `GameEngine.applyAction()` para procesar la acción. Si el juego finaliza,
persiste `FINISHED` y llama a `PlayerStatsService.recordMatchResult()`.

### Cancelación y expiración

- `cancelMatch()`: solo el creador (`PLAYER_ONE`) puede cancelar. Status pasa a
  `FINISHED` con `finishReason=CANCELLED`.
- `expireOldWaitingMatches()`: tarea programada cada 60s que finaliza partidas
  `WAITING` con más de 5 minutos de antigüedad (`finishReason=EXPIRED`).

### Concesión (`concedeMatch`)

Marca la partida como `FINISHED` con `finishReason=CONCEDE`, determina el
ganador (el oponente), persiste el resultado y publica el estado final por
WebSocket.

---

## DTOs de entrada/salida

### CreateMatchRequest

```json
{
  "player1Id": "uuid",
  "player1Name": "string",
  "player1DeckId": "uuid",
  "player2Id": "uuid (opcional)",
  "player2Name": "string (opcional)",
  "player2DeckId": "uuid (opcional)",
  "quickMatch": false
}
```

Usa `@JsonAlias` para aceptar también `playerId`, `playerName` y `deckId` como
alias de los campos `player1*`.

### JoinMatchRequest (record)

```json
{
  "playerName": "string",
  "deckId": "uuid",
  "playerId": "uuid"
}
```

### MatchResponse (record)

```json
{
  "id": "uuid",
  "status": "WAITING | ACTIVE | FINISHED",
  "currentPhase": "string | null",
  "turnNumber": 0,
  "currentPlayerId": "uuid | null",
  "firstPlayerId": "uuid | null",
  "winnerPlayerId": "uuid | null",
  "finishReason": "string | null",
  "players": [
    { "playerId": "uuid", "side": "PLAYER_ONE", "displayName": "string" }
  ],
  "createdAt": "instant",
  "lastSavedAt": "instant | null",
  "lastResumedPlayerId": "uuid | null"
}
```

Usa `@JsonInclude(NON_NULL)` para omitir campos nulos en la serialización.

---

## Modelo de persistencia

### MatchEntity — Tabla `matches`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `UUID` | Generado automáticamente |
| `status` | `String(30)` | `WAITING`, `ACTIVE`, `FINISHED`, etc. |
| `currentPhase` | `String(30)` | Fase actual del juego |
| `turnNumber` | `Integer` | Número de turno |
| `currentPlayerId` | `UUID` | Jugador al que le toca |
| `firstPlayerId` | `UUID` | Jugador que comenzó |
| `winnerPlayerId` | `UUID` | Ganador (si finalizó) |
| `finishReason` | `String(60)` | Motivo de finalización |
| `createdAt` | `Instant` | Autoasignado via `@PrePersist` |
| `updatedAt` | `Instant` | Autoasignado via `@PreUpdate` |
| `finishedAt` | `Instant` | Momento de finalización |
| `handSize` | `Integer` | Cartas iniciales (default 7, 30 en quick match) |
| `latestStateVersion` | `Long` | Versión del estado guardado |
| `lastResumedPlayerId` | `UUID` | Último jugador que reanudó |

### MatchPlayerEntity — Tabla `match_players`

| Campo | Tipo | Notas |
|-------|------|-------|
| `id` | `UUID` | Generado automáticamente |
| `match` | `@ManyToOne` | Relación con `MatchEntity` |
| `playerId` | `UUID` | ID del jugador |
| `playerKind` | `String(20)` | `PLAYER` |
| `side` | `String(30)` | `PLAYER_ONE` o `PLAYER_TWO` |
| `deckId` | `UUID` | Mazo seleccionado |
| `displayName` | `String(80)` | Nombre visible |
| `joinedAt` | `Instant` | Autoasignado via `@PrePersist` |

### MatchJpaRepository

- `findByStatus(String)` — Listar partidas por estado (ej. `WAITING`)
- `findByStatusAndCreatedAtBefore(String, Instant)` — Expirar partidas viejas
- `findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc(String, UUID)` — Partidas activas de un jugador

### MatchPlayerJpaRepository

- `findByMatch_Id(UUID)` — Obtener todos los jugadores de una partida

---

## MatchStatus (enum)

```java
WAITING      → Partida creada, esperando oponente
SETUP        → Configuración inicial (mulligan, etc.)
ACTIVE       → Partida en curso
FINISHED     → Partida finalizada (por victoria, rendición, cancelación o expiración)
```

---

## WebSocket — Lobby

### WebSocketConfig

- Habilita broker simple en `/topic` y `/queue`.
- Prefijo de aplicación: `/app`.
- Endpoint STOMP: `/ws` con SockJS y `setAllowedOriginPatterns("*")`.
- Límite de tamaño de mensaje: 4 MB.

### MatchWebSocketPublisher

Implementa `EventPublisherPort` para publicar eventos en tiempo real:

| Método | Destino | Propósito |
|--------|---------|-----------|
| `publishEvents(matchId, events)` | `/topic/matches/{id}/events` | Eventos genéricos del juego |
| `publishPublicState(matchId, response)` | `/topic/matches/{id}/events` | Estado público (`GameActionResponse` sin `privateState`) |
| `publishPrivateState(matchId, playerId, state)` | `/topic/matches/{id}/player/{playerId}` | Estado privado de cada jugador |
| `publishPlayerConnected(matchId, playerId, name)` | `/topic/matches/{id}/events` | Reconexión de jugador |
| `publishPlayerDisconnected(matchId, playerId)` | `/topic/matches/{id}/events` | Desconexión de jugador |

### MatchWebSocketController

Maneja mensajes entrantes STOMP:

| Destino | Método | Descripción |
|---------|--------|-------------|
| `/app/matches/{matchId}/actions` | `handleMatchAction` | Ejecuta una acción de juego y publica respuesta pública + privada para todos los jugadores |
| `/app/matches/{matchId}/chat` | `handleChatMessage` | Publica mensaje de chat en `/topic/matches/{id}/chat` y lo cachea en `ChatMessageCacheService` |

---

## Flujo sala de espera

```
Jugador A → POST /api/matches → MatchEntity(status=WAITING)
                                  ↓
Jugador B → GET /api/matches?status=WAITING → ve la partida
                                  ↓
Jugador B → POST /api/matches/{id}/join → MatchEntity(status=ACTIVE)
                                            ↓
                        setupManager.setup() → GameState inicial
                                            ↓
                    Ambos navegan a /match/{id} para jugar
```
