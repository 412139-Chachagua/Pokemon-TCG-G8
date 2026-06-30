# Mazos — Backend

## DeckEntity

Entidad JPA que representa un mazo en la base de datos. Mapea a la tabla `decks`.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `UUID` | Identificador único generado automáticamente. |
| `ownerPlayer` | `PlayerEntity` (ManyToOne, LAZY) | Jugador propietario del mazo. Puede ser `null` para mazos predefinidos. |
| `name` | `String` (varchar 120, not null) | Nombre del mazo. |
| `source` | `String` (varchar 30) | Origen del mazo: `"USER"`, `"PREDEFINED"` o `"RANDOM"`. |
| `valid` | `Boolean` (not null) | Indica si el mazo pasó todas las validaciones del formato. |
| `mainCardId` | `String` (varchar 80) | ID de la carta principal del mazo (usado para mostrar la imagen de portada). |
| `validationErrors` | `TEXT` | Errores de validación serializados como JSON array de strings. |
| `createdAt` | `Instant` (not null, updatable=false) | Timestamp de creación, seteado automáticamente con `@PrePersist`. |
| `updatedAt` | `Instant` (not null) | Timestamp de última modificación, seteado automáticamente con `@PreUpdate`. |
| `cards` | `List<DeckCardEntity>` (OneToMany, cascade ALL, orphanRemoval=true, LAZY) | Cartas que componen el mazo. |

La relación con `DeckCardEntity` usa `orphanRemoval = true`, por lo que al limpiar o reemplazar la lista de cartas las entradas huérfanas se eliminan automáticamente.

---

## DeckCardEntity

Entidad JPA que vincula una carta con un mazo y su cantidad. Mapea a la tabla `deck_cards`.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `UUID` | Identificador único. |
| `deck` | `DeckEntity` (ManyToOne, LAZY, not null) | Mazo al que pertenece esta entrada. |
| `cardId` | `String` (varchar 80, not null) | Identificador de la carta (ej: `"xy1-78"`). |
| `quantity` | `Integer` (not null) | Cantidad de copias de esta carta en el mazo. Por defecto 1. |
| `createdAt` | `Instant` (not null, updatable=false) | Timestamp de creación, seteado automáticamente con `@PrePersist`. |

No existe una relación directa con una entidad `CardEntity`; el `cardId` es un string que se resuelve a través de `CardLookupPort` en la capa de dominio/servicio.

---

## DTOs

### CreateDeckRequest

```java
public record CreateDeckRequest(
    String name,
    String playerId,
    List<DeckCardRequest> cards
) {
    public record DeckCardRequest(String cardId, int quantity) {}
}
```

### UpdateDeckRequest

```java
public record UpdateDeckRequest(
    String name,
    List<CreateDeckRequest.DeckCardRequest> cards
)
```

### DeckResponse

```java
public record DeckResponse(
    String id,
    String name,
    String ownerPlayerId,
    String source,
    int totalCards,
    boolean valid,
    String mainCardId,
    String mainCardImageUrl,
    List<DeckCardResponse> cards,
    DeckValidationResponse validation,
    String createdAt
)
```

Tiene un constructor secundario que omite `mainCardId` y `mainCardImageUrl` (los setea a `null`).

### DeckCardResponse

```java
public record DeckCardResponse(
    String cardId,
    String name,
    int quantity,
    String supertype,       // "POKEMON", "TRAINER", "ENERGY"
    boolean isBasicEnergy,
    List<String> subtypes,
    String stage            // "BASIC", "STAGE_1", "STAGE_2", null
)
```

### DeckValidationResponse

```java
public record DeckValidationResponse(
    boolean valid,
    List<DeckValidationError> errors
) {
    public record DeckValidationError(String code, String message, Object details) {}
}
```

### ValidateDeckRequest

```java
public record ValidateDeckRequest(
    List<ValidateCardEntry> cards
) {
    public record ValidateCardEntry(String cardId, int quantity) {}
}
```

### PredefinedDeckTemplate

```java
public record PredefinedDeckTemplate(
    UUID id,
    String name,
    String mainCardId,
    List<PredefinedDeckCardEntry> cards
)
```

### PredefinedDeckCardEntry

```java
public record PredefinedDeckCardEntry(
    String cardId,
    String name,
    String supertype,
    int quantity
)
```

---

## DeckJpaRepository

Repositorio JPA para `DeckEntity`. Proporciona:

