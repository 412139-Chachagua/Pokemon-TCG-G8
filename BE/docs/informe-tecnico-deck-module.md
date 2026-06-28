com# Informe Técnico - Módulo de Decks (Persona B)

## Resumen

Implementación completa del módulo de gestión de mazos (decks) siguiendo arquitectura hexagonal,
contratos definidos en `/docs/contracts_ai/` y las tareas especificadas en
`/openspec/changes/implement-persona-b-deck-module/tasks.md`.

---

## Archivos creados

| Archivo | Propósito |
|---|---|
| `engine/ports/DeckLoadPort.java` | Puerto de entrada del engine para cargar decks |
| `engine/ports/impl/DeckLoadAdapter.java` | Adaptador que implementa `DeckLoadPort`, valida y mapea |
| `controllers/decks/DeckControllerTest.java` | Tests de integración del controller |
| `engine/ports/impl/DeckLoadAdapterTest.java` | Tests del adapter (deck válido, no encontrado, inválido) |
| `mappers/decks/DeckMapperTest.java` | Tests de todos los mappings |
| `services/decks/DeckServiceTest.java` | Tests CRUD + validación |
| `services/decks/DeckValidatorTest.java` | Tests de reglas de validación |
| `services/decks/SeedDeckServiceTest.java` | Tests de perfil dev |

## Archivos modificados

| Archivo | Cambio |
|---|---|
| `controllers/decks/DeckController.java` | Endpoints REST completos (POST, GET, PUT, DELETE, list, validate) |
| `decks/domain/DeckValidationResult.java` | Agregados constructor sin args, constructor con args, setters |
| `dtos/decks/CreateDeckRequest.java` | Agregado campo `playerId` |
| `mappers/decks/DeckMapper.java` | Implementación completa de todos los mappings |
| `repositories/jpa/DeckJpaRepository.java` | Agregado método `findByOwnerUserId` |
| `services/decks/DeckService.java` | Lógica CRUD completa con validación integrada |
| `services/decks/DeckValidator.java` | Reglas de validación + exención de Basic Energy |
| `services/decks/SeedDeckService.java` | Seed decks Fire y Water con 60 cartas c/u |
| `docs/api_doc/swagger.json` | Actualización automática por SpringDoc |

## Endpoints REST implementados

| Método | Ruta | Código HTTP | Descripción |
|---|---|---|---|
| POST | `/api/decks` | 201 Created | Crear mazo |
| GET | `/api/decks/{id}` | 200 OK | Obtener mazo por ID |
| PUT | `/api/decks/{id}` | 200 OK | Actualizar mazo |
| DELETE | `/api/decks/{id}` | 204 No Content | Eliminar mazo |
| GET | `/api/decks?playerId={id}` | 200 OK | Listar mazos por jugador |
| POST | `/api/decks/{id}/validate` | 200 OK | Validar mazo existente |

Los errores 404 (not found) y 400 (validation) son manejados por `GlobalExceptionHandler`.

## Reglas de validación (`DeckValidator`)

1. **DECK_SIZE_INVALID** — El mazo debe tener exactamente 60 cartas.
2. **MORE_THAN_4_COPIES** — Máximo 4 copias por carta (exentas: Basic Energy).
3. **MISSING_BASIC_POKEMON** — Al menos 1 Pokémon Básico.

## Arquitectura

```
[DeckController] → [DeckService] → [DeckJpaRepository]
                      ↕
               [DeckValidator] → [CardLookupPort]
                      ↕
                [DeckMapper] → [CardLookupPort]

[DeckLoadAdapter] → [DeckJpaRepository]
                 → [DeckValidator]
                 → [DeckMapper]
                 → implements [DeckLoadPort] (usado por Game Engine)
```

## Seed Decks

- **Seed Fire Deck**: 60 cartas (4 Pokémon básicos fuego, 2 Stage 1, 18 Energy fuego)
- **Seed Water Deck**: 60 cartas (4 Pokémon básicos agua, 2 Stage 1, 18 Energy agua)
- Ejecutado solo con perfil `@Profile("dev")`
- Idempotente: skip si ya existen mazos en DB

## Problemas encontrados y corregidos

1. **Seed deck Water con 58 cartas** — Faltaban 2 para llegar a 60. Agregado `xy1-28` con cantidad 2.
2. **Validator sin exención de Basic Energy** — El contrato requiere que las Basic Energy estén exentas del límite de 4 copias. Agregado check con `instanceof EnergyCardDefinition`.
3. **DeckMapperTest usaba string no-UUID** — `"player-1"` no es UUID válido. Cambiado a `UUID.randomUUID()`.
4. **UnnecessaryStubbings en 3 tests** — Stubs de `toDomainCard` que nunca se ejecutaban (entity sin cartas).

