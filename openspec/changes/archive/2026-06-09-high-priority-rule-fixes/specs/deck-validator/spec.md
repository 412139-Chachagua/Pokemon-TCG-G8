## MODIFIED Requirements

### Requirement: DeckValidator validates Special Energy by name

`DeckValidator` SHALL validate that Special Energy cards (`EnergyCardType.SPECIAL`) do not exceed 4 copies total by card name, in addition to the existing per-cardId check.

#### Scenario: 4 copies of same Special Energy by name is allowed
- **WHEN** a deck contains 4 copies of the same Special Energy card (same name, possibly different IDs)
- **THEN** `DeckValidator` SHALL NOT report `MORE_THAN_4_COPIES`

#### Scenario: 5 copies of same Special Energy by name is rejected
- **WHEN** a deck contains 5 copies of the same Special Energy card (same name)
- **THEN** `DeckValidator` SHALL report `MORE_THAN_4_COPIES`

#### Scenario: Basic Energy is not affected by name check
- **WHEN** a deck contains more than 4 copies of a Basic Energy card
- **THEN** the name-based validation SHALL NOT apply
- **THEN** existing per-cardId validation continues to apply

### Requirement: DeckValidator validates Ace Spec limit

`DeckValidator` SHALL validate that a deck contains at most 1 card with `TrainerCardDefinition.isAceSpec() == true` (considering quantities).

#### Scenario: 1 Ace Spec card is allowed
- **WHEN** a deck contains exactly 1 Ace Spec card (any quantity)
- **THEN** `DeckValidator` SHALL NOT report `ACE_SPEC_LIMIT_EXCEEDED`

#### Scenario: 2 Ace Spec cards are rejected
- **WHEN** a deck contains 2 or more Ace Spec cards (considering quantities)
- **THEN** `DeckValidator` SHALL report `ACE_SPEC_LIMIT_EXCEEDED`

#### Scenario: 0 Ace Spec cards is allowed
- **WHEN** a deck contains no Ace Spec cards
- **THEN** `DeckValidator` SHALL NOT report `ACE_SPEC_LIMIT_EXCEEDED`
