## Why

Persona C — Gestión de Mazos — necesita las páginas FrontEnd de listado y edición de mazos, construidas con Tailwind CSS, para que los usuarios puedan explorar, crear, editar y validar sus mazos Pokémon. Esto completa el flujo de administración de mazos necesario para el MVP.

## What Changes

- Nueva `DeckListPage` (`FE-03`) con carga por `playerId`, listado de mazos, badges de validación, y acciones CRUD (eliminar, validar, jugar).
- Nueva `DeckBuilderPage` (`FE-04`) con modos creación/edición, búsqueda de cartas, gestión de cantidades (máx. 4), resumen, validación y guardado.
- Todos los componentes UI con clases Tailwind CSS (sin CSS global ni SCSS).
- Breakpoint mobile fijado a 600px para el layout adaptable del builder.
- Código nuevo dentro de `FE/src/app/features/decks/`, consumiendo servicios y modelos existentes.

## Capabilities

### New Capabilities

- `deck-list-page`: Deck list page que permite cargar mazos por `playerId`, mostrar lista con badges de validación (✅/❌), y ejecutar acciones eliminar/validar/jugar con navegación al builder.
- `deck-builder-page`: Deck builder page con soporte para creación y edición, búsqueda de cartas vía `CardApiService`, gestión de cantidades (máx. 4), resumen con validación, y guardado vía `DeckApiService`.

### Modified Capabilities

*(none — funcionalidad nueva)*

## Impact

- Nuevos componentes en `FE/src/app/features/decks/`: `DeckListPage`, `DeckListComponent`, `DeckItemComponent`, `DeckValidationComponent`, `DeckBuilderPage`, `DeckSearchComponent`, `DeckCardListComponent`, `DeckSummaryComponent`.
- Consume servicios existentes: `DeckApiService`, `CardApiService`, `NotificationService`, `DeckBuilderFacadeService`.
- UI construida exclusivamente con Tailwind CSS utilities.
- No modifica `features/cards/`, `features/lobby/`, `features/match/` ni `core/`.
