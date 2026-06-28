# Tasks — Persona A: Card Catalog UI

> All frontend tasks are in `FE/src/app/` unless noted otherwise.
> Backend tasks are in `BE/src/main/java/ar/edu/utn/frc/tup/piii/`.

---

## 1. CardCatalogFacadeService

- [x] 1.1 Create `features/cards/services/card-catalog-facade.service.ts`
  - **Done when:** file exists with `@Injectable({ providedIn: 'root' })` and `CardApiService` injection
- [x] 1.2 Implement private signals `_query`, `_supertype`, `_page`, `_pageSize`, `_cards`, `_totalItems`, `_loading`, `_error`
  - **Done when:** all 8 signals defined with proper types
- [x] 1.3 Expose readonly signals: `query`, `supertype`, `page`, `pageSize`, `cards`, `totalItems`, `loading`, `error`
  - **Done when:** each has an `asReadonly()` public getter
- [x] 1.4 Implement `totalPages` computed signal (`Math.ceil(totalItems / pageSize)`, min 0)
  - **Done when:** `totalPages()` returns correct value for 0, 1, and multi-page scenarios
- [x] 1.5 Implement `setQuery(query: string)` — updates `_query`, resets `_page` to 0, calls `search()`
  - **Done when:** search triggers with new query from page 0
- [x] 1.6 Implement `setSupertype(supertype: string)` — updates `_supertype`, resets `_page` to 0, calls `search()`
  - **Done when:** search triggers with new supertype from page 0
- [x] 1.7 Implement `setPage(page: number)` — updates `_page`, calls `search()`
  - **Done when:** search triggers with new page index
- [x] 1.8 Implement `search()` — set loading, clear error, call `cardApi.searchCards()` with current state, handle next/error
  - **Done when:** successful response populates `_cards` + `_totalItems`, error sets `_error` message

## 2. SearchBarComponent

- [x] 2.1 Create `shared/components/search-bar/search-bar.component.ts`
  - **Done when:** file exists with `@Component({ selector: 'app-search-bar', changeDetection: OnPush })`
- [x] 2.2 Implement `placeholder` input and `queryChange` output
  - **Done when:** both declared with `input()` and `output()` functions
- [x] 2.3 Implement private `Subject<string>` piped through `debounceTime(300)` + `distinctUntilChanged()` + `takeUntilDestroyed`
  - **Done when:** 300ms debounce confirmed by logging timestamps
- [x] 2.4 Subscribe to debounced subject and emit via `queryChange`
  - **Done when:** `queryChange.emit()` fires 300ms after last keystroke
- [x] 2.5 Implement `onInput(event)` — update `value` signal, push to subject
  - **Done when:** each keystroke updates signal and pushes to subject
- [x] 2.6 Implement `clear()` — reset `value` signal, push empty string to subject
  - **Done when:** clear button visible when `value()` is truthy, triggers empty search

## 3. CardFilterComponent

- [x] 3.1 Create `shared/components/card-filter/card-filter.component.ts` with `FilterOption` interface
  - **Done when:** file exists with typed `options` and `selected` inputs
- [x] 3.2 Implement `onChange(value)` — emit `filterChange`
  - **Done when:** selecting an option emits the new value

## 4. PaginationComponent

- [x] 4.1 Create `shared/components/pagination/pagination.component.ts`
  - **Done when:** file exists with `currentPage` and `totalPages` required inputs
- [x] 4.2 Implement `visiblePages` computed — window of 5 pages centered on current
  - **Done when:** page 0 → [0,1,2,3,4], page 5 of 10 → [3,4,5,6,7], page 9 of 10 → [5,6,7,8,9]
- [x] 4.3 Implement `goToPage(page)` with bounds checking
  - **Done when:** emits `pageChange` only for valid, different page
- [x] 4.4 Render Anterior button (disabled at page 0), page buttons, Siguiente button (disabled at last page)
  - **Done when:** pagination nav renders only when `totalPages > 1`

## 5. CardViewComponent

- [x] 5.1 Create `shared/components/card-view/card-view.component.ts`
  - **Done when:** file exists with `card` required input typed to `CardSummaryResponse`
- [x] 5.2 Render card image (small) with `CardImagePipe`, name, supertype, setCode
  - **Done when:** all fields visible in compact card layout
- [x] 5.3 Implement `onImageError` fallback — hide broken img, show text placeholder
  - **Done when:** failed image shows card name centered instead

