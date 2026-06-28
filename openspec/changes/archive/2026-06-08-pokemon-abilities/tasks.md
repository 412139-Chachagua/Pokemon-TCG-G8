## Orden de ejecución

Ejecutar las secciones en este orden exacto (dependencias):
**1 → 2 → 3 → 5 → 4 → 6 → 7 → 8 → 9 → 10 → 11**

Las fases internas del plan original no coinciden con el orden de compilación.
La sección 5 (Registry) debe ejecutarse ANTES de la 4 (Tracking)
porque el handler depende del registry, y la 6 (Handler) depende de ambas.

---

## 1. Domain Model

- [x] 1.1 Create `AbilityType.java` enum in `cards/domain/` with values: ABILITY, POKEMON_POWER, POKEMON_BODY
- [x] 1.2 Create `AbilityDefinition.java` in `cards/domain/` with fields: name (String), text (String), type (AbilityType)
- [x] 1.3 Add `List<AbilityDefinition> abilities` field with getter/setter to `PokemonCardDefinition.java`
- [x] 1.4 Update contract `04-card-model-contract.md` to include AbilityDefinition and abilities field

## 2. Enums & Error Codes

- [x] 2.1 Add `USE_ABILITY` to `GameActionType.java`
- [x] 2.2 Add `ABILITY_USED`, `ABILITY_BLOCKED` to `GameEventType.java`
- [x] 2.3 Add `ABILITY_NOT_FOUND`, `ABILITY_ALREADY_USED`, `POKEMON_CANNOT_USE_ABILITY` to `ErrorCode.java`
- [x] 2.4 Update contract `03-enums-contract.md` with new enum values

## 3. DB Hydration & REST Exposure

- [x] 3.1 Add `toAbilityDefinitions(String jsonAbilities)` method to `CardMapper.java` — deserializes JSON `[{name, text, type}]` to `List<AbilityDefinition>`
- [x] 3.2 Modify `CardLookupAdapter.toPokemon()` to read `e.getAbilities()`, call `toAbilityDefinitions()`, and set on `PokemonCardDefinition`
- [x] 3.3 Add `abilities` field to `CardDetailResponse.java`
- [x] 3.4 Modify `CardMapper.toDetailResponse()` to include abilities from entity
- [x] 3.5 Test: verify abilities are hydrated from DB and exposed in `GET /api/cards/{cardId}`

## 4. Ability Tracking

