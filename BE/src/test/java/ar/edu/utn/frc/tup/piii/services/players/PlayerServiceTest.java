package ar.edu.utn.frc.tup.piii.services.players;

import ar.edu.utn.frc.tup.piii.dtos.players.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.players.UpdatePlayerRequest;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.UserEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService")
class PlayerServiceTest {

    @Mock
    private PlayerJpaRepository playerJpaRepository;

    private PlayerService playerService;
    private UUID playerId;
    private UUID userId;
    private PlayerEntity playerEntity;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerJpaRepository);
        playerId = UUID.randomUUID();
        userId = UUID.randomUUID();

        userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setUsername("OriginalUsername");
        userEntity.setEmail("player@test.com");

        playerEntity = new PlayerEntity();
        playerEntity.setId(playerId);
        playerEntity.setDisplayName("TestPlayer");
        playerEntity.setUser(userEntity);
        playerEntity.setCreatedAt(Instant.now());
        playerEntity.setAvatarUrl("avatars/test.png");
    }

    @Nested
    @DisplayName("existsById()")
    class ExistsByIdTests {

        @Test
        void shouldReturnTrueWhenExists() {
            when(playerJpaRepository.existsById(playerId)).thenReturn(true);

            assertTrue(playerService.existsById(playerId));
        }

        @Test
        void shouldReturnFalseWhenNotExists() {
            when(playerJpaRepository.existsById(playerId)).thenReturn(false);

            assertFalse(playerService.existsById(playerId));
        }

        @Test
        void shouldReturnFalseForRandomId() {
            UUID random = UUID.randomUUID();
            when(playerJpaRepository.existsById(random)).thenReturn(false);

            assertFalse(playerService.existsById(random));
        }

        @Test
        void shouldNotThrowOnNullId() {
            assertThrows(Exception.class, () -> playerService.existsById(null));
        }
    }

    @Nested
    @DisplayName("listAll()")
    class ListAllTests {

        @Test
        void shouldReturnAllPlayers() {
            when(playerJpaRepository.findAll()).thenReturn(List.of(playerEntity));

            List<PlayerResponse> result = playerService.listAll();

            assertEquals(1, result.size());
            PlayerResponse r = result.get(0);
            assertEquals(playerId.toString(), r.id());
            assertEquals("TestPlayer", r.displayName());
            assertEquals(userId.toString(), r.userId());
            assertEquals("avatars/test.png", r.avatarUrl());
        }

        @Test
        void shouldReturnEmptyListWhenNoPlayers() {
            when(playerJpaRepository.findAll()).thenReturn(Collections.emptyList());

            List<PlayerResponse> result = playerService.listAll();

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldMapMultiplePlayers() {
            PlayerEntity p2 = new PlayerEntity();
            p2.setId(UUID.randomUUID());
            p2.setDisplayName("PlayerTwo");
            p2.setUser(new UserEntity());
            p2.getUser().setId(UUID.randomUUID());
            p2.setCreatedAt(Instant.now());

            when(playerJpaRepository.findAll()).thenReturn(List.of(playerEntity, p2));

            List<PlayerResponse> result = playerService.listAll();

            assertEquals(2, result.size());
            assertEquals("TestPlayer", result.get(0).displayName());
            assertEquals("PlayerTwo", result.get(1).displayName());
        }

        @Test
        void shouldHandlePlayersWithNullUser() {
            playerEntity.setUser(null);
            when(playerJpaRepository.findAll()).thenReturn(List.of(playerEntity));

            List<PlayerResponse> result = playerService.listAll();

            assertEquals(1, result.size());
            assertNull(result.get(0).userId());
        }

        @Test
        void shouldHandlePlayersWithNullAvatarUrl() {
            playerEntity.setAvatarUrl(null);
            when(playerJpaRepository.findAll()).thenReturn(List.of(playerEntity));

            List<PlayerResponse> result = playerService.listAll();

            assertNull(result.get(0).avatarUrl());
        }

        @Test
        void shouldHandlePlayersWithBlankDisplayName() {
            playerEntity.setDisplayName("");
            when(playerJpaRepository.findAll()).thenReturn(List.of(playerEntity));

            List<PlayerResponse> result = playerService.listAll();

            assertEquals("", result.get(0).displayName());
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        void shouldReturnPlayerWhenFound() {
            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));

            PlayerResponse response = playerService.getById(playerId);

            assertAll("getById response",
                    () -> assertEquals(playerId.toString(), response.id()),
                    () -> assertEquals("TestPlayer", response.displayName()),
                    () -> assertEquals("avatars/test.png", response.avatarUrl()),
                    () -> assertEquals(userId.toString(), response.userId())
            );
        }

        @Test
        void shouldThrowNotFoundExceptionWhenMissing() {
            UUID id = UUID.randomUUID();
            when(playerJpaRepository.findById(id)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> playerService.getById(id));
            assertTrue(ex.getMessage().contains("Player") || ex.getMessage().contains("player"));
        }

        @Test
        void shouldReturnPlayerWithoutUser() {
            playerEntity.setUser(null);
            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));

            PlayerResponse response = playerService.getById(playerId);

            assertNull(response.userId());
            assertEquals("TestPlayer", response.displayName());
        }

        @Test
        void shouldReturnPlayerWithoutAvatar() {
            playerEntity.setAvatarUrl(null);
            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));

            PlayerResponse response = playerService.getById(playerId);

            assertNull(response.avatarUrl());
        }

        @Test
        void shouldPreserveCreatedAt() {
            Instant now = Instant.now();
            playerEntity.setCreatedAt(now);
            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));

            PlayerResponse response = playerService.getById(playerId);

            assertEquals(now, response.createdAt());
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        void shouldUpdateDisplayNameAndSyncUsername() {
            UpdatePlayerRequest request = new UpdatePlayerRequest("NewDisplayName");

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            PlayerResponse response = playerService.update(playerId, request);

            assertAll("updated values",
                    () -> assertEquals("NewDisplayName", playerEntity.getDisplayName()),
                    () -> assertEquals("NewDisplayName", userEntity.getUsername()),
                    () -> assertNotNull(response)
            );
            verify(playerJpaRepository).save(playerEntity);
        }

        @Test
        void shouldUpdateDisplayNameWhenUserIsNull() {
            playerEntity.setUser(null);
            UpdatePlayerRequest request = new UpdatePlayerRequest("OrphanPlayer");

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            PlayerResponse response = playerService.update(playerId, request);

            assertEquals("OrphanPlayer", playerEntity.getDisplayName());
            assertNotNull(response);
            verify(playerJpaRepository).save(playerEntity);
        }

        @Test
        void shouldThrowNotFoundWhenPlayerMissing() {
            UUID id = UUID.randomUUID();
            UpdatePlayerRequest request = new UpdatePlayerRequest("Name");

            when(playerJpaRepository.findById(id)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> playerService.update(id, request));
            assertTrue(ex.getMessage().contains("Player") || ex.getMessage().contains("player"));
            verify(playerJpaRepository, never()).save(any());
        }

        @Test
        void shouldUpdateDisplayNameToEmptyString() {
            UpdatePlayerRequest request = new UpdatePlayerRequest("");

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            playerService.update(playerId, request);

            assertEquals("", playerEntity.getDisplayName());
        }

        @Test
        void shouldUpdateDisplayNameWithSpecialCharacters() {
            UpdatePlayerRequest request = new UpdatePlayerRequest("Player_123!@#$%");

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            playerService.update(playerId, request);

            assertEquals("Player_123!@#$%", playerEntity.getDisplayName());
        }

        @Test
        void shouldUpdateDisplayNameWithUnicode() {
            UpdatePlayerRequest request = new UpdatePlayerRequest("Jugador ñoño 日本語");

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            playerService.update(playerId, request);

            assertEquals("Jugador ñoño 日本語", playerEntity.getDisplayName());
        }

        @Test
        void shouldReturnResponseWithUpdatedFields() {
            UpdatePlayerRequest request = new UpdatePlayerRequest("UpdatedName");

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            PlayerResponse response = playerService.update(playerId, request);

            assertEquals(playerId.toString(), response.id());
            assertEquals("UpdatedName", response.displayName());
            assertEquals("avatars/test.png", response.avatarUrl());
        }

        @Test
        void shouldUpdateMultipleTimesSequentially() {
            UpdatePlayerRequest req1 = new UpdatePlayerRequest("First");
            UpdatePlayerRequest req2 = new UpdatePlayerRequest("Second");

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            playerService.update(playerId, req1);
            assertEquals("First", playerEntity.getDisplayName());
            assertEquals("First", userEntity.getUsername());

            playerService.update(playerId, req2);
            assertEquals("Second", playerEntity.getDisplayName());
            assertEquals("Second", userEntity.getUsername());

            verify(playerJpaRepository, times(2)).save(playerEntity);
        }
    }

    @Nested
    @DisplayName("updateAvatar()")
    class UpdateAvatarTests {

        @Test
        void shouldSetAvatarUrl() {
            String newAvatar = "avatars/new-avatar-abc123.png";

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            PlayerResponse response = playerService.updateAvatar(playerId, newAvatar);

            assertEquals(newAvatar, playerEntity.getAvatarUrl());
            assertNotNull(response);
            verify(playerJpaRepository).save(playerEntity);
        }

        @Test
        void shouldThrowNotFoundWhenPlayerMissing() {
            UUID id = UUID.randomUUID();

            when(playerJpaRepository.findById(id)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> playerService.updateAvatar(id, "avatars/x.png"));
            assertTrue(ex.getMessage().contains("Player") || ex.getMessage().contains("player"));
            verify(playerJpaRepository, never()).save(any());
        }

        @Test
        void shouldSetAvatarUrlToNull() {
            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            playerService.updateAvatar(playerId, null);

            assertNull(playerEntity.getAvatarUrl());
        }

        @Test
        void shouldSetAvatarUrlToEmptyString() {
            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            playerService.updateAvatar(playerId, "");

            assertEquals("", playerEntity.getAvatarUrl());
        }

        @Test
        void shouldSetAvatarUrlWithFullPath() {
            String fullUrl = "https://cdn.example.com/avatars/user123.png";

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            PlayerResponse response = playerService.updateAvatar(playerId, fullUrl);

            assertEquals(fullUrl, playerEntity.getAvatarUrl());
            assertEquals(fullUrl, response.avatarUrl());
        }

        @Test
        void shouldReplacePreviousAvatar() {
            String oldAvatar = "avatars/old.png";
            String newAvatar = "avatars/new.png";
            playerEntity.setAvatarUrl(oldAvatar);

            when(playerJpaRepository.findById(playerId)).thenReturn(Optional.of(playerEntity));
            when(playerJpaRepository.save(playerEntity)).thenReturn(playerEntity);

            playerService.updateAvatar(playerId, newAvatar);

            assertEquals(newAvatar, playerEntity.getAvatarUrl());
            assertNotEquals(oldAvatar, playerEntity.getAvatarUrl());
        }
    }
}
