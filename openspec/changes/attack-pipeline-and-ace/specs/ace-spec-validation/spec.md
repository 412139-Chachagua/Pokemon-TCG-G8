## ADDED Requirements

### Requirement: DeckValidator enforces AS TÁCTICO limit

`DeckValidator.validate()` SHALL check that no more than 1 AS TÁCTICO card is present in a deck.

- SHALL iterate all `DeckCard` entries in the deck
- SHALL use `cardLookupPort.getCardById()` to resolve each `DeckCard.cardId` to its `CardDefinition`
- SHALL check `def instanceof TrainerCardDefinition trainer && trainer.isAceSpec()`
- SHALL sum the `DeckCard.quantity` for all matching cards
- SHALL add `DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED` if the total exceeds 1
- SHALL perform this check after deck size validation and before the 4-copies check

#### Scenario: Single AS TÁCTICO is valid

- **WHEN** a deck contains exactly 1 card with `isAceSpec() = true` (with quantity 1)
- **THEN** no `ACE_SPEC_LIMIT_EXCEEDED` error SHALL be added

#### Scenario: Two different AS TÁCTICO cards is invalid

- **WHEN** a deck contains 2 different cards both with `isAceSpec() = true` (each with quantity 1)
- **THEN** `ACE_SPEC_LIMIT_EXCEEDED` SHALL be added to errors

#### Scenario: Multiple copies of same AS TÁCTICO is invalid

- **WHEN** a deck contains 1 card with `isAceSpec() = true` with quantity 2
- **THEN** `ACE_SPEC_LIMIT_EXCEEDED` SHALL be added to errors
