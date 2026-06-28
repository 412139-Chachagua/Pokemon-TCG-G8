## Why

Las cartas de Entrenador (`PlayTrainerHandler`) actualmente son un MVP que solo mueve la carta al descarte sin ejecutar efecto alguno. Sin efectos reales (robar cartas, curar, buscar, etc.), ninguna carta de Entrenador del set `xy1` funciona, bloqueando la jugabilidad básica del TCG.

## What Changes

- Implementar sistema de resolución de efectos de cartas de Entrenador basado en `TrainerCardDefinition.effectCode`
- Crear resolvers concretos para cada tipo de efecto necesario en el set `xy1` (DRAW_CARDS, HEAL, SEARCH, SWITCH, STADIUM, TOOL, etc.)
- Implementar zona de Estadio compartida en el estado del juego
- Implementar equipamiento de Herramientas Pokémon
- Agregar soporte para selección de targets en efectos que lo requieran
- Publicar nuevos eventos WebSocket para efectos resueltos

## Capabilities

### New Capabilities
- `trainer-effects-core`: Sistema de resolución de efectos con patrón Strategy, registro de EffectType, y ejecución de efectos desde `PlayTrainerHandler`
- `stadium-zone`: Zona de Estadio compartida con reemplazo y persistencia en `GameState.stadiumCardInstanceId`
- `pokemon-tools`: Equipamiento de Herramientas Pokémon con límite de 1 por Pokémon y descarte al KO
- `trainer-target-selection`: Soporte en `GameAction.payload` para selección de targets (Pokémon, cartas en mano/mazo/descarte)

### Modified Capabilities
- *(ninguna — los specs existentes no cubren efectos de entrenador)*

## Impact

- Backend: `PlayTrainerHandler.java` se reescribe para delegar en resolvers; se crean ~15 clases nuevas en `engine/trainer/`; `RuleValidator.java` agrega validaciones para nuevos tipos de acción; `GameActionType.java` agrega `ATTACH_TOOL`
- Eventos WebSocket: se agregan 7 nuevos `GameEventType`
- Base de datos: sin cambios (los campos `effectCode` y `stadiumCardInstanceId`/`toolCardInstanceId` ya existen)
- Sin cambios en frontend ni API REST
