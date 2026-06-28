# Code–Contract Alignment

**Discrepancias detectadas entre el código real de `code-integration` y los contratos en `BE/docs/contracts_ai/`.**

Cada discrepancia incluye qué dice el contrato, qué hace el código, y una o más tareas correctivas para alinear.

---

## DC-01: GameStateConverter no se usa

| | Detalle |
|---|---------|
| **Contrato** | 12 p.35: `MatchEntity` debe tener `@Convert(converter = GameStateConverter.class)` sobre el campo `state` |
| **Código hace** | Usa `MatchStateEntity` separada + ObjectMapper inline en `StatePersisterAdapter` |
| **Impacto** | La persistencia funciona, pero la arquitectura no coincide con el contrato. Dificulta mantenimiento futuro. |

### Tareas
- [X] DC-01.1 Decidir: **se actualiza el contrato** para reflejar el diseño actual con `MatchStateEntity` versionado + `ObjectMapper`.
- [ ] DC-01.2 ~~Si se refactoriza...~~ (descartado, se actualiza contrato)
- [X] DC-01.3 Actualizado Contrato 12: `MatchEntity` sin `GameStateConverter`, nueva sección `MatchStateEntity`, secciones `StatePersisterAdapter` y versioning actualizadas.

---

## DC-02: MatchLogEntity existe pero contrato dice que no

| | Detalle |
|---|---------|
| **Contrato** | 12 p.161-163: "No `MatchLogEntity` o `MatchLogJpaRepository` existen. No event log in V1" |
| **Código hace** | `MatchLogEntity` y `MatchLogJpaRepository` existen en `repositories/` |
| **Impacto** | Tabla extra en BD que el contrato prohibe explícitamente. Puede generar confusión. |

### Tareas
- [X] DC-02.1 Decidir: **eliminar** `MatchLogEntity` y `MatchLogJpaRepository` (código muerto, nunca usado).
- [X] DC-02.2 Eliminadas clases `MatchLogEntity.java` y `MatchLogJpaRepository.java`. Removido campo `logs` de `MatchEntity.java`.
- [ ] DC-02.3 ~~Si se actualiza contrato...~~ Contrato 12 ya actualizado en DC-01 (refleja que no deben existir).

---

## DC-03: DamageCalculator y EnergyRequirementValidator como clases separadas

| | Detalle |
|---|---------|
| **Contrato** | 10 p.16: "No separate `DamageCalculator`, `EnergyRequirementValidator`, or `AttackStep` classes exist in V1. They are private helper methods within `AttackResolver`." + Contrato 02 estructura: solo `AttackResolver.java` en `engine/attack/` |
| **Código hace** | `DamageCalculator.java` y `EnergyRequirementValidator.java` existen como clases públicas separadas en `engine/attack/` |
| **Impacto** | El contrato exige encapsulamiento total en AttackResolver. Las clases separadas rompen esa regla de diseño. |

### Tareas
- [X] DC-03.1 Decidir: **fusionar** ambas clases como métodos privados dentro de `AttackResolver`.
- [X] DC-03.2 Fusionadas: `checkEnergyRequirements()` y `calculateDamage()` como métodos privados estáticos en `AttackResolver`. `DamageCalcResult` como record interno. Eliminados `DamageCalculator.java` y `EnergyRequirementValidator.java`.
- [ ] DC-03.3 ~~Si se actualizan contratos...~~ (descartado, se fusionó)

---

## DC-04: ACE_SPEC_DUPLICATE falta en DeckValidationError

| | Detalle |
|---|---------|
| **Contrato** | 03 p.126-132: `DeckValidationError` enum tiene: `DECK_SIZE_INVALID, DUPLICATE_CARDS, MISSING_BASIC_POKEMON, MORE_THAN_4_COPIES, INVALID_DECK_FORMAT`. No tiene `ACE_SPEC_DUPLICATE`. |
| **Código hace** | Validación ACE_SPEC no implementada aún. El enum coincide con el contrato (ambos sin el valor). |
| **Impacto** | Para implementar PB.1 (AS TÁCTICO max 1) se necesita un error code. Si no se agrega al enum, no hay forma de reportar la violación. |

### Tareas
- [X] DC-04.1 Agregar `ACE_SPEC_DUPLICATE` al enum `DeckValidationError` (Contrato 03 y código).
- [X] DC-04.2 Actualizar Contrato 03 p.126-132 para incluir el nuevo valor.

---

## DC-05: CHOOSE_KNOCKOUT_REPLACEMENT existe pero contrato dice que no

| | Detalle |
|---|---------|
| **Contrato** | 10 p.62 (paso 12): "Replace Active if KO'd (pick first Bench Pokémon automatically — **no CHOOSE_KNOCKOUT_REPLACEMENT action in V1**)" |
| **Código hace** | `GameActionType.CHOOSE_KNOCKOUT_REPLACEMENT` existe como acción pública. `ChooseNewActiveAfterKnockoutHandler` espera que el cliente elija manualmente. `DeclareAttackHandler` no promueve automáticamente desde bench. |
| **Impacto** | El contrato exige reemplazo automático. El código obliga al cliente a enviar una acción extra. Rompe el flujo de juego previsto. |

