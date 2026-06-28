# SPEC 1A — ITEMS XY1 Backend (7 cartas)

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `BE/src/.../repositories/entities/CardEntity.java` | Agregar `@Column(name = "effect_code") private String effectCode` |
| `BE/src/.../mappers/cards/CardMapper.java` | En seccion TRAINER, agregar `entity.setEffectCode(generateTrainerEffectCode(request.name()))` con switch por nombre |
| `BE/src/.../engine/ports/impl/CardLookupAdapter.java` | En `toTrainer()`, agregar `d.setEffectCode(e.getEffectCode())` |
| `BE/src/.../engine/trainer/EffectType.java` | Agregar: `EVOLVE_DIRECT`, `LOOK_TOP_SEARCH`, `REVIVE_TO_DECK`, `SEARCH_ENERGY_TO_HAND`, `OPPONENT_SHUFFLE_HAND_DRAW`, `COIN_FLIP_DRAW`, `HEAL_WITH_DISCARD` |
| `BE/src/.../engine/trainer/TrainerEffectRegistry.java` | Agregar cases en `getRequiredTargetKeys()` |
| `BE/src/.../configs/GameEngineConfig.java` | Registrar 7 resolvers + 7 effect codes |

## Archivos a crear (7 resolvers en `BE/src/.../engine/trainer/resolvers/`)

| Archivo | EffectType | EffectCode | Payload |
|---------|-----------|------------|---------|
| `EvolveDirectResolver.java` | `EVOLVE_DIRECT` | `EVOSODA` | `targetPokemonInstanceId`, `targetCardIndex` |
| `LookTopSearchResolver.java` | `LOOK_TOP_SEARCH` | `GREAT_BALL` | `targetCardIndex` |
| `ReviveToDeckResolver.java` | `REVIVE_TO_DECK` | `MAX_REVIVE` | `targetCardIndex` |
| `SearchEnergyToHandResolver.java` | `SEARCH_ENERGY_TO_HAND` | `PROFESSORS_LETTER` | `targetCardIndexes` |
| `RedCardResolver.java` | `OPPONENT_SHUFFLE_HAND_DRAW` | `RED_CARD` | — |
| `CoinFlipDrawResolver.java` | `COIN_FLIP_DRAW` | `COIN_FLIP_DRAW_3` | — |
| `HealWithDiscardResolver.java` | `HEAL_WITH_DISCARD` | `HEAL_60_DISCARD_1` | `targetPokemonInstanceId`, `energyIndex` |

## Detalle por resolver

### 1. EvolveDirectResolver — EVOSODA

```
SHALL buscar PokemonInPlay por targetPokemonInstanceId
SHALL lookup carta en deck por targetCardIndex
SHALL validar que sea PokemonCardDefinition con evolvesFrom = nombre del target
SHALL remover del deck
SHALL actualizar cardDefinitionId del Pokemon target
SHALL setear evolvedThisTurn = true
SHALL limpiar specialConditions
SHALL shufflear deck
SHALL disparar evento POKEMON_EVOLVED
```

### 2. LookTopSearchResolver — GREAT_BALL

```
SHALL tomar primeras 7 cartas del deck (top 7)
SHALL validar que targetCardIndex este entre 0 y 6
SHALL validar que la carta seleccionada sea PokemonCardDefinition
SHALL remover SOLO esa carta del deck
SHALL agregar a mano
SHALL shufflear las 6 restantes de vuelta al deck
SHALL disparar evento POKEMON_SEARCHED
```

### 3. ReviveToDeckResolver — MAX_REVIVE

```
SHALL buscar PokemonCardDefinition en discard por targetCardIndex
SHALL remover del discard
SHALL insertar en posicion 0 del deck (tope)
SHALL NO shufflear (especificacion de la carta: pone al tope)
SHALL disparar evento POKEMON_REVIVED
```

### 4. SearchEnergyToHandResolver — PROFESSORS_LETTER

```
SHALL iterar el deck buscando EnergyCardDefinition con energyCardType = BASIC
SHALL seleccionar hasta 2 (segun targetCardIndexes)
SHALL removerlas del deck
SHALL agregar a mano
SHALL shufflear el deck
SHALL disparar evento ENERGY_SEARCHED
```

