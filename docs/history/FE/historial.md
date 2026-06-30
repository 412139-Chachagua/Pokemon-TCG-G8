# Historial de Partidas — Frontend

## Ruta

El historial se carga mediante lazy loading en la ruta raíz del módulo de historial (`FE/.../history/routes/routes.ts`):

```typescript
{ path: '', loadComponent: () => import('../pages/history-page/history-page').then(m => m.HistoryPage) }
```

La ruta se integra en el enrutador principal con `historyRoutes`.

## Layout de la página

El componente `HistoryPage` (`FE/.../history/pages/history-page/history-page.ts`) muestra un título **HISTORIAL** y una tabla con las siguientes columnas:

| Columna | Descripción |
|---|---|
| `#` | Número de orden (1-indexed, según la posición en el array). |
| `FECHA` | Fecha y hora de creación de la partida en formato `dd/MM/yy HH:mm`. |
| `DURACIÓN` | Duración total en formato `Xm Ys`. |
| `RIVAL` | Nombre del oponente (se determina comparando con el nombre del jugador actual). |
| `RESULTADO` | Texto: `Victoria`, `Derrota` o `Empate`, con color semántico (verde/rojo/gris). |
| `MOTIVO` | Traducción legible del `finishReason`. |
| `TURNOS` | Número total de turnos jugados. |

### Estados visuales

- **Carga**: Muestra un `LoadingSpinnerComponent` mientras se obtienen los datos.
- **Error**: Muestra una fila con el mensaje de error ocupando todas las columnas.
- **Vacío**: Muestra `"No tenés partidas registradas aún."`.
- **Datos**: Lista de partidas en filas de la tabla.

## Modelo de datos

`MatchHistoryEntry` (`FE/.../shared/models/match-history.models.ts`):

```typescript
interface MatchHistoryEntry {
  id: string;
  winnerName: string;
  loserName: string;
  totalTurns: number;
  createdAt: string;          // ISO string
  durationSeconds: number | null;
  finishReason: string | null;
}
```

## Carga de datos

El servicio `MatchApiService.getHistory(playerId)` (`FE/.../core/api/match-api.service.ts`) realiza una petición `GET` a `/api/matches/history?playerId=${playerId}` y devuelve un `Observable<MatchHistoryEntry[]>`.

En el constructor del componente se llama a `loadHistory()`, que:
1. Obtiene el `playerId` desde `AuthService`.
2. Si no hay playerId, muestra error `"No se pudo identificar al jugador"`.
3. Llama a `matchApi.getHistory(pid)`.
4. En `next`: asigna los datos a la señal `entries`.
5. En `error`: asigna el mensaje `"Error al cargar el historial"`.

## Clasificación del resultado

- **Victoria** (`isWin`): No es empate y `winnerName` coincide con el `displayName` del jugador autenticado.
- **Derrota** (`isLoss`): No es empate y `loserName` coincide con el `displayName` del jugador.
- **Empate** (`isDraw`): `finishReason` es `CANCELLED`, `EXPIRED` o `SUDDEN_DEATH`.

### Nombre del rival

Se obtiene comparando el nombre del jugador actual: si es el ganador, el rival es `loserName`; si no, el rival es `winnerName`.

### Traducción de motivos

| `finishReason` | Etiqueta mostrada |
|---|---|
| `KNOCKOUT` | KO |
| `PRIZES` | Premios |
| `DECK_OUT` | Sin cartas |
| `CONCEDE` | Rendición |
| `EXPIRED` | Tiempo agotado |
| `CANCELLED` | Cancelada |
| otros / `null` | — |

## Filtros y ordenamiento

Actualmente no hay filtros ni opciones de ordenamiento en la UI. El backend devuelve las partidas ordenadas por fecha descendente. No se implementa paginación; se obtiene la lista completa de partidas finalizadas del jugador (excluyendo las expiradas).
