## Context

El engine de juego (`GameEngine`) ya detecta condiciones de victoria a través de `VictoryConditionChecker` y setea `winnerPlayerId` y `finishReason` en el `GameState` en memoria. Ese estado se serializa como JSON en `MatchStateEntity`. Sin embargo, la entidad `MatchEntity` en la tabla `matches` nunca recibe el ganador — sus columnas `winner_player_id`, `finish_reason` y `finished_at` quedan siempre `null` para partidas terminadas. No existe ninguna tabla de estadísticas de jugador ni endpoint de ranking.

## Goals / Non-Goals

**Goals:**
- Persistir el ganador en `MatchEntity` cuando una partida finaliza
- Crear `PlayerStatsEntity` para almacenar wins/losses/streaks por jugador
- Calcular racha de victorias: incrementar en victoria consecutiva, resetear a 0 en derrota
- Exponer `GET /api/ranking` ordenado por wins descendente
- Exponer `GET /api/players/{id}/stats` para estadísticas individuales
- Crear página de ranking en el frontend
- Actualizar el contrato REST API

**Non-Goals:**
- Sistema de rating ELO/Glicko
- Ranking por temporadas
- Historial de partidas por jugador
- Seguimiento de estadísticas detalladas (cartas usadas, daño infligido, etc.)
- WebSocket en tiempo real para actualización de ranking

## Decisions

1. **Opción B: PlayerStatsEntity dedicada** — Se crea una entidad JPA separada para estadísticas en lugar de calcular rankings on-the-fly desde la tabla `matches`. Esto evita tener que deserializar JSON de `MatchStateEntity` para cada consulta y permite consultas eficientes `ORDER BY total_wins DESC`.

2. **Inyección directa de PlayerStatsService (no eventos de Spring)** — Se inyecta `PlayerStatsService` en `MatchApplicationService` y se llama `recordMatchResult()` al final de `executeAction()`. Alternativa evaluada: `ApplicationEventPublisher` de Spring. Se descartó porque el código base no usa ese patrón en el flujo de partidas. La inyección directa es simple, testable y consistente con el estilo existente.

3. **Racha de victorias en la misma entidad** — `currentWinStreak` y `maxWinStreak` se almacenan directamente en `PlayerStatsEntity`. Se incrementan en cada victoria consecutiva, se resetear a 0 en derrota. Esto es más eficiente que calcular streaks desde cero ordenando partidas históricas.

4. **Sudden death no registra resultado** — Cuando ocurre un empate por sudden death (ambos jugadores cumplen condición de victoria simultáneamente), no se actualizan estadísticas. No hay ganador ni perdedor.

5. **5 iteraciones incrementaless** — Se implementó en iteraciones cortas y validables: 0) fix winner persistence, 1) entity+repo, 2) service+DTOs, 3) integración en MatchApplicationService, 4) controller+frontend.

## Risks / Trade-offs

- **[Datos históricos sin backfill]** Las partidas terminadas antes de esta implementación no tienen winner en `MatchEntity`. Si se necesita ranking histórico, se requiere un backfill one-time que lea `MatchStateEntity.serialized_state`. Aceptable para MVP.
- **[CONCEDE sin implementar]** `FinishReason.CONCEDE` existe en el enum pero no hay handler ni endpoint de rendición. Cuando se implemente, debe tratar la rendición como derrota para el que se rinde.
- **[Sin paginación en ranking]** Con muchos jugadores, la lista puede crecer. Aceptable para MVP con usuarios de prueba limitados.
