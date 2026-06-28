# High Priority Rule Fixes

Corrección de reglas oficiales incompletas categorizadas como impacto **🟠 Alto** en el informe de validaciones.

**Referencia:** `docs/informe-validaciones-faltantes-backend.md` — Detalles 14, 26, 42, 59, 65, 67, 70.

---

## Background / Context

| # | Aspecto | Estado | Archivo clave | Detalle |
|---|---------|--------|---------------|---------|
| 14 | Evolución permitida en primer turno del jugador | ❌ | `RuleValidator.java` | Usa `turnNumber == 1` global, no por jugador |
| 26 | Confusión deja estado inconsistente | ❌ | `AttackResolver.java`, `DeclareAttackHandler.java` | Daño aplicado pero acción reporta error |
| 42 | Reemplazo de KO es automático | ❌ | `DeclareAttackHandler.java` | No existe `CHOOSE_KO_REPLACEMENT` |
| 59 | Muerte Súbita no implementada | ❌ | `VictoryConditionChecker.java` | Detecta simultáneo pero no inicia nueva partida |
| 65 | Energía Especial no validada correctamente | ❌ | `DeckValidator.java` | No distingue Básica vs Especial para límite de 4 |
| 67 | AS TÁCTICO: máximo 1 por mazo | ❌ | `DeckValidator.java` | Idem detalle 77 (cubierto en spec crítico) |
| 70 | Estadio no permanece en juego | ❌ | `PlayTrainerHandler.java` | Se descarta inmediatamente |

---

## Requirements

### R1 — Item 14: Evolución en primer turno del jugador

**Problema:** `RuleValidator.java` verifica `gameState.getTurnNumber() == 1` para prohibir evolución, pero la regla oficial dice *"Ningún jugador puede hacer evolucionar en **su** primer turno"*. Cuando el jugador 2 está en su primer turno, `turnNumber` global ya es 2, y la evolución se permite incorrectamente.

**Solución:**

- SHALL agregar en `GameState` un flag por jugador: `Map<UUID, Boolean> firstTurnCompleted` o `Set<UUID> playersWhoCompletedFirstTurn`
- SHALL settear `firstTurnCompleted` para el jugador actual al finalizar su primer turno (en `EndTurnHandler` o `TurnManager.endTurn()`)
- SHALL modificar `RuleValidator.validate(EVOLVE_POKEMON)` para verificar:

```java
// Actual (incorrecto):
if (gameState.getTurnNumber() == 1) { throw ... }

// Nuevo:
if (!gameState.hasPlayerCompletedFirstTurn(playerId)) {
    throw new GameEngineException(ErrorCode.EVOLVE_NOT_ALLOWED);
}
```

- SHALL aplicar la misma lógica a `DECLARE_ATTACK` (validar que no sea primer turno del jugador) — ya existe pero verificar consistencia

**Archivos afectados:**
- MODIFY `BE/.../engine/model/GameState.java` — agregar `Set<UUID> playersWhoCompletedFirstTurn` o similar
- MODIFY `BE/.../engine/rules/RuleValidator.java` — cambiar condición
- MODIFY `BE/.../engine/turn/TurnManager.java` — marcar completed al finalizar primer turno
- MODIFY `BE/.../engine/handlers/EndTurnHandler.java` — invocar marca de primer turno completado

---

### R2 — Item 26: Confusión — estado inconsistente

**Problema:** `AttackResolver.resolve()` (líneas 47-54) aplica 3 contadores de daño al Pokémon confundido cuando sale cruz, retorna `confusedSelfHit=true`. Luego `DeclareAttackHandler` (líneas 51-83) detecta el flag, **no descarta el daño ya aplicado**, setea error y retorna sin avanzar fase. El daño queda aplicado pero el ataque no se consumió.

**Solución:**

Opción recomendada — **no aplicar daño en AttackResolver**, delegar toda la mutación a `DeclareAttackHandler`:

- SHALL modificar `AttackResolver.resolve()` para que **no** mute el estado del Pokémon confundido. En lugar de `attacker.setDamageCounters(...)`, retornar solo la información del autogolpe en `AttackResolutionResult` sin aplicar el daño.
- SHALL modificar `DeclareAttackHandler` para que, si `confusedSelfHit=true`:
  1. Aplicar los 3 contadores de daño al atacante
  2. Verificar KO del atacante (igual que la lógica actual en líneas 52-76, pero sin entrar en modo error)
  3. Si el atacante no queda KO, setear `hasAttacked = true` y avanzar fase (el ataque se consumió, el Pokémon se golpeó a sí mismo)
  4. Publicar eventos `CONFUSION_SELF_HIT` y `DAMAGE_APPLIED`
  5. **No** reportar error — la acción fue exitosa (el Pokémon atacó pero se golpeó a sí mismo)

