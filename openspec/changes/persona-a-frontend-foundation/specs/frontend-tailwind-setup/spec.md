## ADDED Requirements

### Requirement: Tailwind CSS v4 installed
The system SHALL install Tailwind CSS v4 using `@tailwindcss/postcss` and configure it for Angular's build system.

#### Scenario: Tailwind classes work in components
- **WHEN** a component uses Tailwind classes like `class="flex items-center"`
- **THEN** the styles SHALL be applied correctly in the rendered output

#### Scenario: Build succeeds with Tailwind
- **WHEN** `ng build` is run
- **THEN** it SHALL complete without errors related to Tailwind processing
