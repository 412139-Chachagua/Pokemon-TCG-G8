# Catálogo de Cartas — Backend

## CardEntity

`CardEntity` (`BE/src/main/java/.../repositories/entities/CardEntity.java`) es la entidad JPA principal que modela una carta en la base de datos. Mapea a la tabla `cards`.

### Campos principales

| Campo                  | Tipo      | Descripción                                                |
|------------------------|-----------|------------------------------------------------------------|
| `id`                   | String    | Identificador único (ej. `xy1-1`). Es la PK.               |
| `name`                 | String    | Nombre de la carta.                                        |
| `supertype`            | String    | Supertipo: `POKEMON`, `TRAINER` o `ENERGY`.                |
| `subtypes`             | String    | Subtipos separados por coma (ej. `BASIC,EX`).              |
| `setCode`              | String    | Código de la colección a la que pertenece.                 |
| `number`               | String    | Número de carta dentro de la colección.                    |
| `rarity`               | String    | Rareza (ej. `Common`, `Rare Holo`).                        |
| `imageSmallUrl`        | String    | URL de la imagen pequeña.                                  |
| `imageLargeUrl`        | String    | URL de la imagen grande.                                   |
| `hp`                   | Integer   | PS (puntos de salud). Solo aplica a Pokémon.               |
| `pokemonStage`         | String    | Etapa evolutiva: `BASIC`, `STAGE_1`, `STAGE_2`, `MEGA`.   |
| `evolvesFrom`          | String    | Nombre del Pokémon del que evoluciona.                     |
| `evolvesTo`            | String    | Nombres de evoluciones posibles (separados por coma).      |
| `pokemonTypes`         | String    | Tipos de energía del Pokémon (ej. `Fire,Water`).           |
| `retreatCost`          | String    | Coste de retirada (tipos de energía separados por coma).   |
| `convertedRetreatCost` | Integer   | Coste de retirada convertido a número.                     |
| `isEx`                 | Boolean   | Indica si es carta EX.                                     |
| `isMega`               | Boolean   | Indica si es carta MEGA.                                   |
| `energyCardType`       | String    | `BASIC` o `SPECIAL` (solo energía).                        |
| `providesEnergyTypes`  | String    | Tipos de energía que provee (solo energía).                |
| `trainerSubtype`       | String    | Subtipo de entrenador: `SUPPORTER`, `STADIUM`, `ITEM`, etc.|
| `effectCode`           | String    | Código de efecto del entrenador (ej. `GREAT_BALL`).        |
| `isAceSpec`            | Boolean   | Indica si es carta ACE SPEC.                               |
| `strategyKey`          | String    | Clave de estrategia para energía (ej. `DOUBLE_COLORLESS`). |
| `rulesText`            | String    | Texto de reglas (separado por `\|`).                       |
| `rawJson`              | String    | JSON original de la API externa.                           |
| `createdAt`            | Instant   | Fecha de creación.                                         |
| `updatedAt`            | Instant   | Fecha de última modificación.                              |

### Relaciones

- `attacks` → lista de `CardAttackEntity` (relación `@OneToMany` con `CascadeType.ALL`, orphan removal).
- `weaknesses` → lista de `CardWeaknessEntity`.
- `resistances` → lista de `CardResistanceEntity`.

Todas las relaciones se cargan de forma diferida (`FetchType.LAZY`).

---

## CardController

`CardController` (`BE/src/main/java/.../controllers/cards/CardController.java`) expone los endpoints REST del catálogo bajo `/api/cards`.

### Endpoints

#### `GET /api/cards` — Buscar cartas con filtros

Busca cartas en el catálogo local con filtros opcionales y paginación.

**Parámetros (query params):**

| Parámetro   | Obligatorio | Descripción                                      |
|-------------|-------------|--------------------------------------------------|
| `query`     | No          | Texto de búsqueda (LIKE sobre el nombre).        |
| `supertype` | No          | Filtro por supertipo: `POKEMON`, `TRAINER`, `ENERGY`. |
| `setCode`   | No          | Filtro por código de colección.                  |
| `stage`     | No          | Filtro por etapa evolutiva: `BASIC`, `STAGE_1`, etc. |
| `page`      | No          | Número de página (base 0). Por defecto `0`.      |
| `size`      | No          | Tamaño de página. Por defecto `20`.              |

**Respuesta (200 OK):** `CardSearchResponse` con lista de `CardSummaryResponse` y metadatos de paginación.

#### `GET /api/cards/{id}` — Obtener detalle de carta

Devuelve el detalle completo de una carta incluyendo ataques, debilidades y resistencias.

**Path variable:**
- `id`: Identificador de la carta (ej. `xy1-1`).

**Respuesta (200 OK):** `CardDetailResponse` con toda la información detallada.

**Errores:**
- `404` si no existe la carta.

#### `POST /api/cards/sync` — Sincronización manual

Dispara una sincronización manual de todas las cartas desde la API externa de Pokémon TCG.

---

## CardCatalogService