```java
// En DeclareAttackHandler, reemplazar el bloque actual de confusedSelfHit:
if (result.confusedSelfHit()) {
    attacker.setDamageCounters(attacker.getDamageCounters() + result.selfDamageCounters());
    // verificar KO del atacante...
    state.getTurnFlags().setHasAttacked(true);
    turnManager.advancePhase(state);
    return; // acción exitosa, no error
}
```

**Archivos afectados:**
- MODIFY `BE/.../engine/attack/AttackResolver.java` — no mutar estado en confused, solo informar
- MODIFY `BE/.../engine/handlers/DeclareAttackHandler.java` — tratar confused como acción exitosa

---

### R3 — Item 42: Reemplazo de KO por elección del jugador

**Problema:** Cuando un Pokémon Activo es derrotado, `DeclareAttackHandler` selecciona automáticamente `getBench().get(0)`. No hay mecanismo para que el jugador elija el reemplazo.

**Solución:**

- SHALL crear `GameActionType.CHOOSE_KO_REPLACEMENT` en el enum
- SHALL crear `ChooseKOReplacementHandler` que procese la selección del jugador
- SHALL modificar `DeclareAttackHandler` para que, al detectar KO del Activo rival:
  1. Marcar estado `pendingKOReplacement` en `GameState` con `knockedOutPlayerId` y `pendingReplacement = true`
  2. **No** seleccionar reemplazo automáticamente
  3. Publicar evento `KO_REPLACEMENT_REQUIRED` con la lista de candidatos (bench del jugador afectado)
  4. La fase NO avanza hasta que el jugador afectado envíe `CHOOSE_KO_REPLACEMENT`
- SHALL modificar `RuleValidator` para permitir `CHOOSE_KO_REPLACEMENT` solo cuando `pendingReplacement == true`
- SHALL integrar con el flujo existente: después de elegir reemplazo, verificar si el jugador se queda sin Pokémon (derrota)

```java
// En GameState, agregar:
private boolean pendingKOReplacement;
private UUID knockedOutPlayerId;

// En RuleValidator:
case CHOOSE_KO_REPLACEMENT -> validateKOReplacement(action, state);
```

**Archivos afectados:**
- MODIFY `BE/.../engine/action/GameActionType.java` — agregar `CHOOSE_KO_REPLACEMENT`
- MODIFY `BE/.../engine/model/GameState.java` — agregar flags de pending replacement
- MODIFY `BE/.../engine/handlers/DeclareAttackHandler.java` — no auto-reemplazar, delegar
- MODIFY `BE/.../engine/rules/RuleValidator.java` — validar CHOOSE_KO_REPLACEMENT
- CREATE `BE/.../engine/handlers/ChooseKOReplacementHandler.java`
- MODIFY `BE/.../engine/GameEngine.java` — registrar nuevo handler

---

### R4 — Item 59: Muerte Súbita

**Problema:** `VictoryConditionChecker` detecta victoria simultánea y setea `suddenDeath = true`, pero no existe flujo para iniciar una nueva partida con 1 carta de Premio.

**Solución:**

- SHALL crear `GameActionType.START_SUDDEN_DEATH` o un mecanismo automático
- SHALL modificar el flujo post-`VictoryConditionChecker.check()`: si `suddenDeath == true`, no finalizar la partida sino reiniciar el estado de juego con:
  - Mismos jugadores, mismos mazos (restaurados al estado completo)
  - 1 carta de Premio cada uno (no 6)
  - Manos repartidas de nuevo (7 cartas)
  - Moneda para decidir quién empieza (el perdedor de la moneda inicial, o nueva moneda)
- SHALL agregar flag `isSuddenDeath` a `GameState` para que el setup use 1 prize en lugar de 6
- SHALL publicar evento `SUDDEN_DEATH_STARTED` notificando a ambos jugadores
- SHALL reutilizar `SetupManager` o `MatchApplicationService` para el reseteo parcial

```java
// En GameState, agregar:
private boolean suddenDeath;
private int prizeCountPerPlayer; // 6 normal, 1 sudden death

// En VictoryConditionChecker.check():
if (suddenDeathDetected) {
    return new VictoryCheckResult(false, null, null, true); // suddenDeath = true, no hay ganador aún
}
```

**Archivos afectados:**
- MODIFY `BE/.../engine/model/GameState.java` — agregar `suddenDeath`, `prizeCountPerPlayer`
- MODIFY `BE/.../engine/victory/VictoryConditionChecker.java` — retornar suddenDeath sin finalizar
- MODIFY `BE/.../engine/setup/SetupManager.java` — aceptar `prizeCount` parametrizado
- MODIFY `BE/.../engine/GameEngine.java` — manejar transición a sudden death
- MODIFY `BE/.../services/matches/MatchApplicationService.java` — manejar reset parcial

---

### R5 — Item 70: Estadio permanece en juego

**Problema:** `PlayTrainerHandler` mueve las cartas de Estadio al descarte inmediatamente. La regla oficial dice que los Estadios permanecen en una zona compartida hasta ser reemplazados o removidos por efecto.

