# Deck Viewer y Discard Viewer (Visores de Mazo y Descarte)

## Ubicación
- Visor de mazo: `FE/src/app/features/match/components/deck-viewer/deck-viewer.component.ts`
- Visor de descarte: `FE/src/app/features/match/components/discard-viewer/discard-viewer.component.ts`
- Pila de descarte: `FE/src/app/features/match/components/discard-pile/discard-pile.component.ts`

## Propósito
Permiten al jugador **visualizar su mazo** y su **pila de descarte** durante la partida. El mazo se muestra para buscar cartas (selección), mientras que el descarte es informativo y permite inspeccionar las cartas descartadas.

---

## DeckViewerComponent

### Propósito
Selector de cartas del mazo que se abre como modal. Usado cuando el juego requiere que el jugador busque cartas en su mazo (ej. efectos de entrenadores o habilidades).

### Inputs
| Input | Tipo | Default | Descripción |
|---|---|---|---|
| `deck` | `PrivateHandCardModel[]` | — | Cartas del mazo con `instanceId`, `cardId`, `name`, `supertype` |
| `title` | `string` | `'Seleccionar carta'` | Título del modal |
| `selectionMode` | `'single' \| 'multi'` | `'single'` | Selección simple o múltiple |
| `maxSelections` | `number` | `2` | Máximo de cartas seleccionables en modo multi |
| `allowedSupertype` | `string \| null` | `null` | Filtro por supertipo (ej. solo `'POKEMON'`) |

### Outputs
| Output | Tipo | Descripción |
|---|---|---|
| `close` | `void` | Cerrar el modal sin seleccionar |
| `cardSelected` | `{cardIndex, cardId, instanceId}` | Una carta seleccionada (modo single) |
| `cardsSelected` | `{cardIndexes[], cardIds[]}` | Múltiples cartas seleccionadas (modo multi) |

### Funcionamiento
- Transforma `PrivateHandCardModel[]` en `DeckCardVm[]` usando `CardRepositoryService.getFromCache()` para enriquecer con nombre y supertipo.
- Modal responsivo con grilla de cartas (`grid-cols-4` a `lg:grid-cols-7`).
- Las cartas no permitidas según `allowedSupertype` se muestran con opacidad reducida y cursor `not-allowed`.
- En modo `multi`: las cartas seleccionadas tienen un ring verde y escala. Se muestra contador `"N / max seleccionadas"`.
- Botón "Cancelar" y "Confirmar" (multi) en el footer.
- Se cierra con tecla Escape. Previene scroll del body mientras está abierto.

---

## DiscardViewerComponent

### Propósito
Modal que muestra la **pila de descarte** completa del jugador o del oponente (cartas públicas). También puede actuar como selector cuando se necesita elegir un Pokémon de la pila de descarte (ej. efecto de carta).

### Inputs
| Input | Tipo | Descripción |
|---|---|---|
| `discard` | `PublicDiscardCardModel[]` | Cartas en la pila de descarte |
| `discardCount` | `number` | Cantidad total de descartes |
| `selectable` | `boolean` | Si es `true`, permite seleccionar Pokémon |

### Outputs
| Output | Tipo | Descripción |
|---|---|---|
| `close` | `void` | Cerrar el modal |
| `cardSelected` | `{cardIndex, cardId, instanceId}` | Pokémon seleccionado |

### Funcionamiento
- Las cartas se muestran en **orden inverso** (más reciente primero) para emular el comportamiento físico de la pila.
- En modo `selectable`, solo los Pokémon (`supertype === 'POKEMON'`) son cliqueables; las demás cartas aparecen atenuadas.
- Cada carta tiene un botón "+" que abre una vista previa ampliada vía `CardPreviewService`.
- Muestra contador de cartas en el encabezado.
- Estado vacío: imagen de card-back con texto "Pila de descarte vacía".
- Se cierra con Escape. Previene scroll del body.

---

## DiscardPileComponent

### Propósito
Componente **visual** que representa la pila de descarte en el tablero de juego. Muestra la última carta descartada y la cantidad total.

### Inputs
| Input | Tipo | Descripción |
|---|---|---|
| `discardCount` | `number` | Cantidad de cartas en descarte |
| `discard` | `PublicDiscardCardModel[]` | Cartas de descarte |
| `openViewer` | `output` | Evento para abrir el visor completo |

### Funcionamiento
- `topCard` es un `computed` que devuelve la última carta del arreglo (`discard[discard.length - 1]`).
- Si hay al menos una carta, muestra su imagen. Si no, muestra un card-back genérico.
- Tooltip al hacer hover: "Descarte (N cartas)".
- Al hacer clic, emite `openViewer` para que el padre abra el `DiscardViewerComponent`.

---

## Dependencias comunes
- `CardImagePipe` — resolución de URLs de imágenes de cartas.
- `CardRepositoryService` — caché de definiciones de cartas.
- `CardPreviewService` — vista previa ampliada de carta individual.
- `PrivateHandCardModel` y `PublicDiscardCardModel` — modelos de estado de juego.
