## Why

`SetupManager` is currently an empty stub class (`BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/setup/SetupManager.java`). The full setup flow (deck loading, shuffling, dealing hands, mulligan resolution, active selection, prize assignment, coin flip) must be implemented before the match flow and GameEngine integration can work. This change implements the real `SetupManager.setup()` logic under Persona 2 ownership, strictly following contract 07 and the existing specs.

## What Changes

- Replace empty `SetupManager` stub with a fully implemented class
- Add `DeckLoadPort` as constructor dependency (interface already exists)
- Add `CardLookupPort` as constructor dependency (needed for BASIC Pokémon detection)
- Add `RandomizerPort` as constructor dependency (needed for shuffling and coin flip)
- Implement mulligan resolution as a private method within `SetupManager` (no separate `MulliganService` class per contract 07)
- Remove MulliganService.java stub (contract 07 explicitly states mulligan is a private method within SetupManager — no separate class exists)
- Return a fully initialized `GameState` with `status=ACTIVE`, `phase=DRAW`, both `PlayerState` configured

## Capabilities

### New Capabilities
- `setup-manager`: The real `SetupManager.setup()` implementation covering deck loading via `DeckLoadPort`, shuffling via `RandomizerPort`, initial hand dealing, mulligan resolution (auto-redraw + opponent extra cards), first Basic Pokémon auto-selection as Active, prize assignment (6 cards), coin flip for first player, and construction of the initial `GameState`.

### Modified Capabilities
- (none — the specs already exist at the project level; this change implements them without modifying requirements)

## Impact

- `BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/setup/SetupManager.java` — fully implemented
- `BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/setup/MulliganService.java` — deleted (contract 07 explicitly states mulligan is a private method within SetupManager, no separate class exists)
- No other files modified — pure Persona 2 ownership boundary
- No Spring/JPA dependencies introduced
