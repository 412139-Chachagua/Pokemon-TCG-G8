# Match Summary — Resumen de Partida

## Resumen

Cuando una partida termina se persiste un resumen con el resultado: ganador, perdedor, razón de finalización, duración y cantidad de turnos. Este resumen se sirve a través de una API REST para consultar el historial de partidas de un jugador o ver el detalle de una partida específica.

## Cómo finaliza una partida

### 1. Victoria por condiciones de juego

En `MatchApplicationService.executeAction()` (líneas 265-279), después de aplicar una acción se verifica el estado del juego:

```java
if (state != null && state.getStatus() == MatchStatus.FINISHED) {
    var match = matchJpaRepository.findById(matchId);
    match.setWinnerPlayerId(state.getWinnerPlayerId());
    match.setFinishReason(state.getFinishReason() != null ? state.getFinishReason().name() : null);
    match.setFinishedAt(Instant.now());
    match.setTurnNumber(state.getTurnNumber());
    match.setStatus("FINISHED");
    matchJpaRepository.save(match);
    playerStatsService.recordMatchResult(matchId, state.getWinnerPlayerId(), state.getFinishReason());
}
```

Las condiciones de victoria las evalúa `VictoryConditionChecker.check()`:

| Condición | Gana si... |
|-----------|-----------|
| **PRIZES** | El jugador tomó todas sus cartas de premio (`p.getPrizes().isEmpty()`) |
| **KNOCKOUT** | El oponente no tiene Pokémon activo ni en banca |
| **DECK_OUT** | El mazo del oponente está vacío |

Si ambos jugadores cumplen condiciones simultáneamente, se comparan por cantidad de condiciones cumplidas. Si hay empate, se declara `SUDDEN_DEATH` sin ganador.

### 2. Rendición (`concedeMatch`)

El método `concedeMatch()` (línea 395) permite a un jugador rendirse:

- Se determina al oponente como ganador.
- Se persiste con `finishReason = "CONCEDE"`.
- Se genera manualmente un evento `VICTORY_DECIDED` y se publica por WebSocket.
- Se actualiza el `GameState` a `FINISHED` y se persiste.

### 3. Cancelación (`cancelMatch`)

Solo el creador de la partida puede cancelarla (línea 373). Se marca como `FINISHED` con `finishReason = "CANCELLED"`. **No** se registra en estadísticas de jugador.

### 4. Expiración (`expireOldWaitingMatches`)

Un scheduled task ejecutado cada 60 segundos (línea 358) expira partidas en estado `WAITING` creadas hace más de 5 minutos, marcándolas como `FINISHED` con `finishReason = "EXPIRED"`. Estas partidas **se excluyen del historial** (`MatchHistoryService.java:30`).

## FinishReason — Razones de finalización

Enum en `FinishReason.java`:

| Valor | Origen | Descripción |
|-------|--------|-------------|
| `KNOCKOUT` | `VictoryConditionChecker` | El oponente se quedó sin Pokémon |
| `PRIZES` | `VictoryConditionChecker` | El jugador tomó todos sus premios |
| `DECK_OUT` | `VictoryConditionChecker` | El oponente agotó su mazo |
| `CONCEDE` | `concedeMatch()` | Un jugador se rindió |
| `SUDDEN_DEATH` | `VictoryConditionChecker` | Empate en condiciones — se inicia muerte súbita |

Valores adicionales usados solo a nivel de entidad (no en el enum): `"CANCELLED"` y `"EXPIRED"`.

## Resumen para el historial (`MatchSummaryResponse.java`)

```java
public record MatchSummaryResponse(
    String id,                  // UUID de la partida
    String winnerName,          // nombre del ganador
    String loserName,           // nombre del perdedor
    int totalTurns,             // turnos jugados
    Instant createdAt,          // fecha de creación
    Long durationSeconds,       // duración en segundos
    String finishReason         // razón de finalización (texto)
) {}
```

Campos calculados:
- `formattedDate()` — formato `dd/MM/yy` UTC
- `formattedTime()` — formato `HH:mm` UTC
- `formattedDuration()` — formato `Xm Ys`

## Persistencia

### `MatchEntity` (tabla `matches`)

Campos relevantes para el resumen:
- `status` → `"FINISHED"`
- `winnerPlayerId` → UUID del ganador
- `finishReason` → texto del enum (ej: `"PRIZES"`)
- `finishedAt` → timestamp de finalización
- `turnNumber` → turnos totales
- `createdAt` → timestamp de creación (para calcular duración)

### `MatchStateEntity` (tabla `match_states`)

Almacena snapshots serializados del estado completo del juego (`serializedState` como TEXT) con versionado. Cada partida puede tener múltiples versiones. El repositorio `MatchStateJpaRepository` permite obtener la última versión:

```java
Optional<MatchStateEntity> findTopByMatchIdOrderByVersionDesc(UUID matchId);
```

## API REST (`MatchHistoryController.java`)

### Listar historial de un jugador

```
GET /api/matches/history?playerId={uuid}
```

Retorna `List<MatchSummaryResponse>` con las partidas finalizadas del jugador (excluye `EXPIRED`).

### Ver detalle de una partida

```
GET /api/matches/history/{id}
```

Retorna `MatchSummaryResponse` de una partida específica, o `404` si no existe o está expirada.

El `MatchHistoryService.buildSummary()` construye el resumen a partir de la entidad:
1. Obtiene los jugadores ordenados por `side` (PLAYER_ONE, PLAYER_TWO).
2. Determina ganador/perdedor comparando `winnerPlayerId` con los jugadores.
3. Calcula `durationSeconds` como la diferencia entre `createdAt` y `finishedAt`.
4. Si no hay ganador (ej: `SUDDEN_DEATH`), asigna el primer jugador como winnerName.