- `findByOwnerPlayerId(UUID)` — Lista los mazos de un jugador.
- `findByIdWithCards(UUID)` — Busca un mazo con sus cartas usando `LEFT JOIN FETCH`.
- `findPredefinedDecks()` — Busca mazos sin propietario (`ownerPlayer IS NULL`).

---

## DeckMapper

Componente responsable de la conversión entre entidades, DTOs y objetos de dominio.

| Método | Origen → Destino | Detalles |
|--------|------------------|----------|
| `toResponse(DeckEntity, DeckValidationResult)` | Entidad → `DeckResponse` | Convierte cada `DeckCardEntity` a `DeckCardResponse` usando `CardLookupPort` para resolver nombre, supertipo, subtipos y etapa. Calcula `totalCards` sumando cantidades. |
| `toCardResponse(DeckCardEntity)` | Entidad de carta → `DeckCardResponse` | Resuelve `CardDefinition` vía `CardLookupPort` para obtener nombre, supertipo, subtipos y etapa. |
| `toEntity(CreateDeckRequest)` | Request → `DeckEntity` | Crea la entidad con source `"USER"`, valid `false`, y mapea cada `DeckCardRequest` a `DeckCardEntity`. El owner se setea después en el servicio. |
| `toCardEntity(DeckEntity, DeckCardRequest)` | Request → `DeckCardEntity` | Asocia la carta al mazo. |
| `updateEntity(DeckEntity, UpdateDeckRequest)` | Actualiza entidad existente | Limpia la lista de cartas y la reemplaza con las nuevas. |
| `toDomain(DeckEntity)` | Entidad → `Deck` (dominio) | Convierte a objeto de dominio para el motor de juego. |
| `toDomainCard(DeckCardEntity)` | Entidad → `DeckCard` (dominio) | Convierte carta de entidad a dominio. |
| `toValidationResponse(DeckValidationResult)` | Resultado de validación → `DeckValidationResponse` | Mapea cada `DeckValidationError` a su correspondiente `DeckValidationError` con código y mensaje en español. |

**Mensajes de error de validación** (mapeados en `toValidationError`):

| Código | Mensaje |
|--------|---------|
| `DECK_SIZE_INVALID` | "El mazo debe tener exactamente 60 cartas." |
| `MORE_THAN_4_COPIES` | "No puede haber más de 4 copias de la misma carta." |
| `MISSING_BASIC_POKEMON` | "El mazo debe tener al menos 1 Pokémon Básico." |
| `ACE_SPEC_LIMIT_EXCEEDED` | "El mazo no puede tener más de 1 carta AS TÁCTICO." |
| (default) | "El mazo no es válido." |

---

## DeckController

Controlador REST bajo la ruta `/api/decks`.

### Endpoints

| Método | Ruta | Descripción | Código de respuesta |
|--------|------|-------------|---------------------|
| `POST` | `/api/decks` | Crea un mazo. Recibe `CreateDeckRequest` en el body. | `201 Created` |
| `GET` | `/api/decks/{id}` | Obtiene un mazo por ID. | `200 OK` |
| `PUT` | `/api/decks/{id}` | Actualiza un mazo existente. Recibe `UpdateDeckRequest`. | `200 OK` |
| `DELETE` | `/api/decks/{id}` | Elimina un mazo. | `204 No Content` |
| `GET` | `/api/decks?playerId={uuid}` | Lista todos los mazos de un jugador. | `200 OK` |
| `POST` | `/api/decks/predefined` | Lista los mazos predefinidos (templates). | `200 OK` |
| `POST` | `/api/decks/{id}/copy?playerId={uuid}` | Copia un mazo predefinido a un jugador. | `201 Created` |
| `POST` | `/api/decks/{id}/validate` | Valida un mazo existente por ID. | `200 OK` |
| `POST` | `/api/decks/validate` | Valida una lista de cartas sin persistencia. Recibe `ValidateDeckRequest`. | `200 OK` |
| `POST` | `/api/decks/random` | Genera un mazo aleatorio válido. | `200 OK` o `422 Unprocessable Entity` si no se puede generar. |
| `POST` | `/api/decks/import` | Importa mazos desde archivo (JSON, TXT o PDF). Parámetros: `file`, `playerId`, `format`. | `201 Created` |
| `GET` | `/api/decks/{id}/export` | Exporta un mazo como PDF. | `200 OK` con `Content-Type: application/pdf` |

