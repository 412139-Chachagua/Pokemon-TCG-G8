## 1. Foundation — Core Infrastructure

- [x] 1.1 Create `EffectType` enum with all effect types (`DRAW_CARDS`, `HEAL`, `SEARCH_BASIC_POKEMON`, `SEARCH_ENERGY`, `EVOLVE_SEARCH`, `DISCARD_AND_DRAW`, `SWITCH_POKEMON`, `SHUFFLE_HAND_INTO_DECK`, `ATTACH_EXTRA_ENERGY`, `DAMAGE_MODIFY`, `CONDITION_REMOVE`, `REVIVE`, `TOOL_ATTACH`, `STADIUM_PLAY`)
- [x] 1.2 Create `TrainerEffectResolver` interface with `resolve(EngineContext, PlayerState, TrainerCardDefinition, Map<String,Object>)` and `getType()` methods
- [x] 1.3 Create `TrainerEffectRegistry` class mapping `effectCode: String` → `EffectType` and `EffectType` → `TrainerEffectResolver`, with `resolve()` facade method
- [x] 1.4 Add new `GameEventType` entries: `TRAINER_EFFECT_RESOLVED`, `STADIUM_PLAYED`, `STADIUM_REMOVED`, `TOOL_ATTACHED`, `CARDS_DRAWN`, `POKEMON_HEALED`, `POKEMON_SEARCHED`
- [x] 1.5 Rewrite `PlayTrainerHandler.handle()` to delegate to `TrainerEffectRegistry.resolve()` instead of MVP discard-only logic (keep tier flags for Supporter/Stadium limits)

## 2. Core Resolvers — Draw / Heal / Search

- [x] 2.1 Implement `DrawCardsResolver` — roba N cartas del mazo a la mano, publica `CARDS_DRAWN`
- [x] 2.2 Implement `HealResolver` — remueve N contadores de daño de `targetPokemonInstanceId`, publica `POKEMON_HEALED`
- [x] 2.3 Implement `SearchBasicPokemonResolver` — busca Pokémon Básico en mazo, coloca en Banca, baraja
- [x] 2.4 Implement `SearchEnergyResolver` — busca Energía Básica en mazo, une a Pokémon
- [x] 2.5 Implement `EvolveSearchResolver` — busca carta de Evolución en mazo, la pone en la mano
- [x] 2.6 Implement `DiscardAndDrawResolver` — descarta N cartas de mano y roba M cartas

## 3. Core Resolvers — Switch / Shuffle / Modify

- [x] 3.1 Implement `SwitchPokemonResolver` — cambia Activo por Pokémon de Banca seleccionado
- [x] 3.2 Implement `ShuffleHandIntoDeckResolver` — pone mano en mazo, baraja, roba N cartas
- [x] 3.3 Implement `AttachExtraEnergyResolver` — une una Energía adicional (supera el límite de 1 por turno)
- [x] 3.4 Implement `DamageModifyResolver` — almacena modificador de daño en `GameState` para consulta en `AttackResolver`
- [x] 3.5 Implement `ConditionRemoveResolver` — remover condición(es) especial(es) de un Pokémon
- [x] 3.6 Implement `ReviveResolver` — toma Pokémon Básico de pila de descartes y lo coloca en Banca

## 4. Stadium Zone

- [x] 4.1 Implement `StadiumPlayResolver` — asigna `cardInstanceId` a `GameState.stadiumCardInstanceId`, descarta Estadio anterior si existe, publica eventos
- [x] 4.2 Update `GameState.getStadiumCardInstanceId()` integration — asegurar que persiste correctamente en `StatePersisterAdapter`

## 5. Pokemon Tools

- [x] 5.1 Add `ATTACH_TOOL` to `GameActionType` enum
- [x] 5.2 Implement `AttachToolHandler` — valida herramienta en mano, Pokémon sin herramienta, asigna `toolCardInstanceId`, publica `TOOL_ATTACHED`
- [x] 5.3 Add `TOOL_ALREADY_EQUIPPED` to `ErrorCode` enum
- [x] 5.4 Register `AttachToolHandler` in `GameEngine.buildDefaultHandlers()`
- [x] 5.5 Ensure `DeclareAttackHandler` descarta herramienta al KO del Pokémon (junto con energías y descarte)

## 6. RuleValidator Updates

- [x] 6.1 Add validation for `ATTACH_TOOL` action in `RuleValidator.validate()`: fase MAIN, carta es herramienta, target en campo y sin herramienta
- [x] 6.2 Add validation for target requirements: verificar que los targets requeridos por `EffectType` estén presentes en el payload
- [x] 6.3 Add validation for unknown `effectCode`: rechazar cartas con effectCode no registrado con error `UNKNOWN_EFFECT_CODE`
- [x] 6.4 Add `UNKNOWN_EFFECT_CODE` and `MISSING_TARGET` to `ErrorCode` enum

## 7. GameEngine Integration

- [x] 7.1 Register all resolvers in `TrainerEffectRegistry` and inject into `GameEngine`
- [x] 7.2 Wire `PlayTrainerHandler` with `TrainerEffectRegistry` via constructor injection
- [x] 7.3 Update `GameEngineConfig` (or equivalent Spring config) to instantiate and wire new components
- [x] 7.4 Verify game compiles and boots without errors

## 8. Unit Tests — Resolvers

- [x] 8.1 Unit test `DrawCardsResolver` — draw with default count, payload override, deck smaller than draw count
- [x] 8.2 Unit test `HealResolver` — heal with payload count, not exceeding damage counters, missing target
- [x] 8.3 Unit test `SearchBasicPokemonResolver` — bench limit, invalid card index, non-Pokemon card, non-Basic card, successful search
- [x] 8.4 Unit test `SearchEnergyResolver` — invalid index, non-energy card, non-basic energy, successful attachment
- [x] 8.5 Unit test `EvolveSearchResolver` — invalid index, non-Pokemon card, successful search to hand
- [x] 8.6 Unit test `DiscardAndDrawResolver` — discard specific index, discard all hand, draw after discard
- [x] 8.7 Unit test `SwitchPokemonResolver` — empty bench, missing target, successful switch
- [x] 8.8 Unit test `ShuffleHandIntoDeckResolver` — hand shuffled into deck, draw after shuffle
- [x] 8.9 Unit test `AttachExtraEnergyResolver` — invalid hand index, non-energy card, successful attach
- [x] 8.10 Unit test `DamageModifyResolver` — modifier stored in TurnFlags
- [x] 8.11 Unit test `ConditionRemoveResolver` — remove specific condition, remove all conditions, missing target
- [x] 8.12 Unit test `ReviveResolver` — bench limit, invalid discard index, non-Pokemon card, non-Basic card, successful revive
- [x] 8.13 Unit test `ToolAttachResolver` — missing target, tool already equipped, successful attach
- [x] 8.14 Unit test `StadiumPlayResolver` — play with existing stadium (replacement), play without existing stadium

## 9. Unit Tests — Registry and Handlers

- [x] 9.1 Unit test `TrainerEffectRegistry` — registration, lookup, resolve
- [x] 9.2 Unit test `PlayTrainerHandler` — supporter flag set, stadium not discarded, effect resolution on known effectCode
- [x] 9.3 Unit test `AttachToolHandler` — success, tool already equipped, missing target, non-item card
