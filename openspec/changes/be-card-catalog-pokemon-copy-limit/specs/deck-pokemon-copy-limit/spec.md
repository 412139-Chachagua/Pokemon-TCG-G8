## ADDED Requirements

### Requirement: Limit Pokémon cards to four copies per canonical name
The system SHALL prevent adding more than 4 copies of the same Pokémon card to a deck, counting by canonical name per `05-deck-contract.md` name normalization rules.

#### Scenario: Add first copies
- **WHEN** the same Pokémon card is added fewer than 4 times
- **THEN** the system SHALL allow the addition

#### Scenario: Reach the limit
- **WHEN** the deck already has 4 copies of the same Pokémon card (same canonical name)
- **THEN** the system SHALL reject any further addition of that card with error code `MORE_THAN_4_COPIES`

#### Scenario: Same canonical name, different card IDs
- **WHEN** two Pokémon cards have different IDs but the same canonical name (e.g., "Gengar" and "Gengar Nv. 43")
- **THEN** the system SHALL count them together for the 4-copy limit

#### Scenario: Different canonical names
- **WHEN** two Pokémon cards have different canonical names
- **THEN** the system SHALL count them separately for the 4-copy limit

#### Scenario: Non-Pokémon cards unaffected
- **WHEN** adding Basic Energy or Trainer cards
- **THEN** the system SHALL NOT apply the 4-copy limit
