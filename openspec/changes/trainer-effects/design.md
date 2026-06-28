## Context

`PlayTrainerHandler` es actualmente un MVP que mueve la carta de la mano al descarte y actualiza flags (`hasPlayedSupporter`, `hasPlayedStadium`), sin ejecutar efecto alguno. El modelo `TrainerCardDefinition` ya tiene un campo `effectCode: String` pero no existe infraestructura para interpretarlo.

El set `xy1` contiene ~40 cartas de Entrenador que cubren: robo de cartas, curación, búsqueda en mazo, cambio de Pokémon, Estadios y Herramientas. Cada carta de Entrenador tiene un `effectCode` que identifica su comportamiento.

El Game Engine está aislado de Spring/JPA vía puertos. Cualquier nuevo componente debe mantener ese aislamiento: los resolvers de efectos deben ser POJOs inyectados en el engine.

## Goals / Non-Goals

**Goals:**
- Sistema extensible de resolución de efectos basado en `TrainerCardDefinition.effectCode`
- 14 resolvers concretos cubriendo las cartas de Entrenador del set `xy1`
- Zona de Estadio compartida en `GameState.stadiumCardInstanceId`
- Equipamiento de Herramientas Pokémon con nuevo `GameActionType.ATTACH_TOOL`
- Soporte para selección de targets desde el payload de `GameAction`
- Nuevos eventos WebSocket para efectos resueltos

**Non-Goals:**
- No se modifican DTOs de API REST
- No se modifican entidades JPA
- No se implementan efectos de cartas de Entrenador de expansiones posteriores a `xy1`
- No se implementa interacción de frontend (drag & drop de entrenadores)

## Decisions

### D1 — Efecto identificado por `effectCode` (String), no por enum

Si bien existe `TrainerSubtype` (ITEM, SUPPORTER, STADIUM, ACE_SPEC), el comportamiento concreto se define por `effectCode` (ej. `"DRAW_7"`, `"HEAL_20"`, `"SEARCH_BASIC"`). Se usará un `Map<String, EffectType>` para mapear effectCode → EffectType, y un `Map<EffectType, TrainerEffectResolver>` para la ejecución.

**Alternativa:** usar solo `TrainerSubtype`. Descartada porque no distingue entre Prof. Ciprés (roba 7) e Investigadora (roba 6), que comparten subtipo SUPPORTER.

### D2 — Strategy pattern para resolvers

Cada `EffectType` tiene su propio resolver implementando `TrainerEffectResolver`. Los resolvers se registran en `TrainerEffectRegistry`, que expone `resolve(EngineContext, PlayerState, TrainerCardDefinition, Map<String, Object>)`.

**Alternativa:** switch gigante en `PlayTrainerHandler`. Descartada por violar Open/Closed y dificultar testing.

### D3 — Estadio se almacena en `GameState.stadiumCardInstanceId`

El campo ya existe en el modelo. El resolver `StadiumPlayResolver` asigna el `cardInstanceId` de la carta de Estadio a este campo. Si ya había un Estadio, se descarta (se mueve al discard del dueño anterior).

### D4 — Herramientas como nuevo `GameActionType.ATTACH_TOOL`

Se crea un nuevo tipo de acción y handler separado. La validación verifica que el Pokémon objetivo no tenga ya `toolCardInstanceId` asignado. `PokemonInPlay.toolCardInstanceId` ya existe en el modelo.

**Alternativa:** tratar Herramientas como un `PLAY_TRAINER` con target. Descartada porque la acción de equipar una Herramienta es semánticamente distinta (no se descarta, permanece unida).

### D5 — Target selection vía `GameAction.payload`

Los efectos que requieren selección (curar un Pokémon, buscar en mazo, etc.) reciben los targets como claves en el payload del `GameAction`. Los resolvers validan que los targets existan y sean válidos antes de ejecutar.

Payload esperado:
```json
{
  "handIndex": 0,
  "targetPokemonInstanceId": "uuid-del-pokemon",
  "targetPlayerId": "uuid-del-jugador",
  "targetCardIndex": 2
}
```

### D6 — Los resolvers son POJOs sin dependencias Spring

Para mantener el aislamiento del Game Engine, `TrainerEffectResolver` y sus implementaciones son clases planas que reciben `EngineContext`. El engine las instancia y registra en `GameEngineConfig`.

## Risks / Trade-offs

- [Riesgo] Cobertura incompleta de `effectCode` → Mitigación: validar en `RuleValidator` que el effectCode sea conocido y soportado; rechazar cartas con effectCode no implementado con error claro
- [Riesgo] Efectos con selección de target malformada → Mitigación: validar targets requeridos en `RuleValidator` antes de ejecutar el resolver
- [Trade-off] No se implementan efectos complejos como "elige entre robar 2 o descartar 1 Energía del rival" en esta iteración — solo se implementan los efectos que aparecen en cartas de `xy1`
