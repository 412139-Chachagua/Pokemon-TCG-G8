## Context

Actualmente el backend permite validar mazos (`POST /api/decks/validate`, `POST /api/decks/{id}/validate`) pero no ofrece generación aleatoria. Los seed decks (Fuego, Agua) existen como datos precargados, pero no hay un algoritmo que genere combinaciones aleatorias válidas bajo demanda. La generación aleatoria debe reutilizar `DeckValidator` existente y trabaja exclusivamente con cartas del set xy1.

## Goals / Non-Goals

**Goals:**
- Nuevo endpoint `POST /api/decks/random` que devuelva `DeckResponse`
- Algoritmo que seleccione cartas de xy1 respetando todas las reglas de construcción
- Reintentar N veces (ej: 10) si el mazo generado no es válido
- Responder 422 si no se puede generar un mazo válido

**Non-Goals:**
- No modificar `DeckValidator`, `DeckService`, ni el engine
- No generar mazos para sets distintos a xy1
- No generar mazos de torneo ni metagame-aware
- No persistir el mazo generado (es temporal para el cliente)

## Decisions

1. **Nuevo servicio `RandomDeckService`** en `services/decks/`. Separado de `DeckService` y `SeedDeckService` para mantener responsabilidades únicas. Recibe `CardRepository` para consultar cartas xy1 y `DeckValidator` para validar el resultado.

2. **Estrategia de selección**: Dividir la selección en pasos:
   - Primero: elegir 1-2 Pokémon Básicos aleatorios
   - Segundo: elegir evoluciones compatibles (Stage 1, Stage 2) según los básicos seleccionados
   - Tercero: agregar Trainer/Supporter/Item aleatorios
   - Cuarto: completar con Energías Básicas hasta llegar a 60 cartas
   - Quinto: validar con `DeckValidator`. Si falla, reintentar

3. **Límite de reintentos**: 10 intentos como default. Si todos fallan, se responde `422` con mensaje "No se pudo generar un mazo válido".

4. **No persistence**: El mazo generado se devuelve en la respuesta pero no se persiste en BD. El cliente decide si guardarlo (usando `POST /api/decks`).

5. **Reuso de `DeckValidator`**: No se duplica lógica de validación. `RandomDeckService` llama a `DeckValidator.validate()`.

## Risks / Trade-offs

- **Rendimiento**: Si la card pool es pequeña o las restricciones muy estrictas, pueden necesitarse múltiples reintentos. → El límite de 10 evita loops infinitos.
- **Calidad del mazo**: El algoritmo prioriza validez sobre jugabilidad. Podría generar mazos débiles o sin sinergia. → Aceptable para MVP; mejora futura.
- **Set único (xy1)**: Limita la variedad de mazos posibles. → Aceptable por ahora.
