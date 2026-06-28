## Why

Siete reglas oficiales del Pokémon TCG están implementadas de forma incorrecta o incompleta en el game engine, categorizadas como impacto **alto** en el informe de validaciones. Estos bugs causan comportamientos antirreglamentarios: evolución en primer turno del rival, daño aplicado en estado inconsistente por confusión, falta de reemplazo de KO por elección, muerte súbita no implementada, estadios que se descartan inmediatamente, y validaciones faltantes de Energía Especial y AS TÁCTICO. Sin estas correcciones el juego permite jugadas ilegales y estados inconsistentes.

## What Changes

- **R1 — Evolución en primer turno del jugador (item 14):** Reemplazar el chequeo global `turnNumber == 1` por un flag por jugador (`playersWhoCompletedFirstTurn`) en `GameState`. La evolución y ataque se prohiben solo si el jugador específico no ha completado su primer turno.
- **R2 — Confusión como acción exitosa (item 26):** `AttackResolver.resolve()` deja de mutar el estado del Pokémon confundido. `DeclareAttackHandler` aplica el autogolpe, verifica KO del atacante, y trata la acción como exitosa (no reporta error). El daño se consumió, el turno avanza.
- **R3 — Reemplazo de KO por elección (item 42):** Nuevo `GameActionType.CHOOSE_KO_REPLACEMENT`, nuevo `ChooseKOReplacementHandler`. `DeclareAttackHandler` ya no selecciona `bench.get(0)` automáticamente; publica evento y espera la elección del jugador afectado.
- **R4 — Muerte Súbita (item 59):** `VictoryConditionChecker` detecta empate simultáneo y dispara reinicio parcial: mismos jugadores, mismos mazos restaurados, 1 carta de Premio, manos repartidas de nuevo. Nuevo flag `suddenDeath` + `prizeCountPerPlayer` parametrizado en `SetupManager`.
- **R5 — Estadio permanece en juego (item 70):** Verificar y corregir `StadiumPlayResolver` para que el Estadio se quede en `GameState.stadiumCardInstanceId` hasta ser reemplazado o removido por efecto. No se descarta al jugarlo. `PlayTrainerHandler` no debe descartar Estadios.
- **R6 — Energía Especial validada por nombre (item 65):** `DeckValidator` agrega validación adicional: para cartas de tipo `EnergyCardType.SPECIAL`, verifica el total por `name` (no solo por `cardId`) y rechaza si supera 4 copias.
- **R7 — AS TÁCTICO máximo 1 (item 67):** Ya cubierto en `openspec/specs/attack-pipeline-and-ace-spec.md` (R5). Se implementa según ese spec, contando `TrainerCardDefinition.isAceSpec()` y agregando error `ACE_SPEC_LIMIT_EXCEEDED` si supera 1.

## Capabilities

### New Capabilities
- `ko-replacement`: Flujo de elección de reemplazo cuando un Pokémon Activo es derrotado. Nuevo `GameActionType`, nuevo handler, estado pendiente en GameState, integración con RuleValidator y GameEngine.
- `sudden-death`: Flujo de desempate por Muerte Súbita cuando ambos jugadores toman su última carta de Premio en el mismo turno. Reinicio parcial del juego con 1 carta de Premio.

### Modified Capabilities
- `game-state`: Agregar `playersWhoCompletedFirstTurn` (Set<UUID>), `pendingKOReplacement`, `knockedOutPlayerId`, `suddenDeath`, `prizeCountPerPlayer`. Verificar persistencia de `stadiumCardInstanceId`.
- `engine-p2/rule-validator-spec`: Modificar validación de `EVOLVE_POKEMON` y `DECLARE_ATTACK` para usar flag por jugador. Agregar validación de `CHOOSE_KO_REPLACEMENT`.
- `engine-p2/turn-manager-spec`: Marcar `firstTurnCompleted` para el jugador actual al finalizar su primer turno. Asegurar que Estadio persiste entre turnos.
- `engine-p2/setup-manager-spec`: Aceptar `prizeCount` parametrizado para soportar Muerte Súbita (1 premio en lugar de 6).
- `attack-pipeline-and-ace-spec`: Modificar comportamiento de `AttackResolver` (no mutar en confusión) y `DeclareAttackHandler` (confusión como acción exitosa, delegar KO replacement). Agregar validación ACE_SPEC en DeckValidator.
- `enums`: Agregar `GameActionType.CHOOSE_KO_REPLACEMENT`. NO crear nuevos enums para Muerte Súbita (se maneja con flags booleanos + evento).
- `deck-validator`: Nueva validación de Energía Especial por nombre (límite de 4).

## Impact

- **Archivos BE modificados**: `GameState.java`, `RuleValidator.java`, `TurnManager.java`, `EndTurnHandler.java`, `AttackResolver.java`, `DeclareAttackHandler.java`, `DeckValidator.java`, `PlayTrainerHandler.java`, `VictoryConditionChecker.java`, `SetupManager.java`, `GameEngine.java`, `MatchApplicationService.java`, `GameActionType.java`.
- **Archivos BE creados**: `ChooseKOReplacementHandler.java`.
- **No impact en Frontend**: Todos los cambios son server-side. El frontend ya abstrae las acciones vía `GameAction` y eventos WebSocket.
- **No impact en DB/API REST**: Los cambios de estado son en memoria del engine. No hay nuevos endpoints ni columnas de BD.
- **Eventos nuevos**: `KO_REPLACEMENT_REQUIRED`, `SUDDEN_DEATH_STARTED`, `CONFUSION_SELF_HIT`, `STADIUM_PLAYED`, `STADIUM_REMOVED`.
