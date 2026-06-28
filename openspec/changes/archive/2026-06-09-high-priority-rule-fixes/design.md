## Context

El game engine del Pokémon TCG tiene 7 reglas oficiales implementadas incorrectamente. El spec `high-priority-rule-fixes.md` detalla cada problema y solución. Todas las correcciones son server-side (backend Java), sin impacto en frontend, base de datos, ni API REST. El engine está aislado de Spring/JPA/servicios externos.

Estado actual relevante:
- `RuleValidator` usa `turnNumber == 1` global para prohibir evolución, ignorando por jugador.
- `AttackResolver` muta daño de confusión directamente, pero `DeclareAttackHandler` rechaza la acción dejando estado inconsistente.
- `DeclareAttackHandler` selecciona `bench.get(0)` automáticamente al detectar KO del Activo rival.
- `VictoryConditionChecker` detecta empate simultáneo pero no inicia Muerte Súbita.
- `PlayTrainerHandler` descarta Estadios inmediatamente; `StadiumPlayResolver` ya existe en trainer-effects pero debe verificarse.
- `DeckValidator` no valida Energía Especial por nombre (solo por cardId).
- `DeckValidator` no verifica límite de AS TÁCTICO (cubierto en spec attack-pipeline).

## Goals / Non-Goals

**Goals:**
- Implementar las 7 reglas correctas del Pokémon TCG según el spec de referencia.
- Mantener engine aislado: todos los cambios dentro del paquete `engine/` (y `services/decks/` para DeckValidator).
- Asegurar que todas las acciones del engine sean atómicas y consistentes (no más mutaciones parciales como el caso de confusión).
- Agregar los eventos WebSocket necesarios para que el frontend reaccione (KO_REPLACEMENT_REQUIRED, SUDDEN_DEATH_STARTED, etc.)
- Reutilizar infraestructura existente (TurnFlags, GameEvent, GameActionType, handlers) en lugar de crear mecanismos nuevos.

**Non-Goals:**
- NO cambiar frontend, base de datos, API REST, ni WebSocket wiring existente.
- NO implementar event sourcing / log persistido (items 89/90, excluidos del spec).
- NO agregar nuevos controladores REST ni endpoints.
- NO modificar SetupManager más allá de parametrizar prizeCount.
- NO refactorizar arquitectura del engine (solo cambios localizados).

## Decisions

### D1 — Flag por jugador en GameState para primer turno (R1, item 14)

Usar `Set<UUID> playersWhoCompletedFirstTurn` en `GameState`. Alternativa considerada: `Map<UUID, Boolean>` — un Set es más simple porque la semántica es "está o no está en el conjunto". La presencia del UUID significa "completó su primer turno". Setter: `markPlayerCompletedFirstTurn(UUID playerId)`. Getter: `hasPlayerCompletedFirstTurn(UUID playerId)`.

### D2 — Confusión: delegar mutación a DeclareAttackHandler (R2, item 26)

`AttackResolver.resolve()` retorna `AttackResolutionResult` con `confusedSelfHit=true` y `selfDamageCounters` pero NO aplica el daño. `DeclareAttackHandler` recibe el resultado, aplica el daño, verifica KO del atacante, setea `hasAttacked=true`, y avanza fase. Esto elimina el estado inconsistente actual donde el daño quedaba aplicado pero la acción se reportaba como error.

Alternativa descartada: revertir el daño en el handler (más riesgoso, require lógica de rollback).

### D3 — KO Replacement como GameAction (R3, item 42)

Nuevo `GameActionType.CHOOSE_KO_REPLACEMENT`. El handler existente `DeclareAttackHandler` detecta KO del Activo rival, setea `pendingKOReplacement=true` y `knockedOutPlayerId` en GameState, publica `KO_REPLACEMENT_REQUIRED`, y detiene el avance de fase. `RuleValidator` permite CHOOSE_KO_REPLACEMENT solo cuando `pendingKOReplacement == true`. El nuevo `ChooseKOReplacementHandler` valida que el Pokémon seleccionado esté en bench, lo mueve a Active, limpia flags, publica evento, y avanza fase.

### D4 — Muerte Súbita como reinicio parcial (R4, item 59)

