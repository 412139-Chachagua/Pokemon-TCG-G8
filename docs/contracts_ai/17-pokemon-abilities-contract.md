# Pokemon Abilities Contract

## Goal

Define the Pokemon Abilities system: model, registry, resolvers, hooks, and integration with the game engine.

Pokemon Abilities are special effects that some Pokemon possess. They are NOT attacks. They can be used during MAIN phase and do not consume the attack.

## Backend location

```
engine/ability/
  AbilityResolver.java
  AbilityRegistry.java
  resolvers/
    MysticalFireResolver.java
    WaterShurikenResolver.java
    FairyTransferResolver.java
    DriveOffResolver.java
    StanceChangeResolver.java
    UpsideDownEvolutionResolver.java
  hooks/
    FurCoatHook.java
    SweetVeilHook.java
    ForestsCurseHook.java
    SpikyShieldHook.java
    DestinyBurstHook.java
cards/domain/
  AbilityType.java
  AbilityDefinition.java
```

## Model

### AbilityType

Enum in `cards/domain/AbilityType.java`:

- ABILITY
- POKEMON_POWER
- POKEMON_BODY

### AbilityDefinition

Class in `cards/domain/AbilityDefinition.java`:

- name: String
- text: String
- type: AbilityType

### PokemonCardDefinition

Extends CardDefinition. Contains:

- abilities: List\<AbilityDefinition\>

Abilities are hydrated from the DB via `CardMapper.toAbilityDefinitions()` using `AbilityDto`.

## GameActionType

`USE_ABILITY` is registered in `GameActionType.java`.

## GameEventType

- `ABILITY_USED` - when an active ability resolves successfully
- `ABILITY_BLOCKED` - when a passive ability blocks an effect

## ErrorCode

- `ABILITY_NOT_FOUND` - ability does not exist in Pokemon's definition
- `ABILITY_ALREADY_USED` - ability was already used this turn
- `POKEMON_CANNOT_USE_ABILITY` - Pokemon is ASLEEP or PARALYZED

## Game state tracking

`PokemonInPlay` contains:

- abilitiesUsedThisTurn: Set\<String\>

This is reset at the start of each turn by `TurnManager.startTurn()`.

## AbilityRegistry

Registry in `engine/ability/AbilityRegistry.java`. Maps ability name (String) to `AbilityResolver`.

```java
public class AbilityRegistry {
    private final Map<String, AbilityResolver> resolvers;

    public void register(String abilityName, AbilityResolver resolver);
    public AbilityResolver getResolver(String abilityName);
    public boolean hasResolver(String abilityName);
}
```

## AbilityResolver interface

Interface in `engine/ability/AbilityResolver.java`:

```java
public interface AbilityResolver {
    void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon,
                 AbilityDefinition ability, Map<String, Object> payload);
}
```

## Active resolvers (registered in AbilityRegistry)

### MysticalFireResolver

- Ability: "Mystical Fire" (Delphox)
- Effect: Draw cards until player has 6 in hand
- Payload: none

### WaterShurikenResolver

- Ability: "Water Shuriken" (Greninja)
- Effect: Discard 1 Water Energy from this Pokemon, place 3 damage counters on opponent's Active
- Payload: none

### FairyTransferResolver

- Ability: "Fairy Transfer" (Xerneas)
- Effect: Move 1 Fairy Energy from this Pokemon to another player's Pokemon
- Payload: targetPokemonInstanceId

### DriveOffResolver

- Ability: "Drive Off" (Hawlucha)
- Effect: Opponent switches their Active to a Benched Pokemon
- Payload: none

### StanceChangeResolver

- Ability: "Stance Change" (Aegislash)
- Effect: Switch this Pokemon with an Aegislash from hand
- Payload: none
- Special: If no Aegislash in hand, returns MISSING_TARGET error (does NOT register usage)

### UpsideDownEvolutionResolver

- Ability: "Upside Down Evolution" (Inkay)
- Effect: Evolve into Malamar if this Pokemon is Confused
- Payload: targetPokemonInstanceId
- Special: Requires Confused condition on self

