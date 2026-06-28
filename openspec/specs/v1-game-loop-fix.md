# AI Proposal Spec: v1-game-loop-fix

## Change name

v1-game-loop-fix

## Purpose

Corregir los problemas críticos y de alta prioridad que impiden que la aplicación sea una V1 funcional. El código base tiene la arquitectura correcta (hexagonal, handler pattern, engine aislado) pero varios defectos concretos rompen el flujo de juego o dejan features enteras inoperativas.

Esta propuesta se enfoca exclusivamente en arreglar lo necesario para que un match complete su ciclo de principio a fin, respetando el diseño de catálogo planteado en `/docs/divisionCatalogo.md`.

## Mandatory context files

OpenCode debe leer para guía (parcialmente desactualizados):

- `/openspec/config.yaml`
- `/BE/docs/contracts_ai/00-contract-index.md`
- `/BE/docs/contracts_ai/01-project-scope-contract.md`
- `/BE/docs/contracts_ai/02-project-structure-contract.md`
- `/BE/docs/contracts_ai/03-enums-contract.md`
- `/BE/docs/contracts_ai/04-card-model-contract.md`
- `/BE/docs/contracts_ai/05-deck-contract.md`
- `/BE/docs/contracts_ai/06-game-state-contract.md`
- `/BE/docs/contracts_ai/07-setup-flow-contract.md`
- `/BE/docs/contracts_ai/08-game-action-contract.md`
- `/BE/docs/contracts_ai/09-rule-validation-contract.md`
- `/BE/docs/contracts_ai/10-attack-pipeline-contract.md`
- `/BE/docs/contracts_ai/11-status-effects-contract.md`
- `/BE/docs/contracts_ai/12-persistence-log-contract.md`
- `/BE/docs/contracts_ai/13-rest-api-contract.md`
- `/BE/docs/contracts_ai/14-websocket-contract.md`
- `/BE/docs/contracts_ai/15-frontend-state-contract.md`
- `/BE/docs/contracts_ai/16-test-scenarios-contract.md`

OpenCode también debe leer el diseño original de catálogo:

- `/docs/divisionCatalogo.md`

Y toda la base de código actual bajo `/BE/src/` para contrastar el estado real.

## Goal

Lograr que la aplicación backend sea funcional para una V1 MVP de Pokémon TCG, donde dos jugadores puedan:

1. Crear y unirse a una partida
2. El setup automático distribuya manos, active, bench y premios
3. Los jugadores puedan realizar turnos completos (robar, jugar energía, poner básicos, evolucionar, retirar, atacar, terminar turno)
4. Los turnos subsiguientes reseteen correctamente los flags de turno
5. El daño se calcule, los KO se detecten, los premios se tomen y la victoria se declare
6. El catálogo de cartas funcione (sincronización + búsqueda)
7. Los mazos seed se carguen y validen correctamente

## Database strategy

La V1 corre sobre **H2 en memoria** (configuración actual). No se migra a PostgreSQL. El schema se maneja con `ddl-auto` de JPA, no con Flyway. El archivo `BE/docs/V1__init_schema.sql` es documentación de referencia para futuro deploy a PostgreSQL, no un script de migración activo.

## Design decisions

### Card catalog unified

Se sigue el diseño de `/docs/divisionCatalogo.md`: una sola tabla `cards` con `CardEntity` como entidad unificada. Las tablas especializadas (`pokemon_cards`, `trainer_cards`, `energy_cards`) y sus entidades/repositorios asociados se eliminan o se dejan como código muerto marcado paralimpieza futura.

`CardCacheSyncService` escribe al `CardJpaRepository`. `CardLookupAdapter` lee de `CardJpaRepository`. `CardMapper` mapea `PokemonTcgApiCardDto → CardEntity` para persistencia y `CardEntity → CardDefinition` para el engine.

### Turn flags reseteados