`VictoryConditionChecker.check()` retorna `VictoryCheckResult(suddenDeath=true)` sin finalizar la partida. `GameEngine` recibe esto y orquesta un reinicio parcial reutilizando `SetupManager.setupGame()` con `prizeCount=1`. Se usa un nuevo constructor/factory method en `GameState` que toma el estado de los jugadores y mazos del juego anterior. NO se crea un GameActionType nuevo — la transición es automática (no requiere acción del jugador). Se publica `SUDDEN_DEATH_STARTED` para que el frontend muestre el reinicio.

### D5 — Estadio: verificar PlayTrainerHandler + StadiumPlayResolver (R5, item 70)

Ya existe `StadiumPlayResolver` en el sistema de trainer-effects. La corrección principal es en `PlayTrainerHandler.handle()`: cuando la carta es de tipo `TrainerSubtype.STADIUM`, NO moverla al discard. Delegar a `StadiumPlayResolver` que maneja: (1) reemplazo de Estadio anterior, (2) asignación a `GameState.stadiumCardInstanceId`, (3) publicación de eventos. `TurnManager.startTurn()` no debe limpiar `stadiumCardInstanceId`.

### D6 — Energía Especial: validación por nombre (R6, item 65)

En `DeckValidator`, después del chequeo actual de `MORE_THAN_4_COPIES` por `cardId`, agregar un segundo agrupado por `name` (resolviendo `CardDefinition` vía `cardLookupPort`) para cartas `EnergyCardType.SPECIAL`. Si el total por nombre supera 4, agregar `MORE_THAN_4_COPIES`. Esto cubre el caso donde la misma carta de Energía Especial aparece con distintos IDs.

### D7 — AS TÁCTICO: implementar según attack-pipeline spec (R7, item 67)

Ya especificado en `attack-pipeline-and-ace-spec.md` R5. En `DeckValidator`, iterar cartas, contar aquellas con `TrainerCardDefinition.isAceSpec() == true` (considerando quantities), y agregar `ACE_SPEC_LIMIT_EXCEEDED` si > 1. No duplicar diseño aquí.

### D8 — Eventos del sistema

Todos los eventos nuevos se agregan al enum `GameEventType` existente (o equivalente según el contrato). No se crea un mecanismo de eventos nuevo — se reutiliza `GameEvent` existente.

| Evento | Disparador | Payload |
|--------|-----------|---------|
| `CONFUSION_SELF_HIT` | DeclareAttackHandler aplica autogolpe | attackerId, damageCounters |
| `DAMAGE_APPLIED` | Ya existe, verificar cobertura | — |
| `KO_REPLACEMENT_REQUIRED` | DeclareAttackHandler detecta KO | knockedOutPlayerId, candidates (bench list) |
| `KO_REPLACEMENT_DONE` | ChooseKOReplacementHandler completa | playerId, newActiveId |
| `SUDDEN_DEATH_STARTED` | GameEngine inicia reinicio | playerIds, prizeCount=1 |
| `STADIUM_PLAYED` | StadiumPlayResolver | cardId, playerId |
| `STADIUM_REMOVED` | StadiumPlayResolver (reemplazo) | oldCardId, ownerPlayerId |

## Risks / Trade-offs

| Riesgo | Mitigación |
|--------|-----------|
| **R1 — Confusión**: si `DeclareAttackHandler` no verifica KO del atacante después del autogolpe, el Pokémon podría quedar con 0 PS y seguir en juego | Verificar KO inmediatamente después de aplicar daño de confusión, igual que la lógica actual de daño al rival |
| **R2 — KO Replacement**: el frontend debe poder mostrar la lista de candidatos y enviar CHOOSE_KO_REPLACEMENT. Si no hay candidatos (bench vacío), se pierde automáticamente | En `DeclareAttackHandler`, si bench del jugador afectado está vacío, el jugador pierde directamente (sin pedir elección). Publicar GAME_OVER |
| **R3 — Muerte Súbita**: el reinicio parcial es complejo — hay que restaurar mazos completos, limpiar estados, etc. | Reutilizar `SetupManager` con estados clonados. Los mazos se restauran desde el estado inicial guardado en Match |
| **R4 — Estadio**: si `StadiumPlayResolver` tiene bugs (ya reportado en trainer-effects), se necesitan pruebas específicas | Incluir verificación manual + test de integración en esta change. Si el resolver no funciona correctamente, corregirlo dentro del mismo change |
| **R5 — Side effects**: cambios en GameState (primerTurno, KO pending, suddenDeath) afectan a serialización/deserialización para WebSocket | Todos los flags nuevos son `transient` o se incluyen en el DTO de estado. Verificar contrato 06-game-state-contract.md |
