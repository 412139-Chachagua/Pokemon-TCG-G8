# Card Model Contract

## Goal

Define the canonical internal card model.

The card cache stores official card data.

The game engine uses cached local data only.

The game engine must not call the external Pokémon API during a match.

## Backend location

```
cards/domain/
repositories/entities/
repositories/jpa/
engine/ports/CardLookupPort.java
```

## Frontend location

```
shared/models/card.models.ts
```

## CardDefinition

Canonical fields:
- id: string
- name: string
- supertype: String (no CardSupertype enum — el campo es String en la clase real)
- subtypes: string[]
- setCode: string
- number: string
- imageSmallUrl: string | null
- imageLargeUrl: string | null
- rulesText: string[]

## PokemonCardDefinition

Extends CardDefinition.
- hp: int
- stage: String (no PokemonStage enum — el campo es String en la clase real)
- evolvesFrom: String | null
- types: List<EnergyType> (String en el setter)
- attacks: List<AttackDefinition> (clase anidada dentro de PokemonCardDefinition)
- weaknesses: List<WeaknessDefinition> (clase anidada dentro de PokemonCardDefinition)
- resistances: List<ResistanceDefinition> (clase anidada dentro de PokemonCardDefinition)
- retreatCost: List<EnergyType>
- isEx: boolean
- isMega: boolean
- abilities: List<AbilityDefinition>

### AbilityDefinition

Clase en `cards/domain/AbilityDefinition.java`:

- name: String
- text: String
- type: AbilityType

### AttackDefinition (anidada en PokemonCardDefinition)

- index: int
- name: String
- cost: List<EnergyType>
- damage: String
- text: String

### WeaknessDefinition (anidada en PokemonCardDefinition)

- type: String
- value: String

### ResistanceDefinition (anidada en PokemonCardDefinition)

- type: String
- value: String (el valor se lee de la carta, ej: "-20", "-40". NO hardcodeado.)

**NOTA:** El `DamageCalculator` actual hardcodea resistencia como -20. Debe refactorizarse para leer el valor real desde `ResistanceDefinition.value` en la carta.

## EnergyCardDefinition

Extends CardDefinition.
- energyCardType: EnergyCardType
- provides: List<EnergyType>

## TrainerCardDefinition

Extends CardDefinition.
- trainerSubtype: TrainerSubtype
- isAceSpec: boolean
- effectCode: String | null

## EnergyCost

No existe como clase separada en el código real. Los costos de energía se modelan como `List<EnergyType>` dentro de `AttackDefinition`.

## JPA persistence

El código migró de tablas separadas (`pokemon_cards`, `trainer_cards`, `energy_cards`) a una tabla unificada `cards` con `CardEntity`.

Las entidades viejas en `repositories/entities/api_card/` están **@Deprecated**:
- PokemonCardEntity, PokemonCardAttackEntity, PokemonCardWeaknessEntity, PokemonCardResistanceEntity
- TrainerCardEntity
- EnergyCardEntity

## CardLookupPort

`CardLookupPort.getCardById(String cardId)` retorna `CardDefinition` (clase base), no subtipos específicos. El caller debe hacer el cast al subtipo correspondiente.

## CardInstance (engine model)

The engine represents individual card copies via `CardInstance` in `engine/model/`:

```
instanceId: UUID              (unique copy identifier, generated per match)
cardDefinitionId: String      (references CardDefinition.id from catalogue)
```

Constraints:
- `CardDefinition` lives in `cards/domain/` and is persisted in the relational catalogue (DB).
- `CardInstance` lives in `engine/model/` and is serialized as part of `GameState` JSON.
- The engine never queries the catalogue during gameplay; it resolves card details via `CardLookupPort.getCardById()` during setup.
- In `GameState`, card instances are referenced by their `instanceId` within zone lists (deck, hand, prizes, discard, attachedEnergies).

## CardInstance JSON example

```json
{
  "instanceId": "a1b2c3d4-...",
  "cardDefinitionId": "xy1-10"
}
```

## Card API format

```json
{
  "id": "xy1-10",
  "name": "Slugma",
  "supertype": "POKEMON",
  "subtypes": ["Stage 1"],
  "setCode": "xy1",
  "number": "10",
  "imageSmallUrl": "https://images.pokemontcg.io/xy1/10.png",
  "imageLargeUrl": "https://images.pokemontcg.io/xy1/10_hires.png",
  "rulesText": [],
  "hp": 80,
  "stage": "STAGE_1",
  "evolvesFrom": "magcargo",
  "types": ["FIRE"],
  "attacks": [
    {
      "index": 0,
      "name": "Rock Throw",
      "cost": ["COLORLESS", "COLORLESS"],
      "damage": "30",
      "text": ""
    }
  ],
  "weaknesses": [
    {
      "type": "WATER",
      "value": "x2"
    }
  ],
  "resistances": [],
  "retreatCost": ["COLORLESS", "COLORLESS"],
  "isEx": false,
  "isMega": false
}
```

## Frontend Card model

```typescript
interface CardModel {
  id: string;
  name: string;
  supertype: CardSupertype;
  subtypes: string[];
  setCode: string;
  number: string;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  rulesText: string[];
  hp?: number;
  stage?: PokemonStage;
  evolvesFrom?: string;
  types?: EnergyType[];
  attacks?: AttackModel[];
  weaknesses?: WeaknessModel[];
  resistances?: ResistanceModel[];
  retreatCost?: EnergyType[];
  isEx?: boolean;
}
```
