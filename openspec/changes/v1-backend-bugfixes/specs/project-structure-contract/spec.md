## REMOVED Requirements

### Requirement: GamePhase enum exists
**Reason**: GamePhase enum is dead code. The system uses TurnPhase instead. GamePhase was never referenced by any production code.
**Migration**: Remove `GamePhase.java` entirely. All code that uses TurnPhase continues to work unchanged.

### Requirement: StatusEffectManager exists
**Reason**: StatusEffectManager is an empty class with no methods and no references. Status effects (poison, burn, confusion, etc.) are not implemented in V1.
**Migration**: Remove `StatusEffectManager.java` entirely. If status effects are implemented in a future version, a new class should be created.

### Requirement: AttackStep enum exists
**Reason**: AttackStep is an empty enum with no values and no references.
**Migration**: Remove `AttackStep.java` entirely.

### Requirement: GameMetadata class exists
**Reason**: GameMetadata is an empty class with no fields and no references.
**Migration**: Remove `GameMetadata.java` entirely.

### Requirement: VictoryResult class exists
**Reason**: VictoryResult class has no references in production code. The system uses VictoryCheckResult record instead.
**Migration**: Remove `VictoryResult.java` entirely.

### Requirement: Typed Payload DTOs exist
**Reason**: The classes `AttachEnergyPayload`, `DeclareAttackPayload`, `EvolvePokemonPayload`, `GameActionPayload`, `PlayTrainerPayload`, `PutBasicOnBenchPayload`, `RetreatPayload` and the `GameActionPayload` interface are never used. All handlers extract values from `Map<String, Object>` directly.
**Migration**: Remove all files in `engine/action/` except `GameAction.java`, `GameActionType.java`, `ActionResult.java`, `GameError.java`.
