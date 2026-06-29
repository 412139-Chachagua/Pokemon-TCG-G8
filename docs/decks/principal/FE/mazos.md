# Mazos — Frontend (Angular)

## Vista general

La funcionalidad de mazos se divide en dos páginas principales: **lista de mazos** (`DeckListPage`) y **constructor de mazos** (`DeckBuilderPage`). Ambas son cargadas de forma diferida (_lazy loading_) a través de las rutas definidas en `routes.ts`.

## Enrutamiento (`routes.ts`)

| Ruta | Componente |
|---|---|
| `/decks` | `DeckListPage` |
| `/decks/new` | `DeckBuilderPage` (creación) |
| `/decks/:id/edit` | `DeckBuilderPage` (edición) |

---

## Página de lista de mazos (`DeckListPage`)

Es el punto de entrada de la feature. Se encarga de obtener los mazos del jugador autenticado y renderizar la lista.

### Flujo de datos

1. En el constructor llama a `loadDecks()`, que obtiene el `playerId` desde `AuthService` y ejecuta `DeckApiService.listByPlayer(playerId)`.
2. Las respuestas se almacenan en la señal `deckData: Signal<{ decks, loading, error }>`.
3. Un `computed` llamado `filteredDecks` filtra los mazos localmente según `searchQuery` (búsqueda por nombre).

### Barra de herramientas

- Input de texto para filtrar mazos por nombre (filtro local, sin llamada al backend).
- Botón **"+ Importar"** → abre `ImportDeckModalComponent`.
- Botón **"+ Nuevo mazo"** → abre `NewDeckModalComponent`.

### Modal "Nuevo Mazo" (`NewDeckModalComponent`)

Ofrece dos opciones en tarjetas visuales:

- **PREDEFINIDO**: dispara el evento `predefined`, que abre `PredefinedDeckModalComponent`.
- **DESDE CERO**: navega a `/decks/new?playerId=xxx` donde se abre el `DeckBuilderPage` vacío.

### Modal "Mazos Predefinidos" (`PredefinedDeckModalComponent`)

Carga desde `DeckApiService.getPredefinedDecks()` una lista de mazos oficiales. Cada mazo se muestra como una tarjeta con:

- Imagen de la carta principal (`mainCardImageUrl`).
- Nombre del mazo.
- Conteo de Pokémon / Entrenador / Energía.

Al hacer clic en un mazo, se llama a `DeckApiService.copyDeck(deckId, playerId)`, que copia el mazo predefinido al jugador actual. Muestra un spinner mientras se copia y una notificación de éxito/error.

### Modal "Importar Mazos" (`ImportDeckModalComponent`)

Ver sección [Exportar/Importar PDF — Frontend](../externo/FE/exportar-importar-pdf.md).

---

## Componente `DeckListComponent`

Es un componente puramente de presentación. Recibe como inputs:

- `decks: DeckResponse[]` — la lista a renderizar.
- `loading: boolean` — muestra un spinner.
- `error: string | null` — muestra el error con botón "Reintentar".
- `empty: boolean` — muestra mensaje "No hay mazos disponibles".

Emite eventos: `delete`, `play`, `edit`, `retry`.

Renderiza un `DeckItemComponent` por cada mazo.

---

## Componente `DeckItemComponent`

Muestra la información de un mazo individual en un panel tipo fila.

### Información visible

- **Nombre** del mazo (con truncado si es largo).
- **Composición**: calculada con un `computed` que filtra `deck().cards` por supertype:
  - `{pokemon} PC · {trainer} ENTRENADOR · {energy} ENERGÍA`
- **Fecha de creación** formateada como `DD/MM/AAAA`.

### Acciones disponibles

| Botón | Comportamiento |
|---|---|
| **Descargar PDF** (ícono) | Muestra confirmación "¿Descargar mazo?", luego llama a `DeckApiService.exportDeckPdf(id)` que retorna un `Blob`. Crea un enlace de descarga temporal con `URL.createObjectURL`. |
| **EDITAR** | Navega a `/decks/{id}/edit`. |
| **JUGAR** | Solo visible si `deck().valid === true`. Navega a `/lobby?deckId={id}`. |
| **ELIMINAR** | Muestra confirmación "¿Eliminar?" con botón confirmar/cancelar. Al confirmar llama a `DeckApiService.delete(id)`. |

### Confirmación de acciones

Tanto eliminar como descargar requieren una confirmación explícita (estado `confirmingDelete` / `confirmingDownload` mediante señales), evitando acciones accidentales.

