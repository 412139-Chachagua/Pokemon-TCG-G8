# AI Proposal Spec: engine-persona2-master-spec

## Change name

engine-persona2-master-spec

---

# Purpose

Definir:

- ownership,
- restricciones arquitectónicas,
- límites de responsabilidad,
- dependencias permitidas,
- reglas globales,
- y alcance V1

para Persona 2 del engine Pokémon TCG.

Este documento es el contrato arquitectónico principal para:

- `SetupManager`
- `TurnManager`
- `RuleValidator`

Las implementaciones detalladas viven en specs separadas.

Este spec NO debe usarse para implementar directamente código completo.

---

# Mandatory context files

OpenCode MUST read and obey:

- `BE/docs/contracts_ai/00-contract-index.md`
- `BE/docs/contracts_ai/01-project-scope-contract.md`
- `BE/docs/contracts_ai/02-project-structure-contract.md`
- `BE/docs/contracts_ai/03-enums-contract.md`
- `BE/docs/contracts_ai/06-game-state-contract.md`
- `BE/docs/contracts_ai/07-setup-flow-contract.md`
- `BE/docs/contracts_ai/08-game-action-contract.md`
- `BE/docs/contracts_ai/09-rule-validation-contract.md`
- `BE/docs/contracts_ai/16-test-scenarios-contract.md`
- `docs/Reglas_Pokemon_TCG.md`

The contracts above are the source of truth.

If this spec conflicts with project contracts:
the contracts win.

---

# Package root

`ar.edu.utn.frc.tup.piii`

---

# Persona 2 ownership

Persona 2 exclusively owns:

- `engine/setup/`
- `engine/turn/`
- `engine/rules/`

Persona 2 implementations include:

- `SetupManager`
- `TurnManager`
- `RuleValidator`

---

# Explicit non-ownership

Persona 2 MUST NOT modify:

- `engine/model/*`
- `engine/action/*`
- `engine/handlers/*`
- `engine/attack/*`
- `engine/victory/*`
- `GameEngine`
- `GameState`
- `PlayerState`
- `PokemonInPlay`
- `CardInstance`
- `EngineContext`
- engine enums
- action payloads
- persistence layer
- repositories
- controllers
- services
- catalog modules
- frontend modules

Exception:
Persona 2 may temporarily define minimal engine port stubs required to unblock implementation if those ports do not yet exist.

---

# Responsibilities

Persona 2 is responsible for:

- setup flow
- mulligan flow
- initial board initialization
- turn flow
- phase transitions
- turn flags reset
- legality validation of actions
- enforcing turn restrictions
- enforcing phase restrictions

Persona 2 validates legality only.

Persona 2 does NOT resolve:
- damage,
- attack effects,
- knockouts,
- victory conditions,
- or card effects.

---

# Architecture constraints

- Todo el código de Persona 2 vive exclusivamente bajo:
  - `engine/setup/`
  - `engine/turn/`
  - `engine/rules/`

- Ninguna clase puede usar:
  - `@Service`
  - `@Repository`
  - `@Autowired`
  - `@Entity`
  - ni ninguna anotación Spring/JPA.

- Ninguna clase puede importar:
  - repositories
  - services
  - controllers
  - Spring Data
  - JPA

- `CardLookupPort` se usa únicamente como interface.

- `RandomizerPort` se usa únicamente como interface.

- Toda aleatoriedad debe pasar exclusivamente por:
  - `RandomizerPort`

- Está prohibido usar:
  - `Random`
  - `ThreadLocalRandom`
  - `Collections.shuffle`
  - cualquier fuente directa de aleatoriedad

---

# RuleValidator constraints

`RuleValidator` MUST be side-effect free.

Los métodos de validación:
- NO mutan estado,
- NO consumen recursos,
- NO cambian phase,
- NO mueven cartas,
- NO modifican flags.

`RuleValidator` únicamente:
- valida legalidad de acciones.

