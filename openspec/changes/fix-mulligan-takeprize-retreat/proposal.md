## Why

Tres bugs detectados en el engine de juego durante la auditoría de Personas 1-3: el flujo de mulligan no revela la mano al oponente (mecánica oficial), `TakePrizeCardHandler` permite tomar premio sin verificar quién hizo el KO, y `RetreatActiveHandler` no actualiza `enteredTurnNumber` al Pokémon que pasa a la banca. Estas omisiones pueden causar partidas inconsistentes y comportamientos incorrectos en reglas de evolución y victoria.

## What Changes

- `SetupManager`: Agregar evento que revele la mano sin básico al oponente durante el mulligan, antes del reshuffle.
- `TakePrizeCardHandler`: Validar que el jugador que toma el premio sea el mismo que realizó el KO (verificar `pendingDecision` o flag de KO pendiente en `GameState`).
- `RetreatActiveHandler`: Setear `enteredTurnNumber` al Pokémon que se mueve del Active al Bench tras la retirada.

Ningún cambio es **BREAKING**.

## Capabilities

### New Capabilities
_(none — son correcciones a capacidades existentes)_

### Modified Capabilities
- `engine-p2/setup-manager-spec`: El flujo de mulligan debe revelar la mano al oponente antes del reshuffle.
- `engine-persona3-handlers`: Los handlers `TakePrizeCardHandler` y `RetreatActiveHandler` deben corregir las validaciones faltantes.

## Impact

- `BE/src/main/java/.../engine/setup/SetupManager.java` — modificar flujo de mulligan + agregar evento de revelación.
- `BE/src/main/java/.../engine/handlers/TakePrizeCardHandler.java` — agregar validación de KO pendiente.
- `BE/src/main/java/.../engine/handlers/RetreatActiveHandler.java` — setear `enteredTurnNumber`.
- `BE/src/main/java/.../engine/model/GameState.java` — posible agregar campo `pendingPrizeOwnerPlayerId` si no existe.
- NO afecta frontend, API REST, persistencia ni contratos existentes.