### Dependencias

- `DeckService` — lógica principal de CRUD, validación e importación/exportación.
- `RandomDeckService` — generación de mazos aleatorios.
- `PredefinedDeckService` — gestión de mazos predefinidos.

---

## DeckService

Servicio principal con la lógica de negocio para la gestión de mazos.

### Métodos principales

- **`createDeck(CreateDeckRequest)`**: Valida que el jugador exista (si `playerId` no es null), mapea la request a entidad, asocia el jugador, ejecuta `DeckValidator.validate()`, y persiste. Si la validación falla, lanza `ValidationException` con los errores concatenados.

- **`getDeck(UUID)`**: Busca el mazo por ID, lo valida y devuelve la respuesta. Si no existe, lanza `NotFoundException`.

- **`updateDeck(UUID, UpdateDeckRequest)`**: Actualiza nombre y cartas del mazo, revalida, y persiste. Si la validación falla, lanza `ValidationException`.

- **`deleteDeck(UUID)`**: Elimina el mazo de la base de datos.

- **`listDecksByPlayer(UUID)`**: Lista todos los mazos de un jugador, cada uno con su validación.

- **`validateDeck(UUID)`**: Valida un mazo existente y devuelve `DeckValidationResponse`.

- **`validateCards(ValidateDeckRequest)`**: Valida una lista de cartas sin necesidad de tener un mazo persistido.

- **`loadDeckDomain(UUID)`**: Carga un mazo como objeto de dominio (`Deck`) para el motor de juego.

- **`importDecks(String content, String format, UUID playerId, String filename)`**: Importa uno o más mazos desde contenido JSON o TXT. Cada mazo se persiste con su estado de validación.

- **`importPdfDecks(MultipartFile, UUID)`**: Importa mazos desde un archivo PDF usando `PdfImportService`.

- **`exportDeckPdf(UUID)`**: Exporta un mazo a PDF usando `PdfExportService`.

### Importación de formatos

**JSON**: Espera un objeto o array de objetos con `name` y `cards` (array de `{ cardId, quantity }`).

**TXT**: Líneas con formato `cardId: quantity` o `cantidad nombre`. Las líneas que empiezan con `# ` indican el nombre del mazo. Las líneas vacías o que empiezan con `*` se ignoran. El patrón `^(\\d+)\\s+(.+)$` permite parsear formato `2 Pikachu` buscando la carta por nombre (case-insensitive) en `CardJpaRepository`.

**PDF**: Usa `PdfImportService` para extraer los mazos.

### Validación en importación

Al importar, si el catálogo de cartas está vacío (`cardJpaRepository.count() == 0`) se lanza `ValidationException`. Cada mazo importado se persiste con `valid = validation.isValid()` y los errores serializados en `validationErrors`.

---

## DeckValidator

Componente que implementa las reglas de validación de mazos según el formato de juego.

### Reglas de validación

| Regla | Código de error | Descripción |
|-------|-----------------|-------------|
| **Tamaño exacto** | `DECK_SIZE_INVALID` | El mazo debe tener exactamente 60 cartas (suma de todas las cantidades). |
| **Límite de copias** | `MORE_THAN_4_COPIES` | No puede haber más de 4 copias de una misma carta. Excepciones: Energías básicas (sin límite) y Pokémon (se validan por nombre canónico, no por ID). |
| **Pokémon Básico** | `MISSING_BASIC_POKEMON` | El mazo debe contener al menos un Pokémon Básico (`stage == null` o `"BASIC"`). |
| **Límite AS TÁCTICO** | `ACE_SPEC_LIMIT_EXCEEDED` | El mazo no puede tener más de 1 carta de tipo AS TÁCTICO (Ace Spec). |
| **Energías Especiales** | `MORE_THAN_4_COPIES` | Las energías especiales se validan por nombre, no por ID (una misma carta puede tener IDs distintos en diferentes sets). |

### Flujo de validación

1. Calcula el total de cartas. Si no es 60 → error `DECK_SIZE_INVALID`.
2. Cuenta cartas AS TÁCTICO (entrenadores con `isAceSpec()`). Si suma más de 1 → error `ACE_SPEC_LIMIT_EXCEEDED`.
3. Agrupa cartas por ID y verifica que ninguna (excepto energías básicas y Pokémon) exceda 4 copias.
4. Agrupa Pokémon por nombre canónico (usando `CardNameNormalizer`) y verifica que ningún nombre exceda 4 copias.
5. Agrupa energías especiales por nombre y verifica que ningún nombre exceda 4 copias.
6. Verifica que al menos una carta sea un Pokémon Básico. Si no → error `MISSING_BASIC_POKEMON`.

