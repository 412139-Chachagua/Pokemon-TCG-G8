## Context

El backend tiene la arquitectura hexagonal correcta (engine aislado, handler pattern, ports/adapters) pero varios defectos de implementación rompen la V1. El análisis de código reveló 28+ discrepancias entre lo contratado y lo implementado; este change ataca las 7 que bloquean un match funcional.

La base de datos actual es H2 en memoria con `ddl-auto`. El diseño original del catálogo (`/docs/divisionCatalogo.md`) plantea una sola tabla `cards` — pero la implementación creó tablas especializadas adicionales que nadie lee.

## Goals / Non-Goals

**Goals:**
- Turn flags resetean correctamente entre turnos → el juego es jugable más allá del turno 1
- Card catalog escribe y lee de la misma tabla → búsqueda y `CardLookupAdapter` retornan datos
- RuleValidator acepta evolución de Pokémon activo
- Payload helpers toleran UUID como String o UUID object sin ClassCastException
- Seed decks se insertan sin violación de FK
- `base_damage` se persiste y expone en el API

**Non-Goals:**
- Refactor de `DeckMapper` reflection → setters
- Implementación de `StatusEffectManager` real
- Tests de integración de match completo
- Migración a PostgreSQL
- Implementación de Flyway
- Nuevos endpoints, eventos WebSocket, o frontend

## Decisions

### 1. Llamada a `startTurn()` en EndTurnHandler

**Decisión**: `EndTurnHandler` invoca `turnManager.startTurn()` para el nuevo jugador después de `endTurn()`.

**Alternativa considerada**: que `DrawCardHandler` llame a `startTurn()`. Se descartó porque ataría el reset de flags a una acción explícita del jugador; si el jugador demora en hacer DRAW_CARD, los flags quedarían incorrectos por un periodo. `EndTurnHandler` ejecuta siempre al finalizar el turno, garantizando que al comenzar el próximo turno los flags ya están limpios.

### 2. Catálogo unificado en `cards`

**Decisión**: `CardCacheSyncService` escribe a `CardJpaRepository` usando `CardMapper.toCardEntity()`. `CardMapper.toCardEntity()` se completa (settea `number` correctamente). Las entidades/repos especializados (`PokemonCardEntity`, etc.) se marcan `@Deprecated`.

**Alternativa considerada**: Hacer que `CardCatalogService` lea de las tablas especializadas. Se descartó porque `/docs/divisionCatalogo.md` explícitamente define el catálogo como una sola entidad `CardEntity`. Las tablas especializadas fueron un error de implementación.

### 3. Payload helpers en GameAction

**Decisión**: Agregar `getPayloadString(key)` y `getPayloadInt(key)` en `GameAction` que usan `toString()` para tolerar tanto `String` como `UUID` en el Map.

**Alternativa considerada**: Cambiar todos los handlers a usar las clases tipadas (`AttachEnergyPayload`, etc.). Se descartó porque implicaría refactor mayor del handler dispatch y no es necesario para V1.

### 4. Validación de evolución busca active + bench

**Decisión**: `RuleValidator` para `EVOLVE_POKEMON` busca el target en `player.getActivePokemon()` primero, y si no está ahí, en `player.getBench()`. Si no está en ninguno, retorna error.

**Justificación**: El handler ya soporta ambos casos; la validación estaba incorrectamente restrictiva. Es un bug de una línea.

## Risks / Trade-offs

| Riesgo | Mitigación |
|--------|------------|
| `@Deprecated` en entidades especializadas podría romper `CardCacheSyncService` si queda código que las importa | Se reemplaza el writer en `CardCacheSyncService` antes de marcar deprecated |
| Si `CardMapper.toCardEntity()` tiene bugs ocultos, el sync puede persistir datos corruptos | Se agrega `CardMapperTest` para verificar el mapeo |
| Llamar `startTurn()` desde `EndTurnHandler` podría causar doble reset si alguien más lo llama | `startTurn()` es idempotente — resetea flags a `false` siempre |
| Los payload helpers pueden ocultar errores de tipo silenciosamente | `getPayloadInt()` lanza `NumberFormatException` si el valor no es numérico; `getPayloadString()` retorna `null` si la key no existe, que los handlers ya manejan |
