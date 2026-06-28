## Why

El backend tiene la arquitectura correcta (hexagonal, handler pattern, engine aislado) pero ~15 bugs concretos impiden que un match complete su ciclo de principio a fin. Algunos son errores de compilación (`PutBasicOnBenchHandler` referencia variable no declarada), otros son fallas de lógica (`DeclareAttackHandler` asume que el defensor es el activo, `VictoryConditionChecker` no chequéa deck-out del oponente) y otros son fragilidades que rompen flujos enteros (sin lock de concurrencia, estado público siempre null en respuestas, sin validación de `hasDrawnForTurn`).

Sin estas correcciones no es posible tener una V1 jugable.

## What Changes

- Corregir error de compilación en `PutBasicOnBenchHandler` (variable `payload` no declarada)
- Poblar `publicState` y `privateState` en `ActionResult` para que el cliente reciba el estado actualizado
- Agregar lock por matchId para prevenir race conditions en load→modify→save
- Agregar validación en `RuleValidator` para `TAKE_PRIZE_CARD` y `CHOOSE_KNOCKOUT_REPLACEMENT` (verificar pending decision)
- Agregar guarda `hasDrawnForTurn` en `DRAW_CARD`
- Corregir `DeclareAttackHandler` KO: solo nullificar active si el defensor ES el active
- Corregir `VictoryConditionChecker` para detectar deck-out de cualquier jugador
- Validar tipos de energía descartados en retreat contra `retreatCost`
- Remover o deshabilitar `USE_ABILITY` de `GameActionType` (no tiene handler)
- Ejecutar sync de catálogo en hilo separado (`@Async`)
- Invalidar cache de cards después de resync
- Reemplazar IDs falsos en `SeedDeckService` por IDs reales del set XY1
- Publicar error cuando `DeclareAttackHandler` detecta energía insuficiente
- Reemplazar reflection en `DeckMapper` por setters/constructores
- Mapear campos faltantes en `CardMapper` (abilities, evolvesTo, energyCardType, trainerSubtype, isAceSpec)
- Alinear formatos de respuesta BE con lo que el FE espera (`CardSearchResponse`, match creation)
- Publicar estado privado a `/queue/matches/{id}/{playerId}` vía WebSocket
- Eliminar código muerto: `GamePhase.java`, `StatusEffectManager.java`, `AttackStep.java`, `GameMetadata.java`, `VictoryResult.java`, Payload DTOs tipados
- Implementar efectos básicos de `PlayTrainerHandler` (descarte, flag Supporter/Stadium)

## Capabilities

### New Capabilities

- `match-state-locking`: Lock por matchId para prevenir race conditions en acciones concurrentes
- `websocket-private-state`: Publicación de estado privado del jugador a cola `/queue` vía WebSocket

### Modified Capabilities

- `card-catalog-management`: Sincronización asincrónica del catálogo (no bloquear startup), invalidación de cache en resync, mapeo de campos faltantes en `CardMapper` (abilities, evolvesTo, energyCardType, trainerSubtype, isAceSpec), formato de respuesta `CardSearchResponse` alineado con FE
- `engine-p2`: Corrección de compilación en `PutBasicOnBenchHandler`, validación de pending decision en `TAKE_PRIZE_CARD` y `CHOOSE_KNOCKOUT_REPLACEMENT`, guarda `hasDrawnForTurn`, corrección de KO en bench, deck-out del oponente, validación de tipos de energía en retreat, eliminación/deshabilitación de `USE_ABILITY`, error explícito en ataque sin energía, estado público/populado en `ActionResult`, reflexión reemplazada en `DeckMapper`, código muerto eliminado, `PlayTrainerHandler` con efectos básicos
- `project-structure-contract`: Eliminación de clases muertas (`GamePhase.java`, `StatusEffectManager.java`, `AttackStep.java`, `GameMetadata.java`, `VictoryResult.java`, Payload DTOs tipados)
- `card-lookup-adapter`: Cache invalidado después de resync de catálogo

## Impact

- **Backend**: ~20 archivos modificados, ~6 archivos eliminados
- **API**: Cambios en formato de respuesta `GET /api/cards` (de `Page` a `CardSearchResponse`), endpoint `POST /api/matches` acepta nuevo formato, nuevo endpoint `POST /api/decks/validate`
- **WebSocket**: Nuevo publisher para cola `/queue` con estado privado
- **Datos**: Cache de cards se invalida en resync
- **Base de datos**: Sin cambios en schema (H2 in-memory se mantiene)
- **Frontend**: Sin cambios (los ajustes de contrato son en el BE para que el FE existente funcione cuando se implemente)
