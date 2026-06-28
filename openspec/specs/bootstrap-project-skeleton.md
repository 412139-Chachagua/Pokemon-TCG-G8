# AI Proposal Spec: bootstrap-project-skeleton

## Change name

bootstrap-project-skeleton

## Purpose

Create the initial backend and frontend project skeleton for the Pokémon TCG TPI.

The project must have two root folders:

- `/backend`
- `/frontend`

This change must create only the compilable skeleton. It must not implement gameplay logic yet.

## Mandatory context files

OpenCode must read and obey:

- `/docs/contracts/00-contract-index.md`
- `/docs/contracts/01-project-scope-contract.md`
- `/docs/contracts/02-project-structure-contract.md`
- `/docs/contracts/03-enums-contract.md`
- `/docs/contracts/04-card-model-contract.md`
- `/docs/contracts/05-deck-contract.md`
- `/docs/contracts/06-game-state-contract.md`
- `/docs/contracts/08-game-action-contract.md`
- `/docs/contracts/13-rest-api-contract.md`
- `/docs/contracts/14-websocket-contract.md`
- `/docs/contracts/15-frontend-state-contract.md`

## Backend target

Create a Java 21 + Spring Boot backend under:

```txt
/backend

Use package root:

com.pokemontcg

The backend structure must follow:

/docs/contracts/02-project-structure-contract.md

The backend must include:

Maven project files.
PokemonTcgApplication.java.
Canonical package structure.
Placeholder classes for config, common errors, IDs, cards, decks, matches and engine.
Enum files from /docs/contracts/03-enums-contract.md.
Placeholder DTO folders according to the structure contract.
A Spring context load smoke test.
Backend must compile.
Frontend target

Create an Angular 21+ frontend under:

/frontend

The frontend structure must follow:

/docs/contracts/02-project-structure-contract.md

The frontend must include:

Angular project structure.
core/api services.
core/websocket service.
shared model files.
placeholder pages for:
lobby;
match;
deck list;
deck builder.
placeholder match components:
board;
player-area;
opponent-area;
active-pokemon-slot;
bench-zone;
hand-zone;
prize-zone;
discard-zone;
action-panel;
game-log.
features/auth/README.md only. Auth is postponed.
Frontend must build.
Explicit non-goals

Do not implement:

game rules;
attack resolution;
setup flow;
card API sync;
deck builder behavior;
database migrations;
JWT;
login/register;
ranking;
chat;
animations.
Architectural constraints
Do not create alternative package names.
Do not create alternative DTO/model folders.
Do not rename enum values from the contracts.
Do not duplicate concepts already defined in contracts.
The backend must remain the source of truth.
The frontend must not decide game rules.
The engine package must not depend on Spring, JPA, REST controllers, WebSocket classes or repositories.
If something is missing from the contracts, OpenCode must report it instead of inventing a new architecture.
Expected OpenSpec output

Generate an OpenSpec change named:

bootstrap-project-skeleton

The change should include:

proposal;
tasks;
design notes if needed;
spec delta for project structure.

The generated OpenSpec change must be implementation-ready but must not over-expand the scope.




