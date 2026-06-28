## 1. Backend — Persistir winner en MatchEntity (Iteración 0)

- [x] 1.1 Modificar `MatchApplicationService.executeAction()` para extraer `GameState state` del bloque de carga
- [x] 1.2 Agregar bloque `if (state.getStatus() == MatchStatus.FINISHED)` que actualice `MatchEntity.winnerPlayerId`, `finishReason`, `finishedAt`, `status`
- [x] 1.3 Verificar compilación: `mvnw compile`
- [x] 1.4 Verificar tests existentes: `mvnw test` (sin regresiones)

## 2. Backend — PlayerStatsEntity + Repository (Iteración 1)

- [x] 2.1 Crear `PlayerStatsEntity` con `playerId` (PK), `totalWins`, `totalLosses`, `currentWinStreak`, `maxWinStreak`, `updatedAt`
- [x] 2.2 Crear `PlayerStatsJpaRepository` con `findByPlayerId()` y `findAllByOrderByTotalWinsDescMaxWinStreakDesc()`
- [x] 2.3 Verificar DDL de Hibernate: tabla `player_stats` generada correctamente

## 3. Backend — PlayerStatsService + DTOs (Iteración 2)

- [x] 3.1 Crear `RankingEntryResponse` DTO (rank, playerId, displayName, totalWins, totalLosses, winRate, currentWinStreak, maxWinStreak)
- [x] 3.2 Crear `PlayerStatsResponse` DTO (playerId, displayName, totalWins, totalLosses, currentWinStreak, maxWinStreak)
- [x] 3.3 Crear `PlayerStatsService.recordMatchResult()`: actualiza MatchEntity + PlayerStatsEntity para winner y loser
- [x] 3.4 Crear `PlayerStatsService.getRanking()`: ordena por wins descendente, luego max streak
- [x] 3.5 Crear `PlayerStatsService.getPlayerStats()`: stats individuales con defaults a 0
- [x] 3.6 Implementar lógica de streaks: incremento en victoria, reset en derrota, sudden death ignorado
- [x] 3.7 Escribir `PlayerStatsServiceTest` con 7 escenarios

## 4. Backend — Integrar en MatchApplicationService (Iteración 3)

- [x] 4.1 Agregar field + constructor param `PlayerStatsService` en `MatchApplicationService`
- [x] 4.2 Llamar `playerStatsService.recordMatchResult(matchId, winnerPlayerId, finishReason)` cuando el estado es FINISHED
- [x] 4.3 Verificar compilación: `mvnw compile`
- [x] 4.4 Verificar tests: `mvnw test` (82 tests, 0 fallos, 2 errores pre-existentes)

## 5. Backend — RankingController (Iteración 4)

- [x] 5.1 Crear `RankingController` con `GET /api/ranking` y `GET /api/players/{id}/stats`
- [x] 5.2 Escribir `RankingControllerTest` con 2 escenarios (MockMvc standalone)
- [x] 5.3 Verificar compilación + tests

## 6. Frontend — Ranking Page (Iteración 4)

- [x] 6.1 Crear `shared/models/ranking.models.ts` con interfaces `RankingEntry` y `PlayerStats`
- [x] 6.2 Crear `core/api/ranking-api.service.ts` con métodos `getRanking()` y `getPlayerStats()`
- [x] 6.3 Crear `features/ranking/routes.ts` con lazy loading de `RankingPage`
- [x] 6.4 Crear `features/ranking/pages/ranking-page/ranking-page.ts` con tabla responsive
- [x] 6.5 Registrar `/ranking` en `app.routes.ts`
- [x] 6.6 Verificar build de frontend: `npm run build` (sin errores de ranking)

## 7. Documentación

- [x] 7.1 Crear `.openspec.yaml`
- [x] 7.2 Crear `proposal.md`
- [x] 7.3 Crear `design.md`
- [x] 7.4 Crear `specs/ranking-api/spec.md`
- [x] 7.5 Crear `specs/player-stats-persistence/spec.md`
- [x] 7.6 Crear `specs/win-streak-calculation/spec.md`
- [x] 7.7 Crear `tasks.md`
- [x] 7.8 Actualizar `docs/contracts_ai/13-rest-api-contract.md` con endpoints de ranking
