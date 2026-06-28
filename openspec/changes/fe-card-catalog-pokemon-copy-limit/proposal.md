## Why

The deck builder's card search (catálogo) allows adding unlimited copies of the same Pokémon card, bypassing the 4-copy rule already enforced in the deck card list `+` button. Users can click a search result card multiple times and exceed the limit without feedback.

## What Changes

- `DeckBuilderFacadeService.addCard()` or `DeckBuilderPage.onCardSelected()` checks the current quantity before adding
- If the deck already has 4 copies of that card, the addition is blocked and the user is notified
- The existing `+` button in `DeckCardListComponent` already caps at 4 — no change needed there

## Capabilities

### New Capabilities
- `catalog-copy-limit`: Prevents adding more than 4 copies of the same Pokémon card from the catalog search, with user feedback

### Modified Capabilities
- *(none — purely additive frontend behavior)*

## Impact

- `FE/src/app/features/decks/pages/deck-builder-page/deck-builder-page.ts` — `onCardSelected()` checks limit before calling `facade.addCard()`
- `FE/src/app/features/decks/services/deck-builder-facade.service.ts` — `addCard()` may return or signal when limit reached (optional, depending on approach)
- No backend changes
- No new API endpoints