El resultado se encapsula en `DeckValidationResult(boolean valid, List<DeckValidationError> errors)`.

---

## CardNameNormalizer

Utilidad estática para normalizar nombres de cartas Pokémon. Se usa en `DeckValidator` para agrupar Pokémon por nombre canónico y así detectar duplicados más allá de IDs distintos.

### Reglas de normalización

1. **Trim**: elimina espacios al inicio y final.
2. **Sufijo "Nv."**: elimina `" Nv."` y todo lo que le sigue (ej: `"Pikachu Nv.4"` → `"Pikachu"`).
3. **"del Equipo Plasma"**: elimina esta frase (case-insensitive) cuando aparece.
4. **"Equipo Plasma"**: elimina esta frase (case-insensitive) cuando aparece sola.
5. **Símbolo δ (Delta)**: elimina el caracter δ y espacios circundantes (ej: `"Ditto δ"` → `"Ditto"`).

Esto permite que cartas como `"Pikachu"` y `"Pikachu Nv.4"` se consideren la misma carta a efectos del límite de 4 copias.

### Código

```java
public static String normalize(String name) {
    if (name == null) return null;
    String result = name.trim();
    result = result.replaceAll("\\s+Nv\\..*", "");
    result = result.replaceAll("(?i)\\s+del\\s+Equipo\\s+Plasma", "");
    result = result.replaceAll("(?i)\\s+Equipo\\s+Plasma", "");
    result = result.replaceAll("\\s+δ", "");
    result = result.replaceAll("\\s*δ\\s*", "");
    return result.trim();
}
```

---

## PredefinedDeckService

Servicio que gestiona plantillas de mazos predefinidos. Los templates se construyen en memoria en un `LinkedHashMap` para preservar el orden de inserción.

### Mazos predefinidos

| ID (UUID) | Nombre | Carta principal |
|-----------|--------|-----------------|
| `00000000-0000-0000-0000-000000000003` | Destruction Rush | `xy1-78` |
| `00000000-0000-0000-0000-000000000004` | Resilient Life | `xy1-96` |

### Métodos

- **`getAllTemplates()`**: Devuelve todos los templates como `List<PredefinedDeckTemplate>`.
- **`getTemplateById(UUID)`**: Busca un template por ID. Lanza `NotFoundException` si no existe o si el ID es null.
- **`getAllAsResponse()`**: Devuelve todos los templates como `List<DeckResponse>`, con source `"PREDEFINED"` y valid siempre `true`.
- **`copyToPlayer(UUID templateId, UUID playerId)`**: Copia un template predefinido a un jugador. Crea una nueva entidad `DeckEntity` con source `"USER"`, valid `false`, y las cartas del template. Si el jugador no existe, lanza `NotFoundException`.

### Métodos auxiliares

- `supertypeOf(String cardId)`: Determina el supertipo según el número de la carta (lógica simplificada basada en rangos numéricos).
- `buildImageUrl(String cardId)`: Construye la URL de la imagen en `https://images.pokemontcg.io/`.

### Composición de Destruction Rush

- Pokémon Siniestro + Lucha (20 cartas)
- Entrenadores (11 cartas)
- Energías Siniestro + Lucha (18 cartas)
- **Total**: ~49 cartas (dependiendo de conteo exacto)

### Composición de Resilient Life

- Pokémon Psíquico + Planta (24 cartas)
- Entrenadores (11 cartas)
- Energías Planta + Psíquico (18 cartas)
- **Total**: ~53 cartas (dependiendo de conteo exacto)

> Nota: Los templates predefinidos no necesariamente suman 60 cartas exactas ya que son plantillas conceptuales.

---

## RandomDeckService

Servicio que genera mazos aleatorios válidos. Utiliza un enfoque de "intento y validación": genera combinaciones aleatorias hasta encontrar una que pase todas las validaciones o agota los reintentos.

### Constantes

