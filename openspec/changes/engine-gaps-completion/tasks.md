## 1. TurnManager — advancePhase

- [x] 1.1 Add `advancePhase(GameState state)` method to `TurnManager` with DRAW→MAIN→ATTACK→BETWEEN_TURNS cycle
- [x] 1.2 Return early if phase is already BETWEEN_TURNS

## 2. TurnManager — evolvedThisTurn reset

- [x] 2.1 In `startTurn()`, iterate over current player's active and bench Pokémon and set `evolvedThisTurn = false`

## 3. TurnManager — Auto-resolve DRAW

- [x] 3.1 In `startTurn()`, draw 1 card from deck to hand (if phase DRAW and !hasDrawnForTurn)
- [x] 3.2 Handle deck-out: if deck is empty, generate deck-out event and check victory
- [x] 3.3 Set `hasDrawnForTurn = true` after auto-resolve

## 4. DeclareAttackHandler — Auto-end-turn

- [x] 4.1 After a successful attack, call `advancePhase(state)` to move to BETWEEN_TURNS

## 5. RetreatActiveHandler — Clear specialConditions

- [x] 5.1 After moving Pokémon from Active to Bench, clear its `specialConditions` list

## 6. EvolvePokemonHandler — Clear specialConditions

- [x] 6.1 After creating the evolved Pokémon, set `specialConditions` to empty list

## 7. Jakarta validation — DTOs

- [x] 7.1–7.6 Add `@NotBlank`/`@NotNull` on all required fields in `CreateMatchRequest`, `JoinMatchRequest`, `GameActionRequest`

## 8. Jakarta validation — Controllers

- [x] 8.1 Add `@Valid` on `MatchController.createMatch()`
- [x] 8.2 Add `@Valid` on `MatchController.joinMatch()`
- [x] 8.3 Add `@Valid` on `GameActionController.executeAction()`
- [x] 8.4 Add `MethodArgumentNotValidException` handler in `GlobalExceptionHandler` returning 400

## 9. Verification

- [x] 9.1 `mvn compile` passes
- [ ] 9.2 `mvn test` — 35/36 pass; 1 pre-existing failure in `TakePrizeCardHandlerTest` (not introduced by this change)