---

## Componente `DeckValidationComponent`

Pequeño componente de presentación que muestra una etiqueta visual:

- `valid = true` → badge verde "Válido" con ícono check.
- `valid = false` → badge rojo "Inválido" con ícono X.
- `valid = null` → no renderiza nada.

---

## Página constructora de mazos (`DeckBuilderPage`)

Es la página principal de edición/creación. Se usa tanto para mazos nuevos como para editar existentes (determinado por la presencia del parámetro `:id` en la ruta).

### Layout

Diseño de dos columnas (responsive: `lg:grid-cols-2`):

- **Columna izquierda**: panel de búsqueda de cartas (`DeckSearchComponent`).
- **Columna derecha**: lista de cartas del mazo (`DeckCardListComponent`), resumen (`DeckSummaryComponent`) y botones de acción.

### Barra superior

Campo de texto para el **nombre del mazo**, fuera de las columnas.

### Inicialización y carga

- Si la ruta contiene un `:id`, se llama a `DeckApiService.get(id)` para cargar el mazo existente y se setean las cartas en el `DeckBuilderFacadeService`.
- Si no hay `:id` (ruta `/decks/new`), se parte de un mazo vacío.

### Manejo de selección de cartas (`onCardSelected`)

Al seleccionar una carta desde la búsqueda (clic o drag & drop):

1. **Límite de 4 copias**: si la carta no es energía y ya tiene 4 copias, se rechaza con notificación.
2. **Regla ACE SPEC**: solo se permite 1 carta con subtipo `ACE_SPEC` por mazo.
3. Se agrega la carta vía `facade.addCard(...)`.

### Botones de acción

| Botón | Comportamiento |
|---|---|
| **Guardar** | Valida el mazo (si es nuevo, llama a `facade.validate()`; si es edición, llama a `deckApi.validate(id)`). Si es válido, llama a `deckApi.create()` o `deckApi.update()` según corresponda. Luego navega a `/decks`. |
| **Al Azar** | Llama a `DeckApiService.generateRandom()` y carga las cartas devueltas en el facade, junto con el resultado de validación. |
| **Limpiar** | Resetea el facade y el nombre del mazo. |

---

## `DeckBuilderFacadeService`

Servicio singleton (`providedIn: 'root'`) que actúa como **orquestador del estado del constructor**. Mantiene el conjunto actual de cartas en una señal privada `_cards` y expone múltiples computed signals derivadas.

### Estado interno (`_cards: Signal<DeckCardEntry[]>`)

Cada `DeckCardEntry` contiene:
```
{ cardId, name, supertype, subtypes[], stage?, isBasicEnergy, quantity }
```

### Computed signals expuestas

| Señal | Descripción |
|---|---|
| `cards` | Lista de cartas (solo lectura). |
| `totalCards` | Suma total de cantidades. |
| `isEmpty` | `true` si no hay cartas. |
| `aceSpecCount` | Conteo de cartas ACE SPEC. |
| `basicPokemonCount` | Conteo de Pokémon básicos. |
| `hasBasicPokemon` | `true` si hay al menos un básico. |
| `pokemonCount` | Suma de cantidades de tipo POKEMON. |
| `trainerCount` | Suma de cantidades de tipo TRAINER. |
| `energyCount` | Suma de cantidades de tipo ENERGY. |

### Métodos principales

| Método | Descripción |
|---|---|
| `addCard(cardId, name, supertype, ...)` | Incrementa cantidad si existe, o agrega nueva entrada con quantity=1. |
| `removeCard(cardId)` | Decrementa cantidad; si llega a 0, elimina la entrada. |
| `setCards(entries)` | Reemplaza todo el conjunto (útil al cargar un mazo existente o uno aleatorio). |
| `reset()` | Vacía el conjunto de cartas. |
| `validate()` | Envía las cartas a `DeckApiService.validateCards()` y retorna el `Observable<DeckValidationModel>`. |
| `createDeck(name)` | Crea el mazo en el backend y resetea el estado local. |

---

## `DeckSearchComponent`

Panel de búsqueda de cartas para agregar al mazo.

### Búsqueda

- Input de texto con **debounce de 300ms** y `distinctUntilChanged`.
- Filtro por supertipo: Todas / Pokémon / Energía / Entrenador.
- Filtro de etapa (solo visible cuando supertipo es "POKEMON"): Todas / Básico / Stage 1 / Stage 2 / MEGA.
- Paginación de **12 cartas por página**.

