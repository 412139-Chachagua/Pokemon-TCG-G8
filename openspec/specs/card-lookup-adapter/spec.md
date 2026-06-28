## ADDED Requirements

### Requirement: CardLookupAdapter implements CardLookupPort
The system SHALL provide a CardLookupAdapter that implements CardLookupPort to serve card data to the game engine.

#### Scenario: Lookup existing card by ID
- WHEN the engine calls getCardById with a valid card ID
- THEN the adapter SHALL query CardJpaRepository for the entity
- THEN the adapter SHALL map CardEntity to the appropriate CardDefinition subclass
- THEN the adapter SHALL return the domain CardDefinition object

#### Scenario: Lookup non-existing card
- WHEN the engine calls getCardById with an invalid card ID
- THEN the adapter SHALL throw NotFoundException

#### Scenario: Cached lookup
- WHEN the engine calls getCardById with the same card ID twice
- THEN the second call SHALL be served from cache
- THEN no additional database query SHALL be made

### Requirement: Domain mapping by supertype
The adapter SHALL map CardEntity to the correct CardDefinition subclass based on supertype.

#### Scenario: Pokemon card domain mapping
- WHEN a CardEntity with supertype POKEMON is found
- THEN the adapter SHALL return a PokemonCardDefinition with hp, stage, evolvesFrom, types, attacks, weaknesses, resistances, retreatCost

#### Scenario: Trainer card domain mapping
- WHEN a CardEntity with supertype TRAINER is found
- THEN the adapter SHALL return a TrainerCardDefinition with trainerSubtype, isAceSpec

#### Scenario: Energy card domain mapping
- WHEN a CardEntity with supertype ENERGY is found
- THEN the adapter SHALL return an EnergyCardDefinition with energyCardType, provides
