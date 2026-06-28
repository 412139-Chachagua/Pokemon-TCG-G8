## Why

El backend carece de un endpoint que genere mazos aleatorios válidos. Los jugadores necesitan poder obtener un mazo jugable (que respete reglas de construcción: 60 cartas, límite de copias, Pokémon básico, etc.) para probar el juego sin tener que construirlo manualmente. Esto es necesario para las pruebas y para ofrecer una experiencia de onboarding rápida.

## What Changes

- Agregar endpoint `POST /api/decks/random` que devuelva un mazo aleatorio válido
- Implementar algoritmo de generación aleatoria que:
  - Seleccione cartas del set xy1
  - Respete límite de 4 copias por nombre canónico
  - Exija exactamente 60 cartas
  - Incluya al menos 1 Pokémon Básico
  - Respete máximo 1 ACE_SPEC
  - Evite combinaciones imposibles o injugables
- Re-usar `DeckValidator` existente para validar el mazo generado
- Si la generación falla, reintentar N veces antes de responder error
- Si no hay suficientes cartas compatibles, responder con error 422 indicando que no se pudo generar

## Capabilities

### New Capabilities
- `be-deck-builder-random-valid-decks`: Generación aleatoria de mazos válidos desde el backend

### Modified Capabilities

_(None)_

## Impact

- **Backend**: Nuevo servicio `RandomDeckService` en `services/decks/`, nuevo endpoint en `DeckController`, tests unitarios y de integración
- **Endpoints nuevos**: `POST /api/decks/random`
- **Dependencias**: `DeckValidator`, `CardRepository`, set xy1 completo sincronizado
- **No afecta**: UI, reglas de validación existentes, engine de juego
