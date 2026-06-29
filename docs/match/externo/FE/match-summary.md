# Match Summary (Resumen de Partida)

## Ubicación
- `FE/src/app/features/match/components/match-summary/match-summary.component.ts`
- `FE/src/app/features/match/components/summary-charts/summary-charts.component.ts`
- `FE/src/app/features/match/components/summary-charts/summary-charts.component.html`

## Estado actual
> ⚠️ **Importante**: Ambos componentes son plantillas vacías que aún no tienen lógica implementada. El componente `MatchSummaryComponent` referencia un template externo (`match-summary.component.html`) que **no existe** en el código base. El componente `SummaryChartsComponent` tiene un HTML placeholder con solo un título "Summary Charts" sin datos reales.

## Propósito (diseño esperado)
Estos componentes están destinados a mostrar la pantalla de **resumen post-partida** con:
- Información del ganador.
- Estadísticas de la partida (daño infligido, cartas robadas, etc.).
- Gráficos visuales comparativos entre jugadores.

## `MatchSummaryComponent`
- **Selector**: `app-match-summary`
- **Template**: busca `./match-summary.component.html` (archivo no encontrado).
- **Lógica**: clase vacía sin propiedades ni métodos (componente sin implementar).

## `SummaryChartsComponent`
- **Selector**: `app-summary-charts`
- **Template**: `summary-charts.component.html` existente.
- **HTML actual**: contenedor gris con texto "Summary Charts" — placeholder visual.
- **Lógica**: clase vacía sin propiedades ni métodos.

## Datos disponibles para la implementación futura
Los modelos existentes en el proyecto que probablemente alimentarán estos componentes incluyen:
- `MatchHistoryModel` — datos históricos de partidas.
- `PublicGameStateModel` — estado final de la partida (ganador, razón de finalización).
- `PublicPlayerStateModel` — stats de cada jugador (premios tomados, descartes, etc.).
- `RankingApiService.getPlayerStats()` — estadísticas de ranking para cada jugador.
