# Detalle de Carta — Backend

## Endpoint: GET /api/cards/{id}

El endpoint de detalle (`CardController.getCardById()`) devuelve la información completa de una carta individual, a diferencia del endpoint de búsqueda que retorna solo un resumen.

### Datos adicionales vs. CardSummaryResponse

Mientras que `CardSummaryResponse` contiene solo los campos básicos (`id`, `name`, `supertype`, `setCode`, `number`, `imageSmallUrl`, `subtypes`, `stage`), `CardDetailResponse` añade:

- `imageLargeUrl` — Imagen en tamaño grande.
- `rulesText` — Texto de reglas de la carta.
- `hp` — Puntos de salud (solo Pokémon).
- `evolvesFrom` — De qué Pokémon evoluciona.
- `types` — Tipos de energía del Pokémon.
- `attacks` — Lista completa de ataques con coste, daño y efectos.
- `weaknesses` — Debilidades con tipo y multiplicador.
- `resistances` — Resistencias con tipo y valor de reducción.
- `retreatCost` — Coste de retirada.
- `isEx`, `isMega` — Indicadores de rareza especial.
- `abilities` — Habilidades con nombre, texto, tipo e indicador de activabilidad.
- `providesEnergyTypes` — Tipos de energía que provee (cartas de energía).

---

## CardDetailResponse

`CardDetailResponse` (`BE/src/main/java/.../dtos/cards/CardDetailResponse.java`) es el record que encapsula la respuesta completa:

```java
public record CardDetailResponse(
    String id,
    String name,
    String supertype,
    List<String> subtypes,
    String setCode,
    String number,
    String imageSmallUrl,
    String imageLargeUrl,
    List<String> rulesText,
    Integer hp,
    String stage,
    String evolvesFrom,
    List<String> types,
    List<AttackDto> attacks,
    List<WeaknessDto> weaknesses,
    List<ResistanceDto> resistances,
    List<String> retreatCost,
    Boolean isEx,
    Boolean isMega,
    List<CardAbilityResponse> abilities,
    List<String> providesEnergyTypes
)
```

---

## CardAbilityResponse

`CardAbilityResponse` (`BE/src/main/java/.../dtos/cards/CardAbilityResponse.java`) representa una habilidad en la respuesta detallada:

```java
public record CardAbilityResponse(
    String name,       // Nombre de la habilidad
    String text,       // Descripción textual del efecto
    String type,       // "Ability", "Pokemon Power" o "Pokemon Body"
    boolean isActivable // Indica si la habilidad es activable en el motor de juego
)
```

El campo `isActivable` se determina consultando `AbilityRegistry.has(name)` en el `CardMapper.toCardAbilityResponseList()`. Esto permite al frontend saber qué habilidades tienen implementación en el motor de juego.

---

## Modelo api_card (deprecado)

Existe un segundo modelo de entidades bajo el paquete `api_card`, todas marcadas con `@Deprecated`. Este modelo fue el original y separaba las cartas por supertipo en tablas distintas. Actualmente el modelo activo es `CardEntity` que unifica todos los supertipos en una sola tabla `cards`.

### PokemonCardEntity

`PokemonCardEntity` (`BE/src/main/java/.../repositories/entities/api_card/PokemonCardEntity.java`) — Tabla `pokemon_cards`.

Campos específicos:
- `id`, `name`, `supertype`, `subtypes`, `hp`, `pokemonTypes`, `evolvesFrom`, `retreatCost`, `convertedRetreatCost`, `isEx`, `isMega`, `rulesText`, `rarity`, `imageSmallUrl`, `imageLargeUrl`, `rawJson`, `createdAt`, `updatedAt`.

Relaciones:
- `attacks` → `List<PokemonCardAttackEntity>`.
- `weaknesses` → `List<PokemonCardWeaknessEntity>`.
- `resistances` → `List<PokemonCardResistanceEntity>`.

### TrainerCardEntity

`TrainerCardEntity` (`BE/src/main/java/.../repositories/entities/api_card/TrainerCardEntity.java`) — Tabla `trainer_cards`.

Campos: `id`, `name`, `supertype`, `subtypes`, `rulesText`, `rarity`, `imageSmallUrl`, `imageLargeUrl`, `createdAt`, `updatedAt`.

