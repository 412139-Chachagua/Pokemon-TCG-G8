## 1. GameState — Foundation model changes

- [x] 1.1 Agregar `Set<UUID> playersWhoCompletedFirstTurn` a `GameState` con `markPlayerCompletedFirstTurn(UUID)` y `hasPlayerCompletedFirstTurn(UUID)`
- [x] 1.2 Agregar `boolean pendingKOReplacement` y `UUID knockedOutPlayerId` a `GameState` con getters/setters
- [x] 1.3 Agregar `boolean suddenDeath` e `int prizeCountPerPlayer` a `GameState` con getters/setters (default: suddenDeath=false, prizeCountPerPlayer=6)
- [x] 1.4 Agregar tests unitarios para todos los nuevos campos de GameState

## 2. Enums — GameActionType y GameEventType

- [x] 2.1 Agregar `CHOOSE_KO_REPLACEMENT` a `GameActionType`
- [x] 2.2 Agregar `KO_REPLACEMENT_REQUIRED`, `KO_REPLACEMENT_DONE`, `SUDDEN_DEATH_STARTED`, `CONFUSION_SELF_HIT` a `GameEventType` (STADIUM_PLAYED/REMOVED ya existen)
- [x] 2.3 Agregar tests unitarios verificando que los nuevos enums existen

## 3. R1 — Evolución en primer turno del jugador (Item 14)

- [x] 3.1 Modificar `EndTurnHandler` para invocar `GameState.markPlayerCompletedFirstTurn(playerId)` al finalizar el primer turno del jugador (en TurnManager.endTurn)
- [x] 3.2 Modificar `TurnManager.endTurn()` para invocar `markPlayerCompletedFirstTurn` si es el primer turno del jugador actual
- [x] 3.3 Modificar `RuleValidator.validate(EVOLVE_POKEMON)` para usar `gameState.hasPlayerCompletedFirstTurn(playerId)` en lugar de `gameState.getTurnNumber() == 1`
- [x] 3.4 Verificar que `RuleValidator.validate(DECLARE_ATTACK)` también use la misma lógica per-player
- [x] 3.5 Agregar tests unitarios: evolución prohibida en primer turno de cada jugador, permitida después

## 4. R2 — Confusión como acción exitosa (Item 26)

- [x] 4.1 Modificar `AttackResolver.resolve()` para NO mutar damageCounters del Pokémon confundido en autogolpe; solo retornar `confusedSelfHit=true` y `selfDamageCounters=3`
- [x] 4.2 Modificar `DeclareAttackHandler` para aplicar daño de autogolpe cuando `result.confusedSelfHit() == true`
- [x] 4.3 En `DeclareAttackHandler`, verificar KO del atacante después del autogolpe y manejar reemplazo si aplica
- [x] 4.4 En `DeclareAttackHandler`, setear `hasAttacked=true` y avanzar fase (acción exitosa, NO reportar error)
- [x] 4.5 Publicar eventos `CONFUSION_SELF_HIT` y `DAMAGE_APPLIED` en el flujo de autogolpe
- [x] 4.6 Agregar tests: confusión autogolpe sin KO, confusión autogolpe con KO

## 5. R3 — Reemplazo de KO por elección del jugador (Item 42)

- [x] 5.1 Modificar `DeclareAttackHandler`: al detectar KO del Activo rival, setear `pendingKOReplacement=true` y `knockedOutPlayerId`, publicar `KO_REPLACEMENT_REQUIRED`, NO auto-seleccionar bench.get(0)
- [x] 5.2 Si el bench del jugador afectado está vacío, declarar derrota directamente (GAME_OVER)
- [x] 5.3 Implementar `ChooseKOReplacementHandler` que recibe CHOOSE_KO_REPLACEMENT con el benchPokemonId, valida que esté en bench, mueve a Active, limpia flags, publica KO_REPLACEMENT_DONE, avanza fase
- [x] 5.4 Modificar `RuleValidator` para validar CHOOSE_KO_REPLACEMENT solo cuando `pendingKOReplacement == true`
- [x] 5.5 Registrar `ChooseKOReplacementHandler` en `GameEngine` (el handler registry)
- [x] 5.6 Agregar tests: reemplazo válido, reemplazo inválido, bench vacío = derrota

## 6. R5 — Estadio permanece en juego (Item 70)

- [x] 6.1 Verificar que `StadiumPlayResolver` ya existe y funciona: coloca carta en `stadiumCardInstanceId`, mueve anterior al discard, publica STADIUM_PLAYED/STADIUM_REMOVED
- [x] 6.2 Modificar `PlayTrainerHandler.handle()` para NO descartar cartas de tipo `TrainerSubtype.STADIUM`; delegar a `StadiumPlayResolver`
- [x] 6.3 Verificar que `TurnManager.startTurn()` NO limpie `stadiumCardInstanceId`
- [x] 6.4 Agregar tests: Estadio juega y permanece, reemplazo de Estadio, Estadio persiste entre turnos

## 7. R6 — Validación de Energía Especial por nombre (Item 65)

- [x] 7.1 En `DeckValidator`, después del chequeo actual por cardId, agregar agrupado por `name` para cartas `EnergyCardType.SPECIAL`
- [x] 7.2 Si el total por nombre supera 4, agregar error `MORE_THAN_4_COPIES`
- [x] 7.3 Agregar tests: 4 Special Energy same name = OK, 5 = ERROR, Basic Energy no afectado

## 8. R7 — AS TÁCTICO máximo 1 (Item 67)

- [x] 8.1 En `DeckValidator`, iterar cartas y contar aquellas con `TrainerCardDefinition.isAceSpec() == true` considerando quantities
- [x] 8.2 Si el total supera 1, agregar error `ACE_SPEC_LIMIT_EXCEEDED`
- [x] 8.3 Agregar tests: 0 Ace Spec = OK, 1 Ace Spec = OK, 2 Ace Spec = ERROR

## 9. R4 — Muerte Súbita (Item 59)

- [x] 9.1 Modificar `VictoryConditionChecker.check()` para retornar `VictoryCheckResult(suddenDeath=true, gameOver=false)` cuando ambos jugadores toman su última Prize en el mismo turno
- [x] 9.2 Modificar `SetupManager` para aceptar `prizeCount` parametrizado en lugar de hardcode 6 (factory method o parámetro)
- [x] 9.3 En `GameEngine`, detectar `suddenDeath=true` de VictoryConditionChecker y orquestar reinicio parcial: restaurar mazos, 1 premio, repartir manos, decidir turno
- [x] 9.4 Publicar `SUDDEN_DEATH_STARTED` al iniciar la muerte súbita
- [x] 9.5 Agregar tests: detección de empate simultáneo, SetupManager con prizeCount=1, reinicio parcial

## 10. Build & verify

- [x] 10.1 Ejecutar `mvn compile` y verificar que compila sin errores
- [x] 10.2 Ejecutar tests unitarios de las áreas modificadas
- [x] 10.3 Ejecutar test suite completa del backend