- [x] 4.1 Add `Set<String> abilitiesUsedThisTurn` field to `PokemonInPlay.java` with getter/setter, initialized as `new HashSet<>()`
- [x] 4.2 Modify `TurnManager.startTurn()` to clear `abilitiesUsedThisTurn` for activePokemon + bench of current player only (not other player, not KO'd Pokemon)
- [x] 4.3 Update contract `06-game-state-contract.md` with new PokemonInPlay field

## 5. Ability Registry

- [x] 5.1 Create `AbilityResolver.java` interface in `engine/ability/` with signature: `void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon, AbilityDefinition ability, Map<String, Object> payload)`
- [x] 5.2 Create `AbilityRegistry.java` in `engine/ability/` with `Map<String, AbilityResolver>` and methods: register(String, AbilityResolver), get(String), has(String)
- [x] 5.3 Create `AbilityRegistry` bean in `GameEngineConfig.java`
- [x] 5.4 Modify `GameEngine` constructor to accept `AbilityRegistry` parameter
- [x] 5.5 Pass `AbilityRegistry` to `UseAbilityHandler` constructor

## 6. Handler & Validation

- [x] 6.1 Create `UseAbilityHandler.java` in `engine/handlers/` implementing `GameHandler`
- [x] 6.2 Handler flow (10 steps): (1) extract pokemonInstanceId + abilityName from payload, (2) find Pokemon via HandlerHelper.findPokemon(), (3) resolve PokemonCardDefinition via CardLookupPort, (4) find ability in definition.abilities by name — if not found set ABILITY_NOT_FOUND error and return, (5) check Pokemon not ASLEEP/PARALYZED — if so set POKEMON_CANNOT_USE_ABILITY error and return, (6) check ability not in pokemon.abilitiesUsedThisTurn — if so set ABILITY_ALREADY_USED error and return, (7) get resolver from AbilityRegistry — if null set ABILITY_NOT_FOUND error and return, (8) call resolver.resolve(), (9) **only if ctx.getError() is null after resolve()**: add abilityName to pokemon.abilitiesUsedThisTurn and emit ABILITY_USED event, (10) if ctx.getError() is not null: emit ABILITY_BLOCKED event
- [x] 6.3 Add `USE_ABILITY` case to `RuleValidator.validate()` switch — this method already exists in RuleValidator.java as a switch statement over GameActionType
- [x] 6.4 Implement `validateUseAbility()`: phase==MAIN, target exists via HandlerHelper.findPokemon(), ability exists in PokemonCardDefinition.abilities, ability not in pokemon.abilitiesUsedThisTurn, Pokemon not ASLEEP/PARALYZED
- [x] 6.5 Add `Map.entry(GameActionType.USE_ABILITY, new UseAbilityHandler(abilityRegistry))` to the existing `buildDefaultHandlers()` method in `GameEngine.java` — this method already exists and returns `Map<GameActionType, GameHandler>`, add the new entry alongside existing ones like ATTACH_ENERGY, PLAY_TRAINER, etc.
- [x] 6.6 Update contract `08-game-action-contract.md` with USE_ABILITY request format: `{ type: "USE_ABILITY", payload: { pokemonInstanceId: "uuid", abilityName: "string" } }`
- [x] 6.7 Update contract `09-rule-validation-contract.md` with USE_ABILITY validation rules

## 7. Active Ability Resolvers

- [x] 7.1 Create `MysticalFireResolver.java` implementing AbilityResolver — draw cards from deck until player has 6 in hand (or deck is empty)
- [x] 7.2 Create `WaterShurikenResolver.java` implementing AbilityResolver — payload: `energyCardInstanceId` (UUID of Water Energy card in hand), `targetPokemonInstanceId` (opponent's Pokemon). Find the energy card in player's hand by instanceId, remove it, add to discard. Place 3 damage counters on target. Note: use instanceId to identify the energy card, NOT handIndex, to be consistent with the rest of the spec.
- [x] 7.3 Create `FairyTransferResolver.java` implementing AbilityResolver — payload: `sourceEnergyInstanceId` (UUID of Fairy Energy attached to source Pokemon), `targetPokemonInstanceId` (destination Pokemon). Move exactly 1 Fairy Energy per activation. No "once per turn" restriction — player can send multiple USE_ABILITY actions.
- [x] 7.4 Create `DriveOffResolver.java` implementing AbilityResolver — payload: `targetPokemonInstanceId` (UUID of opponent's Benched Pokemon). Force opponent to switch their Active with the specified Benched Pokemon.
- [x] 7.5 Create `StanceChangeResolver.java` implementing AbilityResolver — search player's hand for a card named "Aegislash". If found: discard current Aegislash from field, put hand card as Active. If NOT found: set ctx.setError() with MISSING_TARGET — do NOT register usage (consistent with D9).
- [x] 7.6 Create `UpsideDownEvolutionResolver.java` implementing AbilityResolver — checks: (1) Inkay must have CONFUSED condition, if not → error. (2) Search player's deck for a Pokemon card where `evolvesFrom` equals "Inkay". (3) If found, player chooses which one (payload: `handIndex` or `deckIndex` of chosen card). (4) Evolve Inkay with chosen card. (5) If no evolution found in deck → error. The Pokémon that evolves is ALWAYS the one with the ability (the user Pokemon from the handler context).
- [x] 7.7 Register all 6 active resolvers in `GameEngineConfig` AbilityRegistry bean using `registry.register("Ability Name", new XxxResolver())`

## 8. Passive Ability Hooks

- [x] 8.1 Create `FurCoatHook.java` (utility class, does NOT implement AbilityResolver) — static method: `int reduceDamage(int damage, PokemonInPlay defender, CardLookupPort cardLookup)` that checks if defender has ability "Fur Coat" in its PokemonCardDefinition, returns damage - 20 (min 0)
- [x] 8.2 Integrate Fur Coat in `AttackResolver` damage calculation: call `FurCoatHook.reduceDamage()` after base damage + modifiers but before applying to defender. The defender parameter is the Pokemon receiving damage.
- [x] 8.3 Create `SweetVeilHook.java` (utility class, does NOT implement AbilityResolver) — static method: `boolean isImmune(PokemonInPlay target, PlayerState owner, CardLookupPort cardLookup)` that returns true if ANY Pokemon belonging to `owner` (active or bench) has ability "Sweet Veil" AND `target` has at least 1 Fairy Energy attached. Sweet Veil protects ALL Pokemon of the player, not just the one with the ability.
- [x] 8.4 Integrate Sweet Veil in condition application logic (wherever special conditions are set on a Pokemon): before applying condition, call `SweetVeilHook.isImmune(target, targetOwner, cardLookup)` — if true, skip condition application.
- [x] 8.5 Create `ForestsCurseHook.java` (utility class, does NOT implement AbilityResolver) — static method: `boolean isItemBlocked(PlayerState playerWhoseItemIsBlocked, GameState state, CardLookupPort cardLookup)` that returns true if the ACTIVE Pokemon of `playerWhoseItemIsBlocked`'s opponent has ability "Forest's Curse". This blocks Items of the PLAYER WHO WOULD USE THE ITEM, when the OPPONENT's Active has Forest's Curse.
- [x] 8.6 Integrate Forest's Curse in `RuleValidator.validatePlayTrainer()`: if card is ITEM, call `ForestsCurseHook.isItemBlocked(player, state, cardLookup)` — if true, reject. Here `player` is the player attempting to play the Item.

## 9. Triggered Ability Hooks

- [x] 9.1 Create `SpikyShieldHook.java` (utility class, does NOT implement AbilityResolver) — static method: `void afterDamageTaken(PokemonInPlay defender, PokemonInPlay attacker, EngineContext ctx)` that checks if defender has "Spiky Shield" ability, places 3 damage counters on attacker
- [x] 9.2 Integrate Spiky Shield in attack damage resolution: after damage is applied to defender, call `SpikyShieldHook.afterDamageTaken(defender, attacker, ctx)`
- [x] 9.3 Create `DestinyBurstHook.java` (utility class, does NOT implement AbilityResolver) — static method: `void onKnockout(PokemonInPlay knockedOut, PokemonInPlay attacker, EngineContext ctx)` that checks if knockedOut has "Destiny Burst" ability, flips coin via ctx.getRandomizer(), if heads places 5 damage counters on attacker
- [x] 9.4 Integrate Destiny Burst in KO logic: after a Pokemon is KO'd by attack damage, call `DestinyBurstHook.onKnockout(koPokemon, attacker, ctx)`

## 10. Contract Updates

- [x] 10.1 Update `03-enums-contract.md` with USE_ABILITY, ABILITY_USED, ABILITY_BLOCKED, new ErrorCodes
- [x] 10.2 Update `04-card-model-contract.md` with AbilityDefinition and abilities field
- [x] 10.3 Update `06-game-state-contract.md` with abilitiesUsedThisTurn in PokemonInPlay
- [x] 10.4 Update `08-game-action-contract.md` with USE_ABILITY request/response format
- [x] 10.5 Update `09-rule-validation-contract.md` with USE_ABILITY validation rules

## 11. Build & Verify

- [x] 11.1 Run `mvn compile` — verify no compilation errors
- [x] 11.2 Run `mvn test` — verify all existing tests pass (TakePrizeCardHandlerTest failures are pre-existing)
- [x] 11.3 Verify abilities appear in `GET /api/cards/{cardId}` for xy1 Pokemon with abilities
- [x] 11.4 Verify `USE_ABILITY` action is accepted by GameEngine for registered abilities
- [x] 11.5 Verify passive abilities (Fur Coat, Sweet Veil, Forest's Curse) affect gameplay
- [x] 11.6 Verify triggered abilities (Spiky Shield, Destiny Burst) fire on correct events
- [x] 11.7 Verify ability tracking resets at turn start and supports multiple copies
