## Context

El proyecto Pokémon TCG TPI requiere un **engine de juego aislado** (`engine/`) que contenga toda la lógica de estado y acciones sin dependencias de Spring, JPA, REST ni WebSockets. Actualmente existen stubs parciales (`GameState`, `PlayerState`, `PokemonInPlay`, `TurnFlags`, `GameAction`, `ActionResult`, `GameEngine`) que deben completarse, y faltan clases enteras (`CardInstance`, `GameMetadata`, `GamePhase`, `EngineContext`, `ActionHandler`, todos los payloads concretos, `ErrorCode`).

Este change es la base que **desbloquea** a las Personas 2 (reglas), 3 (handlers) y 4 (REST/WebSocket). Los contratos congelados (03, 06, 08, 09, 12) definen la forma canónica de cada tipo.

## Goals / Non-Goals

**Goals:**
- Definir todos los modelos del estado de juego (`engine/model/`) según contrato 06
- Definir todos los tipos de acción y sus payloads (`engine/action/`) según contrato 08
- Crear `ErrorCode` enum con todos los códigos del contrato 09
- Crear `EngineContext` como wrapper mutable de `GameState` + ports + acumulador de eventos
- Definir la interface `ActionHandler` que usarán los handlers concretos (Persona 3)
- Implementar `GameEngine.applyAction()` con el flujo completo de 11 pasos
- Verificar `GameActionType`, `PlayerSide`, `SpecialCondition` contra contrato 03
- Proveer stubs compilables para `RuleValidator`, `SetupManager`, `VictoryConditionChecker`, `TurnManager`

**Non-Goals:**
- Lógica real de reglas (`RuleValidator`) — solo stub
- Lógica real de setup (`SetupManager`) — solo stub
- Lógica real de victoria (`VictoryConditionChecker`) — solo stub
- Lógica real de turno (`TurnManager`) — solo stub
- Ningún handler concreto (`PutBasicOnBenchHandler`, etc.) — Persona 3
- Ningún endpoint REST — Persona 4
- Ninguna clase con `@Entity`, `@Service`, `@Repository`, `@Autowired`
- Ninguna clase del catálogo, mazos o frontend

## Decisions

1. **EngineContext como único punto de mutación** — Toda modificación del `GameState` durante una acción pasa por `EngineContext.getState()`. Esto centraliza el acceso y facilita testing y auditoría. Alternativa considerada: pasar `GameState` directamente a los handlers. Se descartó porque no habría un lugar natural para acumular eventos ni exponer ports.

2. **Eventos como `List<String>` (texto plano)** — Tal como establece el contrato 08/12, no existe `GameEventType` ni objetos de evento en V1. Los eventos son strings human-readable generados por cada handler. Esto simplifica el diseño inicial; si se requiere audit trail en el futuro se agrega como nuevo subsistema.

3. **Payloads como records/POJO independientes (no inner classes)** — Cada payload concreto (`AttachEnergyPayload`, etc.) es una clase pública separada que implementa `GameActionPayload` (interface marker). Esto permite que el handler de cada acción tenga un tipo específico sin acoplamiento entre acciones.

4. **ActionHandler como interface funcional** — `void handle(EngineContext ctx)` sin genéricos. Cada handler concreto obtiene su payload casteando `action.getPayload()`. Alternativa considerada: `ActionHandler<T extends GameActionPayload>` con genéricos. Se descartó porque introduce complejidad de tipos en el dispatcher sin beneficio real, dado que la validación ya verificó el tipo antes de llamar al handler.

5. **GameEngine.applyAction() con 11 pasos fijos** — El flujo está explícitamente definido en el spec: load → verify active → verify turn → create context → validate → dispatch → check victory → update state → persist → publish → return. No se permiten desviaciones; si un handler necesita un paso extra, se agrega dentro del handler, no en el flujo principal.

6. **Stubs que compilan pero no tienen lógica** — `RuleValidator.validate()` siempre retorna válido, `VictoryConditionChecker.check()` siempre retorna `Optional.empty()`, `SetupManager` y `TurnManager` son clases vacías con constructor por defecto. Esto permite que `GameEngine.applyAction()` compile y sea testeable sin depender de la implementación real de reglas.

## Risks / Trade-offs

- **[Riesgo] Los stubs pueden esconder errores de integración** cuando las otras Personas implementen la lógica real. → Mitigación: los stubs tienen tipos y firmas exactas; las pruebas de integración detectarán discrepancias.
- **[Riesgo] EngineContext como objeto mutable puede llevar a efectos secundarios no deseados** si los handlers modifican el estado en orden incorrecto. → Mitigación: el flujo en `GameEngine.applyAction()` es secuencial y cada handler es responsable de su propia mutación.
- **[Trade-off] Payloads sin genéricos** — el handler debe castear, pero la validación previa garantiza que el tipo sea correcto. Se prioriza simplicidad sobre type-safety extremo.
- **[Riesgo] Si los contratos 03/06/08/09 cambian**, las clases de este change se desincronizan. → Mitigación: los contratos están congelados (specs acordados). Cualquier cambio debe ser deliberado y actualizar los contratos primero.
