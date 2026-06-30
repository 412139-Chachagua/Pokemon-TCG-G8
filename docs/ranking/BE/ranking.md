# Ranking — Backend

## PlayerStatsEntity

Entidad JPA que mapea la tabla `player_stats`. Almacena las estadísticas de cada jugador:

| Campo | Tipo | Descripción |
|---|---|---|
| `playerId` | `UUID` (PK) | Identificador único del jugador |
| `totalWins` | `int` | Victorias totales |
| `totalLosses` | `int` | Derrotas totales |
| `currentWinStreak` | `int` | Rachas de victorias actual |
| `maxWinStreak` | `int` | Máxima racha de victorias histórica |
| `updatedAt` | `Instant` | Última actualización (se setea automáticamente con `@PrePersist`/`@PreUpdate`) |

## Lógica de cálculo del ranking

El ranking se construye en `PlayerStatsService.getRanking()`. El servicio:

1. Obtiene todas las estadísticas ordenadas por `totalWins DESC, maxWinStreak DESC` mediante la query `findAllByOrderByTotalWinsDescMaxWinStreakDesc()` del `PlayerStatsJpaRepository`.
2. Para cada registro, calcula el **win rate** como `(totalWins / (totalWins + totalLosses)) * 100`, redondeado a 2 decimales.
3. Resuelve el `displayName` del jugador consultando `PlayerJpaRepository`.
4. Asigna un número de **rank** correlativo (1, 2, 3…).
5. Devuelve una lista de `RankingEntryResponse` con: `rank`, `playerId`, `displayName`, `totalWins`, `totalLosses`, `winRate`, `currentWinStreak`, `maxWinStreak`.

## Endpoints

### `GET /api/ranking`

Devuelve la lista completa de jugadores ordenados por performance (más victorias, y como desempate máxima racha).

### `GET /api/players/{id}/stats`

Devuelve las estadísticas individuales de un jugador específico (`PlayerStatsResponse`), incluyendo `playerId`, `displayName`, `totalWins`, `totalLosses`, `currentWinStreak`, `maxWinStreak`.

## Actualización de estadísticas post-partida

El método `recordMatchResult(matchId, winnerPlayerId, finishReason)` en `PlayerStatsService` se encarga de actualizar las estadísticas:

1. **Validaciones**: si no hay ganador o la partida terminó en `SUDDEN_DEATH`, no se registra nada.
2. Determina el perdedor buscando el `MatchPlayerEntity` que no sea el ganador.
3. Para el **ganador**:
   - Incrementa `totalWins` en 1.
   - Incrementa `currentWinStreak` en 1.
   - Si `currentWinStreak` supera a `maxWinStreak`, actualiza este último.
4. Para el **perdedor**:
   - Incrementa `totalLosses` en 1.
   - Reinicia `currentWinStreak` a 0.

Ambas operaciones se ejecutan dentro de una misma transacción (`@Transactional`). Si el jugador no tenía estadísticas previas se crea una nueva entidad con valores por defecto (0).
