## Context

The project has existing `UserEntity`, `GuestPlayerEntity`, `DeckEntity`, and `MatchPlayerEntity` entities. `UserJpaRepository` and `GuestPlayerJpaRepository` exist as stubs. No service layer or REST endpoints exist for users or guest players. Matches currently generate random UUIDs for player IDs.

## Goals / Non-Goals

**Goals:**
- Create user registration and login endpoints (plain text password, no hashing for MVP)
- Link guest players to users via optional `ownerUser` FK
- Link decks to users via `@ManyToOne UserEntity ownerUser`
- Modify match creation to accept real player IDs with existence validation
- Add `playerKind` (USER/GUEST) tracking in match players

**Non-Goals:**
- No password hashing or encryption (plain text per spec)
- No JWT, sessions, or authentication tokens
- No changes to Game Engine, card catalog, or WebSocket
- No frontend changes
- No contract modifications (contracts will be updated separately)
- No role/permission management beyond the existing `role` field

## Decisions

1. **Password Storage**: Plain text in `password` field (renamed from `passwordHash`). MVP decision — will be replaced with hashing in a future iteration.

2. **User Entity Relationships**: `UserEntity` owns bidirectional `@OneToMany` to both `GuestPlayerEntity` and `DeckEntity`. Cascade ALL + orphanRemoval for players (user owns players), no cascade for decks (decks are independently managed).

3. **Deck Ownership Change**: `DeckEntity.ownerUserId` (`UUID`) → `DeckEntity.ownerUser` (`@ManyToOne UserEntity`). The JPA query `findByOwnerUserId()` still works via Spring Data property traversal (`WHERE ownerUser.id = ?`).

4. **Mapper Pattern**: `DeckMapper.toEntity()` no longer sets `ownerUser` — it remains null and is assigned in `DeckService.createDeck()` via `userJpaRepository.getReferenceById()`. `DeckMapper.toResponse()` and `toDomain()` navigate `entity.getOwnerUser().getId()`.

5. **Player Identity in Matches**: `MatchApplicationService` resolves `playerKind` by checking existence in `UserJpaRepository` then `GuestPlayerJpaRepository`. If neither exists, throws `ValidationException`. This replaces the old `UUID.randomUUID()` + `"HUMAN"` hardcode.

6. **Controller Thinness**: `UserController` and `GuestPlayerController` delegate all logic to their respective services.

## Risks / Trade-offs

- [Risk] Plain text passwords are insecure → Mitigation: Explicit MVP decision; password hashing will be added later
- [Risk] `DeckJpaRepository.findByOwnerUserId(UUID)` may not resolve with `@ManyToOne` → Mitigation: Spring Data JPA property traversal (`ownerUser.id`) handles this; tested with compilation
- [Risk] `UserEntity.players` collection with `orphanRemoval = true` could accidentally delete players → Mitigation: Cascade is deliberate; players are owned by user
- [Risk] Match controller tests may need updating for new required fields → Mitigation: All existing tests pass (31/31)
