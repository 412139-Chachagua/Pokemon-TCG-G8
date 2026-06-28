## Why

The deck save endpoints (create and update) may persist invalid decks if validation is not enforced before persistence. The deck builder must guarantee that every save runs the same validation as the explicit validate endpoint, blocking persistence on failure.

## What Changes

- Enforce `DeckValidator.validate()` before persisting in both create and update flows
- Block persistence and return validation errors when the deck is invalid
- Reuse existing `DeckValidationResponse` format for error responses on save
- Seed deck creation must also pass validation

## Capabilities

### New Capabilities
- `deck-save-validates`: Backend enforces full deck validation before any save operation (create, update, seed)

### Modified Capabilities

- *(none — no existing capability's requirements change)*

## Impact

- Backend: `DeckService.createDeck()` and `DeckService.updateDeck()` already call `DeckValidator` — verify consistency and ensure seed flow also validates
- Backend: `SeedDeckService` must validate before persisting
- REST API returns validation errors in save responses when deck is invalid
