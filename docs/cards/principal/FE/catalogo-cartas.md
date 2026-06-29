# Catálogo de Cartas

## Vista General

El catálogo de cartas (`CardCatalogPage`) es la página principal de exploración de cartas. Presenta una cuadrícula responsiva de cartas con funcionalidades de búsqueda, filtrado y paginación.

### Estructura de la Página

La página se compone de las siguientes secciones:

1. **Barra de búsqueda** (`SearchBarComponent`): campo de texto para buscar cartas por nombre.
2. **Filtro de supertipo** (`CardFilterComponent`): selector para filtrar por tipo general (Pokémon, Energía, Entrenador, Todas).
3. **Filtro de etapa** (`CardFilterComponent`): selector para filtrar por etapa evolutiva (Básico, Stage 1, Stage 2, MEGA). Se deshabilita cuando el supertipo no es `POKEMON`.
4. **Cuadrícula de cartas** (`CardViewComponent`): grilla responsiva que muestra las cartas en miniatura.
5. **Paginación** (`PaginationComponent`): controles de navegación entre páginas de resultados.

Rutas definidas en `routes.ts`:

- `''` → `CardCatalogPage` (listado)
- `:id` → `CardDetailPage` (detalle individual)

---

## CardCatalogFacadeService

Servicio fachada que orquesta todo el flujo de datos del catálogo. Estados manejados como señales (`signal`):

| Señal | Tipo | Descripción |
|-------|------|-------------|
| `_query` | `string` | Término de búsqueda por nombre |
| `_supertype` | `string` | Filtro de supertipo (POKEMON, ENERGY, TRAINER) |
| `_stage` | `string` | Filtro de etapa evolutiva |
| `_page` | `number` | Página actual (0-indexed) |
| `_pageSize` | `number` | Tamaño de página (24) |
| `_cards` | `CardSummaryResponse[]` | Resultados de la búsqueda |
| `_totalItems` | `number` | Total de resultados disponibles |
| `_loading` | `boolean` | Indicador de carga |
| `_error` | `string \| null` | Mensaje de error |

Propiedades computadas:

- `totalPages`: calcula el número total de páginas como `Math.ceil(totalItems / pageSize)`.

### Flujo de datos

1. El usuario interactúa con los componentes de UI.
2. Los métodos `setQuery()`, `setSupertype()`, `setStage()` actualizan su respectiva señal y resetean la página a 0.
3. Cada setter llama internamente a `search()`.
4. `search()` establece `loading = true`, llama a `CardApiService.searchCards()` con los parámetros actuales y suscribe al Observable.
5. En éxito: actualiza `_cards` y `_totalItems`, desactiva loading.
6. En error: establece `_error` con mensaje "Error al cargar las cartas".

---

## CardApiService

Servicio de API que define los endpoints para interactuar con el backend de cartas.

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `searchCards(request)` | `GET /cards?query=&supertype=&stage=&page=&size=` | Búsqueda paginada de cartas |
| `getCardById(cardId)` | `GET /cards/{id}` | Obtener detalle de una carta |
| `syncCards()` | `POST /cards/sync` | Disparar sincronización con API externa |

### Interfaces

```typescript
interface CardSearchRequest {
  query?: string;
  supertype?: string;
  setCode?: string;
  stage?: string;
  page?: number;
  size?: number;
}
```

El método `searchCards` construye los query params dinámicamente omitiendo aquellos indefinidos y delega en `ApiClientService.get()`.

---

## CardRepositoryService

Capa de caché en memoria para cartas individuales (detalle completo). Utiliza un `Map<string, CardDetailResponse>` envuelto en una señal.

### Métodos principales

| Método | Descripción |
|--------|-------------|
| `resolve(cardId)` | Obtiene una carta. Si está en caché la retorna inmediatamente; si otro llamado la está cargando, espera; si no, hace `getCardById()` y la guarda en caché. |
| `preload(cardIds)` | Precarga múltiples cartas en paralelo. |
| `getFromCache(cardId)` | Retorna la carta si está en caché, o `null`. |

Manejo de concurrencia: si dos componentes solicitan la misma carta simultáneamente, el segundo espera mediante `waitForLoad()` usando un `setInterval` de 50ms hasta que la carta deje de estar en el set `_loading`.

---

## CardFilterComponent

Componente de filtro tipo `<select>` desplegable. Opera con la interfaz `FilterOption`:

```typescript
interface FilterOption {
  label: string;
  value: string;
}
```

Inputs: `options` (arreglo de opciones), `selected` (valor actual), `disabled` (booleano).
Output: `filterChange` emite el valor seleccionado.

Se usa dos veces en el catálogo: una para supertipo y otra para etapa evolutiva.

---

## PaginationComponent

Componente de navegación entre páginas. Inputs: `currentPage`, `totalPages`. Output: `pageChange`.

- Calcula `visiblePages()` como una ventana deslizante de máximo 5 páginas centradas en la actual.
- Botones de anterior (`<`) y siguiente (`>`) que se deshabilitan en los extremos.
- Cada botón de página ejecuta `goToPage()` que valida el rango y emite el cambio.

---

## Modelos de Cartas

### CardSummaryResponse

Modelo resumido para el listado del catálogo:

```typescript
interface CardSummaryResponse {
  id: string;
  name: string;
  supertype: string;
  setCode: string;
  number: string;
  imageSmallUrl: string;
  subtypes: string[];
  stage?: string;
}
```

### CardDetailResponse

Modelo completo con toda la información de la carta (ver `detalle-carta.md`).

### PaginatedCardsResponse

Envoltura de la respuesta paginada:

```typescript
interface PaginatedCardsResponse {
  items: CardSummaryResponse[];
  page: number;
  size: number;
  totalItems: number;
}
```

### CardModel

Modelo base compartido que incluye todos los campos de una carta Pokémon (hp, stage, types, attacks, weaknesses, resistances, retreatCost, etc.).

---

## Visualización de Cartas

### CardViewComponent

Componente de tarjeta en miniatura para la cuadrícula del catálogo. Muestra:

- Imagen pequeña de la carta (usando `CardImagePipe` con tamaño `small`).
- Nombre truncado.
- Supertipo y código del set.
- Botón flotante (+) para previsualización rápida mediante `CardPreviewService`.

Manejo de error de imagen: si la imagen falla, la oculta y muestra el nombre de la carta como placeholder.

### PokemonCardComponent

Componente de visualización detallada de una carta (usado en la página de detalle). Muestra:

- **Imagen grande** con `CardImagePipe` (tamaño `large`).
- **Encabezado**: nombre, HP (si es Pokémon), supertipo y etapa evolutiva con colores distintivos.
- **Tipo(s)**: etiquetas con colores según el tipo de energía.
- **Evoluciona de**: texto informativo.
- **Metadatos**: set code, número, subtipos, badges EX/MEGA.
- **Reglas**: texto de reglas si aplica.
- **Ataques**: cada ataque muestra nombre, coste de energía (círculos de colores con abreviaturas), daño y texto descriptivo.
- **Estadísticas**: debilidades, resistencias y coste de retiro (mostrados como círculos de tipo con multiplicador/valor).

Colores definidos en constantes:
- `TYPE_BG`: colores para cada tipo de energía.
- `TYPE_ABBR`: abreviaturas de una letra para cada tipo.
- `STAGE_BG`: colores para etapa evolutiva.
- `TRAINER_SUBTYPE_BG`: colores para subtipos de entrenador.
