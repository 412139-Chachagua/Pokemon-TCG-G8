# SPEC 3A — SUPPORTER XY1 Backend (4 cartas)

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `BE/src/.../engine/trainer/EffectType.java` | Agregar `RETURN_POKEMON_TO_DECK`, `DISCARD_OPPONENT_ENERGY` |
| `BE/src/.../engine/trainer/TrainerEffectRegistry.java` | Agregar cases en `getRequiredTargetKeys()` |
| `BE/src/.../configs/GameEngineConfig.java` | Agregar `"SHUFFLE_DRAW_5" -> 5` a shuffleDrawCounts; registrar 2 resolvers + 2 effect codes |

## Archivos a crear (2 resolvers)

| Archivo | EffectType | EffectCode | Payload |
|---------|-----------|------------|---------|
| `ReturnPokemonToDeckResolver.java` | `RETURN_POKEMON_TO_DECK` | `CASSIUS` | `targetPokemonInstanceId` |
| `DiscardOpponentEnergyResolver.java` | `DISCARD_OPPONENT_ENERGY` | `TEAM_FLARE_GRUNT` | `targetPokemonInstanceId`, `energyIndex` |

## Cards con sistema existente (solo config)

| Carta | EffectType | Config |
|-------|-----------|--------|
| **#122 Professor Sycamore** | `DISCARD_AND_DRAW` | `"DISCARD_HAND_DRAW_7"` ya registrado |
| **#127 Shauna** | `SHUFFLE_HAND_INTO_DECK` | Agregar `"SHUFFLE_DRAW_5" -> 5` a shuffleDrawCounts |

## Detalle por resolver

### 1. ReturnPokemonToDeckResolver — CASSIUS

```
SHALL buscar PokemonInPlay por targetPokemonInstanceId (Active o Bench)
SHALL recolectar todas las cartas adjuntas:
  - attachedEnergies (List<CardInstance>)
  - toolCardInstanceId (si tiene)
SHALL remover Pokemon del campo:
  - SI es Active y hay Bench:
    - DELEGAR reemplazo al KO handler existente
  - SI es el unico Pokemon en juego (no hay bench, es active):
    - NO permitir (return sin efecto, o error)
SHALL poner la carta Pokemon + adjuntos en el deck
SHALL shufflear el deck
SHALL disparar evento POKEMON_RETURNED_TO_DECK
```

### 2. DiscardOpponentEnergyResolver — TEAM_FLARE_GRUNT

```
SHALL obtener PlayerState del oponente (ctx.getOpponent(player))
SHALL buscar PokemonInPlay del oponente por targetPokemonInstanceId
SHALL validar que energyIndex este en rango de attachedEnergies
SHALL remover la energia en energyIndex del Pokemon
SHALL validar que sea Energia Basica (EnergyCardDefinition con energyCardType=BASIC)
SHALL agregar la energia al discard del oponente
SHALL disparar evento OPPONENT_ENERGY_DISCARDED
```

### 3. Professor Sycamore — DISCARD_HAND_DRAW_7 (existente)

```
Usa DiscardAndDrawResolver existente con config:
  "DISCARD_HAND_DRAW_7" -> new int[]{-1, 7}
Donde -1 significa "descartar toda la mano"
```

### 4. Shauna — SHUFFLE_DRAW_5 (existente)

```
Usa ShuffleHandIntoDeckResolver existente con config:
  "SHUFFLE_DRAW_5" -> 5
```

## getRequiredTargetKeys nuevos

```java
case RETURN_POKEMON_TO_DECK -> List.of("targetPokemonInstanceId");
case DISCARD_OPPONENT_ENERGY -> List.of("targetPokemonInstanceId", "energyIndex");
```

## GameEngineConfig registration

```java
shuffleDrawCounts.put("SHUFFLE_DRAW_5", 5);

registry.registerEffectCode("CASSIUS", EffectType.RETURN_POKEMON_TO_DECK);
registry.registerEffectCode("TEAM_FLARE_GRUNT", EffectType.DISCARD_OPPONENT_ENERGY);

registry.registerResolver(new ReturnPokemonToDeckResolver());
registry.registerResolver(new DiscardOpponentEnergyResolver());
```
