# Tasks: fe-deck-builder-save-validates

## Task 1: Modify onSave() to validate before saving

Edit `FE/src/app/features/decks/pages/deck-builder-page/deck-builder-page.ts`:

- [x] In `onSave()`, before the create/update API call, run the same validation as `onValidate()`
- [x] If validation fails (`result.valid === false`), set `validationResult`, show warning notification, and do NOT save
- [x] If validation passes, proceed with the existing save logic
- [x] Disable the "Guardar" button while validation/save is in progress (reuse `saving` signal)

## Verification

- [x] Confirm that `onSave()` in create mode calls `facade.validate()` then conditionally `deckApi.create()`
- [x] Confirm that `onSave()` in edit mode calls `deckApi.validate(id)` then conditionally `deckApi.update()`
- [x] Confirm that failed validation blocks save and renders errors in `DeckSummaryComponent`