## Resultado de tests

```
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Pruebas desde Swagger UI

### Prerrequisito

Levantar la aplicación con perfil `dev` para que los seed decks se carguen automáticamente:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Swagger UI disponible en: `http://localhost:8080/swagger-ui.html`

### 1. Listar todos los mazos (seed data)

```
GET /api/decks?playerId=00000000-0000-0000-0000-000000000000
```

Response esperado: `200 OK` con un array vacío `[]` (los seed decks tienen `ownerPlayerId = null`, por lo que el filtro no los trae). Para ver los seed decks se puede usar el siguiente paso.

### 2. Crear un mazo válido

```
POST /api/decks
Content-Type: application/json

{
  "name": "Mazo de Prueba",
  "playerId": "00000000-0000-0000-0000-000000000001",
  "cards": [
    { "cardId": "xy1-1", "quantity": 4 },
    { "cardId": "xy1-2", "quantity": 4 },
    { "cardId": "xy1-3", "quantity": 4 },
    { "cardId": "xy1-4", "quantity": 4 },
    { "cardId": "xy1-5", "quantity": 4 },
    { "cardId": "xy1-6", "quantity": 4 },
    { "cardId": "xy1-7", "quantity": 4 },
    { "cardId": "xy1-8", "quantity": 4 },
    { "cardId": "xy1-9", "quantity": 4 },
    { "cardId": "xy1-10", "quantity": 4 },
    { "cardId": "xy1-11", "quantity": 4 },
    { "cardId": "xy1-12", "quantity": 4 },
    { "cardId": "xy1-13", "quantity": 4 },
    { "cardId": "xy1-14", "quantity": 4 },
    { "cardId": "xy1-15", "quantity": 4 }
  ]
}
```

Response esperado: `201 Created` con `"valid": true`, `"totalCards": 60`.

### 3. Obtener mazo por ID

```
GET /api/decks/{id-del-paso-anterior}
```

Response esperado: `200 OK` con los datos del mazo creado.

### 4. Validar mazo existente

```
POST /api/decks/{id}/validate
```

Response esperado: `200 OK` con `"valid": true`.

### 5. Crear un mazo inválido (sin Basic Pokémon)

```
POST /api/decks
Content-Type: application/json

{
  "name": "Mazo Invalido",
  "playerId": null,
  "cards": [
    { "cardId": "xy1-1", "quantity": 60 }
  ]
}
```

Response: `201 Created` pero con `"valid": false` y error `MISSING_BASIC_POKEMON` (si xy1-1 no tiene stage BASIC).

### 6. Obtener mazo inexistente

```
GET /api/decks/00000000-0000-0000-0000-000000000000
```

Response esperado: `404 Not Found`.

### 7. Actualizar mazo

```
PUT /api/decks/{id}
Content-Type: application/json

{
  "name": "Mazo Actualizado",
  "cards": [
    { "cardId": "xy1-1", "quantity": 4 },
    { "cardId": "xy1-2", "quantity": 4 },
    { "cardId": "xy1-3", "quantity": 4 },
    { "cardId": "xy1-4", "quantity": 4 },
    { "cardId": "xy1-5", "quantity": 4 },
    { "cardId": "xy1-6", "quantity": 4 },
    { "cardId": "xy1-7", "quantity": 4 },
    { "cardId": "xy1-8", "quantity": 4 },
    { "cardId": "xy1-9", "quantity": 4 },
    { "cardId": "xy1-10", "quantity": 4 },
    { "cardId": "xy1-11", "quantity": 4 },
    { "cardId": "xy1-12", "quantity": 4 },
    { "cardId": "xy1-13", "quantity": 4 },
    { "cardId": "xy1-14", "quantity": 4 },
    { "cardId": "xy1-15", "quantity": 4 }
  ]
}
```

Response esperado: `200 OK` con el nombre actualizado.

### 8. Eliminar mazo

```
DELETE /api/decks/{id}
```

Response esperado: `204 No Content`.

## Limitación conocida

Los seed decks usan IDs `"energy-fire-basic"` y `"energy-water-basic"` para las Energy.
Si estas cartas no existen en el catálogo (`CardLookupPort.getCardById()` retorna null),
el validador no podrá identificarlas como Basic Energy y el mazo se guardará con
`valid=false`. Corregir cuando se agreguen dichas cartas a la base de datos.
