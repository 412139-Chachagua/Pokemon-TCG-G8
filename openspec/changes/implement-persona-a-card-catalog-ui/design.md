## Context

The frontend has existing base infrastructure (models, core API services, deck/match services) from the Persona A frontend foundation change. The card catalog page exists as a minimal placeholder. Shared components like SearchBar, CardFilter, Pagination, CardView, and PokemonCard were stubs or missing. Backend card search works but supertype filtering breaks on accented values (Pokémon) and case mismatch.

## Goals / Non-Goals

**Goals:**
- Implement signal-based `CardCatalogFacadeService` with search, filter, pagination orchestration
- Build reusable `SearchBarComponent` with debounce (300ms), distinct, clear button
- Build reusable `CardFilterComponent` accepting options/selected inputs
- Build reusable `PaginationComponent` with sliding 5-page window
- Build `CardViewComponent` for card thumbnail grid display
- Integrate `CardCatalogPage` with all states: loading spinner, error + retry, empty, card grid + pagination
- Add lazy-loaded `CardDetailPage` at route `/cards/:id`
- Build `PokemonCardComponent` with full card layout (image, HP, types, attacks, weakness, resistance, retreat, rules)
- Fix PostCSS config for Angular 20 (`.js` → `.json`)
- Fix backend supertype normalization on save (`normalizeSupertype` with accent replacement)
- Fix backend supertype filter with `UPPER()` for case-insensitive matching
- Add WebConfig CORS for `localhost:4200`

**Non-Goals:**
- No auth/JWT integration
- No deck builder integration (card selection/count)
- No match integration
- No advanced card search (types, HP range, set filters)
- No card comparison or favorites
- No mobile responsiveness beyond Tailwind responsive classes
- No animations or transitions beyond basic hover states

## Decisions

1. **State Management**: Signals (Angular 16+) over RxJS BehaviorSubject. Facade exposes readonly signals, encapsulating write signals privately. Derived state via `computed()`.

2. **Debounce Strategy**: `SearchBarComponent` owns a private `Subject<string>` piped through `debounceTime(300)` + `distinctUntilChanged()`. The component emits `queryChange` only after debounce settles. The facade then calls `search()`.

3. **Filter Reset**: Both `setQuery()` and `setSupertype()` reset page to 0 before searching — ensuring results always start from page 0 after any filter change.

4. **Pagination Window**: Sliding window of 5 visible pages centered on current page. Computed via `Math.max(0, current - 2)` to `Math.min(total, start + 5)`. This avoids massive page lists for large result sets.

5. **OnPush Change Detection**: All components use `ChangeDetectionStrategy.OnPush` — no mutations, only signal-driven input changes trigger re-renders.

6. **Image Fallback**: `CardViewComponent` handles image load errors by hiding the `<img>` tag and showing a centered text placeholder with the card name.

7. **Component Thinness**: Components only bind inputs, emit outputs, and provide minimal template logic. All state orchestration lives in `CardCatalogFacadeService`.

8. **Backend Consistency**: Supertype is normalized at write time (`é` → `e`, uppercase) in `CardMapper`, and queried via `UPPER()` in the JPA Specification so any read scenario (migrated data, mixed case) still matches correctly.
