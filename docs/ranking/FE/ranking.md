# Ranking — Frontend

## Ruta

El módulo de ranking se carga mediante lazy loading en la ruta `''` (vacía) dentro del feature `ranking`. Definido en `routes.ts` usando `loadComponent` para cargar `RankingPage`.

## Modelos de datos

`ranking.models.ts` define dos interfaces:

- **`RankingEntry`**: `rank`, `playerId`, `displayName`, `totalWins`, `totalLosses`, `winRate`, `currentWinStreak`, `maxWinStreak`.
- **`PlayerStats`**: `playerId`, `displayName`, `totalWins`, `totalLosses`, `currentWinStreak`, `maxWinStreak`.

## Servicio de API

`RankingApiService` (en `ranking-api.service.ts`) expone dos métodos:

- `getRanking()` → `GET /api/ranking` → devuelve `Observable<RankingEntry[]>`
- `getPlayerStats(playerId)` → `GET /api/players/{playerId}/stats` → devuelve `Observable<PlayerStats>`

Utiliza `ApiClientService` como cliente HTTP base.

## Página de ranking (`RankingPage`)

### Layout

La página renderiza una tabla HTML de 7 columnas con el siguiente diseño:

| # | Jugador | Victorias | Derrotas | Win Rate | Racha | Máx racha |
|---|---|---|---|---|---|---|

### Flujo de datos

1. Al inicializarse el componente (`constructor`), se dispara `loadRanking()`.
2. Durante la carga se muestra un `LoadingSpinner`.
3. Si la petición falla, se muestra un mensaje de error: *"Error al cargar el ranking"*.
4. Si no hay datos, se muestra: *"Ningún jugador tiene partidas registradas aún."*.
5. Los datos se almacenan en una signal `entries` y se iteran con un `@for`.

### Presentación visual

- **Loading**: spinner con `LoadingSpinnerComponent`.
- **Rank (#)**: resaltado en color `--pk-accent` (púrpura) y bold.
- **Jugador**: texto brillante con `--pk-text-bright`, bold.
- **Victorias**: color verde (`--pk-success`), bold.
- **Derrotas**: color rojo (`--pk-error`).
- **Win Rate**: color tenue (`--pk-text-dim`), formateado con `toFixed(1)` más símbolo `%`.
- **Racha**: si `currentWinStreak >= 3` se aplica la clase `hot` que lo muestra en `--pk-accent-glow` (efecto neón). Si es 0 se muestra un guión `-`.
- **Máx racha**: color tenue; si es 0 se muestra `-`.
- **Título**: celda `RANKING` con `colspan="7"`, letras espaciadas, borde inferior con acento púrpura y text-shadow glow.
- **Hover**: cada fila cambia su fondo a `--pk-surface` al pasar el mouse.

### Sorting / Filtrado

No hay sorting ni filtrado en el frontend. El orden viene dado por el backend (más victorias, desempate por máxima racha). La tabla se muestra tal cual la devuelve la API.