`TurnManager.startTurn()` se invoca al comenzar cada turno, no solo al inicio. Los flags de turno (`hasAttacked`, `hasAttachedEnergy`, `hasRetreated`, `hasPlayedSupporter`) se resetean correctamente entre turnos.

### H2 compatibility

No se usan tipos PostgreSQL (`text[]`, `jsonb`, `gen_random_uuid()`). Las columnas de tipo array se almacenan como `TEXT` con valores separados por coma. Las columnas JSON se almacenan como `TEXT`. Los IDs se generan con `UUID` de Java, no con funciones de base de datos.

## Scope — Modificaciones

### 1. TurnManager.startTurn() — ADDED wiring

**Problema**: `TurnManager.startTurn()` nunca es llamado. Los flags de turno nunca se resetean, haciendo que después del turno 1 el jugador no pueda atacar, unir energía ni retirar.

**Solución**: El `EndTurnHandler` debe llamar a `turnManager.startTurn()` después de `turnManager.endTurn()` para el siguiente jugador. Alternativamente, `DrawCardHandler` debe llamar a `turnManager.startTurn()` si `startTurn()` no se ejecutó aún en este turno.

**MODIFIED** — `engine/turn/TurnManager.java`:
- `startTurn()` debe ser invocada desde el flujo de cambio de turno

**MODIFIED** — `engine/handlers/EndTurnHandler.java`:
- Después de `turnManager.endTurn()`, debe llamar a `turnManager.startTurn()` para preparar el turno del oponente

**Scenario**: Turno 2 comienza con flags reseteados
Given: Player 1 completó su turno (unió energía, atacó)
When: Player 1 ejecuta END_TURN
And: Player 2 ejecuta DRAW_CARD
Then: `hasAttachedEnergy` es `false`, `hasAttacked` es `false`, `hasRetreated` es `false`

### 2. Card catalog sync — FIXED

**Problema**: `CardCacheSyncService.syncAll()` escribe en `pokemon_cards`/`trainer_cards`/`energy_cards` a través de `PokemonCardJpaRepository`/`TrainerCardJpaRepository`/`EnergyCardJpaRepository`, pero `CardCatalogService.searchCards()` y `CardLookupAdapter` leen de `cards` vía `CardJpaRepository`. Después del sync, la búsqueda retorna 0 resultados.

**Solución**: Según `/docs/divisionCatalogo.md`, el catálogo usa una sola tabla `cards`. Se modifica `CardCacheSyncService` para que grabe en `CardJpaRepository` usando `CardMapper.toCardEntity()`, y se elimina (o desactiva) el uso de los repositorios especializados.

**MODIFIED** — `services/cards/CardCacheSyncService.java`:
- `syncAll()` debe usar `CardJpaRepository.saveAll()` con `CardMapper.toCardEntity()` en lugar de los repositorios `PokemonCardJpaRepository`/`TrainerCardJpaRepository`/`EnergyCardJpaRepository`
- Debe existir un único punto de escritura para el catálogo

**MODIFIED** — `mappers/cards/CardMapper.java`:
- `toCardEntity(PokemonTcgApiCardDto)` debe settear `number` correctamente (actualmente está hardcodeado a `null`)
- La lógica de mapeo existente (que parsea `set.id` para extraer `number`) debe completarse

**REMOVED** — Si no hay otro consumidor de los repositorios especializados:
- Las entidades `PokemonCardEntity`, `TrainerCardEntity`, `EnergyCardEntity` pueden marcarse como deprecated o eliminarse
- Los repos `PokemonCardJpaRepository`, `TrainerCardJpaRepository`, `EnergyCardJpaRepository` pueden marcarse como deprecated o eliminarse
- Los mappers `ApiCardMapper` puede marcarse como deprecated

**Scenario**: Sync escribe en la tabla correcta
Given: El catálogo está vacío (tabla `cards` vacía)
When: Se ejecuta `POST /api/cards/sync`
Then: La tabla `cards` contiene las cartas sincronizadas
And: `GET /api/cards?query=slugma` retorna resultados