`CardCatalogService` (`BE/src/main/java/.../services/cards/CardCatalogService.java`) es la capa de servicio que orquesta las consultas al catálogo.

### `searchCards(CardSearchRequest)`

1. Construye un `Pageable` con paginación y orden por nombre ascendente.
2. Crea una `Specification<CardEntity>` dinámica usando JPA Criteria API.
3. Aplica filtros condicionales:
   - `query`: `LIKE` insensible a mayúsculas sobre el campo `name`.
   - `supertype`: igualdad exacta (en mayúsculas) sobre `supertype`.
   - `setCode`: igualdad exacta sobre `setCode`.
   - `stage`: igualdad exacta sobre `pokemonStage`.
4. Ejecuta la consulta con `cardJpaRepository.findAll(spec, pageable)`.
5. Mapea cada `CardEntity` a `CardSummaryResponse` usando `CardMapper.toSummaryResponse()`.
6. Logea el rendimiento (warning si supera 500ms).
7. Retorna un `CardSearchResponse` con los resultados y metadatos de paginación.

### `getCardById(String id)`

1. Busca la entidad por ID en el repositorio.
2. Si no existe, lanza `NotFoundException` (resultando en 404).
3. Mapea a `CardDetailResponse` usando `CardMapper.toDetailResponse()`.
4. Logea el rendimiento.
5. Retorna el detalle.

---

## CardMapper

`CardMapper` (`BE/src/main/java/.../mappers/cards/CardMapper.java`) es un componente Spring que convierte entre entidades y DTOs.

### Métodos principales

| Método                          | Origen → Destino                          |
|---------------------------------|-------------------------------------------|
| `toSummaryResponse(CardEntity)` | `CardEntity` → `CardSummaryResponse`      |
| `toDetailResponse(CardEntity)`  | `CardEntity` → `CardDetailResponse`       |
| `toCardEntity(PokemonTcgApiCardDto)` | DTO de API externa → `CardEntity`    |

### `toSummaryResponse`
Convierte solo los campos esenciales para la vista de lista: `id`, `name`, `supertype`, `setCode`, `number`, `imageSmallUrl`, `subtypes`, `stage`.

### `toDetailResponse`
Convierte todos los campos incluyendo:
- `rulesText`: dividido por `|`.
- `hp`, `pokemonStage`, `evolvesFrom`, `types`, `retreatCost`.
- `attacks`: mapea cada `CardAttackEntity` a `AttackDto`.
- `weaknesses`: mapea cada `CardWeaknessEntity` a `WeaknessDto`.
- `resistances`: mapea cada `CardResistanceEntity` a `ResistanceDto`.
- `abilities`: deserializa el JSON de habilidades a `List<CardAbilityResponse>`.
- `isEx`, `isMega`, `providesEnergyTypes`.

### Métodos auxiliares

- `toAttackEntity(AttackDto, CardEntity, int)`: convierte un DTO de ataque a `CardAttackEntity` con índice, nombre, coste impreso, energía convertida, texto de daño, texto de efecto y código de efecto generado.
- `toWeaknessEntity(WeaknessDto, CardEntity)`: crea `CardWeaknessEntity` parseando el valor del multiplicador.
- `toResistanceEntity(ResistanceDto, CardEntity)`: crea `CardResistanceEntity` parseando el valor numérico.
- `toAttackDto(CardAttackEntity)`: reconvierte a `AttackDto` para la respuesta.
- `generateEffectCode(String)`: parsea el texto de efecto del ataque usando `TextEffectParser` y genera un código estructurado (ej. `DRAW_CARDS:2;;DISCARD_ENERGY:1`).
- `toCardAbilityResponseList(String)`: deserializa JSON de habilidades a `List<CardAbilityResponse>` y consulta si cada habilidad es activable mediante `AbilityRegistry.has()`.
- `listToCommaString` / `commaStringToList`: conversión entre lista y string separado por comas.

---

## CardSearchRequest

`CardSearchRequest` (`BE/src/main/java/.../dtos/cards/CardSearchRequest.java`) es un record que encapsula los filtros de búsqueda:

```java
public record CardSearchRequest(
    String query,     // Búsqueda por nombre (LIKE)
    String supertype, // POKEMON | TRAINER | ENERGY
    String setCode,   // Código de colección
    String stage,     // BASIC | STAGE_1 | STAGE_2 | MEGA
    Integer page,     // Número de página (0-based)
    Integer size      // Tamaño de página
)
```

---

## CardSearchResponse y CardSummaryResponse

### CardSearchResponse
`CardSearchResponse` (`BE/src/main/java/.../dtos/cards/CardSearchResponse.java`) es la respuesta paginada de la búsqueda:

```java
public record CardSearchResponse(
    List<CardSummaryResponse> items,  // Cartas en la página actual
    int page,                          // Número de página actual
    int size,                          // Tamaño de página
    long totalItems                    // Total de cartas que coinciden
)
```

### CardSummaryResponse
Cada elemento de la lista es un `CardSummaryResponse` con datos resumidos para el listado:

