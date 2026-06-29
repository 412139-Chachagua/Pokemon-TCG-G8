# Historial de Partidas — Backend

## MatchEntity

La entidad `MatchEntity` (en `BE/.../repositories/entities/MatchEntity.java`) representa una partida persistida en la tabla `matches`. Contiene:

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador único generado automáticamente. |
| `status` | `String` | Estado actual de la partida (`WAITING`, `SETUP`, `ACTIVE`, `FINISHED`). |
| `currentPhase` | `String` | Fase del juego en curso (ej. `DRAW`, `ATTACH_ENERGY`, `ATTACK`, etc.). |
| `turnNumber` | `Integer` | Número de turno transcurrido. |
| `currentPlayerId` | `UUID` | ID del jugador al que le toca jugar. |
| `firstPlayerId` | `UUID` | ID del jugador que comenzó la partida. |
| `winnerPlayerId` | `UUID` | ID del jugador ganador (si la partida finalizó). |
| `finishReason` | `String` | Motivo de finalización (`KNOCKOUT`, `PRIZES`, `DECK_OUT`, `CONCEDE`, `EXPIRED`, `CANCELLED`, etc.). |
| `createdAt` | `Instant` | Marca de tiempo de creación. |
| `updatedAt` | `Instant` | Marca de última modificación. |
| `finishedAt` | `Instant` | Marca de tiempo de finalización (se completa cuando la partida termina). |
| `handSize` | `Integer` | Cantidad de cartas iniciales en mano (por defecto 7). |
| `latestStateVersion` | `Long` | Versión del último estado guardado (control de concurrencia). |
| `lastResumedPlayerId` | `UUID` | Jugador que reanudó la partida (útil en reconexiones). |
| `players` | `List<MatchPlayerEntity>` | Lista de jugadores participantes (relación `@OneToMany`). |
| `states` | `List<MatchStateEntity>` | Historial de estados (relación `@OneToMany`). |

Los campos `createdAt` y `updatedAt` se asignan automáticamente con `@PrePersist` / `@PreUpdate`. El campo `finishedAt` se establece manualmente al terminar la partida.

## MatchStatus

El enum `MatchStatus` (en `BE/.../engine/MatchStatus.java`) define cuatro estados:

- `WAITING` — La partida fue creada pero aún no comenzó; espera a un segundo jugador.
- `SETUP` — Los jugadores están configurando su mano inicial y mulligans.
- `ACTIVE` — La partida está en curso, los jugadores realizan acciones por turnos.
- `FINISHED` — La partida terminó por algún motivo. Este es el estado que se consulta para el historial.

## Cómo se consulta el historial

### Repositorio

`MatchJpaRepository` (en `BE/.../repositories/jpa/MatchJpaRepository.java`) provee el método:

```java
List<MatchEntity> findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc(String status, UUID playerId);
```

- Filtra por `status = "FINISHED"`.
- Busca partidas donde el `playerId` exista en la lista de jugadores.
- Ordena por `createdAt` descendente (más recientes primero).

### Servicio

`MatchHistoryService.getHistoryByPlayer(UUID playerId)` (en `BE/.../services/matches/MatchHistoryService.java`):

1. Llama al repositorio con `status = "FINISHED"`.
2. Filtra adicionalmente las partidas cuyo `finishReason` sea `"EXPIRED"` (partidas expiradas **no** se muestran en el historial).
3. Por cada partida construye un `MatchSummaryResponse` mediante el método `buildSummary`.

`MatchHistoryService.getHistoryDetail(UUID matchId)` permite obtener el detalle de una partida específica por su ID, aplicando los mismos filtros (`FINISHED` y no `EXPIRED`).

### Controlador

`MatchHistoryController` (en `BE/.../controllers/matches/MatchHistoryController.java`):

| Endpoint | Método | Descripción |
|---|---|---|
| `GET /api/matches/history?playerId={playerId}` | `listHistory` | Devuelve la lista de partidas finalizadas de un jugador. |
| `GET /api/matches/history/{id}` | `getHistoryDetail` | Devuelve el detalle de una partida específica. |

## MatchSummaryResponse

Estructura de respuesta (`BE/.../dtos/matches/MatchSummaryResponse.java`):

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` | ID de la partida (convertido a string). |
| `winnerName` | `String` | Nombre visible del ganador. |
| `loserName` | `String` | Nombre visible del perdedor. |
| `totalTurns` | `int` | Número total de turnos jugados. |
| `createdAt` | `Instant` | Fecha/hora de creación de la partida. |
| `durationSeconds` | `Long` | Duración total en segundos (`finishedAt - createdAt`), o `null` si no está disponible. |
| `finishReason` | `String` | Motivo de finalización. |

Además expone métodos helper:
- `formattedDate()` — retorna `"dd/MM/yy"`.
- `formattedTime()` — retorna `"HH:mm"`.
- `formattedDuration()` — retorna `"Xm Ys"` a partir de `durationSeconds`.

### Cómo se determina ganador/perdedor

En `MatchHistoryService.buildSummary`:
- Se obtiene `winnerPlayerId` de la entidad.
- Se ordenan los jugadores por `side` (0, 1).
- El que coincide con `winnerPlayerId` es el ganador; el otro es el perdedor.
- `winnerName` y `loserName` se toman de `MatchPlayerEntity.displayName`.
- Si no hay ganador (partida sin resolución), ambos nombres se asignan igual.

## Motivos de finalización (finishReason)

| Valor | Descripción |
|---|---|
| `KNOCKOUT` | Un Pokémon quedó sin HP y el jugador no pudo reemplazarlo. |
| `PRIZES` | Un jugador tomó todas sus cartas de premio. |
| `DECK_OUT` | Un jugador intentó robar y no le quedaban cartas en el mazo. |
| `CONCEDE` | Un jugador se rindió voluntariamente. |
| `EXPIRED` | La partida expiró (no se muestra en el historial). |
| `CANCELLED` | La partida fue cancelada sin resolución de ganador. |

Las partidas con `finishReason = "EXPIRED"` se excluyen del historial.
