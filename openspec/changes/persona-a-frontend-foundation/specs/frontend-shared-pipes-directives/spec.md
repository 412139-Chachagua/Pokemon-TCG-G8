## ADDED Requirements

### Requirement: CardImagePipe transforms cardId to URL
The system SHALL create `shared/pipes/card-image.pipe.ts` with `transform(cardId: string, size: 'small'|'large'): string` that converts a card ID like `xy1-1` to `https://images.pokemontcg.io/xy1/1.png` (small) or `https://images.pokemontcg.io/xy1/1_hires.png` (large).

#### Scenario: Small image URL
- **WHEN** `cardImage('xy1-1', 'small')` is called
- **THEN** it SHALL return `https://images.pokemontcg.io/xy1/1.png`

#### Scenario: Large image URL
- **WHEN** `cardImage('xy1-1', 'large')` is called
- **THEN** it SHALL return `https://images.pokemontcg.io/xy1/1_hires.png`

### Requirement: EnergyIconPipe transforms EnergyType to asset path
The system SHALL create `shared/pipes/energy-icon.pipe.ts` with `transform(type: EnergyType): string` that returns `assets/icons/energy/energy-{type lowercase}.svg`.

#### Scenario: Fire energy icon
- **WHEN** `energyIcon('FIRE')` is called
- **THEN** it SHALL return `assets/icons/energy/energy-fire.svg`

#### Scenario: Water energy icon
- **WHEN** `energyIcon('WATER')` is called
- **THEN** it SHALL return `assets/icons/energy/energy-water.svg`

### Requirement: ConditionIconPipe transforms SpecialCondition to asset path
The system SHALL create `shared/pipes/condition-icon.pipe.ts` with `transform(condition: SpecialCondition): string` that returns `assets/icons/conditions/condition-{condition lowercase}.svg`.

#### Scenario: Burned condition icon
- **WHEN** `conditionIcon('BURNED')` is called
- **THEN** it SHALL return `assets/icons/conditions/condition-burned.svg`

### Requirement: ClickOutsideDirective emits on outside click
The system SHALL create `shared/directives/click-outside.directive.ts` that emits `clickOutside: void` when a click occurs outside the host element.

#### Scenario: Click outside host element
- **WHEN** the user clicks outside the element with `(clickOutside)` directive
- **THEN** the `clickOutside` output SHALL emit

#### Scenario: Click inside host element
- **WHEN** the user clicks inside the element with `(clickOutside)` directive
- **THEN** the `clickOutside` output SHALL NOT emit