---

# Separation of responsibilities

## Persona 2 responsibilities

Persona 2 valida:
- turn ownership
- current phase
- action legality
- resource availability
- setup legality
- evolution legality
- retreat legality
- attack legality

---

## Persona 3 responsibilities

Las siguientes responsabilidades pertenecen exclusivamente a Persona 3:

- `AttackResolver`
- damage calculation
- weakness/resistance
- attack execution
- KO resolution
- prize handling
- victory checking
- status effects
- attack effects

Persona 2 MUST NOT implement any combat resolution logic.

---

# V1 scope restrictions

V1 explicitly excludes:

- poison
- burn
- confusion
- status condition resolution (application, effects, removal)
- special energy behavior
- complex trainer effects
- triggered abilities
- passive abilities
- event sourcing
- action history
- replay systems
- matchmaking
- ranking
- chat
- animations

---

# SetupManager scope

Detailed implementation lives in:

- `setup-manager-spec.md`

`SetupManager` is responsible for:
- deck initialization
- shuffle flow
- mulligan handling
- prize assignment
- active Pokémon initialization
- initial state creation

`SetupManager` MUST follow:
- contract 07
- setup simplifications defined by the project

---

# TurnManager scope

Detailed implementation lives in:

- `turn-manager-spec.md`

`TurnManager` is responsible for:
- turn transitions
- phase transitions
- draw flow
- turn reset flow
- current player switching

`TurnManager` does NOT:
- process combat
- resolve attacks
- apply effects
- process status conditions

---

# RuleValidator scope

Detailed implementation lives in:

- `rule-validator-spec.md`

`RuleValidator` validates:
- attach energy legality
- attack legality
- retreat legality
- evolution legality
- trainer legality
- bench legality

`RuleValidator` only validates.

Execution belongs to handlers and combat systems.

---

# Dependency rules

Persona 2 may depend ONLY on:

- `engine/model/*`
- `engine/action/*`
- `engine/EngineContext`
- `engine/ports/CardLookupPort`
- `engine/ports/RandomizerPort`
- `engine/ports/DeckLoadPort`
- `cards/domain/*` (TrainerSubtype, etc.)
- If `DeckLoadPort` does not yet exist, Persona 2 may define the minimal interface contract required by SetupManager.

Persona 2 MUST NOT depend on:

- repositories
- services
- JPA entities
- controllers
- REST DTOs
- persistence classes

---

# Engine compatibility

Persona 2 implementations MUST remain compatible with:

- `GameEngine`
- action handlers
- existing payloads
- engine model contracts
- engine enums
- existing ports

Persona 2 MUST follow method signatures and contracts already defined by Persona 1.

Persona 2 MUST NOT redesign engine architecture.

---

# Testing expectations

Detailed tests live in:
- contract 16
- implementation specs

Persona 2 implementations are expected to support:

- deterministic behavior
- isolated testing
- reproducible random flows via `RandomizerPort`

---

# Verification requirements

All Persona 2 implementations MUST:

1. compile with:
   - `mvn compile`

2. pass:
   - `mvn test`

3. preserve:
   - `ApplicationTests.contextLoads`

4. avoid modifying classes outside Persona 2 ownership

5. avoid introducing Spring/JPA dependencies into engine logic

---

# Implementation strategy

This master spec is NOT intended for direct implementation.

Implementations MUST be split into dedicated specs:

- `setup-manager-spec.md`
- `turn-manager-spec.md`
- `rule-validator-spec.md`

Each subsystem should generate:
- proposal
- design
- tasks
- implementation

independently.

---

# Scope control

This spec defines:
- architecture,
- ownership,
- responsibilities,
- constraints,
- and subsystem boundaries.

Detailed implementation logic belongs to smaller subsystem specs.

The priority is:
- deterministic engine behavior,
- low coupling,
- compatibility with Persona 1,
- and minimizing AI hallucinations during implementation.