```java
public record CardSummaryResponse(
    String id,             // Identificador único
    String name,           // Nombre de la carta
    String supertype,      // POKEMON | TRAINER | ENERGY
    String setCode,        // Código de colección
    String number,         // Número en la colección
    String imageSmallUrl,  // URL de imagen pequeña
    List<String> subtypes, // Subtipos (BASIC, EX, etc.)
    String stage           // Etapa evolutiva
)
```

---

## CardDetailResponse

`CardDetailResponse` (`BE/src/main/java/.../dtos/cards/CardDetailResponse.java`) contiene toda la información detallada de una carta:

```java
public record CardDetailResponse(
    String id,                      // Identificador
    String name,                    // Nombre
    String supertype,               // POKEMON | TRAINER | ENERGY
    List<String> subtypes,           // Subtipos
    String setCode,                 // Código de colección
    String number,                  // Número en colección
    String imageSmallUrl,           // Imagen pequeña
    String imageLargeUrl,           // Imagen grande
    List<String> rulesText,         // Texto de reglas
    Integer hp,                     // PS (Pokémon)
    String stage,                   // Etapa evolutiva
    String evolvesFrom,             // Evoluciona de
    List<String> types,             // Tipos de energía
    List<AttackDto> attacks,        // Ataques
    List<WeaknessDto> weaknesses,   // Debilidades
    List<ResistanceDto> resistances,// Resistencias
    List<String> retreatCost,       // Coste de retirada
    Boolean isEx,                   // Es EX
    Boolean isMega,                 // Es MEGA
    List<CardAbilityResponse> abilities, // Habilidades
    List<String> providesEnergyTypes      // Tipos de energía que provee
)
```

---

## Ataques, Debilidades y Resistencias

### CardAttackEntity

`CardAttackEntity` (`BE/src/main/java/.../repositories/entities/CardAttackEntity.java`) modela un ataque individual. Mapea a la tabla `card_attacks`.

| Campo                    | Tipo    | Descripción                                          |
|--------------------------|---------|------------------------------------------------------|
| `id`                     | UUID    | Identificador único (autogenerado).                  |
| `card`                   | CardEntity | Relación ManyToOne con la carta.                  |
| `attackIndex`            | Integer | Índice del ataque (0, 1, etc.).                      |
| `name`                   | String  | Nombre del ataque.                                   |
| `printedCost`            | String  | Coste de energía impreso (ej. `Fire,Colorless`).     |
| `convertedEnergyCost`    | Integer | Coste convertido a número.                           |
| `damageText`             | String  | Texto del daño (ej. `30`, `×10`, `-`).               |
| `baseDamage`             | Integer | Daño base como número (si aplica).                   |
| `effectText`             | String  | Texto descriptivo del efecto.                        |
| `effectCode`             | String  | Código estructurado del efecto generado por `TextEffectParser`. |
| `createdAt`              | Instant | Fecha de creación.                                   |

**AttackDto** es el DTO que transporta la información del ataque en las respuestas:

```java
public record AttackDto(
    Integer index,
    String name,
    List<String> cost,
    Integer convertedEnergyCost,
    String damage,
    String text,
    Integer baseDamage
)
```

### CardWeaknessEntity

`CardWeaknessEntity` (`BE/src/main/java/.../repositories/entities/CardWeaknessEntity.java`) modela una debilidad. Mapea a `card_weaknesses`.

| Campo        | Tipo    | Descripción                          |
|--------------|---------|--------------------------------------|
| `id`         | UUID    | Identificador único autogenerado.    |
| `card`       | CardEntity | Relación ManyToOne con la carta.  |
| `energyType` | String  | Tipo de energía (ej. `Fire`).        |
| `multiplier` | Integer | Multiplicador de daño (por defecto `2`). |

**WeaknessDto:** `record WeaknessDto(String type, String value)`.

### CardResistanceEntity

`CardResistanceEntity` (`BE/src/main/java/.../repositories/entities/CardResistanceEntity.java`) modela una resistencia. Mapea a `card_resistances`.

| Campo        | Tipo    | Descripción                           |
|--------------|---------|---------------------------------------|
| `id`         | UUID    | Identificador único autogenerado.     |
| `card`       | CardEntity | Relación ManyToOne con la carta.   |
| `energyType` | String  | Tipo de energía (ej. `Fighting`).     |
| `value`      | Integer | Valor de reducción (por defecto `-20`). |

**ResistanceDto:** `record ResistanceDto(String type, String value)`.

---

## AbilityDto

`AbilityDto` (`BE/src/main/java/.../dtos/cards/AbilityDto.java`) se usa internamente para deserializar las habilidades desde el JSON de la API:

```java
public record AbilityDto(String name, String text, String type) {}
```

- `name`: Nombre de la habilidad.
- `text`: Descripción textual.
- `type`: Tipo de habilidad (`Ability`, `Pokemon Power`, `Pokemon Body`).

En la respuesta se convierte a `CardAbilityResponse`, que además incluye `isActivable` (booleano que indica si la habilidad se puede activar en el juego, consultado contra `AbilityRegistry`).
