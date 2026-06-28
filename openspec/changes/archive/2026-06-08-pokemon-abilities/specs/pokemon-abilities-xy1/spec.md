## ADDED Requirements

### Requirement: Active ability resolvers
The system SHALL implement resolvers for active abilities that are activated via `USE_ABILITY`: Mystical Fire, Water Shuriken, Fairy Transfer, Drive Off, Stance Change, Upside-Down Evolution.

#### Scenario: Mystical Fire activation
- **WHEN** Delphox uses Mystical Fire
- **THEN** the player SHALL draw cards until they have 6 in hand (or deck is empty)

#### Scenario: Water Shuriken activation
- **WHEN** Greninja uses Water Shuriken with payload {energyCardInstanceId, targetPokemonInstanceId}
- **THEN** the Water Energy card identified by energyCardInstanceId SHALL be removed from player's hand, added to discard, and 3 damage counters SHALL be placed on the opponent's Pokemon identified by targetPokemonInstanceId

#### Scenario: Water Shuriken without valid energy
- **WHEN** Greninja uses Water Shuriken and the energyCardInstanceId does not reference a Water Energy in hand
- **THEN** the activation SHALL be rejected with error

#### Scenario: Fairy Transfer activation
- **WHEN** Aromatisse uses Fairy Transfer with payload {sourceEnergyInstanceId, targetPokemonInstanceId}
- **THEN** exactly 1 Fairy Energy identified by sourceEnergyInstanceId SHALL be moved from source Pokemon to target Pokemon

#### Scenario: Fairy Transfer multiple uses
- **WHEN** Aromatisse uses Fairy Transfer and wants to move more energies
- **THEN** the player MAY send additional USE_ABILITY actions (no "once per turn" restriction)

#### Scenario: Drive Off activation
- **WHEN** Swellow uses Drive Off with payload {targetPokemonInstanceId}
- **THEN** the opponent SHALL switch their Active Pokemon with the Benched Pokemon identified by targetPokemonInstanceId

#### Scenario: Stance Change activation success
- **WHEN** Aegislash uses Stance Change and there is an Aegislash card in player's hand
- **THEN** the current Aegislash in play SHALL be discarded and the one from hand SHALL become Active

#### Scenario: Stance Change activation failure
- **WHEN** Aegislash uses Stance Change and there is NO Aegislash card in player's hand
- **THEN** the activation SHALL be rejected with error MISSING_TARGET and SHALL NOT be registered in abilitiesUsedThisTurn

#### Scenario: Upside-Down Evolution activation success
- **WHEN** Inkay uses Upside-Down Evolution while Inkay has CONFUSED condition, and deck contains a Pokemon card where evolvesFrom == "Inkay"
- **THEN** the player SHALL choose one evolution from deck (payload: deckIndex), and Inkay SHALL evolve into the chosen Pokemon

#### Scenario: Upside-Down Evolution not confused
- **WHEN** Inkay uses Upside-Down Evolution while NOT Confused
- **THEN** the activation SHALL be rejected with error

#### Scenario: Upside-Down Evolution no evolution in deck
- **WHEN** Inkay uses Upside-Down Evolution while Confused but no Pokemon in deck has evolvesFrom == "Inkay"
- **THEN** the activation SHALL be rejected with error

### Requirement: Passive ability hooks
The system SHALL implement passive abilities as hardcoded utility methods called from existing engine points. These are NOT registered in AbilityRegistry and do NOT use *Resolver naming — they use *Hook naming for consistency.

#### Scenario: Fur Coat reduces damage
- **WHEN** a Pokemon with Fur Coat is targeted by an attack
- **THEN** damage SHALL be reduced by 20 (minimum 0) before applying to the defender

#### Scenario: Sweet Veil prevents conditions
- **WHEN** any Pokemon belonging to a player is targeted by a special condition, and ANY of that player's Pokemon (active or bench) has Sweet Veil ability AND the target Pokemon has at least 1 Fairy Energy attached
- **THEN** the condition SHALL NOT be applied to the target

#### Scenario: Sweet Veil does not protect without Fairy Energy
- **WHEN** a Pokemon is targeted by a condition and the player has a Pokemon with Sweet Veil, but the target has NO Fairy Energy attached
- **THEN** the condition SHALL be applied normally

#### Scenario: Forest's Curse blocks Items
- **WHEN** a player tries to play an ITEM trainer card, and the OPPONENT's Active Pokemon has Forest's Curse ability
- **THEN** the ITEM play action SHALL be rejected

### Requirement: Triggered ability hooks
The system SHALL implement triggered abilities as hardcoded utility methods called from attack resolution and KO logic. These are NOT registered in AbilityRegistry and do NOT use USE_ABILITY.

#### Scenario: Spiky Shield on damage
- **WHEN** a Pokemon with Spiky Shield is damaged by an attack
- **THEN** 3 damage counters SHALL be placed on the attacking Pokemon after damage resolution

#### Scenario: Destiny Burst on KO
- **WHEN** Voltorb with Destiny Burst is Knocked Out by attack damage
- **THEN** a coin SHALL be flipped; if heads, 5 damage counters SHALL be placed on the attacking Pokemon

### Requirement: Resolver registration
All xy1 active resolvers SHALL be registered in `AbilityRegistry` during `GameEngineConfig` bean creation. Passive and triggered abilities are NOT registered — they are hooks.

#### Scenario: All active abilities registered
- **WHEN** the application starts
- **THEN** the 6 active ability resolvers (Mystical Fire, Water Shuriken, Fairy Transfer, Drive Off, Stance Change, Upside-Down Evolution) SHALL be registered in AbilityRegistry

#### Scenario: Passive abilities not in registry
- **WHEN** the application starts
- **THEN** Fur Coat, Sweet Veil, Forest's Curse SHALL NOT be in AbilityRegistry (they are hooks called from engine points)

#### Scenario: Triggered abilities not in registry
- **WHEN** the application starts
- **THEN** Spiky Shield, Destiny Burst SHALL NOT be in AbilityRegistry (they are hooks called from engine points)