| Constante | Valor | Descripción |
|-----------|-------|-------------|
| `MAX_RETRIES` | 20 | Intentos máximos antes de fallar. |
| `DECK_SIZE` | 60 | Tamaño objetivo del mazo. |
| `MAX_COPIES` | 4 | Máximo de copias por carta. |
| `MIN_ENERGY` | 10 | Energías mínimas a incluir. |
| `MAX_ENERGY` | 15 | Energías máximas a incluir. |
| `MIN_POKEMON` | 12 | Pokémon mínimos a incluir. |
| `MAX_POKEMON` | 20 | Pokémon máximos a incluir. |
| `MIN_TRAINERS` | 25 | Entrenadores mínimos a incluir. |
| `MAX_TRAINERS` | 35 | Entrenadores máximos a incluir. |

### Flujo de generación

1. **Filtrado**: Obtiene todas las cartas del set `"xy1"` y las clasifica por supertipo (POKEMON, TRAINER, ENERGY) y etapa evolutiva (BASIC, STAGE_1, STAGE_2).
2. **Selección de energías**: Escoge entre 1 y 3 tipos de energía (evitando exceder 3 tipos, manejando COLORLESS). Distribuye la cantidad total (10-15) entre los tipos elegidos mediante splits predefinidos.
3. **Selección de Pokémon**: Escoge 1-2 líneas evolutivas principales compatibles con los tipos de energía seleccionados. Cada línea puede ser Stage 2 (con básico + stage1 + stage2), Stage 1 (básico + stage1), o solo básico. Completa hasta el objetivo de Pokémon con cartas de soporte adicionales.
4. **Selección de entrenadores**: Distribuye el espacio restante entre Supporter (8-12), Item (12-19), Stadium (0-4) y Tool (0-4). Opcionalmente incluye 1 carta Ace Spec.
5. **Validación**: Ejecuta `DeckValidator.validate()`. Si es válido, devuelve el mazo; si no, reintenta hasta `MAX_RETRIES` veces.
6. **Fallo**: Si no se genera un mazo válido, lanza `ValidationException`.

### Métodos auxiliares

- `extractEnergyType(CardEntity)`: Extrae el tipo de energía del nombre o del campo `providesEnergyTypes`.
- `extractPokemonTypes(CardEntity)`: Extrae los tipos de un Pokémon del campo `pokemonTypes`.
- `isPokemonCompatible(CardEntity, Set<String>)`: Verifica que un Pokémon sea compatible con los tipos de energía del mazo (los Pokémon COLORLESS son siempre compatibles).
- `addOrIncrement(List<DeckCard>, cardId, quantity)`: Agrega una carta a la lista o incrementa su cantidad si ya existe.
- `addRandomFromPool(...)`: Agrega cartas aleatorias de un pool respetando el límite de 4 copias.

---

## Validación — Flujo completo

### Creación/Actualización de mazo

```
Request (CreateDeckRequest / UpdateDeckRequest)
    ↓
DeckService.createDeck() / updateDeck()
    ↓
Mapper: request → DeckEntity (toEntity / updateEntity)
    ↓
Mapper: DeckCardEntity → DeckCard (toDomainCard)
    ↓
DeckValidator.validate(List<DeckCard>)
    ├─ ¿Total = 60? → DECK_SIZE_INVALID
    ├─ ¿Ace Spec ≤ 1? → ACE_SPEC_LIMIT_EXCEEDED
    ├─ ¿Copias por ID ≤ 4? (excepto energía básica y Pokémon) → MORE_THAN_4_COPIES
    ├─ ¿Copias por nombre canónico de Pokémon ≤ 4? → MORE_THAN_4_COPIES
    ├─ ¿Copias por nombre de energía especial ≤ 4? → MORE_THAN_4_COPIES
    └─ ¿Al menos 1 Pokémon Básico? → MISSING_BASIC_POKEMON
    ↓
DeckValidationResult
    ↓
¿Válido? → Sí: entity.setValid(true), guardar, responder 201/200
→ No: lanzar ValidationException o guardar con valid=false (importación)
```

### Validación independiente

```
POST /api/decks/validate  (ValidateDeckRequest)
    ↓
DeckService.validateCards()
    ↓
DeckValidator.validate()
    ↓
DeckMapper.toValidationResponse()
    ↓
DeckValidationResponse
```

### Validación de mazo existente

```
POST /api/decks/{id}/validate
    ↓
DeckService.validateDeck(id)
    ↓
findEntity → DeckValidator.validate
    ↓
DeckMapper.toValidationResponse
    ↓
DeckValidationResponse
```
