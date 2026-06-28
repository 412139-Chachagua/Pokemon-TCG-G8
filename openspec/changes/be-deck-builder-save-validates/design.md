## Context

The deck save operations (`createDeck`, `updateDeck`) currently call `DeckValidator.validate()` but do not block persistence when validation fails — they set `valid=false` and save anyway. The `SeedDeckService` also calls `createDeck` for seed decks. The requirement is to block persistence entirely when validation fails, reusing the same validation flow.

Affected contracts: `docs/contracts_ai/05-deck-contract.md`, `docs/contracts_ai/13-rest-api-contract.md`.

## Goals / Non-Goals

**Goals:**
- Block persistence when deck validation fails on create and update
- Return validation errors in save response
- Seed decks must also pass validation before persisting

**Non-Goals:**
- No new validation rules
- No UI changes
- No changes to the Game Engine

## Decisions

1. **Throw on validation failure** – When `DeckValidator.validate()` returns errors, throw a `ValidationException` containing the `DeckValidationResponse` errors, rather than persisting with `valid=false`. The `GlobalExceptionHandler` maps this to the appropriate HTTP response.
2. **Seed decks reuse the same flow** – `SeedDeckService` calls `deckService.createDeck()` which will now throw on invalid decks, naturally blocking seed creation without separate validation logic.
3. **Entity `valid` flag** – Remove the `setValid(false)` path; only valid decks are persisted (always `valid=true`).

## Risks / Trade-offs

- [Risk] Seed decks might silently fail if card data changes → Mitigation: seed deck data is static and pre-validated in the spec
- [Risk] Existing invalid decks in DB become uneditable → Mitigation: the frontend must ensure decks are valid before save
