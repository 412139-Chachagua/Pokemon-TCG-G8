## ADDED Requirements

### Requirement: AbilityResolver interface
The system SHALL create `AbilityResolver` interface in `engine/ability/` with method `void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon, AbilityDefinition ability, Map<String, Object> payload)`.

#### Scenario: Resolver interface contract
- **WHEN** a class implements `AbilityResolver`
- **THEN** it SHALL provide a `resolve` method that receives EngineContext, PlayerState, PokemonInPlay, AbilityDefinition, and payload

#### Scenario: Resolver communicates via EngineContext
- **WHEN** a resolver needs to signal an error
- **THEN** it SHALL call `ctx.setError(new GameError(...))` — the handler checks `ctx.getError() == null` after resolve() to decide whether to register usage

### Requirement: AbilityRegistry
The system SHALL create `AbilityRegistry` in `engine/ability/` with `Map<String, AbilityResolver>` that maps ability names to resolvers.

#### Scenario: Register resolver
- **WHEN** `registry.register("Ability Name", resolver)` is called
- **THEN** the resolver SHALL be stored keyed by ability name

#### Scenario: Get resolver
- **WHEN** `registry.get("Ability Name")` is called for a registered ability
- **THEN** it SHALL return the associated resolver

#### Scenario: Get unregistered resolver
- **WHEN** `registry.get("Unknown Ability")` is called for an unregistered ability
- **THEN** it SHALL return null

#### Scenario: Has resolver
- **WHEN** `registry.has("Ability Name")` is called
- **THEN** it SHALL return true if the ability is registered, false otherwise

### Requirement: Registry wiring in GameEngineConfig
The system SHALL create `AbilityRegistry` as a bean in `GameEngineConfig`, register all xy1 resolvers, and pass it to the `GameEngine` constructor.

#### Scenario: Registry bean creation
- **WHEN** the Spring application context starts
- **THEN** `AbilityRegistry` SHALL be created as a bean with all xy1 resolvers registered

#### Scenario: GameEngine receives registry
- **WHEN** `GameEngine` is constructed
- **THEN** it SHALL receive the `AbilityRegistry` and pass it to `UseAbilityHandler`

### Requirement: Handler delegates to registry
The system SHALL modify `UseAbilityHandler` to look up the resolver from `AbilityRegistry` by ability name and delegate execution. Usage is registered ONLY if `ctx.getError() == null` after `resolver.resolve()`.

#### Scenario: Resolver found and succeeds
- **WHEN** `UseAbilityHandler` processes an ability with a registered resolver and `ctx.getError()` is null after resolve()
- **THEN** it SHALL call `resolver.resolve()`, then register usage in `abilitiesUsedThisTurn`, then emit `ABILITY_USED`

#### Scenario: Resolver found but fails
- **WHEN** `UseAbilityHandler` processes an ability with a registered resolver but `ctx.getError()` is not null after resolve()
- **THEN** it SHALL NOT register usage and SHALL emit `ABILITY_BLOCKED`

#### Scenario: Resolver not found
- **WHEN** `UseAbilityHandler` processes an ability without a registered resolver
- **THEN** it SHALL set error with code `ABILITY_NOT_FOUND`