### 5. RedCardResolver — RED_CARD

```
SHALL obtener PlayerState del oponente (NO el jugador activo)
SHALL mover TODAS las cartas de la mano del oponente a su deck
SHALL shufflear el deck del oponente
SHALL robar 4 cartas para el oponente
SHALL disparar evento OPPONENT_HAND_SHUFFLED
```

### 6. CoinFlipDrawResolver — ROLLER_SKATES

```
SHALL tirar moneda via ctx.getRandomizer().flipCoin()
SHALL si sale cara: robar 3 cartas del deck a la mano
SHALL si sale cruz: no hacer nada
SHALL disparar COIN_FLIP_RESULT
SHALL si cara, disparar CARDS_DRAWN
```

### 7. HealWithDiscardResolver — SUPER_POTION

```
SHALL buscar PokemonInPlay por targetPokemonInstanceId
SHALL curar hasta 60 (6 contadores de dano)
SHALL si se curo al menos 1 contador:
  SHALL remover energia adjunta por energyIndex
  SHALL agregar energia al discard del jugador
SHALL disparar POKEMON_HEALED
SHALL disparar ENERGY_DISCARDED
```

## getRequiredTargetKeys nuevos

```java
case EVOLVE_DIRECT -> List.of("targetPokemonInstanceId", "targetCardIndex");
case LOOK_TOP_SEARCH -> List.of("targetCardIndex");
case REVIVE_TO_DECK -> List.of("targetCardIndex");
case SEARCH_ENERGY_TO_HAND -> List.of("targetCardIndexes");
case OPPONENT_SHUFFLE_HAND_DRAW -> List.of();
case COIN_FLIP_DRAW -> List.of();
case HEAL_WITH_DISCARD -> List.of("targetPokemonInstanceId", "energyIndex");
```

## GameEngineConfig registration

```java
registry.registerEffectCode("EVOSODA", EffectType.EVOLVE_DIRECT);
registry.registerEffectCode("GREAT_BALL", EffectType.LOOK_TOP_SEARCH);
registry.registerEffectCode("MAX_REVIVE", EffectType.REVIVE_TO_DECK);
registry.registerEffectCode("PROFESSORS_LETTER", EffectType.SEARCH_ENERGY_TO_HAND);
registry.registerEffectCode("RED_CARD", EffectType.OPPONENT_SHUFFLE_HAND_DRAW);
registry.registerEffectCode("COIN_FLIP_DRAW_3", EffectType.COIN_FLIP_DRAW);
registry.registerEffectCode("HEAL_60_DISCARD_1", EffectType.HEAL_WITH_DISCARD);

registry.registerResolver(new EvolveDirectResolver());
registry.registerResolver(new LookTopSearchResolver());
registry.registerResolver(new ReviveToDeckResolver());
registry.registerResolver(new SearchEnergyToHandResolver());
registry.registerResolver(new RedCardResolver());
registry.registerResolver(new CoinFlipDrawResolver());
registry.registerResolver(new HealWithDiscardResolver());
```

## CardMapper — generateTrainerEffectCode

```java
private String generateTrainerEffectCode(String name, List<String> rules) {
    if (name == null) return null;
    return switch (name) {
        case "Evosoda" -> "EVOSODA";
        case "Great Ball" -> "GREAT_BALL";
        case "Max Revive" -> "MAX_REVIVE";
        case "Professor's Letter" -> "PROFESSORS_LETTER";
        case "Red Card" -> "RED_CARD";
        case "Roller Skates" -> "COIN_FLIP_DRAW_3";
        case "Super Potion" -> "HEAL_60_DISCARD_1";
        case "Cassius" -> "CASSIUS";
        case "Professor Sycamore" -> "DISCARD_HAND_DRAW_7";
        case "Shauna" -> "SHUFFLE_DRAW_5";
        case "Team Flare Grunt" -> "TEAM_FLARE_GRUNT";
        case "Fairy Garden" -> "FAIRY_GARDEN";
        case "Shadow Circle" -> "SHADOW_CIRCLE";
        default -> null;
    };
}
```
