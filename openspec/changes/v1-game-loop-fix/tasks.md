## 1. Card Catalog — unified persistence

- [x] 1.1 Fix `CardMapper.toCardEntity()`: set `number` from `set.id` (parsear el número después del guión, ej: `"xy1-10"` → `"10"`)
- [x] 1.2 Modificar `CardCacheSyncService.syncAll()`: reemplazar escritura a repositorios especializados por `CardJpaRepository.saveAll()` usando `CardMapper.toCardEntity()`
- [x] 1.3 Marcar `PokemonCardEntity`, `TrainerCardEntity`, `EnergyCardEntity`, `ApiCardMapper`, y sus repositorios como `@Deprecated`
- [x] 1.4 `CardAttackEntity` pre-existente con `@Column(name = "base_damage")` (ya estaba)
- [x] 1.5 Mapear `baseDamage` en `AttackDto` y `CardMapper.toAttackDto()` / `toAttackEntity()`

## 2. Seed Decks — FK safety

- [x] 2.1 Modificar `SeedDeckService`: antes de insertar seed deck, verificar que cada `cardId` existe en `CardJpaRepository`
- [x] 2.2 Si falta alguna carta, loguear warning y saltar ese mazo sin lanzar excepción

## 3. Turn Lifecycle — flag reset

- [x] 3.1 Modificar `EndTurnHandler.handle()`: después de `turnManager.endTurn()`, invocar `turnManager.startTurn()` para el siguiente jugador
- [x] 3.2 Verificar que `TurnManager.startTurn()` resetea todos los flags (pre-existente, verificado)

## 4. RuleValidator — evolution target scope

- [x] 4.1 Modificar `RuleValidator.validate(EVOLVE_POKEMON)`: validar target en `player.getActivePokemon()` o `player.getBench()`
- [x] 4.2 El resto de validaciones aplican igual para active y bench (misma lógica compartida)

## 5. GameAction — payload helpers

- [x] 5.1 Agregar `getPayloadString(String key)` en `GameAction`
- [x] 5.2 Agregar `getPayloadInt(String key)` en `GameAction`
- [x] 5.3 Reemplazar en todos los handlers los casts directos `(String) payload.get(...)` por `action.getPayloadString(...)`
- [x] 5.4 Reemplazar en todos los handlers los casts `(int) payload.get(...)` por `action.getPayloadInt(...)`

## 6. Backend Tests

- [ ] 6.1 Agregar `CardMapperTest`: verificar que `toCardEntity()` settea `number` correctamente
- [ ] 6.2 Agregar `RuleValidatorTest`: test de evolución con target en active Pokémon
- [ ] 6.3 Extender `TurnManagerTest`: verificar que `startTurn()` resetea todos los flags
- [ ] 6.4 Agregar test para `GameAction.getPayloadString()` con UUID y con String

## 7. Verification

- [x] 7.1 `mvn compile` — sin errores
- [ ] 7.2 Ejecutar `mvn test` — 32/33 tests pasan (1 pre-existing failure: `RetreatActiveHandlerTest`)
- [ ] 7.3 Verificar manualmente: `POST /api/cards/sync` + `GET /api/cards?query=slugma` retorna resultados
