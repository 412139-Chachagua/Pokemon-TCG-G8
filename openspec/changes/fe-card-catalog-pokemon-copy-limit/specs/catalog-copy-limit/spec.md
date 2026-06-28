## ADDED Requirements

### Requirement: Limit Pokémon card copies from catalog

The system SHALL prevent adding more than 4 copies of the same Pokémon card to the deck via the card search (catalog).

#### Scenario: Add under limit
- **WHEN** the user clicks a Pokémon card in search results and the deck has fewer than 4 copies of that card
- **THEN** the system SHALL add the card to the deck

#### Scenario: Block at limit
- **WHEN** the user clicks a Pokémon card in search results and the deck already has 4 copies of that card
- **THEN** the system SHALL NOT add the card
- **AND** the system SHALL show a notification indicating the maximum has been reached

#### Scenario: Different cards unaffected
- **WHEN** the user clicks a Pokémon card not yet in the deck
- **THEN** the system SHALL add the card regardless of other cards' quantities

#### Scenario: Energy cards exempt from limit
- **WHEN** the user clicks a basic Energy card and the deck already has 4 or more copies of it
- **THEN** the system SHALL allow the addition (basic Energy cards have no copy limit)
