# Deck Contract

## Goal

Define deck structure, validation and seed deck behavior.

## Backend location

```
decks/domain/
dtos/decks/
services/decks/DeckService.java
services/decks/DeckValidator.java
services/decks/SeedDeckService.java
engine/ports/DeckLoadPort.java
engine/ports/impl/DeckLoadAdapter.java
```

## Frontend location

```
features/decks/
shared/models/deck.models.ts
```

## Deck rules

A valid deck must have:
- exactly 60 cards
- maximum 4 cards with same card name (ver reglas de nombre abajo)
- Basic Energy cards are exempt from the 4-copy limit
- Special Energy cards: maximum 4 copies with the same name
- maximum 1 ACE_SPEC card total
- at least 1 Basic Pokémon

### Reglas de nombre de Pokémon (para validación de copias)

Qué **forma parte** del nombre canónico:
- **SÍ**: Símbolos al final del nombre (⭐, ☆, etc.). Ej: "Alakazam" ≠ "Alakazam ☆"
- **SÍ**: Nombre del dueño. Ej: "Geodude" ≠ "Geodude de Brock"
- **SÍ**: Sufijo "-EX". Ej: "Kyurem Negro" ≠ "Kyurem Negro-EX"
- **SÍ**: Prefijo "Mega-". Ej: "Venusaur-EX" ≠ "Mega-Venusaur-EX"
- **NO**: El nivel (Nv.). Ej: "Gengar", "Gengar Nv. 43" y "Gengar Nv. X" tienen el mismo nombre
- **NO**: "Equipo Plasma". Ej: "Liepard" y "Liepard del Equipo Plasma" tienen el mismo nombre
- **NO**: La especie Delta (δ). Ej: "Gyarados" y "Gyarados δ" tienen el mismo nombre

La validación de máximo 4 copias se aplica sobre el nombre canónico según estas reglas.

Deck Builder must use set xy1 for the required base version.

## DeckResponse

```json
{
  "id": "deck-fire-seed",
  "name": "Seed Fire Deck",
  "ownerPlayerId": null,
  "source": "SEED",
  "totalCards": 60,
  "valid": true,
  "cards": [
    {
      "cardId": "xy1-10",
      "name": "Slugma",
      "quantity": 4,
      "supertype": "POKEMON",
      "isBasicEnergy": false
    },
    {
      "cardId": "energy-fire-basic",
      "name": "Fire Energy",
      "quantity": 18,
      "supertype": "ENERGY",
      "isBasicEnergy": true
    }
  ],
  "validation": {
    "valid": true,
    "errors": []
  }
}
```

## ValidateDeckRequest

```json
{
  "cards": [
    { "cardId": "xy1-10", "quantity": 4 },
    { "cardId": "energy-fire-basic", "quantity": 18 }
  ]
}
```

## DeckValidationResponse

```json
{
  "valid": false,
  "errors": [
    {
      "code": "DECK_SIZE_INVALID",
      "message": "El mazo debe tener exactamente 60 cartas.",
      "details": {
        "currentSize": 55,
        "requiredSize": 60
      }
    }
  ]
}
```

## DeckValidator

Implementación real en `services/decks/DeckValidator.java`. Reglas de validación:
- Validación de 60 cartas exactas
- Máximo 4 copias por nombre de carta (exento Energías Básicas)
- Energías Especiales (SPECIAL): máximo 4 copias con el mismo nombre
- Mínimo 1 Basic Pokémon
- ACE_SPEC máximo 1

## DeckLoadPort (engine port)

```java
package ar.edu.utn.frc.tup.piii.engine.ports;

public interface DeckLoadPort {
  Deck loadDeck(UUID deckId);
}
```

- Created by Persona 4 (engine) as an interface stub.
- Implemented by Persona B (catálogo) in `engine/ports/impl/DeckLoadAdapter.java`.
- `loadDeck` must validate the deck via `DeckValidator` before returning.
- The engine never calls `DeckValidator` directly; validation is guaranteed by the adapter.

## Seed deck types

The MVP requires at least two seed decks:
- Fire-type seed deck
- Water-type seed deck

Each seed deck must be pre-validated and include:
- Basic Pokémon (at least 1)
- Stage 1 Pokémon (optional)
- Energy cards
- Trainer cards (optional for MVP)

## Deck entity fields

```
id: UUID
name: string
ownerPlayerId: UUID | null
source: DECK_SOURCE (SEED | CUSTOM)
createdAt: Instant
updatedAt: Instant
```

## Deck card entity fields

```
id: UUID
deckId: UUID
cardId: string
quantity: int
```