### Tareas
- [X] DC-05.1 Modificar `DeclareAttackHandler` para que, tras detectar KO del Active oponente con bench no vacío, promueva automáticamente el primer Pokémon del bench a Active.
- [X] DC-05.2 Eliminar `CHOOSE_KNOCKOUT_REPLACEMENT` del enum `GameActionType` (Contrato 03).

---

## DC-06: ErrorApi omite `code` y asigna mal `error`

| | Detalle |
|---|---------|
| **Contrato** | 13 p.23-35: El error JSON debe tener 7 campos: `timestamp`, `status`, `error` ("Bad Request"), `code` ("INSUFFICIENT_ENERGY"), `message`, `path`, `details` |
| **Código hace** | `ErrorApi.java` no tiene campo `code`. El campo `error` se asigna con `ex.getCode()` (ej: "INSUFFICIENT_ENERGY") en vez del HTTP reason phrase (ej: "Bad Request"). |
| **Impacto** | El frontend no puede distinguir entre el código de error y el mensaje HTTP. El campo `code` simplemente no existe. |

### Tareas
- [X] DC-06.1 Agregar campo `private String code` a `ErrorApi.java`.
- [X] DC-06.2 En `GlobalExceptionHandler`, corregir: `error` debe ser el HTTP reason phrase (`HttpStatus.getReasonPhrase()`) y `code` debe ser `ex.getCode()`.
- [X] DC-06.3 Verificar que todos los `@ExceptionHandler` en `GlobalExceptionHandler` construyen `ErrorApi` con los 7 campos.

---

## DC-07: GameActionResponse.events usa objetos estructurados en vez de strings planos

| | Detalle |
|---|---------|
| **Contrato** | 08 p.58-59: "`events` is a `List<String>` of human-readable descriptions. No `GameEvent` objects. Events are inline plain strings." |
| **Código hace** | `GameActionResponse.java` tiene `List<GameEventDto>` donde `GameEventDto` es un record con `type`, `message`, `payload`. |
| **Impacto** | El frontend esperaría strings planos según el contrato. Los objetos estructurados agregan complejidad innecesaria en V1. |

### Tareas
- [X] DC-07.1 Cambiar `GameActionResponse.events` de `List<GameEventDto>` a `List<String>`.
- [X] DC-07.2 Eliminar el record anidado `GameEventDto` dentro de `GameActionResponse`.
- [X] DC-07.3 Actualizar todos los handlers que actualmente agregan `GameEventDto` para que agreguen `String` plano (`event.getMessage()` o similar).
- [X] DC-07.4 Actualizar `MatchWebSocketPublisher` si referencia `GameEventDto`.

---

## DC-08: GameEventType como objeto estructurado en vez de string plano

> **Nota:** Esta discrepancia refiere al Contrato 08. Se recomienda hablar con el encargado de los contratos para determinar si la evolución del diseño (uso de objetos tipados) debe reflejarse en una actualización del contrato o si se debe volver a strings planos.

| | Detalle |
|---|---------|
| **Contrato** | 08 p.58-59: "No `GameEvent` objects, no `GameEventType` enum. Events are inline plain strings." |
| **Código hace** | `GameEventType.java` existe con 14 valores en `engine/event/`. Todos los handlers (`DeclareAttackHandler`, `EvolvePokemonHandler`, `RetreatActiveHandler`, `TakePrizeCardHandler`, `TurnManager`) crean objetos `GameEvent` con `GameEventType`. `MatchWebSocketPublisher` envía `List<GameEvent>` al frontend. |
| **Impacto** | El frontend esperaría strings planos según el contrato. El sistema actual de eventos tipados es más rico pero incompatible con la especificación. |

### Tareas
- [X] DC-08.1 Decidir: **se actualiza el contrato** para reflejar el diseño actual con `GameEventType` y `GameEvent` internos + API expone `List<String>`.
- [ ] DC-08.2 ~~Si se migra a strings planos: eliminar `GameEventType.java`, convertir todos los handlers para que agreguen `String` en vez de `GameEvent`, cambiar `MatchWebSocketPublisher` a `List<String>`.~~ (descartado)
- [X] DC-08.3 Actualizado Contrato 08 p.58-59 para describir el diseño real.

---

## DC-09: GameActionType tiene 9 valores — Contrato 03 lista 7

> **Nota:** `CHOOSE_KNOCKOUT_REPLACEMENT` fue eliminado en DC-05, quedando 9 valores reales.

| | Detalle |
|---|---------|
| **Contrato** | 03 p.99-108: 7 valores. Contrato 09 p.57-58: "no DRAW_CARD action". |
| **Código hace** | 9 valores: agrega `DRAW_CARD` y `TAKE_PRIZE_CARD`. |
| **Impacto** | El frontend puede enviar acciones que el contrato no contempla. |

