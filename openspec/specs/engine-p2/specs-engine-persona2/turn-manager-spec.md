# AI Proposal Spec: persona2-turn-manager

## Change name

persona2-turn-manager

---

# Depends on

OpenCode MUST read and obey:

- `openspec/specs/engine-persona2-master-spec.md`
- `BE/docs/contracts_ai/06-game-state-contract.md`
- `docs/Reglas_Pokemon_TCG.md`
- `BE/docs/contracts_ai/07-setup-flow-contract.md`

Los contratos del proyecto son la fuente de verdad.
Si este spec entra en conflicto con los contratos, los contratos ganan.

---

# Purpose

Implementar la lógica real de `engine/turn/TurnManager`.

Este spec cubre únicamente:
- inicio de turno
- fin de turno
- transiciones de fase
- flujo de robo de carta
- reset de turn flags
- cambio de current player

---

# Existing ports — do not recreate

El siguiente port ya existe y NO debe ser recreado ni modificado:

- `engine/ports/RandomizerPort.java`

---

# Ownership

Persona 2 owns exclusively:

- `engine/turn/TurnManager`

NO modificar `TurnPhase` ni ningún enum del engine.
NO modificar ninguna clase fuera de este ownership.

---

# Non-goals

NO implementar:

- combat
- damage
- attack effects
- KO resolution
- status conditions entre turnos
- poison
- burn
- sleep
- paralysis
- confusion
- coin flips de condiciones especiales
- handlers
- victory checking
- persistence

Status conditions están fuera de scope V1 para `TurnManager`.
Las condiciones especiales pueden existir como datos en `PokemonInPlay`
pero su procesamiento entre turnos no es responsabilidad de este change.

---

# Dependencies allowed

`TurnManager` puede depender únicamente de:

- `RandomizerPort`
- `engine/model/*`
- `engine/EngineContext`
- enums del engine ya definidos por Persona 1

NO usar:
- repositorios
- services
- Spring
- JPA
- `java.util.Random`
- `ThreadLocalRandom`
- `Collections.shuffle`

Las dependencias se inyectan por constructor.

---

# Constructor

`TurnManager` recibe por constructor:

- `RandomizerPort`

---

# TurnPhase usage

Usar exclusivamente las phases ya definidas por Persona 1 en `TurnPhase`:

- `DRAW`
- `MAIN`
- `ATTACK`
- `BETWEEN_TURNS`

NO modificar `TurnPhase` ni crear nuevas phases.

---

# Comportamiento de startTurn

`startTurn(EngineContext ctx)` debe:

1. Obtener el `PlayerState` correspondiente al `currentPlayerId` actual.
2. Resetear todos los campos de `TurnFlags` a `false`.
3. Verificar si el deck del jugador actual está vacío.
   - Si está vacío: registrar un evento mediante `ctx.addEvent()` indicando que el jugador no puede robar. NO modificar `winnerPlayerId` directamente.
4. Si el deck no está vacío y corresponde realizar el robo de turno:
   - Mover la carta superior del deck a la mano del jugador.
   - Establecer `hasDrawnForTurn` en `true`.
5. Cambiar `phase` de `DRAW` a `MAIN`.

---

# Suposiciones de inicialización del turno

`TurnManager` asume que el flujo de setup ya inicializó la partida de acuerdo con `07-setup-flow-contract.md`.

Al comienzo del primer turno:

- `turnNumber = 1`
- `currentPlayerId = firstPlayerId`
- `phase = DRAW`

`TurnManager` debe mantener compatibilidad con estas invariantes del setup.



---

# Regla de robo del primer turno

El jugador identificado por `firstPlayerId` en el `GameState`
no roba una carta cuando `turnNumber == 1`.

En todos los demás casos, el jugador activo roba 1 carta al inicio
de su turno.

---

# Reinicio de TurnFlags

Ver `Comportamiento de startTurn`.

Los siguientes flags deben permanecer en `false` hasta que la acción correspondiente sea realizada durante el turno:

- `hasAttachedEnergy`
- `hasRetreated`
- `hasPlayedSupporter`
- `hasPlayedStadium`
- `hasAttacked`

---

# endTurn behavior

`endTurn(EngineContext ctx)` debe:

1. Cambiar `phase` a `BETWEEN_TURNS`.
2. Registrar evento de fin de turno via `ctx.addEvent()`.
3. Cambiar `currentPlayerId` al otro jugador.
4. Incrementar `turnNumber` en 1.
5. Llamar `startTurn(ctx)` para el nuevo jugador.

---

# Player switching

Para determinar el otro jugador: buscar en `GameState.players` el
`PlayerState` cuyo `playerId` sea distinto al `currentPlayerId` actual.

NO asumir índices fijos. Usar búsqueda por `playerId`.

---

# Escenarios de validación

La implementación debe satisfacer los siguientes escenarios:

* `startTurn` roba 1 carta correctamente durante un turno normal.
* El jugador que comienza la partida no roba una carta en el turno 1.
* `endTurn` cambia `currentPlayerId` al jugador correcto.
* `endTurn` incrementa `turnNumber`.
* `startTurn` reinicia correctamente los `TurnFlags`.
* `hasDrawnForTurn` pasa a `true` después de un robo válido.
* La fase cambia a `MAIN` después de `startTurn`.
* La fase transita por `BETWEEN_TURNS` durante `endTurn`.
* Un deck vacío registra un evento sin modificar `winnerPlayerId`.

Estos escenarios son únicamente criterios de aceptación.

NO crear nuevas clases de test, archivos de test ni tests unitarios como parte de este cambio.

La verificación debe basarse en la revisión de la implementación y en la ejecución de los tests existentes del proyecto mediante `mvn test`.


---

# Scope control

Este spec implementa únicamente el flujo de turnos y fases.

No implementar:
- combat
- attacks
- effects
- handlers
- status conditions
- validaciones de acciones