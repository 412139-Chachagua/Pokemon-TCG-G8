## Why

The deck builder currently allows unlimited copies of the same Pokémon card, which violates the official TCG rule of maximum 4 copies per card (excluding basic Energy). This backend enforcement is needed to ensure deck validity before game start.

## What Changes

- Add backend validation rule limiting Pokémon cards to 4 copies per deck
- Return validation error when attempting to add a 5th copy of the same Pokémon card
- Validation applies at the deck-building API layer (add card to deck endpoint)
- Only affects Pokémon-type cards (Energy and Trainer cards are not affected by this limit)

## Capabilities

### New Capabilities
- `deck-pokemon-copy-limit`: Backend rule that enforces max 4 copies of each Pokémon card in a deck

### Modified Capabilities

- `card-catalog-management`: The existing deck-building capability must enforce the new copy limit rule when adding cards

## Impact

- Backend: `DeckService` or equivalent deck-building service needs new validation logic
- New rule in the rule validation engine for deck constraints
- REST API deck endpoints will return new error code when limit is exceeded
- Frontend: Will receive and display the new validation error (no visual changes to UI layout)
