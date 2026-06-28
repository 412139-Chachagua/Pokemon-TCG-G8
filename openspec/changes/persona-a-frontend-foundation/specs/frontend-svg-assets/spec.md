## ADDED Requirements

### Requirement: Energy type SVG assets
The system SHALL create 10 SVG files in `src/assets/icons/energy/` — one per `EnergyType`: grass, fire, water, lightning, psychic, fighting, darkness, metal, fairy, colorless. Can be simple colored circles with the first letter.

#### Scenario: All 10 energy icons exist
- **WHEN** the project is built
- **THEN** `src/assets/icons/energy/energy-{type}.svg` SHALL exist for all 10 energy types

### Requirement: Special condition SVG assets
The system SHALL create 5 SVG files in `src/assets/icons/conditions/` — one per `SpecialCondition`: asleep, burned, confused, paralyzed, poisoned. Can be simple icons.

#### Scenario: All 5 condition icons exist
- **WHEN** the project is built
- **THEN** `src/assets/icons/conditions/condition-{condition}.svg` SHALL exist for all 5 conditions
