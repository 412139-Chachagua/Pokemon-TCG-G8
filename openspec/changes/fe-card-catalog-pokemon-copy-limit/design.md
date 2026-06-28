## Context

The deck builder's card search (`DeckSearchComponent`) lets users click any search result to add it to the deck via `facade.addCard()`. There is no limit check — clicking the same card 10 times adds 10 copies. The `+` button in `DeckCardListComponent` already caps at 4, so the inconsistency is only in the search-to-add path.

## Goals / Non-Goals

**Goals:**
- Block adding a Pokémon card from search results when the deck already has 4 copies
- Show user feedback when the add is blocked
- Keep the check in the page orchestration layer (not in the facade)

**Non-Goals:**
- No backend changes
- No canonical name grouping (that's the BE validator's domain — the BE `be-card-catalog-pokemon-copy-limit` handles cross-name limits)
- No changes to `DeckCardListComponent` or `DeckBuilderFacadeService` signatures

## Decisions

| Decision | Rationale |
|----------|-----------|
| Check limit in `onCardSelected()` not in `facade.addCard()` | The facade is a general-purpose state container. The UI feedback logic (notification + blocking) belongs in the page component. |
| Use existing `NotificationService` for feedback | Consistent with all other user-facing messages in the feature. |
| Check by `cardId` (not canonical name) | The frontend adds cards by unique cardId. Canonical name grouping is a backend validation concern. The BE already rejects decks that exceed 4 copies of the same canonical name. |

## Risks / Trade-offs

- **[Duplicate notifications]** If the backend also rejects the save for the same reason, the user might see two errors. → Mitigation: The frontend prevents the add entirely, so the save path should never trigger backend copy-limit errors for catalog additions.
