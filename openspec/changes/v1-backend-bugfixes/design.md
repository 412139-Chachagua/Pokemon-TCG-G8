## Context

El backend fue evaluado extensivamente y se identificaron ~20 bugs que impiden una V1 funcional. La arquitectura hexagonal con handler pattern y engine aislado es correcta, pero errores puntuales en handlers, validadores, mapeo de datos y contratos de API rompen el flujo de juego. Esta corrección abarca todas las capas del backend (engine, catálogo, mazos, API, WebSocket) sin modificar la arquitectura existente ni implementar frontend.

## Goals / Non-Goals

**Goals:**
- Todos los handlers compilan y ejecutan correctamente
- El estado de juego se devuelve en cada respuesta de acción
- No hay race conditions en acciones concurrentes sobre un mismo match
- Las validaciones cubren todos los action types (DRAW_CARD, TAKE_PRIZE, CHOOSE_REPLACEMENT)
- El KO por ataque solo afecta al Pokémon correcto (active vs bench)
- La detección de victoria cubre deck-out de cualquier jugador
- El retreat valida tipos de energía descartados
- El catálogo se sincroniza sin bloquear startup
- La cache de cards se invalida en resync
- Los seed decks usan IDs de carta reales
- Los formatos de respuesta API coinciden con lo que el FE espera
- El WebSocket publica estado privado a cola `/queue`
- El código muerto se elimina
- `PlayTrainerHandler` tiene efectos básicos (descarte, flags Supporter/Stadium)

**Non-Goals:**
- No se implementa frontend Angular
- No se implementan efectos de estado (StatusEffectManager sigue siendo stub)
- No se implementan efectos de entrenador complejos (solo descarte + flags)
- No se implementa `USE_ABILITY` handler funcional
- No se migra a PostgreSQL
- No se agrega auth/JWT/ranking
- No se agregan nuevos endpoints que no existan ya en el diseño actual

## Decisions

### D1 — Lock por matchId con ConcurrentHashMap + ReentrantLock
Se agrega un `ConcurrentHashMap<UUID, ReentrantLock>` en `MatchApplicationService` para serializar acciones sobre un mismo match. Alternativa considerada: optimistic locking con versión en `MatchStateEntity`. Se descarta porque el estado se serializa como JSON y no hay columna de versión. El lock en memoria es suficiente para un solo nodo (MVP).

### D2 — ActionResult con estado poblado
En `GameEngine.applyAction()`, después de ejecutar el handler, se construye `PublicGameState` y `PrivatePlayerState` usando `MatchQueryService` y se setean en `ActionResult`. Alternativa: devolver solo matchId y forzar al FE a pedir `GET /state`. Se descarta porque duplica requests y empeora UX.

### D3 — Validadores agregados, no refactorizados
`TAKE_PRIZE_CARD`, `CHOOSE_KNOCKOUT_REPLACEMENT` y `DRAW_CARD` tenían stubs no-op por diseño V1 original. Se agrega validación real sin refactorizar la estructura existente. DRAW_CARD verifica `hasDrawnForTurn`.

### D4 — Eliminación de código muerto, no simple marca @Deprecated
Clases como `GamePhase.java`, `StatusEffectManager.java`, `AttackStep.java`, `GameMetadata.java`, `VictoryResult.java` y los Payload DTOs tipados están completamente sin uso. Se eliminan directamente. No hay código que las referencie.

### D5 — CardSearchResponse como wrapper DTO en vez de Page
El BE actualmente retorna `Page<CardSummaryResponse>` de Spring Data. El FE espera un objeto plano con `items`, `page`, `size`, `totalItems`. Se crea `CardSearchResponse` como DTO wrapper y se mapea desde `Page`.

### D6 — Estado privado vía /queue en WebSocket
`MatchWebSocketPublisher` actualmente publica solo a `/topic/matches/{id}/events`. Se agrega publicación a `/queue/matches/{id}/{playerId}` para cada jugador, filtrando información privada (mano, cartas de premio) del evento público.

### D7 — Sync asíncrono con CompletableFuture
Se reemplaza la ejecución sincrónica en `ApplicationReadyEvent` con `CompletableFuture.runAsync()` para no bloquear el startup. Alternativa considerada: `@Async` de Spring. Se descarta porque requiere `@EnableAsync` y agrega complejidad innecesaria para un solo método.

### D8 — SeedDeckService usa CardJpaRepository ya inyectado
Ya se corrigió en `v1-game-loop-fix`. Los IDs de carta deben actualizarse de `"energy-fire-basic"` a IDs reales del set XY1 (ej: `"xy1-150"` para energía Fire básica si existe en el set).

## Risks / Trade-offs

- **[Lock en memoria]**: Si la app escala a múltiples nodos, el lock no funciona entre instancias. → Mitigación: MVP es single-node. Para multi-nodo se necesita lock externo (Redis, DB pessimista).
- **[Serialización JSON de GameState]**: La persistencia actual serializa `GameState` como JSON. Si se agrega el lock, dos lecturas simultáneas no corromperán el estado. → Mitigación: el lock cubre load→modify→save como transacción atómica.
- **[CardSearchResponse rompe clients existentes]**: Cambiar el formato de respuesta de `Page` a `CardSearchResponse` es breaking para cualquier cliente que consuma el endpoint actual. → Mitigación: no hay clients en producción (MVP), y el FE existente espera el nuevo formato.
- **[Eliminación de código muerto]**: Si alguna clase eliminada es referenciada por código no detectado, falla la compilación. → Mitigación: se verifica con `mvn compile` antes de commit.
- **[WebSocket /queue sin autenticación]**: Cualquier cliente suscrito a `/queue/matches/{id}/{playerId}` recibiría datos privados si adivina el playerId. → Mitigación: MVP no tiene auth. Se documenta como riesgo para V2.
