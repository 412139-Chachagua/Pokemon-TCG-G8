# Sala de Espera — Frontend

## Resumen

El frontend del lobby está implementado en Angular con componentes standalone.
Permite al jugador autenticado crear partidas, ver las disponibles, unirse a
una, y gestionar partidas en curso. La comunicación en tiempo real durante la
partida activa usa STOMP sobre WebSocket; el lobby usa HTTP polling.

---

## Rutas (`routes.ts`)

```typescript
path: '' → LobbyPage
```

La ruta vacía del módulo lobby carga `LobbyPage`. La navegación a
`/quick-match` se define en otro módulo de rutas.

---

## LobbyPage

Página principal del lobby. Contiene cuatro secciones:

1. **Crear partida** — `MatchCreateComponent`
2. **Partidas disponibles** — `MatchListComponent`
3. **Unirse a partida** — `MatchJoinComponent`
4. **Partidas en curso** — lista inline con polling

### Comportamiento

- Al montarse, lee `deckId` de query params para pre-seleccionar un mazo.
- Inicia un `setInterval` de 3 segundos para refrescar partidas activas
  via `matchApi.getActiveMatches()`.
- `onMatchCreated()` / `onMatchJoined()`: resetea los servicios de estado y
  navega a `/match/{id}`.
- `onMatchSelected(match)`: pasa el ID al `MatchJoinComponent` para
  pre-seleccionar la partida.
- `resumeMatch(matchId)`: navega a `/match/{id}?rejoin=true`.
- `deleteMatch(matchId)`: confirma y elimina la partida (solo el creador).

### Partidas en curso — lógica inline

Muestra partidas con status `ACTIVE` o `SETUP`. Para cada una:

- **En juego**: si el oponente ya reanudó (`opponentResumed()`)
- **Esperando...**: si el jugador ya reanudó pero el oponente no
- **Reanudar**: ningún jugador ha reanudado aún
- Botón ✕ para eliminar (solo el creador, otros ven tooltip indicando quién puede)

---

## QuickMatchPage

Página de partida relámpago (debug).

### Flujo

1. Al iniciar, carga los mazos válidos del jugador via `DeckApiService`.
2. Muestra un `<select>` para elegir mazo.
3. Al hacer clic en "Buscar partida relámpago", llama a
   `matchFacade.createQuickMatch(deckId)` que envía `POST /api/matches` con
   `quickMatch: true` y `handSize=30` en el backend.
4. Navega a `/match/{id}` donde el jugador espera a que otro se una desde la
   lista de partidas.

---

## MatchCreateComponent

Formulario para crear una partida.

### Inputs / Outputs

| Nombre | Tipo | Descripción |
|--------|------|-------------|
| `playerId` | `string` (required) | ID del jugador autenticado |
| `preSelectedDeckId` | `string \| null` | Mazo pre-seleccionado (desde query param) |
| `created` | `output<MatchResponse>` | Emite al crearse la partida |

### Lógica

- `ngOnInit()`: carga mazos válidos del jugador via `DeckApiService`.
  Si hay `preSelectedDeckId` válido lo usa; si no, selecciona el único
  mazo disponible.
- `onSubmit()`: llama a `matchFacade.createMatch(name, deckId, deckName)`.
  Deshabilita el botón si falta nombre o mazo.

---

## MatchJoinComponent

Formulario para unirse a una partida.

### Inputs / Outputs

| Nombre | Tipo | Descripción |
|--------|------|-------------|
| `playerId` | `string` (required) | ID del jugador autenticado |
| `joined` | `output<MatchResponse>` | Emite al unirse exitosamente |

### Lógica

- `ngOnInit()`: carga mazos válidos del jugador.
- `selectMatch(id, hostName)`: método público llamado desde `MatchListComponent`
  para pre-seleccionar una partida.
- `onSubmit()`: llama a `matchFacade.joinMatch(matchId, name, deckId)`.

---

## MatchListComponent

Lista de partidas disponibles (status `WAITING`).

### Inputs / Outputs

| Nombre | Tipo | Descripción |
|--------|------|-------------|
| `playerId` | `string` | ID del jugador (para saber si es el creador) |
| `matchSelected` | `output<{ id, hostName }>` | Emite al seleccionar una partida |

### Lógica

- `ngOnInit()`: carga las partidas via `matchApi.listMatches()`.
- `onRefresh()`: recarga manualmente la lista.
- `onCancel(matchId)`: elimina la partida (solo si es el creador).
- Cada ítem muestra el nombre del host y botones contextuales:
  - "Cancelar" si el jugador es el host
  - "Usar este" para otros jugadores

---

## MatchApiService

Servicio HTTP que comunica con el backend.

| Método | Endpoint | Uso |
|--------|----------|-----|
| `createMatch(request)` | `POST /api/matches` | Crear partida |
| `joinMatch(id, request)` | `POST /api/matches/{id}/join` | Unirse |
| `getMatchState(id, pid)` | `GET /api/matches/{id}/state` | Estado de juego |
| `getActiveMatches(pid)` | `GET /api/matches/active` | Partidas activas |
| `listMatches(status?)` | `GET /api/matches?status=` | Partidas WAITING |
| `deleteMatch(id, pid)` | `DELETE /api/matches/{id}` | Cancelar |
| `concedeMatch(id, pid)` | `POST /api/matches/{id}/concede` | Rendirse |
| `sendAction(id, action)` | `POST /api/matches/{id}/actions` | Acción de juego |
| `getHistory(pid)` | `GET /api/matches/history` | Historial |
| `getChatHistory(id)` | `GET /api/matches/{id}/chat` | Chat |

