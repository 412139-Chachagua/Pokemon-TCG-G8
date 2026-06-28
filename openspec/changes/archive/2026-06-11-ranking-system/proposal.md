## Why

El sistema no posee ranking de jugadores ni seguimiento de rachas de victorias. Cuando una partida finaliza, el ganador solo se registra en el estado serializado del engine (`MatchStateEntity.serialized_state`), pero no se persiste en la tabla `matches` ni se contabilizan estadísticas por jugador. No existe forma de consultar quién es el mejor jugador ni su racha actual.

## What Changes

- **Backend - Persistencia:** Se corrige la persistencia del ganador en `MatchEntity.winner_player_id`, `finish_reason` y `finished_at` al finalizar la partida
- **Backend - Entidad:** Se crea `PlayerStatsEntity` con `totalWins`, `totalLosses`, `currentWinStreak`, `maxWinStreak`
- **Backend - Servicio:** Se crea `PlayerStatsService` con lógica de registro de resultados y cálculo de ranking
- **Backend - API:** Se agregan `GET /api/ranking` y `GET /api/players/{id}/stats`
- **Frontend - Página:** Se crea `RankingPage` con tabla de posiciones
- **Frontend - Ruta:** Se registra `/ranking` en el router (ya referenciada desde el menú Home)

## Capabilities

### New Capabilities
- `ranking-api`: Endpoints REST para consultar ranking y estadísticas de jugador
- `player-stats-persistence`: Persistencia de estadísticas de jugador (wins, losses, streaks)
- `win-streak-calculation`: Cálculo de racha de victorias consecutivas

### Modified Capabilities
- `rest-api-contract`: Se actualiza `docs/contracts_ai/13-rest-api-contract.md` para incluir los endpoints de ranking

## Impact

- **Backend:** Se crean 7 archivos nuevos (entity, repository, service, controller, 2 DTOs, test de servicio, test de controlador). Se modifican 2 existentes (`MatchApplicationService`, `MatchJpaRepository` opcionalmente)
- **Frontend:** Se crean 4 archivos nuevos (model, API service, routes, page component). Se modifica 1 (`app.routes.ts`)
- **Dependencias:** Ninguna nueva — todo se resuelve con las dependencias existentes (Spring Data JPA, Angular HttpClient)