**Nota:** ya existe `GameState.stadiumCardInstanceId` y `StadiumPlayResolver` en el sistema de efectos de entrenador. Este item asegura que el ciclo de vida completo del Estadio esté correcto.

**Solución:**

- SHALL verificar que `StadiumPlayResolver` (ya creado en trainer-effects):
  - Coloca la carta en `GameState.stadiumCardInstanceId`
  - Mueve el Estadio anterior al descarte de su dueño al reemplazarlo
  - Publica `STADIUM_PLAYED` al colocar y `STADIUM_REMOVED` al reemplazar
- SHALL asegurar que el Estadio no se descarte al jugarlo (solo al ser reemplazado)
- SHALL verificar que `TurnManager.startTurn()` **no** remueva el Estadio (persiste entre turnos)
- SHALL agregar lógica para remover el Estadio si una carta específica lo indica (ej: efecto de `PLAY_TRAINER` con `effectCode = "REMOVE_STADIUM"`)
- SHALL agregar en `RuleValidator` validación de que solo puede haber 1 Estadio a la vez (ya existe `hasPlayedStadium` flag)

```java
// Comportamiento esperado en StadiumPlayResolver:
1. Si state.stadiumCardInstanceId != null:
   a. Mover el Estadio actual al discard de su dueño
   b. Publicar STADIUM_REMOVED
2. Asignar nueva carta a state.stadiumCardInstanceId
3. Publicar STADIUM_PLAYED
4. No descartar la carta del entrenador (permanece en juego)
```

**Archivos afectados:**
- VERIFY `StadiumPlayResolver.java` — ya creado en trainer-effects, verificar comportamiento
- MODIFY `BE/.../engine/handlers/PlayTrainerHandler.java` — asegurar que Estadio no se descarta
- MODIFY `BE/.../engine/model/GameState.java` — verificar que `stadiumCardInstanceId` se persiste y resetea correctamente

---

### R6 — Item 65: Validación de Energía Especial

**Problema:** `DeckValidator` agrupa por `cardId` y saltea el límite de 4 copias para Energía Básica (`EnergyCardType.BASIC`), pero no valida correctamente que las Energías Especiales (`EnergyCardType.SPECIAL`) respeten el límite de 4 copias por nombre.

**Solución:**

- SHALL mantener el agrupado actual por `cardId` para el límite de cantidad (`DeckCard.quantity > 4`)
- SHALL **además** agregar un agrupado por `name` (resolviendo vía `cardLookupPort`) para detectar la misma carta con distintos IDs
- SHALL en el chequeo de `MORE_THAN_4_COPIES`, cuando la carta es `EnergyCardType.SPECIAL`, verificar el total por `name`:

```java
// Lógica adicional después del chequeo actual:
Map<String, Integer> nameCounts = new HashMap<>();
for (DeckCard dc : cards) {
    CardDefinition def = cardLookupPort.getCardById(dc.getCardId());
    if (def instanceof EnergyCardDefinition energy && energy.getEnergyCardType() == EnergyCardType.SPECIAL) {
        // O usa def.getName() según el modelo
        nameCounts.merge(def.getName(), dc.getQuantity(), Integer::sum);
    }
}
for (Map.Entry<String, Integer> entry : nameCounts.entrySet()) {
    if (entry.getValue() > 4) {
        errors.add(DeckValidationError.MORE_THAN_4_COPIES);
        break;
    }
}
```

**Archivos afectados:**
- MODIFY `BE/.../services/decks/DeckValidator.java` — agregar validación por nombre para Special Energy

---

### R7 — Item 67: AS TÁCTICO máximo 1 por mazo

**Nota:** Este item es idéntico al detalle 77, ya cubierto en `openspec/specs/attack-pipeline-and-ace-spec.md` (R5).

SHALL implementar según lo especificado en dicho spec: contar cartas con `TrainerCardDefinition.isAceSpec() == true` considerando cantidades, y agregar error `ACE_SPEC_LIMIT_EXCEEDED` si supera 1.

---

## Items excluidos

| Item | Motivo |
|------|--------|
| **89/90** — Log de acciones persistido + reconstrucción (event sourcing) | Requiere spec propio: nueva tabla DB, entidad JPA, interceptor de eventos, replay, reconexión WebSocket |

---

## Dependencias entre items

```
14 (evolución primer turno) ──→ independiente
26 (confusión inconsistente) ──→ independiente, toca AttackResolver + DeclareAttackHandler
42 (KO replacement) ──→ nuevo handler, nuevo GameActionType
59 (muerte súbita) ──→ depende de SetupManager parametrizado
65 (energía especial) ──→ independiente, solo DeckValidator
67 (AS TÁCTICO) ──→ ya cubierto en spec crítico
70 (estadio) ──→ ya parcialmente implementado en trainer-effects
```

Orden de implementación sugerido: 65 → 14 → 26 → 42 → 70 → 59 (el más complejo).
