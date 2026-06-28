## Context

Persona C necesita las páginas FrontEnd de gestión de mazos dentro de `features/decks/`, construidas con Tailwind CSS. Los servicios `DeckApiService`, `CardApiService`, `NotificationService` y `DeckBuilderFacadeService` ya existen. Las páginas actuales son stubs sin funcionalidad. Este cambio implementa `DeckListPage` (FE-03) y `DeckBuilderPage` (FE-04).

## Goals / Non-Goals

**Goals:**
- Implementar `DeckListPage` con carga por `playerId`, lista de mazos, badges de validación y acciones CRUD.
- Implementar `DeckBuilderPage` con creación/edición, búsqueda de cartas, gestión de cantidades y guardado.
- Toda la UI con clases Tailwind CSS, sin CSS global ni SCSS.
- Código dentro de `FE/src/app/features/decks/`, consumiendo servicios existentes.

**Non-Goals:**
- No modificar backend, `features/cards/`, `features/lobby/`, `features/match/` ni `core/`.
- No modificar servicios existentes (`DeckApiService`, `CardApiService`, etc.).
- No introducir CSS global, SCSS, ni estilos inline no-Tailwind.
- No implementar autenticación, JWT, reglas nuevas de juego, sincronización de catálogo.

## Decisions

1. **Tailwind CSS v4 para todos los estilos**: Siguiendo la configuración existente del proyecto (`@tailwindcss/postcss`). Sin CSSModules, SCSS ni estilos inline.
2. **Componentes standalone (Angular 21+)**: Sin NgModules, alineado con el resto del proyecto.
3. **DeckBuilderFacadeService como state holder**: Los componentes hijos se comunican vía `@Input()`/`@Output()`.
4. **Routing lazy-loaded**: Ya configurado — rutas `/decks`, `/decks/new`, `/decks/:id/edit` existen.
5. **Two-panel layout con grid de Tailwind**: `lg:grid-cols-2` en desktop, `grid-cols-1` en mobile (breakpoint 600px).
6. **CardViewComponent reutilizado**: El `CardViewComponent` de shared se usa en `DeckSearchComponent` para mostrar resultados.
7. **Confirmación inline para eliminar**: Estado local `confirmingDelete` en `DeckItemComponent`, sin modal.
8. **updateDeck en el facade o directo**: `DeckBuilderFacadeService` no tiene `updateDeck` — se llama a `DeckApiService.update()` directamente desde la page para modo edición.

## Risks / Trade-offs

- **Dependencia de CardViewComponent**: Si cambia la interfaz de Persona B, puede requerir ajustes. Mitigación: usar `CardSummaryResponse` que es un modelo compartido estable.
- **DeckBuilderFacadeService sin updateDeck**: La page llama a `DeckApiService.update()` directamente, desviándose ligeramente del patrón facade. Aceptable para MVP.
- **Sin estado persistente en el builder**: Recargar la página pierde cambios. Aceptable para MVP.
