## Why

The "Guardar" button in the deck builder UI currently lacks client-side validation before sending the save request. The user may attempt to save an invalid deck without immediate feedback. The "Validar" button already shows validation errors — the "Guardar" button should reuse that same validation flow to prevent persistence of invalid decks and provide consistent UX.

## What Changes

- Frontend "Guardar" button triggers the same validation that "Validar" uses before calling the save API
- If validation fails, the save request is not sent and validation errors are displayed using the existing validation feedback UI
- If validation passes, the save request proceeds and the deck is persisted
- The backend save endpoint may reject invalid decks (see `be-deck-builder-save-validates`) — the frontend handles those errors gracefully

## Capabilities

### New Capabilities
- `deck-save-button-validates`: Frontend runs full deck validation before saving, reusing the same validation feedback as the "Validar" action

### Modified Capabilities
- *(none — purely additive frontend behavior)*

## Impact

- Frontend: Deck builder component — wire "Guardar" click to call validation before the save API
- Frontend: Show validation errors in the existing feedback UI when save is blocked
- No backend changes
- No new API endpoints