Interfaces del servicio:

```typescript
interface CreateMatchRequest {
  player1Id: string;
  player1Name: string;
  player1DeckId: string;
  quickMatch?: boolean;
  player2Name?: string;
  player2DeckId?: string;
}

interface JoinMatchRequest {
  playerId: string;
  playerName: string;
  deckId: string;
}

interface MatchResponse {
  id: string;
  status: string;
  currentPhase: string | null;
  turnNumber: number;
  currentPlayerId: string | null;
  firstPlayerId: string | null;
  winnerPlayerId: string | null;
  finishReason: string | null;
  players: MatchPlayerResponse[];
  createdAt: string;
  lastSavedAt: string | null;
  lastResumedPlayerId: string | null;
}
```

---

## MatchSocketService

Servicio WebSocket basado en STOMP sobre SockJS que maneja la comunicación en
tiempo real durante una partida activa.

### Conexión

```typescript
connect(matchId: string, playerId: string)
```

Crea un cliente STOMP apuntando a `http://localhost:8080/ws` con headers de
autorización JWT. Configura `reconnectDelay: 2000ms` y heartbeats cada 10s.

### Desconexión

```typescript
disconnect()
```

Desuscribe todos los topics y desactiva el cliente.

### Suscripciones (al conectarse)

| Topic | Propósito |
|-------|-----------|
| `/topic/matches/{matchId}/events` | Estado público, eventos de juego, errores |
| `/topic/matches/{matchId}/player/{playerId}` | Estado privado del jugador (mano, etc.) |
| `/topic/matches/{matchId}/chat` | Mensajes de chat |

### Flujo de eventos

1. **`publicEvents$`** — `Subject<GameEventDto>`: emite eventos de juego
   (cartas robadas, evoluciones, daño, etc.) y actualizaciones de estado público.
   Soporta tanto `GameActionResponse` completo como eventos standalone
   (ej. `MULLIGAN_REVEALED` publicado por `EventPublisherPort` durante setup).
2. **`privateState$`** — `Subject<PrivatePlayerStateModel>`: emite el estado
   privado del jugador (mano, cartas del bench propias, etc.).
3. **`actionErrors$`** — `Subject<GameActionResponse['error']>`: errores de
   acciones inválidas.
4. **`chatMessages$`** — `Subject<ChatMessage>`: mensajes de chat entrantes.
5. **`connectionStatus$`** — `Subject<ConnectionStatus>`: `CONNECTED`,
   `DISCONNECTED` o `RECONNECTING`.

### Envío de mensajes

| Método | Destino | Propósito |
|--------|---------|-----------|
| `sendAction(action)` | `/app/matches/{id}/actions` | Enviar acción de juego |
| `sendChatMessage(message)` | `/app/matches/{id}/chat` | Enviar mensaje de chat |

---

## Flujo completo: crear partida → esperar oponente → jugar

```
1. Jugador A completa formulario en MatchCreateComponent
        ↓
2. MatchFacadeService.createMatch() → POST /api/matches
        ↓
3. Backend: MatchEntity(status=WAITING), MatchPlayerEntity(PLAYER_ONE)
        ↓
4. LobbyPage.onMatchCreated() → navega a /match/{id}
        ↓
5. MatchSocketService.connect(matchId, playerId)
   → Suscribe a /topic/matches/{id}/events y /player/{playerId}
        ↓
   ─── Jugador A espera en pantalla de match ───
        ↓
6. Jugador B entra al lobby
        ↓
7. MatchListComponent carga GET /api/matches?status=WAITING → ve la partida
        ↓
8. Jugador B hace clic en "Usar este" → MatchJoinComponent.selectMatch(id, hostName)
        ↓
9. Jugador B completa formulario y hace clic en "Unirse"
        ↓
10. MatchFacadeService.joinMatch() → POST /api/matches/{id}/join
        ↓
11. Backend: setupManager.setup() → GameState, MatchEntity(status=ACTIVE)
        ↓
12. MatchResponse retorna con status=ACTIVE
        ↓
13. MatchJoinComponent.onSubmit() emite joined → LobbyPage.onMatchJoined()
    → navega a /match/{id}
        ↓
14. MatchSocketService.connect(matchId, playerId)
    → Ambos jugadores conectados vía WebSocket
        ↓
15. Comienza el juego: turnos, acciones, eventos en tiempo real
```

### Notas

- El lobby **no usa WebSocket** para actualizaciones en tiempo real. Las
  partidas disponibles se refrescan manualmente (botón "Actualizar" en
  `MatchListComponent`). Las partidas activas usan polling cada 3 segundos.
- El WebSocket solo se activa al navegar a la pantalla de juego
  (`/match/{id}`).
- `MatchFacadeService` actúa como fachada entre los componentes del lobby y
  `MatchApiService`, manteniendo estado de señales para `matchId`, `playerId`,
  `side`, `status`, `playerName` y `deckName`.
