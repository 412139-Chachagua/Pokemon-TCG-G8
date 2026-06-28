## Why

El backend no soporta habilidades Pokémon. Las cartas con habilidades del set `xy1` tienen datos persistidos en `CardEntity.abilities` (JSON) pero nunca se hidratan al dominio, no existe `GameActionType.USE_ABILITY`, no hay handler, no hay validación, no hay resolución de efectos, y no hay eventos WebSocket para activación de habilidades. Esto bloquea funcionalidad core del juego de cartas Pokémon.

## What Changes

- Se agrega `List<AbilityDefinition> abilities` a `PokemonCardDefinition` para modelar habilidades en el dominio.
- Se hidratan abilities desde `CardEntity.abilities` (JSON existente) al dominio vía `CardLookupAdapter`.
- Se expone `abilities` en `CardDetailResponse` para el frontend.
- Se agrega `GameActionType.USE_ABILITY` al enum de acciones del juego.
- Se crea `UseAbilityHandler` que procesa la activación de habilidades activas.
- Se crea `AbilityResolver` (interfaz Strategy) y `AbilityRegistry` (registro por nombre) siguiendo el patrón de `TrainerEffectRegistry`.
- Se implementan resolvers concretos para las 12 habilidades del set `xy1` (6 activas, 3 pasivas, 2 disparadas, 1 que requiere análisis).
- Se agrega tracking de habilidades usadas por turno en `PokemonInPlay.abilitiesUsedThisTurn`.
- Se agregan habilidades pasivas como hooks en puntos existentes del engine (cálculo de daño, validación de Items, prevención de condiciones).
- Se agregan eventos `ABILITY_USED`, `ABILITY_BLOCKED` a `GameEventType`.
- Se agregan ErrorCodes `ABILITY_NOT_FOUND`, `ABILITY_ALREADY_USED`, `POKEMON_CANNOT_USE_ABILITY`.

## Capabilities

### New Capabilities

- `pokemon-abilities-domain`: Modelado de habilidades en el dominio (AbilityDefinition, AbilityType, hidratación desde DB, exposición REST).
- `pokemon-abilities-action`: Sistema de activación de habilidades (USE_ABILITY, UseAbilityHandler, validaciones en RuleValidator, eventos).
- `pokemon-abilities-registry`: Sistema de resolución de efectos (AbilityResolver, AbilityRegistry, wiring en GameEngineConfig).
- `pokemon-abilities-xy1`: Implementación concreta de las 12 habilidades del set xy1 (resolvers activos, hooks pasivos, hooks de disparo).
- `pokemon-abilities-tracking`: Tracking de habilidades usadas por turno y reseteo en TurnManager.

### Modified Capabilities

- `game-state`: Se agrega `abilitiesUsedThisTurn` a `PokemonInPlay` y se agrega limpieza en `TurnManager.startTurn()`.
- `enums`: Se agrega `USE_ABILITY` a `GameActionType`, `ABILITY_USED`/`ABILITY_BLOCKED` a `GameEventType`, nuevos `ErrorCode`.

## Impact

- **Archivos a modificar**: `PokemonCardDefinition`, `PokemonInPlay`, `TurnFlags` (implícito vía PokemonInPlay), `TurnManager`, `GameActionType`, `GameEventType`, `ErrorCode`, `RuleValidator`, `GameEngine`, `GameEngineConfig`, `CardLookupAdapter`, `CardMapper`, `CardDetailResponse`.
- **Archivos a crear**: `AbilityDefinition`, `AbilityType`, `AbilityResolver`, `AbilityRegistry`, `UseAbilityHandler`, 12+ resolvers de habilidades, hooks pasivos en `AttackResolver` y `RuleValidator`.
- **APIs**: Se agrega soporte para `USE_ABILITY` en `POST /api/matches/{matchId}/actions`. Se exponde `abilities` en `GET /api/cards/{cardId}`.
- **Contratos a actualizar**: `03-enums-contract.md`, `04-card-model-contract.md`, `06-game-state-contract.md`, `08-game-action-contract.md`, `09-rule-validation-contract.md`.
- **No se modifica**: schema de base de datos, persistencia de estado, handlers existentes, resolvers de trainers, flujo de turnos existente.

## Ambigüedades Críticas

### A1: Firma de AbilityResolver

La interfaz `AbilityResolver` no está definida con precisión. Se requiere especificar:

- **Inputs:** `EngineContext ctx`, `PlayerState player`, `PokemonInPlay pokemon`, `AbilityDefinition ability`, `Map<String, Object> payload`
- **Output:** `void` — el resolver muta `ctx.getState()` directamente y agrega eventos con `ctx.addEvent()`, igual que los handlers y resolvers de trainers existentes.
- **Comunicación de resultado:** El handler verifica `ctx.getError()` después de la ejecución. Si el resolver setea un error, el handler lo propaga. Si no, el handler registra la habilidad en `abilitiesUsedThisTurn` y emite `ABILITY_USED`.

**Referencia:** `TrainerEffectResolver.resolve()` usa exactamente este patrón: `void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload)`.

### A2: Parámetros de los resolvers

Los resolvers acceden al estado del juego vía `EngineContext` directamente (mismo patrón que `TrainerEffectResolver`). No hay wrapper nuevo. `EngineContext` expone:

