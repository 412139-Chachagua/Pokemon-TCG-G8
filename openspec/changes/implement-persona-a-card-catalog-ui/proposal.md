## Why

The card catalog is the frontend entry point for the Pokémon TCG application. Without search, filter, pagination, and card detail views, users cannot browse cards — blocking downstream features (deck builder, match setup) that depend on card selection.

## What Changes

- Create `CardCatalogFacadeService` with signal-based state management (query, supertype, page, pageSize, cards, totalItems, loading, error, totalPages computed)
- Create `SearchBarComponent` with 300ms debounce input and clear button
- Create `CardFilterComponent` with dropdown for supertype filtering (Todos/Pokemon/Energía/Entrenador)
- Create `PaginationComponent` with sliding window of 5 pages + anterior/siguiente buttons
- Create `CardViewComponent` with card thumbnail, name, supertype, setCode, and image fallback
- Integrate `CardCatalogPage` binding search-bar + filter + pagination + loading/empty/error states
- Add `/cards/:id` route with lazy-loaded `CardDetailPage`
- Create `PokemonCardComponent` with full card layout (image, HP, types, attacks, weakness, resistance, retreat, rules)
- Rename `postcss.config.js` → `postcss.config.json` for Angular 20 compatibility
- Fix backend `CardMapper.normalizeSupertype()` to normalize accents on save (Pokémon → POKEMON)
- Fix backend `CardCatalogService` supertype filter to use `cb.upper()` for case-insensitive matching
- Create `WebConfig` CORS configuration allowing `localhost:4200`

## Capabilities

### New Capabilities

- `card-search`: Debounced search by card name with 300ms debounce via RxJS Subject
- `card-filter`: Supertype dropdown filter with reset-to-page-0 on change
- `card-pagination`: Sliding page window (5 pages visible) with computed totalPages
- `card-detail`: Full card detail page with all fields (HP, attacks, weakness, etc.)
- `cors-config`: Spring Web CORS configuration for frontend dev server

### Modified Capabilities

- `card-catalog-facade`: Central signals + search orchestration (was bare/minimal)
- `card-summary-grid`: CardViewComponent + CardCatalogPage template (was inline placeholder)
- `postcss-config`: .js → .json for Angular 20 PostCSS compatibility
- `supertype-normalization`: `normalizeSupertype()` in CardMapper (é→e, uppercase)
- `supertype-query`: Case-insensitive filter via `UPPER()` in JPA Specification

## Impact

- Affected FE packages `features/cards/`, `shared/components/`, `core/services/`
- Affected BE packages `mappers/cards/`, `services/cards/`, `configs/`
- No impact on: deck module, match module, lobby, game engine
- Search debounce prevents excessive API calls on fast typing
- Supertype normalization ensures Pokémon/Pokemon/POKEMON all match correctly
- Angular 20 PostCSS config is forward-compatible

## Mandatory Context Files

- `/docs/contracts_ai/00-contract-index.md`
- `/docs/contracts_ai/02-project-structure-contract.md`
- `/docs/contracts_ai/04-card-catalog-contract.md`
- `/docs/contracts_ai/15-frontend-state-contract.md`
- `/docs/contracts_ai/16-test-scenarios-contract.md`
- `/openspec/specs/card-catalog-person-1.md`
