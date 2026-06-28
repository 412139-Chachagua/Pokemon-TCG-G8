## Why

`RuleValidator` es actualmente un stub que siempre retorna `true`. Persona 2 debe implementar la lógica real de validación de acciones del engine según Contract 09 y la división de trabajo definida en `divisionEngine.md`.

## What Changes

- Implementar `validateAttachEnergy`, `validatePutBasicOnBench`, `validateEvolve`, `validatePlayTrainer`, `validateRetreat`, `validateAttack`, `validateEndTurn` con lógica real
- Crear `validateDrawCard`, `validateTakePrizeCard`, `validateChooseKnockoutReplacement` como no-op stubs (fuera de alcance V1)
- Integrar `RuleValidator.validate()` en `GameEngine.applyAction()` antes del handler
- Registrar `RuleValidator` como `@Bean` en `GameEngineConfig`

## Capabilities

### Modified Capabilities
- `rule-validator`: Actualizar spec existente en `openspec/specs/engine-p2/specs-engine-persona2/rule-validator-spec.md` con validaciones V1 alineadas a Contract 09 y `divisionEngine.md`

## Impact

- `BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/rules/RuleValidator.java` — implementar lógica real
- `BE/src/main/java/ar/edu/utn/frc/tup/piii/engine/GameEngine.java` — integrar `RuleValidator.validate()` en `applyAction()`
- `BE/src/main/java/ar/edu/utn/frc/tup/piii/config/GameEngineConfig.java` — registrar `RuleValidator` como bean
- `BE/docs/contracts_ai/09-rule-validation-contract.md` — fuente de verdad para reglas de validación
- Ningún cambio en handlers, modelos, o clases fuera del ownership de Persona 2
