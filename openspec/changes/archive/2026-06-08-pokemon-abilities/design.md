## Context

El backend del juego de cartas Pokémon TCG tiene un motor de juego (Game Engine) que procesa acciones de los jugadores mediante un flujo: `GameEngine → RuleValidator → Handler → Events`. Actualmente soporta: poner en banca, unir energía, evolver, jugar entrenador, retirar, atacar, terminar turno.

Las cartas del set `xy1` tienen habilidades almacenadas en `CardEntity.abilities` (JSON TEXT) pero nunca se hidratan al dominio. No existe `USE_ABILITY` como acción, no hay handler, no hay resolución de efectos, y no hay tracking de uso. El spec `pokemon-abilities.md` define los requisitos pero deja vacíos el mecanismo de mapping nombre→effectType y la clasificación activa/pasiva.

Los documentos de resolución de ambigüedades definen decisiones ya tomadas: registry por nombre (no por EffectType), tracking por Pokémon instance. Las habilidades pasivas NO usan registry separado; se implementan como hooks hardcoded.

## Goals / Non-Goals

**Goals:**
- Extender el motor existente para soportar habilidades del set `xy1`.
- Seguir exactamente el patrón `TrainerEffectRegistry` para el registry de habilidades.
- Hidratar abilities desde la DB existente al dominio.
- Crear `USE_ABILITY` como nuevo `GameActionType` con handler y validaciones.
- Implementar resolvers concretos para las 12 habilidades del set `xy1`.
- Integrar habilidades pasivas como hooks en puntos existentes del engine.
- No romper handlers, resolvers, persistencia ni contratos existentes.

**Non-Goals:**
- Crear un sistema genérico de interpretación de texto de habilidades.
- Soportar habilidades de otros sets además de `xy1`.
- Crear motor de scripting o DSL para habilidades.
- Modificar el schema de base de datos.
- Crear tablas, entidades o repositorios nuevos.
- Implementar animaciones, auth, JWT, ranking, chat, Mega Evolution.

## Decisions

### D1: Registry por nombre de habilidad (no por AbilityEffectType)

**Decisión:** `AbilityRegistry` usa `Map<String, AbilityResolver>` donde la key es el nombre exacto de la habilidad.

**Alternativa considerada:** `Map<AbilityEffectType, AbilityResolver>` con un enum intermedio.

**Razón:** Las 12 habilidades del xy1 tienen comportamientos heterogéneos que no comparten lógica suficiente para justificar categorías genéricas. El mapping directo nombre→resolver es más simple, más explícito, y evita un enum intermedio que agregaría complejidad sin beneficio. Cada habilidad tiene su propia implementación independiente.

### D2: Tracking de uso por instancia de Pokémon (no por TurnFlags)

**Decisión:** `PokemonInPlay` almacena `Set<String> abilitiesUsedThisTurn`.

**Alternativa considerada:** `TurnFlags.abilitiesUsedThisTurn` centralizado.

**Razón:** El uso de una habilidad es una propiedad de una instancia específica. Si Greninja A usa Water Shuriken, Greninja B (copia en banca) debe poder usarla. El tracking por instancia soporta múltiples copias correctamente. También es consistente con otros estados del Pokémon (daño, energías, condiciones).

### D3: Habilidades pasivas como hooks distribuidos (no como sistema de eventos)

**Decisión:** Las habilidades pasivas se implementan como verificaciones directas en los puntos relevantes del engine: `AttackResolver.calculateDamage()`, `RuleValidator.validatePlayTrainer()`, y resolución de condiciones especiales.

**Alternativa considerada:** Sistema centralizado de eventos de pasivos con registro/desregistro automático.

**Razón:** Con solo 3 habilidades pasivas en el xy1, un sistema centralizado sería overengineering. Las verificaciones directas son más simples, más fáciles de razonar, y no introducen nueva abstracción. Si en el futuro se necesitan más pasivas, se puede refactorizar.

### D4: Una sola interfaz AbilityResolver (no separar activa/pasiva)

**Decisión:** `AbilityResolver` es una interfaz única. Las pasivas simplemente no se registran en el `AbilityRegistry` de USE_ABILITY; se verifican directamente en los hooks.

**Alternativa considerada:** `ActiveAbilityResolver` + `PassiveAbilityResolver` como interfaces separadas.

**Razón:** Las habilidades pasivas no se resuelven vía `USE_ABILITY`, no necesitan el mismo flujo. Mantener una sola interfaz para las activas y resolver las pasivas directamente en los hooks es más simple y no acopla innecesariamente.

### D5: Wiring en GameEngineConfig (no en constructor de GameEngine)

**Decisión:** `AbilityRegistry` se crea como bean en `GameEngineConfig` y se pasa al `GameEngine` constructor, igual que `TrainerEffectRegistry`.

**Razón:** Consistencia con el patrón existente. El `GameEngine` recibe dependencias inyectadas, no las crea internamente.