Sin ataques, debilidades ni resistencias. Solo texto de reglas.

### EnergyCardEntity

`EnergyCardEntity` (`BE/src/main/java/.../repositories/entities/api_card/EnergyCardEntity.java`) — Tabla `energy_cards`.

Campos específicos:
- `energyCardType` — `BASIC` o `SPECIAL`.
- `providesEnergyTypes` — Tipos de energía que provee.
- `id`, `name`, `supertype`, `subtypes`, `rulesText`, `imageSmallUrl`, `imageLargeUrl`, `rawJson`, `createdAt`, `updatedAt`.

Sin ataques, debilidades ni resistencias.

### PokemonCardAttackEntity

`PokemonCardAttackEntity` (`BE/src/main/java/.../repositories/entities/api_card/PokemonCardAttackEntity.java`) — Tabla `pokemon_card_attacks`.

Estructura idéntica a `CardAttackEntity` pero vinculada a `PokemonCardEntity`:
- `id` (UUID), `pokemonCard` (relación ManyToOne), `attackIndex`, `name`, `printedCost`, `convertedEnergyCost`, `damageText`, `effectText`, `effectCode`, `createdAt`.

---

## Diferencias entre CardEntity (activo) y api_card (deprecado)

| Aspecto               | CardEntity (activo)                          | api_card (deprecado)                          |
|-----------------------|---------------------------------------------|----------------------------------------------|
| **Tabla**             | `cards` (única)                             | `pokemon_cards`, `trainer_cards`, `energy_cards` (separadas) |
| **Supertipo**         | Campo `supertype` discrimina el tipo        | Cada entidad representa un supertipo         |
| **Trainer fields**    | `trainerSubtype`, `effectCode`, `isAceSpec`, `strategyKey` | No existen en TrainerCardEntity |
| **Energy fields**     | `energyCardType`, `providesEnergyTypes`, `strategyKey` | `energyCardType`, `providesEnergyTypes` (sin `strategyKey`) |
| **Pokemon fields**    | `pokemonStage`, `evolvesTo`, `abilities` (JSON), `retreatCost`, `convertedRetreatCost` | `evolvesFrom`, `retreatCost`, `convertedRetreatCost` (sin `evolvesTo`, sin `abilities` en JSON) |
| **Relaciones**        | `attacks`, `weaknesses`, `resistances` en una sola entidad | Mismas relaciones pero separadas por tabla |
| **Repositories**      | `CardJpaRepository` (único, con `JpaSpecificationExecutor`) | `PokemonCardJpaRepository`, `TrainerCardJpaRepository`, `EnergyCardJpaRepository` (sin `Specification`) |
| **Estado**            | En uso activo                                | `@Deprecated`, pendiente de eliminar         |

### Motivo del cambio

El modelo unificado (`CardEntity`) simplifica las consultas, permite búsquedas con filtros dinámicos (`Specification`) a través de un solo repositorio, y evita la complejidad de manejar tres tablas separadas con lógica duplicada. El modelo `api_card` se mantiene temporalmente por compatibilidad pero está marcado como deprecado.

---

## Estructura por tipo de carta (api_card)

### PokemonCardEntity
- Tiene `hp`, `pokemonTypes`, `evolvesFrom`, `retreatCost`, `convertedRetreatCost`, `isEx`, `isMega`.
- Contiene ataques (`PokemonCardAttackEntity`), debilidades y resistencias como colecciones.
- Ejemplo: Pikachu (ataques: Thunder Shock, coste: Lightning, daño: 30).

### TrainerCardEntity
- No tiene `hp`, ataques, debilidades ni resistencias.
- Contiene `rulesText` que describe el efecto.
- Subtipos: Supporter, Item, Stadium, Tool, ACE SPEC.
- Ejemplo: Professor Sycamore (efecto: descartar mano y robar 7).

### EnergyCardEntity
- No tiene `hp`, ataques, debilidades ni resistencias.
- Tiene `energyCardType` (`BASIC` / `SPECIAL`) y `providesEnergyTypes`.
- Ejemplo: Double Colorless Energy (provee `Colorless,Colorless`, tipo SPECIAL).