## Passive hooks (NOT registered in AbilityRegistry)

### FurCoatHook

- Ability: "Fur Coat" (Meowstic)
- Effect: Reduce damage dealt to this Pokemon by 20
- Integration: Applied in `AttackResolver.calculateDamage()` after base damage + modifiers
- Requires: Grass Energy attached

### SweetVeilHook

- Ability: "Sweet Veil" (Slurpuff)
- Effect: Prevent special conditions on player's Pokemon
- Integration: Applied in `ApplySpecialConditionResolver.resolve()`
- Requires: Fairy Energy attached
- Scope: Protects ALL player's Pokemon (not just the one with Sweet Veil)

### ForestsCurseHook

- Ability: "Forest's Curse" (Trevenant)
- Effect: Opponent cannot play Item cards
- Integration: Applied in `RuleValidator.validatePlayTrainer()`
- Requires: Grass Energy attached
- Scope: Blocks Items of the player whose opponent's Active has Forest's Curse

### SpikyShieldHook

- Ability: "Spiky Shield" (Klang)
- Effect: Place 3 damage counters on the attacker after dealing damage
- Integration: Applied in `DeclareAttackHandler` after damage is applied
- Requires: Darkness Energy attached

### DestinyBurstHook

- Ability: "Destiny Burst" (Latias)
- Effect: Coin flip, heads = place 5 damage counters on attacker
- Integration: Applied in `DeclareAttackHandler` after KO event
- Requires: Fire Energy attached
- Scope: Triggers when this Pokemon is KO'd

## Validation rules

`RuleValidator.validateUseAbility()`:

- Must be MAIN phase
- Pokemon must be in play (Active or Bench)
- Ability must exist in Pokemon's definition
- Ability must not have been used this turn
- Pokemon must not be ASLEEP or PARALYZED

## UseAbilityHandler

Handler in `engine/handlers/UseAbilityHandler.java`. 10-step flow:

1. Extract pokemonInstanceId and abilityName from payload
2. Find the Pokemon in play
3. Find the ability in the Pokemon's definition
4. Check if ability was already used this turn
5. Check if Pokemon is ASLEEP or PARALYZED
6. Get resolver from AbilityRegistry
7. Call resolver.resolve()
8. If resolver returns error, do NOT register usage
9. Add abilityName to abilitiesUsedThisTurn
10. Add ABILITY_USED event

## Wiring

In `GameEngineConfig.java`:

- AbilityRegistry bean created with 6 resolvers
- UseAbilityHandler receives AbilityRegistry in constructor
- GameEngine receives AbilityRegistry in constructor
- GameEngine registers USE_ABILITY action in buildDefaultHandlers()

## Integration points

- `TurnManager.startTurn()`: resets abilitiesUsedThisTurn
- `AttackResolver.calculateDamage()`: applies FurCoatHook
- `ApplySpecialConditionResolver.resolve()`: applies SweetVeilHook
- `RuleValidator.validatePlayTrainer()`: applies ForestsCurseHook
- `DeclareAttackHandler`: applies SpikyShieldHook and DestinyBurstHook

## REST API

`USE_ABILITY` action via `POST /api/matches/{id}/actions`:

```json
{
  "type": "USE_ABILITY",
  "playerId": "player-1",
  "payload": {
    "pokemonInstanceId": "ci-30",
    "abilityName": "Water Shuriken"
  },
  "clientRequestId": "client-req-012"
}
```

Response events include `ABILITY_USED` on success.

## Card API

`GET /api/cards/{cardId}` returns abilities in the response for Pokemon cards:

```json
{
  "id": "xy1-85",
  "name": "Aegislash",
  "abilities": [
    {
      "name": "Stance Change",
      "text": "Once during your turn...",
      "type": "POKEMON_POWER"
    }
  ]
}
```

Abilities are synced from `api.pokemontcg.io/v2` via `CardMapper.toAbilityDefinitions()`.