**Scenario**: CardLookupAdapter encuentra cartas
Given: El catálogo tiene cartas sincronizadas
When: `CardLookupAdapter.getCardById("xy1-10")`
Then: Retorna un `PokemonCardDefinition` con datos correctos
And: No lanza excepción

### 3. Seed decks FK constraint — FIXED

**Problema**: La FK `deck_cards.card_id` referencia `cards(id)`, pero si las cartas no están en la tabla `cards`, los mazos seed no pueden insertarse.

**Solución**: Depende de la corrección del punto 2. Una vez que `cards` tiene datos, la FK se satisface. Además, `SeedDeckService` debe verificar que las cartas existen antes de insertar.

**MODIFIED** — `services/decks/SeedDeckService.java`:
- Antes de insertar un mazo seed, verificar que cada `cardId` existe en `CardJpaRepository`
- Si falta alguna carta, loguear warning y saltar ese mazo (no fallar)

**Scenario**: Seed deck se crea correctamente
Given: El catálogo tiene cartas sincronizadas (incluyendo `xy1-10`, `energy-fire-basic`)
When: `SeedDeckService` ejecuta en startup
Then: El mazo "Seed Fire Deck" se crea con 60 cartas
And: No hay excepción de FK constraint

### 4. RuleValidator — Active Pokémon evolution — FIXED

**Problema**: `RuleValidator.validate()` para `EVOLVE_POKEMON` solo busca al target en el bench (línea ~109-111). El handler `EvolvePokemonHandler` sí maneja tanto active como bench. Esto hace que evolucionar el Pokémon activo sea imposible.

**Solución**: Ampliar la validación en `RuleValidator` para que el target pueda estar en active o bench.

**MODIFIED** — `engine/rules/RuleValidator.java`:
- En la validación de `EVOLVE_POKEMON`, verificar si el target está en active Pokémon del jugador O en bench
- Si está en active, continuar validación normalmente (mismas reglas que bench)
- Si no está ni en active ni en bench, retornar error `CARD_NOT_IN_PLAY`

**Scenario**: Evolucionar Pokémon activo es válido
Given: Player tiene un `PokemonInPlay` activo con `cardDefinitionId = "xy1-4"` (Charmander)
And: `evolvedThisTurn = false`
And: Player tiene una carta `xy1-6` (Charmeleon) en mano
When: Player ejecuta `EVOLVE_POKEMON` con `targetPokemonInstanceId` del activo
Then: La validación pasa (retorna `true`)
And: El handler evoluciona al activo correctamente

### 5. GameAction payload UUID handling — FIXED

**Problema**: Los handlers esperan valores `String` en el `Map<String, Object>` payload (ej: `(String) payload.get("targetPokemonInstanceId")`), pero las clases tipadas usan `UUID`. Un caller que pase `UUID` object obtendrá `ClassCastException`.

**Solución**: Estandarizar a `String` en los handlers y agregar en `GameAction` un helper de parseo que convierta los valores del mapa al tipo correcto. Documentar que los payloads viajan con UUIDs como strings.

**MODIFIED** — `engine/action/GameAction.java`:
- Agregar método helper `getPayloadString(String key)` que retorne `payload.get(key)?.toString()` (tolera tanto String como UUID)
- Agregar método helper `getPayloadInt(String key)` que convierta a int

**MODIFIED** — Todos los handlers en `engine/handlers/`:
- Reemplazar casts directos `(String) payload.get(...)` por `action.getPayloadString(...)`
- Reemplazar `(int) payload.get(...)` por `action.getPayloadInt(...)`

**Scenario**: Payload con UUID string funciona
Given: Un caller envía `GameAction` con payload conteniendo `"targetPokemonInstanceId": "a1b2c3d4-..."`
When: El handler ejecuta `action.getPayloadString("targetPokemonInstanceId")`
Then: Retorna `"a1b2c3d4-..."`
And: `UUID.fromString(...)` no lanza excepción

