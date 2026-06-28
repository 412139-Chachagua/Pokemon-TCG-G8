# Design: fe-deck-builder-save-validates

## Goal

Wire the "Guardar" button in `DeckBuilderPage` to run the same validation as "Validar" before calling the create/update API.

## Approach

Modify `DeckBuilderPage.onSave()` to:

1. **Validate first**: Use the same validation logic as `onValidate()` — `deckApi.validate(id)` for edit mode, `facade.validate()` for create mode
2. **Block on failure**: If validation returns `valid === false`, set `validationResult` (which `DeckSummaryComponent` already renders) and do NOT send the save request
3. **Proceed on success**: If validation passes, call `deckApi.create()` or `deckApi.update()` as before
4. **Show feedback**: Validation errors appear in the existing `DeckSummaryComponent` error list; a warning notification is shown when save is blocked

## Files Changed

- `FE/src/app/features/decks/pages/deck-builder-page/deck-builder-page.ts` — only `onSave()` method

## Not Changed

- No backend changes
- No new API calls
- No new components
- No model changes
- No service changes