### D6: Eventos como strings descriptivos (no payloads estructurados)

**Decisión:** Los eventos de habilidad se exponen como strings descriptivos en `GameActionResponse.events[]`, consistente con el contrato V1 existente.

**Razón:** El contrato 14-websocket-contract y 10-attack-pipeline-contract establecen que "No typed event objects with structured payloads exist in V1". Internamente el engine usa `GameEvent` con payloads, pero la respuesta REST solo expone strings.

### D7: Firma de AbilityResolver

**Decisión:** `AbilityResolver` tiene firma `void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon, AbilityDefinition ability, Map<String, Object> payload)`.

**Razón:** Misma estructura que `TrainerEffectResolver.resolve()`. El resolver muta `ctx.getState()` directamente, agrega eventos con `ctx.addEvent()`, y signaliza errores con `ctx.setError()`. No retorna valor; el handler verifica `ctx.getError()` después de la ejecución.

### D8: Triggered abilities como hooks (no como AbilityResolver)

**Decisión:** Spiky Shield y Destiny Burst se implementan como hooks hardcoded en `AttackResolver` y la lógica de KO, **no** se registran en `AbilityRegistry`.

**Alternativa considerada:** Registrar triggered abilities en el AbilityRegistry con un campo `triggerType`.

**Razón:** Las triggered abilities se ejecutan automáticamente响应 a un evento del juego (daño recibido, KO), no响应 a `USE_ABILITY`. Meterlas en el registry las acoplaría innecesariamente al flujo de activación manual. Con solo 2 triggered abilities, los hooks hardcoded son más simples.

### D9: Stance Change — error si no hay Aegislash en mano

**Decisión:** Si no hay Aegislash en mano, la activación se rechaza con error `MISSING_TARGET` y **NO** se registra en `abilitiesUsedThisTurn`.

**Razón:** El jugador debe poder reintentar si obtiene Aegislash en mano después. Registrar el uso aunque falle sería injusto. Consistente con el patrón de trainers donde falta un target.

### D10: Fairy Transfer — una energía por activación, sin restricción de turno

**Decisión:** Fairy Transfer mueve exactamente 1 energía Fairy por activación. La habilidad NO tiene restricción "once during your turn", por lo que puede activarse múltiples veces.

**Razón:** Respeta el texto literal de la habilidad. Cada activación es un `USE_ABILITY` separado con su propio tracking. Si se quisiera限制ar, sería un cambio de reglas que no corresponde implementar.

## Risks / Trade-offs

- **[Mapping manual nombre→resolver]** → Cada habilidad nueva requiere crear resolver + registrar en config. Mitigación: el patrón es consistente con trainers, es el mismo trabajo que agregar un trainer effect nuevo.
- **[Hooks pasivos distribuidos]** → Agregar verificaciones en puntos existentes del engine. Mitigación: son solo 3 puntos (daño, Items, condiciones), fáciles de testear.
- **[Contratos a actualizar]** → 5 contratos necesitan cambios antes de implementar. Mitigación: se actualizan primero, antes del código.
- **[Compatibilidad con persistencia]** → `PokemonInPlay.abilitiesUsedThisTurn` se serializa automáticamente. Mitigación: es un `Set<String>` simple, Jackson lo maneja sin problemas.
- **[Habilidades no mapeadas]** → Si una carta tiene una habilidad no registrada, `AbilityRegistry.get()` retorna null y la acción se rechaza. Mitigación: error claro con `ABILITY_NOT_FOUND`.

## Affected Packages

```
cards/domain/        → AbilityDefinition, AbilityType, PokemonCardDefinition (modificar)
engine/action/       → GameActionType (modificar)
engine/model/        → PokemonInPlay (modificar)
engine/handlers/     → UseAbilityHandler (crear)
engine/ability/      → AbilityResolver, AbilityRegistry (crear)
engine/ability/resolvers/ → 12+ resolvers (crear)
engine/rules/        → RuleValidator (modificar)
engine/event/        → GameEventType (modificar)
engine/ErrorCode/    → ErrorCode (modificar)
engine/turn/         → TurnManager (modificar)
engine/ports/impl/   → CardLookupAdapter (modificar)
mappers/cards/       → CardMapper (modificar)
dtos/cards/          → CardDetailResponse (modificar)
configs/             → GameEngineConfig (modificar)
engine/GameEngine/   → GameEngine (modificar)
```

## Backend/Frontend Boundary

- **Backend:** Resuelve toda la lógica de habilidades. Valida, ejecuta efectos, persiste estado, genera eventos.
- **Frontend:** Envía `USE_ABILITY` con `pokemonInstanceId` + `abilityName`. Recibe eventos como strings. Renderiza habilidades en la vista de cartas.
- **Engine isolation:** `AbilityResolver` y `AbilityRegistry` viven en `engine/ability/`, no dependen de Spring, JPA, REST ni WebSocket. Acceden al estado vía `EngineContext`.
