# Contract Index

These contracts are mandatory context for OpenCode and OpenSpec.

## Rule

Before implementing or modifying code, OpenCode must read the relevant contract files.

If a task conflicts with these contracts:
- stop
- report the conflict
- do not invent alternative structures

## Contract list

- 01-project-scope-contract.md
- 02-project-structure-contract.md
- 03-enums-contract.md
- 04-card-model-contract.md
- 05-deck-contract.md
- 06-game-state-contract.md
- 07-setup-flow-contract.md
- 08-game-action-contract.md
- 09-rule-validation-contract.md
- 10-attack-pipeline-contract.md
- 11-status-effects-contract.md
- 12-persistence-log-contract.md
- 13-rest-api-contract.md
- 14-websocket-contract.md
- 15-frontend-state-contract.md
- 16-test-scenarios-contract.md

## Contract status

| # | Contract | Status | Last updated |
|---|----------|--------|-------------|
| 01 | Project Scope | ✅ Aligned | This change |
| 02 | Project Structure | ✅ Aligned | This change |
| 03 | Enums | ✅ Aligned | This change |
| 04 | Card Model | ✅ Aligned | This change |
| 05 | Deck | ✅ Aligned | This change |
| 06 | Game State | ✅ Aligned | This change |
| 07 | Setup Flow | ✅ Aligned | This change |
| 08 | Game Action | ✅ Aligned | This change |
| 09 | Rule Validation | ✅ Aligned | This change |
| 10 | Attack Pipeline | ✅ Aligned | This change |
| 11 | Status Effects | ✅ Aligned | This change |
| 12 | Persistence & Log | ✅ Aligned | This change |
| 13 | REST API | ✅ Aligned | This change |
| 14 | WebSocket | ✅ Aligned | This change |
| 15 | Frontend State | 🔶 Pending review | This change |
| 16 | Test Scenarios | ✅ Aligned | This change |

## Trazabilidad con TPI

Los contratos cubren los siguientes ítems evaluables de la tabla de evaluación del TPI:

| Ítem TPI | Contratos que lo cubren |
|----------|------------------------|
| Reglas del juego (RF-01) | 01-scope, 07-setup, 08-actions, 09-rules, 10-attack, 11-status |
| Tipos de carta (RF-02) | 03-enums, 04-card-model, 05-deck |
| Gestión del juego (RF-03) | 06-state, 07-setup, 12-persistence |
| Construcción de mazos (RF-04) | 05-deck, 13-rest-api |
| Persistencia (RF-05) | 12-persistence |
| Comunicación tiempo real (RF-06) | 14-websocket |
| Interfaz de usuario (RF-07) | 15-frontend-state |
| Separación de responsabilidades | 02-structure (dependency rules) |
| Patrones de diseño (RNF-04) | 02-structure (sección patrones) |
| Cobertura de tests (RNF-03) | 16-test-scenarios |
| Seguridad (RNF-05) | 06-state, 14-websocket, 15-frontend (privacidad) |

## Global implementation rule

Do not create duplicated DTOs, enums, services, folders or payload formats.

If a missing field or rule is detected, update the relevant contract first.
