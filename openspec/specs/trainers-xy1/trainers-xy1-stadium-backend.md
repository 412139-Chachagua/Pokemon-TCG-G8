# SPEC 2A — STADIUM XY1 Backend (2 cartas)

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `BE/src/.../configs/GameEngineConfig.java` | Registrar `"FAIRY_GARDEN"` y `"SHADOW_CIRCLE"` -> `EffectType.STADIUM_PLAY` |
| `BE/src/.../engine/handlers/RetreatActiveHandler.java` | Consultar estadio activo para efecto Fairy Garden |
| `BE/src/.../engine/attack/DamageCalculator.java` | Consultar estadio activo para efecto Shadow Circle |
| `BE/src/.../engine/attack/steps/DamageStep.java` | Pasar informacion del estadio al calculo de debilidad |

## Resolver existente

Ambas cartas usan `StadiumPlayResolver` existente (`STADIUM_PLAY`). No requiere nuevos resolvers.

El `PlayTrainerHandler` ya maneja STADIUM correctamente:
- marca `hasPlayedStadium`
- NO descarta la carta (stadium se queda en juego)
- El `StadiumPlayResolver` setea `state.stadiumCardInstanceId`

## Efectos continuos

### #117 Fairy Garden — EffectCode: `FAIRY_GARDEN`

**Efecto:** Cada Pokemon que tenga alguna Energia Hada (Fairy) adjunta no tiene costo de retirada.

```
SHALL en RetreatActiveHandler (calculo de costo de retirada):
  SHALL si state.stadiumCardInstanceId != null:
    SHALL lookup de la carta de estadio por instanceId via cardLookup
    SHALL si cardDef.effectCode == "FAIRY_GARDEN":
      SHALL para el Pokemon que se retira:
        SHALL si tiene alguna energia de tipo FAIRY adjunta:
          SHALL retreatCost = lista vacia (costo 0)
```

### #126 Shadow Circle — EffectCode: `SHADOW_CIRCLE`

**Efecto:** Los Pokemon Oscuridad (Darkness) de ambos jugadores no tienen debilidad.

```
SHALL en DamageCalculator / DamageStep (calculo de debilidad):
  SHALL si state.stadiumCardInstanceId != null:
    SHALL lookup de la carta de estadio por instanceId via cardLookup
    SHALL si cardDef.effectCode == "SHADOW_CIRCLE":
      SHALL si el defending Pokemon tiene tipo DARKNESS:
        SHALL weaknessMultiplier = 1 (sin debilidad)
```

## GameEngineConfig

```java
registry.registerEffectCode("FAIRY_GARDEN", EffectType.STADIUM_PLAY);
registry.registerEffectCode("SHADOW_CIRCLE", EffectType.STADIUM_PLAY);
```

No requiere nuevos EffectTypes ni nuevos resolvers.
