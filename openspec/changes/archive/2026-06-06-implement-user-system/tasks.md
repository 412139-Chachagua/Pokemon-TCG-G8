## 1. Create User DTOs

- [x] 1.1 Create `dtos/users/CreateUserRequest.java`
- [x] 1.2 Create `dtos/users/LoginRequest.java`
- [x] 1.3 Create `dtos/users/UserResponse.java`

## 2. Create GuestPlayer DTOs

- [x] 2.1 Create `dtos/players/CreateGuestPlayerRequest.java` (with userId field)
- [x] 2.2 Create `dtos/players/GuestPlayerResponse.java` (with ownerUserId field)

## 3. Modify Existing DTOs

- [x] 3.1 Modify `dtos/decks/CreateDeckRequest.java`: rename playerId → userId
- [x] 3.2 Modify `dtos/matches/CreateMatchRequest.java`: add player1Id (required), player2Id (optional)
- [x] 3.3 Modify `dtos/matches/JoinMatchRequest.java`: add playerId (required)

## 4. Modify JPA Entities

- [x] 4.1 Modify `UserEntity.java`: add @OneToMany players + decks, rename passwordHash → password
- [x] 4.2 Modify `GuestPlayerEntity.java`: add @ManyToOne ownerUser
- [x] 4.3 Modify `DeckEntity.java`: replace UUID ownerUserId with @ManyToOne UserEntity ownerUser

## 5. Modify Repository

- [x] 5.1 Modify `GuestPlayerJpaRepository.java`: add findByOwnerUserId(UUID)

## 6. Create UserService

- [x] 6.1 Create `services/users/UserService.java` with register(), login(), getById(), listAll(), existsById()

## 7. Create GuestPlayerService

- [x] 7.1 Create `services/players/GuestPlayerService.java` with createGuestPlayer(), listByUserId(), listAll(), getById()

## 8. Modify DeckService

- [x] 8.1 Inject UserJpaRepository into DeckService
- [x] 8.2 Validate userId exists in createDeck()
- [x] 8.3 Set entity.setOwnerUser() with getReferenceById()

## 9. Modify DeckMapper

- [x] 9.1 Adjust toEntity(): remove ownerUser setting (delegated to service)
- [x] 9.2 Adjust toResponse(): navigate entity.getOwnerUser().getId()
- [x] 9.3 Adjust toDomain(): navigate entity.getOwnerUser().getId()

## 10. Modify MatchApplicationService

- [x] 10.1 Inject UserJpaRepository and GuestPlayerJpaRepository
- [x] 10.2 Add resolvePlayerKind() helper
- [x] 10.3 Update createMatch(): use player1Id from request, validate, set playerKind
- [x] 10.4 Update joinMatch(): use playerId from request, validate, set playerKind

## 11. Create Controllers

- [x] 11.1 Create `controllers/users/UserController.java` (register, login, getById, list)
- [x] 11.2 Create `controllers/players/GuestPlayerController.java` (create, list with optional userId, getById)

## 12. Update Tests

- [x] 12.1 Update `DeckServiceTest.java`: add UserJpaRepository mock, use valid UUID for userId
- [x] 12.2 Update `DeckMapperTest.java`: use setOwnerUser() instead of setOwnerUserId()

## 13. Verification

- [x] 13.1 Run `mvn compile` inside BE/ → no errors
- [x] 13.2 Run `mvn test` inside BE/ → 31/31 tests pass
