## 1. Setup and Routing

- [x] 1.1 Verify lazy-loaded routes for `/decks`, `/decks/new`, `/decks/:id/edit` in `features/decks/routes.ts`
- [x] 1.2 Verify routes are registered in `app.routes.ts`

## 2. Deck List Page (FE-03)

- [x] 2.1 Create `DeckValidationComponent` with Tailwind badges: green "✅ Válido" (`bg-green-100 text-green-800`) and red "❌ Inválido" (`bg-red-100 text-red-800`)
- [x] 2.2 Create `DeckItemComponent` with Tailwind card layout showing name, totalCards, source, validation badge, and action buttons (Eliminar, Validar, Jugar)
- [x] 2.3 Implement inline delete confirmation with Cancelar/Eliminar ⚠️ in `DeckItemComponent`
- [x] 2.4 Create `DeckListComponent` with Tailwind styles for loading spinner, error state ("Reintentar" button), and empty state ("No hay mazos disponibles.")
- [x] 2.5 Rewrite `DeckListPage` with `playerId` input field and "Cargar mazos" button using Tailwind classes
- [x] 2.6 Implement auto-load on `playerId` field change with 300ms debounce
- [x] 2.7 Handle empty `playerId` — use `player-dev` as default in development
- [x] 2.8 Handle empty result — show "No hay mazos disponibles."
- [x] 2.9 Handle load error — show error message with "Reintentar" action
- [x] 2.10 Implement delete flow — call `DeckApiService.delete(id)`, remove from list, show snackbar via `NotificationService`
- [x] 2.11 Implement validate flow — call `DeckApiService.validate(id)`, update `deck.validation`, show snackbar
- [x] 2.12 Implement play action — navigate to `/lobby?deckId={id}`, hide when deck is invalid
- [x] 2.13 Add "[+ Nuevo mazo]" button navigating to `/decks/new`
- [x] 2.14 Add edit action on deck item navigating to `/decks/{id}/edit`

## 3. Deck Builder Page (FE-04)

- [x] 3.1 Create `DeckSearchComponent` with debounced search field and Tailwind form styling, using `CardViewComponent` for results
- [x] 3.2 Implement card selection — emit `cardSelected` and call `DeckBuilderFacadeService.addCard(cardId, name, supertype)`
- [x] 3.3 Create `DeckCardListComponent` with +/- quantity buttons using Tailwind, disable `[+]` at max 4, remove card at 0
- [x] 3.4 Create `DeckSummaryComponent` with Tailwind card showing total cards and validation states ("Aún no validado.", "✅ Listo para jugar.", validation errors)
- [x] 3.5 Rewrite `DeckBuilderPage` supporting create mode (empty deck) and edit mode (load via `DeckApiService.get(id)`, hydrate facade)
- [x] 3.6 Implement two-panel layout with Tailwind grid: `lg:grid-cols-2` desktop / `grid-cols-1` mobile (breakpoint 600px)
- [x] 3.7 Implement validate action — call `DeckApiService.validateCards({ cards })` for new decks or `DeckApiService.validate(id)` for existing
- [x] 3.8 Implement save action — call `DeckApiService.create(req)` for new or `DeckApiService.update(id, req)` for existing
- [x] 3.9 Disable save when deck is empty or name is blank
- [x] 3.10 Handle save error — show snackbar and stay on page
- [x] 3.11 Handle save success — navigate to `/decks`

## 4. Verification

- [x] 4.1 Run `ng build` and fix any compilation errors
- [x] 4.2 Run existing tests (`ng test --watch=false`) and ensure no regressions
- [ ] 4.3 Verify both pages render correctly in desktop and mobile viewports (manual check)
