## Context

El engine de juego tiene tres bugs identificados:

1. **Mulligan sin revelación**: `SetupManager.resolveMulligan()` reshufflea la mano en silencio. La regla oficial exige que la mano sin Pokémon básico se revele al oponente antes de devolverla al deck.

2. **TakePrize sin verificación de KO**: `TakePrizeCardHandler.handle()` permite a cualquier jugador tomar una carta de premio, sin validar que ese jugador haya realizado un KO en esa jugada. `GameState` no tiene un campo que registre quién es el dueño del KO pendiente.

3. **Retreat sin enteredTurnNumber**: `RetreatActiveHandler` mueve el Pokémon activo al bench pero no setea `enteredTurnNumber`, lo que permite evolucionarlo en el mismo turno (violando la regla de que un Pokémon recién puesto en juego no puede evolucionar ese turno).

## Goals / Non-Goals

**Goals:**
- Corregir el mulligan para que revele la mano al oponente mediante un evento.
- Agregar en `GameState` un campo `pendingPrizeOwnerPlayerId` que `DeclareAttackHandler` setea al hacer KO, y que `TakePrizeCardHandler` valida antes de entregar el premio.
- Setear `enteredTurnNumber` en `RetreatActiveHandler` al Pokémon que se retira al bench.
- Propagar los eventos correspondientes para que el frontend pueda mostrar la revelación del mulligan y la toma de premio.

**Non-Goals:**
- No se modifica la lógica de combate, validación de reglas, ni otros handlers.
- No se agregan nuevas APIs REST ni cambios de persistencia.
- No se implementa event sourcing ni historial de acciones.

## Decisions

### Decisión 1: Evento de revelación en mulligan en vez de modificar el contrato del port

Se agrega un `GameEvent` de tipo `MULLIGAN_REVEALED` dentro de `SetupManager` que incluya en su payload los `cardDefinitionId` de las cartas reveladas. Alternativa considerada: agregar un método al `RandomizerPort`. Se descarta porque la revelación no es aleatoria, es un evento de dominio.

### Decisión 2: pendingPrizeOwnerPlayerId en GameState

Se agrega el campo `pendingPrizeOwnerPlayerId` a `GameState`. `DeclareAttackHandler` lo setea al detectar un KO. `TakePrizeCardHandler` lo valida y lo limpia tras la toma. Alternativa considerada: usar el `pendingDecision` existente. Se descarta porque `pendingDecision` es genérico (`Object`) y agregar un campo tipado es más seguro y visible.

### Decisión 3: enteredTurnNumber en RetreatActiveHandler

Se setea `enteredTurnNumber = state.getTurnNumber()` al Pokémon que se mueve del active al bench. Esto ya se hace en otros handlers (EvolvePokemonHandler, SetupManager) — es consistente con el patrón existente.

## Risks / Trade-offs

- [Risk] El nuevo evento `MULLIGAN_REVEALED` requiere que el frontend lo maneje para mostrar las cartas reveladas. → Mitigación: MVP puede ignorarlo visualmente, el evento se emite igual.
- [Risk] `pendingPrizeOwnerPlayerId` se limpia al tomar premio. Si el jugador no toma premio (ej. desconexión), el flag queda sucio. → Mitigación: Se resetea al comenzar un nuevo turno en `TurnManager.resetTurnFlags()` o al procesar el siguiente ataque.
