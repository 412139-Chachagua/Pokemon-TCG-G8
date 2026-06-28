## Why

El backend tiene la arquitectura correcta (hexagonal, handler pattern, engine aislado) pero varios defectos concretos rompen el flujo de juego: los turn flags nunca se resetean (turno 2+ es injugable), el catálogo de cartas escribe en tablas que nadie lee (búsqueda retorna vacío), y los mazos seed fallan por FK constraint. Sin estas correcciones no existe una V1 jugable.

## What Changes

- **TurnManager.startTurn() wireado**: invocar `startTurn()` al comenzar cada turno para resetear flags. Sin esto el juego se rompe después del turno 1.
- **Card catalog unified**: `CardCacheSyncService` escribe a `cards` vía `CardJpaRepository` (no a tablas especializadas). Sigue el diseño de `/docs/divisionCatalogo.md`.
- **Seed decks protegidos**: verificar que las cartas existen antes de insertar; saltar con warning si falta alguna.
- **RuleValidator acepta evolución de Active Pokémon**: validación busca target en active O bench, no solo bench.
- **Payload helpers**: `GameAction.getPayloadString()` y `getPayloadInt()` toleran tanto `String` como `UUID` en el Map.
- **CardAttackEntity mapea base_damage**: agrega el field faltante.
- **Dead code marcado `@Deprecated`**: clases vacías o duplicadas se marcan sin eliminar.

## Capabilities

### New Capabilities

- `turn-lifecycle`: gestión correcta del ciclo de turno con reset de flags y transiciones entre DRAW/MAIN/ATTACK/BETWEEN_TURNS

### Modified Capabilities

- `card-catalog-management`: el sync escribe en la tabla unificada `cards`, no en tablas especializadas; `CardLookupAdapter` consulta desde la misma tabla
- `cross-layer-contract-alignment`: `RuleValidator` ahora permite evolución de Pokémon activo; payload helpers evitan ClassCastException con UUIDs

## Impact

- **Backend**: 7 archivos modificados en engine/handlers, engine/rules, engine/action, services/cards, mappers/cards, repositories/entities, services/decks
- **Base de datos**: H2 existente sin cambios; el schema se maneja con `ddl-auto` de JPA
- **API**: endpoints REST sin cambios en paths ni formatos; `CardAttackEntity` agrega field `baseDamage` al response de detalle
- **Dependencias**: ninguna