## 6. CardCatalogPage

- [x] 6.1 Update `features/cards/pages/card-catalog-page/card-catalog-page.ts`
  - **Done when:** injects `CardCatalogFacadeService`, imports all shared components
- [x] 6.2 Integrate SearchBarComponent with `(queryChange)="facade.setQuery($event)"`
  - **Done when:** typing in search triggers facade search with debounce
- [x] 6.3 Integrate CardFilterComponent with filter options (Todos, Pokemon, Energía, Entrenador)
  - **Done when:** selecting a supertype triggers facade filter
- [x] 6.4 Implement loading state: `<app-loading-spinner />`
  - **Done when:** spinner visible during API call
- [x] 6.5 Implement error state: error message + Retry button calling `facade.search()`
  - **Done when:** error message and retry button appear on failure
- [x] 6.6 Implement empty state: "No se encontraron cartas"
  - **Done when:** empty message shows when 0 cards and not loading
- [x] 6.7 Render card grid with `<app-card-view>` inside responsive grid (2–6 columns)
  - **Done when:** grid adapts from 2 to 6 columns based on breakpoint
- [x] 6.8 Integrate PaginationComponent below grid
  - **Done when:** pagination shows current/total pages and page changes trigger `facade.setPage()`

## 7. CardDetailPage

- [x] 7.1 Add `:id` route to `features/cards/routes.ts` with lazy loading
  - **Done when:** `import('./pages/card-detail-page/card-detail-page').then(m => m.CardDetailPage)`
- [x] 7.2 Create `features/cards/pages/card-detail-page/card-detail-page.ts`
  - **Done when:** file exists, injects `ActivatedRoute` and `CardRepositoryService`
- [x] 7.3 Implement `loadCard()` — read `id` from route params, call `cardRepository.resolve()`, handle success/error
  - **Done when:** card loads on init; error state shows message + retry button
- [x] 7.4 Render loading spinner while loading, error message + retry on failure, not-found message for null card
  - **Done when:** all three states render correctly
- [x] 7.5 Render `<app-pokemon-card>` with full card data when loaded
  - **Done when:** all card fields display in PokemonCardComponent

## 8. PokemonCardComponent

- [x] 8.1 Update `shared/components/pokemon-card/pokemon-card.component.ts`
  - **Done when:** component accepts `CardDetailResponse` as input
- [x] 8.2 Render large card image and header (name, HP, types)
  - **Done when:** image + name + HP + type icons display
- [x] 8.3 Render attacks with energy cost, damage, effect text
  - **Done when:** each attack shows cost icons, damage, and description
- [x] 8.4 Render weakness, resistance, retreat cost
  - **Done when:** weakness/resistance/retreat sections display with type and value
- [x] 8.5 Render evolution info (stage, evolvesFrom)
  - **Done when:** stage and evolution chain show when present
- [x] 8.6 Handle empty/null fields gracefully (hide section)
  - **Done when:** sections without data are not rendered

## 9. PostCSS Config Fix

- [x] 9.1 Create `FE/postcss.config.json` with Tailwind v4 plugin config
  - **Done when:** file exists with `{ "plugins": { "@tailwindcss/postcss": {} } }`
- [x] 9.2 Remove `FE/postcss.config.js` to avoid Angular 20 conflict
  - **Done when:** `.js` file deleted, Angular build succeeds

## 10. Backend: WebConfig CORS

- [x] 10.1 Create `BE/.../configs/WebConfig.java` implementing `WebMvcConfigurer`
  - **Done when:** CORS allows `http://localhost:4200` with GET, POST, PUT, DELETE, OPTIONS

## 11. Backend: Supertype Normalization in CardMapper

- [x] 11.1 Implement `normalizeSupertype(String supertype)` — `é` → `e`, `É` → `E`, uppercase
  - **Done when:** "Pokémon" → "POKEMON", "Energy" → "ENERGY"
- [x] 11.2 Call `normalizeSupertype()` in `toCardEntity()` when setting `entity.setSupertype()`
  - **Done when:** all cards persist with normalized uppercase supertype

## 12. Backend: Case-insensitive Supertype Filter

- [x] 12.1 Add `cb.upper(root.get("supertype"))` in `CardCatalogService.searchCards()` JPA Specification
  - **Done when:** supertype filter matches regardless of case in DB or request