### 6. CardAttackEntity — base_damage — FIXED

**Problema**: La columna `base_damage` existe en el SQL (y JPA la crearía desde la entidad) pero `CardAttackEntity` no tiene el field mapeado.

**Solución**: Agregar field `baseDamage` a `CardAttackEntity` y mapearlo en `CardMapper.toDetailResponse()`.

**MODIFIED** — `repositories/entities/CardAttackEntity.java`:
- Agregar `private Integer baseDamage` con `@Column(name = "base_damage")`

**MODIFIED** — `mappers/cards/CardMapper.java`:
- En `toDetailResponse()`, mapear `attack.baseDamage` al DTO correspondiente

**Scenario**: Ataque incluye base_damage
Given: Una carta tiene ataque con `base_damage = 30`
When: Se consulta `GET /api/cards/{id}`
Then: El response incluye `"baseDamage": 30` en el ataque

### 7. Dead code — REMOVED (opcional, baja prioridad para V1)

Se recomienda marcar como `@Deprecated` (no eliminar) las siguientes clases para no arriesgar compilación:

- `engine/model/GamePhase.java` — `@Deprecated` (usar `TurnPhase`)
- `engine/ErrorCode.java` — `@Deprecated` si no se usa (usar strings directo)
- `engine/model/GameMetadata.java` — solo si está vacío y no se referencia
- `engine/victory/VictoryResult.java` — solo si no se referencia
- `engine/status/StatusEffectManager.java` — solo si está vacío
- `engine/attack/AttackStep.java` — solo si está vacío

## Explicit non-goals

No implementar en este change:

- Refactor de `DeckMapper` reflection → setters (se puede hacer después)
- Refactor de payloads tipados para que los handlers los usen (se deja el `Map<String, Object>` actual)
- Implementación real de `StatusEffectManager` (sigue siendo stub)
- Implementación real de efectos de entrenadores complejos
- Tests de integración de match completo (se agregan tests unitarios mínimos)
- Migración a PostgreSQL
- Implementación de Flyway
- Nuevos endpoints REST
- Nuevos eventos WebSocket
- Frontend
- Auth/JWT/ranking/chat/animaciones
- `USE_ABILITY` handler (sigue retornando UNKNOWN_ACTION)
- `DRAW_CARD` como acción automática (sigue siendo acción explícita)

## Test requirements

El cambio debe agregar o verificar:

### Backend tests nuevos

1. **TurnManagerTest** — Verificar que `startTurn()` resetea flags (ya existe, puede necesitar extensión)
2. **RuleValidatorTest (NUEVO)** — Test de validación de EVOLVE_POKEMON con target en active
3. **CardCacheSyncServiceTest (NUEVO)** — Verificar que sync escribe en `CardJpaRepository`
4. **CardMapperTest (NUEVO)** — Verificar que `toCardEntity()` settea `number` correctamente

### Verificación

- `mvn compile` sin errores
- `mvn test` — todos los tests existentes + nuevos deben pasar
- Verificar manualmente que `POST /api/cards/sync` + `GET /api/cards?query=` funciona
- Verificar manualmente que un match puede completar al menos 3 turnos sin errores

## Expected OpenSpec output

Generar un cambio OpenSpec bajo:

```
openspec/changes/v1-game-loop-fix/
```

El cambio debe incluir:

- `proposal.md` — por qué estos fixes son necesarios para V1
- `design.md` — decisiones: unified card catalog, H2, startTurn wiring, payload helpers
- `tasks.md` — tareas por cada issue, con orden lógico (empezar por card catalog que es base, seguir por turn flags, luego validación, luego payloads)
- `specs/v1-game-loop-fix/spec.md` — Requirements ADDED/MODIFIED con Given/When/Then para cada fix

## Scope control

Este cambio es estrictamente de **corrección V1**. No agrega features nuevas. No cambia la arquitectura. No migra base de datos. Solo hace que lo que ya está implementado funcione correctamente.
