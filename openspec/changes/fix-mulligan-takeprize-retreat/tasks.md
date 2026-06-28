## 1. GameState — pendingPrizeOwnerPlayerId

- [ ] 1.1 Agregar campo `private UUID pendingPrizeOwnerPlayerId` en `GameState.java`
- [ ] 1.2 Agregar getter `getPendingPrizeOwnerPlayerId()` y setter `setPendingPrizeOwnerPlayerId(UUID)`

## 2. GameEventType — MULLIGAN_REVEALED

- [ ] 2.1 Agregar `MULLIGAN_REVEALED` al enum `GameEventType.java`

## 3. SetupManager — revelación en mulligan

- [ ] 3.1 En `resolveMulligan()`, antes de devolver las cartas al deck, generar un `GameEvent` con tipo `MULLIGAN_REVEALED` que incluya `playerId` y `revealedCardIds` (los `cardDefinitionId` de la mano)
- [ ] 3.2 Verificar que el evento se emite en cada iteración del mulligan

## 4. DeclareAttackHandler — setear pendingPrizeOwnerPlayerId

- [ ] 4.1 Cuando se detecta un KO, setear `state.setPendingPrizeOwnerPlayerId(player.getPlayerId())` antes de emitir eventos

## 5. TakePrizeCardHandler — validar KO ownership

- [ ] 5.1 Al inicio de `handle()`, verificar que `state.getPendingPrizeOwnerPlayerId()` sea igual al `playerId` de la acción
- [ ] 5.2 Si no coincide, retornar sin mutar estado ni emitir eventos
- [ ] 5.3 Después de tomar el premio, limpiar `state.setPendingPrizeOwnerPlayerId(null)`

## 6. RetreatActiveHandler — enteredTurnNumber

- [ ] 6.1 Al mover el Pokémon activo al bench, setear `active.setEnteredTurnNumber(state.getTurnNumber())`

## 7. TurnManager — reset pendingPrizeOwnerPlayerId

- [ ] 7.1 En `resetTurnFlags()`, agregar `state.setPendingPrizeOwnerPlayerId(null)`

## 8. Tests

- [ ] 8.1 Test: SetupManager emite `MULLIGAN_REVEALED` cuando un jugador no tiene básico
- [ ] 8.2 Test: TakePrizeCardHandler rechaza si el jugador no es el dueño del KO
- [ ] 8.3 Test: TakePrizeCardHandler acepta si el jugador es el dueño del KO
- [ ] 8.4 Test: RetreatActiveHandler setea `enteredTurnNumber` al benched Pokémon
- [ ] 8.5 Test: pendingPrizeOwnerPlayerId se resetea al iniciar turno

## 9. Verification

- [ ] 9.1 Compilar con `mvn compile`
- [ ] 9.2 Pasar `mvn test`
- [ ] 9.3 Preservar `ApplicationTests.contextLoads`
- [ ] 9.4 No modificar clases fuera del ownership de Persona 2/3