- `state` (GameState mutable)
- `cardLookup` (CardLookupPort para resolver cartas)
- `randomizer` (RandomizerPort para lanzamientos de moneda)
- `addEvent(GameEvent)` para acumular eventos
- `setError(GameError)` para signalizar errores
- `getPlayer(UUID)`, `getOpponent(UUID)` para acceder a PlayerState

### A3: Schema del JSON en CardEntity.abilities

El JSON en `CardEntity.abilities` tiene esta estructura real (de la API pokemontcg.io):

```json
[
  {
    "name": "Water Shuriken",
    "text": "Once during your turn, you may discard a Water Energy card from your hand. If you do, put 3 damage counters on 1 of your opponent's Pokémon.",
    "type": "Ability"
  }
]
```

El campo `type` del API usa strings: `"Ability"`, `"Poké-Power"`, `"Poké-Body"`. Se mapea a `AbilityType` enum: `ABILITY`, `POKEMON_POWER`, `POKEMON_BODY`.

**Nota:** El tipo del API indica cómo se activa, no qué efecto produce. La clasificación activa/pasiva/disparada se determina por el nombre de la habilidad (mapeo estático en el registry), no por este campo.

### A4: Comportamiento de triggered abilities (Spiky Shield, Destiny Burst)

Las habilidades disparadas **no usan `AbilityResolver`** del registry. Se implementan como **hooks hardcoded** en los mismos puntos del engine que las pasivas:

- **Spiky Shield:** Hook en la resolución de daño (después de aplicar daño al Pokémon con Spiky Shield, poner 3 counters al atacante).
- **Destiny Burst:** Hook en la lógica de KO (al KO por ataque, lanzar moneda; si heads, poner 5 counters al atacante).

**Diferencia con pasivas:** Las pasivas modifican el cálculo *antes* del resultado (Fur Coat reduce daño, Sweet Veil bloquea condición). Las disparadas se ejecutan *después* de un evento (daño recibido, KO). Ambas se registran en los mismos puntos del engine, no en `AbilityRegistry`.

### A5: Shape del request USE_ABILITY

```json
{
  "type": "USE_ABILITY",
  "playerId": "player-1",
  "payload": {
    "pokemonInstanceId": "uuid-del-pokemon",
    "abilityName": "Water Shuriken"
  },
  "clientRequestId": "client-req-001"
}
```

Para habilidades que requieren selección adicional (Fairy Transfer, Drive Off), el payload incluye campos extra:

```json
// Fairy Transfer
{
  "pokemonInstanceId": "uuid-aromatisse",
  "abilityName": "Fairy Transfer",
  "sourceEnergyInstanceId": "uuid-energia",
  "targetPokemonInstanceId": "uuid-destino"
}

// Drive Off
{
  "pokemonInstanceId": "uuid-swellow",
  "abilityName": "Drive Off",
  "targetPokemonInstanceId": "uuid-destino-oponente"
}
```

### A6: Selección de objetivo del jugador

Cuando una habilidad requiere selección (Drive Off, Fairy Transfer, Water Shuriken target), el `payload` del request incluye los IDs necesarios. El resolver extraelos del payload. Si falta un campo requerido, el resolver setea `ctx.setError()` con `MISSING_TARGET`.

### A7: String exacto de eventos

Consistente con el patrón existente (strings descriptivos como `"Slugma dealt 30 damage to Froakie."`):

- `ABILITY_USED`: `"Greninja used Water Shuriken."`
- `ABILITY_BLOCKED`: `"Greninja cannot use Water Shuriken: already used this turn."`
- `ABILITY_USED` (pasiva aplicada): `"Fur Coat reduced damage by 20."`

### A8: TurnManager.startTurn() — alcance de limpieza

Se limpian SOLO los Pokémon actualmente en campo del jugador activo: `activePokemon` + `bench[]`. Si un Pokémon fue KOed en el turno anterior, ya no está en el campo y su `abilitiesUsedThisTurn` se descartó con la instancia (R8 del tracking spec).

### A9: Habilidades pasivas — no hay PassiveAbilityRegistry

El design.md y los documentos de ambigüedades mencionan un "PassiveAbilityRegistry". **Esto no se implementa.** Las habilidades pasivas y disparadas se resuelven como hooks hardcoded en los puntos relevantes del engine (AttackResolver, RuleValidator, lógica de KO). La razón: con solo 3 pasivas + 2 disparadas en el xy1, un registry dedicado es overengineering. Si en el futuro se necesitan más, se puede refactorizar.

### A10: Stance Change — comportamiento con error

Si no hay Aegislash en mano, la activación se rechaza con error `MISSING_TARGET` y **NO** se registra en `abilitiesUsedThisTurn` (el jugador puede reintentar). Si hay Aegislash en mano, el Aegislash actual del campo se descarta y se pone el de mano en su lugar.

### A11: Fairy Transfer — una energía por activación

Fairy Transfer mueve exactamente 1 energía Fairy por resolución. La habilidad **no** tiene restricción "once during your turn", por lo que puede activarse múltiples veces en el mismo turno (cada activación es un `USE_ABILITY` separado con su propio tracking). El payload incluye `sourceEnergyInstanceId` y `targetPokemonInstanceId`.