### Flujo reactivo

Usa RxJS para combinar cuatro fuentes de eventos (búsqueda, filtro de supertipo, filtro de etapa, cambio de página) mediante `merge`. Cada fuente reinicia la página a 0 (excepto cambios de página). La suscripción combinada llama a `CardApiService.searchCards(...)` con `switchMap`.

### Drag & Drop

Cada carta se renderiza con `cdkDrag` y lleva como `cdkDragData` un objeto `{ cardId, name, supertype, subtypes, stage }`. La drop list está conectada a `deck-list` mediante `cdkDropListConnectedTo`. Al hacer clic en una carta también se emite el evento `cardSelected`.

---

## `DeckCardListComponent`

Muestra las cartas actualmente en el mazo.

### Layout

Lista vertical, cada entrada muestra:
- Nombre y supertipo de la carta.
- Botones **−** y **+** para decrementar/incrementar cantidad.
- Cantidad actual.

El botón **+** se deshabilita si la carta ya tiene 4 copias (excepto energías básicas).

### Drag & Drop

Actúa como `cdkDropList` conectada a `search-grid`. Cuando se suelta una carta, emite `cardDropped` con los datos de la carta arrastrada, que luego son procesados por `DeckBuilderPage.onCardSelected()`.

---

## `DeckSummaryComponent`

Componente de presentación que muestra estadísticas y validación del mazo.

### Estadísticas

- **Total**: `{n} / 60` — se marca en rojo si no es exactamente 60.
- **Pokémon Básico**: `{n} / mínimo 4` — se marca rojo si < 4.
- **Pokémon**: conteo total de cartas Pokémon.
- **Entrenadores**: conteo total de cartas Entrenador.
- **Energías**: conteo total de cartas Energía.

### Validación

Si `validation` es `null`, muestra "Aún no validado."
Si `validation.valid` es `true`, muestra "✅ Listo para jugar."
Si `validation.valid` es `false`, muestra "❌ Inválido" con una lista de errores (código y mensaje).

---

## Flujo de datos entre frontend y backend

### API (`DeckApiService`)

| Método | Endpoint | Uso |
|---|---|---|
| `listByPlayer(playerId)` | `GET /decks?playerId=` | Obtener mazos del jugador. |
| `get(deckId)` | `GET /decks/{id}` | Cargar mazo para edición. |
| `create(request)` | `POST /decks` | Crear mazo nuevo. |
| `update(deckId, req)` | `PUT /decks/{id}` | Actualizar mazo existente. |
| `delete(deckId)` | `DELETE /decks/{id}` | Eliminar mazo. |
| `validate(deckId)` | `POST /decks/{id}/validate` | Validar mazo guardado. |
| `validateCards(cards)` | `POST /decks/validate` | Validar cartas en memoria (antes de guardar). |
| `generateRandom()` | `POST /decks/random` | Generar mazo aleatorio. |
| `exportDeckPdf(deckId)` | `GET /decks/{id}/export` (blob) | Descargar mazo como PDF. |
| `getPredefinedDecks()` | `GET /decks/predefined` | Obtener mazos predefinidos. |
| `copyDeck(deckId, playerId)` | `POST /decks/{id}/copy?playerId=` | Copiar mazo predefinido al jugador. |
| `importDecks(file, playerId, format)` | `POST /decks/import?playerId=&format=` (multipart) | Importar mazo(s) desde archivo. |

### Modelos compartidos (`deck.models.ts`)

- `DeckResponse`: respuesta completa con id, nombre, cartas, validación, fechas, etc.
- `CreateDeckRequest` / `UpdateDeckRequest`: payloads para crear/actualizar.
- `DeckCardModel`: carta dentro de un mazo (cardId, name, quantity, supertype, subtypes, stage, isBasicEnergy).
- `DeckValidationModel` / `DeckValidationResponse`: resultado de validación con lista de errores.

### Flujo típico de creación

1. Usuario hace clic en "+ Nuevo mazo" → elige "Desde cero".
2. Navega a `/decks/new?playerId=x`.
3. Busca cartas en `DeckSearchComponent` → `CardApiService.searchCards()`.
4. Agrega cartas al facade → `facade.addCard()`.
5. Al hacer clic en "Guardar":
   - `facade.validate()` → `POST /decks/validate`.
   - Si válido → `deckApi.create()` → `POST /decks`.
   - Navega a `/decks`.
6. En la lista, el nuevo mazo aparece con su estado de validación.