### Tareas
- [X] DC-09.1 Decidir: **se actualizan los contratos** para incluir `DRAW_CARD` y `TAKE_PRIZE_CARD`.
- [ ] DC-09.2 ~~Si se eliminan...~~ (descartado, se actualizan contratos)
- [X] DC-09.3 Actualizados Contrato 03 (GameActionType y Event types) y Contrato 09 (DRAW phase + Take Prize Card validations).

---

## DC-10: WebSocket private destination difiere del contrato

| | Detalle |
|---|---------|
| **Contrato** | 14 p.31: Private player state topic: `/user/queue/matches/{matchId}/private-state` |
| **Código hace** | `MatchWebSocketPublisher` envía a `/queue/matches/{matchId}/{playerId}` (línea 42) |
| **Impacto** | El frontend se suscribiría a un topic distinto al que el contrato especifica. No recibiría el private state. |

### Tareas
- [X] DC-10.1 Decidir: **cambiar código** para alinearse al contrato, usando `convertAndSendToUser`.
- [X] DC-10.2 `MatchWebSocketPublisher.publishPrivateState()` cambiado a `convertAndSendToUser(playerId, "/queue/matches/{matchId}/private-state", privateState)`.
- [ ] DC-10.3 ~~Si se actualiza contrato...~~ (descartado, se cambió código)

---

## DC-11: AttackResolver no implementa confusión ni between-turn pipeline

> **Nota:** No es una discrepancia estricta con un contrato específico, sino funcionalidad faltante (gap) identificada durante el análisis de código-vs-contrato.

| | Detalle |
|---|---------|
| **Contrato** | 10 p.16: AttackResolver debe manejar toda la lógica de ataque internamente. Contrato 10 p.62: debe verificar CONFUSED, otorgar ×2 prizes por EX, auto-promover Bench. |
| **Código hace** | `AttackResolver.java` (35 líneas) delega en `DamageCalculator` y `EnergyRequirementValidator`. `DeclareAttackHandler` nunca chequea CONFUSED, nunca otorga 2 prizes por EX, nunca auto-promueve desde Bench tras KO. |
| **Impacto** | Funcionalidad de ataque incompleta. El pipeline between-turn (procesar estados entre turnos) tampoco existe (cubierto por P2 task 1.3). |

### Tareas
- [X] DC-11.1 Movida lógica de `DamageCalculator` y `EnergyRequirementValidator` a métodos privados en `AttackResolver` (DC-03).
- [X] DC-11.2 Agregada verificación de CONFUSED: coin flip, tails = 3 damage counters al atacante + error "CONFUSED_SELF_HIT".
- [X] DC-11.3 Agregada lógica de ×2 prizes para Pokémon EX: `pendingPrizeCount` en `GameState`, set a 2 si `defenderDef.isEx()`.
- [X] DC-11.4 Auto-promoción desde Bench tras KO (DC-05).
- [X] DC-11.5 Implementado pipeline between-turn en `TurnManager.endTurn()` que delega a `AttackResolver.processBetweenTurnStatuses()` (procesa POISONED, BURNED, ASLEEP).

---

## Resumen

| ID | Prioridad | Estado |
|----|-----------|--------|
| DC-01 | 🟡 Media | ✅ Contrato 12 actualizado (MatchStateEntity + versioning) |
| DC-02 | 🟢 Baja | ✅ MatchLogEntity y MatchLogJpaRepository eliminados (código muerto) |
| DC-03 | 🟡 Media | ✅ Fusionado: DamageCalculator y EnergyRequirementValidator como métodos privados en AttackResolver |
| DC-04 | 🔴 Alta | ✅ ACE_SPEC_DUPLICATE agregado al enum |
| DC-05 | 🔴 Alta | ✅ Auto-promoción desde Bench + CHOOSE_KNOCKOUT_REPLACEMENT eliminado |
| DC-06 | 🔴 Alta | ✅ ErrorApi con campo code, GlobalExceptionHandler corregido |
| DC-07 | 🔴 Alta | ✅ GameActionResponse.events como List&lt;String&gt; (solo type name) |
| DC-08 | 🔴 Alta | ✅ Contrato 08 actualizado (eventos tipados internos, strings en API) |
| DC-09 | 🔴 Alta | ✅ Contratos 03 y 09 actualizados (DRAW_CARD + TAKE_PRIZE_CARD) |
| DC-10 | 🟡 Media | ✅ MatchWebSocketPublisher usa convertAndSendToUser (alineado al contrato) |
| DC-11 | 🔴 Alta | ✅ CONFUSED, ×2 prizes EX, between-turn pipeline implementados |

**Todos los DC resueltos.** Se priorizó actualizar contratos cuando el código tenía un diseño superior, y refactorizar cuando el contrato era la especificación correcta.
