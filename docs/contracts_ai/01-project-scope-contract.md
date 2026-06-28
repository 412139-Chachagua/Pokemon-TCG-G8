# Project Scope Contract

## Product goal

Build a playable web version of Pokémon TCG for the Programming III final project.

The system must support:
- Java 21 + Spring Boot backend
- Angular 21+ frontend
- PostgreSQL database
- WebSockets for real-time match sync
- Local card cache based on pokemontcg.io data
- XY1 set as mandatory base set
- Backend-authoritative game rules

## Implemented features

The following features are already implemented and functional:

- Guest players (MatchPlayerEntity con `playerKind=HUMAN`)
- Seed decks funcionales con validación
- Match creation + join (soporte dual: 1 jugador + join o 2 jugadores directo)
- Setup automático (SetupManager con mulligan, auto-fill bench, prize creation, coin flip)
- Turn cycle completo: DRAW (draw automático via TurnManager), MAIN, ATTACK, BETWEEN_TURNS
- Put Basic Pokémon on bench
- Attach Energy (1 per turn)
- Evolve Pokémon
- Retreat Active Pokémon
- Attack with Active Pokémon
- Damage calculation con weakness/resistance/minimum 0
- Knockout detection y Prize taking
- Victory conditions (KNOCKOUT, PRIZES, DECK_OUT, CONCEDE, SUDDEN_DEATH)
- Card catalog sync desde pokemontcg.io (set XY1)
- Search cards REST API (con filtros y paginación)
- Deck builder CRUD completo + validación
- Match state persistence versionada (MatchStateEntity)
- Match action log (MatchLogEntity con eventos tipados)
- WebSocket real-time state sync (STOMP)
- Sistema de eventos tipados (GameEvent/GameEventType enum con 14 valores)

## Postponed until MVP works

Do not implement these before the MVP match is playable:
- JWT authentication
- User registration/login
- Ranking
- Chat
- Animations
- Multiple expansions
- Mega Evolution
- Complex card text interpreter
- Full trainer/effect engine for every card

## Technical priority

Priority order:
1. Game engine rules
2. Match state model
3. Seed decks
4. REST actions
5. WebSocket sync
6. Frontend playable board
7. Deck Builder
8. Auth/login
9. Optional features

## Out of scope for MVP

- AI opponent
- Mobile-first UI
- Tournament system
- Trading cards between users
- Payment/store features
