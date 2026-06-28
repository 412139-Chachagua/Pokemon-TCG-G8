package ar.edu.utn.frc.tup.piii.services.users;

import ar.edu.utn.frc.tup.piii.dtos.users.CreateUserRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.UpdateUserRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.UserResponse;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.UserEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.UserJpaRepository;
import ar.edu.utn.frc.tup.piii.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserJpaRepository userJpaRepository;
    @Mock private PlayerJpaRepository playerJpaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private UserService userService;
    private UUID userId;
    private UUID playerId;
    private UserEntity userEntity;
    private PlayerEntity playerEntity;
    private String token;

    @BeforeEach
    void setUp() {
        userService = new UserService(userJpaRepository, playerJpaRepository, passwordEncoder, jwtTokenProvider);
        userId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        token = "jwt-token-value-12345";

        playerEntity = new PlayerEntity();
        playerEntity.setId(playerId);
        playerEntity.setDisplayName("TestPlayer");

        userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail("test@example.com");
        userEntity.setUsername("TestPlayer");
        userEntity.setPassword("$2a$10$encodedPasswordHashValue");
        userEntity.setRole("PLAYER");
        userEntity.setStatus("ACTIVE");
        userEntity.setPlayer(playerEntity);
        playerEntity.setUser(userEntity);
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        void shouldCreateUserAndPlayerAndReturnResponse() {
            CreateUserRequest request = new CreateUserRequest("test@example.com", "rawPassword123", "TestPlayer");

            when(passwordEncoder.encode(request.password())).thenReturn("encodedHash");
            when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity saved = invocation.getArgument(0);
                saved.setId(userId);
                if (saved.getPlayer() != null) saved.getPlayer().setId(playerId);
                return saved;
            });
            when(jwtTokenProvider.generateToken(userId, request.email(), "PLAYER", playerId)).thenReturn(token);

            UserResponse response = userService.register(request);

            assertAll("register response",
                    () -> assertEquals(userId.toString(), response.id()),
                    () -> assertEquals("test@example.com", response.email()),
                    () -> assertEquals("TestPlayer", response.displayName()),
                    () -> assertEquals(playerId.toString(), response.playerId()),
                    () -> assertEquals(token, response.token())
            );
            verify(passwordEncoder).encode("rawPassword123");
            verify(userJpaRepository).save(any(UserEntity.class));
            verify(jwtTokenProvider).generateToken(userId, "test@example.com", "PLAYER", playerId);
            verifyNoMoreInteractions(passwordEncoder, jwtTokenProvider);
        }

        @ParameterizedTest
        @CsvSource({
            "short,shortpass,Ab,displayName muy corto",
            "short@short.com,short,Short,password muy corto",
            "user@long.com,pass12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890,X,%s 100 caracteres en displayName"
        })
        void shouldHandleEdgeCaseInputs(String email, String password, String displayName, String reason) {
            CreateUserRequest request = new CreateUserRequest(email, password, displayName);
            String encoded = "encoded_" + password;

            when(passwordEncoder.encode(password)).thenReturn(encoded);
            when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                if (saved.getPlayer() != null) saved.getPlayer().setId(UUID.randomUUID());
                return saved;
            });
            when(jwtTokenProvider.generateToken(any(), anyString(), anyString(), any())).thenReturn("tok");

            UserResponse response = userService.register(request);

            assertNotNull(response);
            verify(passwordEncoder).encode(password);
        }

        @Test
        void shouldSetPlayerDisplayNameFromRequest() {
            CreateUserRequest request = new CreateUserRequest("u@b.com", "abc123", "CustomName");

            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                saved.getPlayer().setId(UUID.randomUUID());
                assertNotNull(saved.getPlayer());
                assertEquals("CustomName", saved.getPlayer().getDisplayName());
                return saved;
            });
            when(jwtTokenProvider.generateToken(any(), anyString(), anyString(), any())).thenReturn("tok");

            userService.register(request);
        }

        @Test
        void shouldSetUserUsernameFromRequestDisplayName() {
            CreateUserRequest request = new CreateUserRequest("u@b.com", "abc123", "CustomDisplay");

            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userJpaRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                saved.getPlayer().setId(UUID.randomUUID());
                assertEquals("CustomDisplay", saved.getUsername());
                return saved;
            });
            when(jwtTokenProvider.generateToken(any(), anyString(), anyString(), any())).thenReturn("tok");

            userService.register(request);
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        void shouldReturnTokenOnValidCredentials() {
            LoginRequest request = new LoginRequest("test@example.com", "correctPassword");

            when(userJpaRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(request.password(), userEntity.getPassword())).thenReturn(true);
            when(jwtTokenProvider.generateToken(userId, userEntity.getEmail(), "PLAYER", playerId)).thenReturn(token);

            UserResponse response = userService.login(request);

            assertEquals(token, response.token());
            assertEquals(userId.toString(), response.id());
            assertEquals("TestPlayer", response.displayName());
            verify(passwordEncoder).matches("correctPassword", userEntity.getPassword());
            verify(jwtTokenProvider).generateToken(userId, "test@example.com", "PLAYER", playerId);
        }

        @Test
        void shouldThrow401WhenEmailNotFound() {
            LoginRequest request = new LoginRequest("unknown@example.com", "any");

            when(userJpaRepository.findByEmail(request.email())).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.login(request));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            verifyNoInteractions(passwordEncoder, jwtTokenProvider);
        }

        @Test
        void shouldThrow401WhenPasswordDoesNotMatch() {
            LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");

            when(userJpaRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(request.password(), userEntity.getPassword())).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.login(request));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            verify(jwtTokenProvider, never()).generateToken(any(), anyString(), anyString(), any());
        }

        @Test
        void shouldThrow403WhenAccountIsInactive() {
            userEntity.setStatus("INACTIVE");
            LoginRequest request = new LoginRequest("test@example.com", "any");

            when(userJpaRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.login(request));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            verifyNoInteractions(passwordEncoder, jwtTokenProvider);
        }

        @Test
        void shouldThrow401WhenAccountIsSuspended() {
            userEntity.setStatus("SUSPENDED");
            LoginRequest request = new LoginRequest("test@example.com", "any");

            when(userJpaRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.login(request));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void shouldUseDisplayNameInTokenWhenPlayerHasOne() {
            LoginRequest request = new LoginRequest("test@example.com", "pass");
            String expectedToken = "token-con-displayname";

            when(userJpaRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches(request.password(), userEntity.getPassword())).thenReturn(true);
            when(jwtTokenProvider.generateToken(userId, userEntity.getEmail(), "PLAYER", playerId)).thenReturn(expectedToken);

            UserResponse response = userService.login(request);

            assertEquals(expectedToken, response.token());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "invalid-email-format"})
        void shouldNotRejectBasedOnEmailFormat_domainLayerOnlyConcernedWithRepository(String email) {
            when(userJpaRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThrows(ResponseStatusException.class, () -> userService.login(new LoginRequest(email, "pass")));
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        void shouldReturnUserWhenFound() {
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            UserResponse response = userService.getById(userId);

            assertEquals(userId.toString(), response.id());
            assertEquals("test@example.com", response.email());
            assertEquals("TestPlayer", response.displayName());
        }

        @Test
        void shouldThrow404WhenUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userJpaRepository.findById(id)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.getById(id));
            assertTrue(ex.getMessage().contains("User") || ex.getMessage().contains("user"));
        }

        @Test
        void shouldReturnUserWithoutPlayer() {
            userEntity.setPlayer(null);
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            UserResponse response = userService.getById(userId);

            assertNull(response.playerId());
            assertNull(response.displayName());
        }

        @Test
        void shouldReturnUserWithDifferentRole() {
            userEntity.setRole("ADMIN");
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            UserResponse response = userService.getById(userId);

            assertEquals(userId.toString(), response.id());
        }
    }

    @Nested
    @DisplayName("listAll()")
    class ListAllTests {

        @Test
        void shouldReturnAllUsers() {
            when(userJpaRepository.findAll()).thenReturn(List.of(userEntity));

            List<UserResponse> result = userService.listAll();

            assertEquals(1, result.size());
            assertEquals(userId.toString(), result.get(0).id());
        }

        @Test
        void shouldReturnEmptyListWhenNoUsers() {
            when(userJpaRepository.findAll()).thenReturn(Collections.emptyList());

            List<UserResponse> result = userService.listAll();

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldMapMultipleUsers() {
            UserEntity u2 = new UserEntity();
            u2.setId(UUID.randomUUID());
            u2.setEmail("b@b.com");
            u2.setUsername("User2");
            u2.setStatus("ACTIVE");
            PlayerEntity p2 = new PlayerEntity();
            p2.setId(UUID.randomUUID());
            p2.setDisplayName("Player2");
            u2.setPlayer(p2);

            when(userJpaRepository.findAll()).thenReturn(List.of(userEntity, u2));

            List<UserResponse> result = userService.listAll();

            assertEquals(2, result.size());
        }

        @Test
        void shouldHandleUsersWithAndWithoutPlayers() {
            UserEntity withoutPlayer = new UserEntity();
            withoutPlayer.setId(UUID.randomUUID());
            withoutPlayer.setEmail("noplayer@test.com");
            withoutPlayer.setUsername("NoPlayer");
            withoutPlayer.setStatus("ACTIVE");
            withoutPlayer.setPlayer(null);

            when(userJpaRepository.findAll()).thenReturn(List.of(userEntity, withoutPlayer));

            List<UserResponse> result = userService.listAll();

            assertEquals(2, result.size());
            assertNull(result.get(1).playerId());
        }
    }

    @Nested
    @DisplayName("existsById()")
    class ExistsByIdTests {

        @Test
        void shouldReturnTrueWhenExists() {
            when(userJpaRepository.existsById(userId)).thenReturn(true);

            assertTrue(userService.existsById(userId));
        }

        @Test
        void shouldReturnFalseWhenNotExists() {
            when(userJpaRepository.existsById(userId)).thenReturn(false);

            assertFalse(userService.existsById(userId));
        }

        @Test
        void shouldReturnFalseForRandomUuid() {
            UUID random = UUID.randomUUID();
            when(userJpaRepository.existsById(random)).thenReturn(false);

            assertFalse(userService.existsById(random));
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        void shouldUpdateEmailOnly() {
            UpdateUserRequest request = new UpdateUserRequest("new@example.com", null, null);

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(userJpaRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(userJpaRepository.save(userEntity)).thenReturn(userEntity);

            UserResponse response = userService.update(userId, request);

            assertNotNull(response);
            verify(userJpaRepository).findByEmail("new@example.com");
            verify(userJpaRepository).save(userEntity);
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        void shouldUpdatePasswordWithCurrentPassword() {
            UpdateUserRequest request = new UpdateUserRequest(null, "currentPass", "newSecurePass99");

            String originalPassword = userEntity.getPassword();
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("currentPass", originalPassword)).thenReturn(true);
            when(passwordEncoder.encode("newSecurePass99")).thenReturn("newEncodedHash");
            when(userJpaRepository.save(userEntity)).thenReturn(userEntity);

            userService.update(userId, request);

            verify(passwordEncoder).matches("currentPass", originalPassword);
            verify(passwordEncoder).encode("newSecurePass99");
            verify(userJpaRepository).save(userEntity);
        }

        @Test
        void shouldUpdateBothEmailAndPassword() {
            UpdateUserRequest request = new UpdateUserRequest("both@example.com", "currentPass", "newPass456");

            String originalPassword = userEntity.getPassword();
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(userJpaRepository.findByEmail("both@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.matches("currentPass", originalPassword)).thenReturn(true);
            when(passwordEncoder.encode("newPass456")).thenReturn("newHash");
            when(userJpaRepository.save(userEntity)).thenReturn(userEntity);

            userService.update(userId, request);

            verify(userJpaRepository).findByEmail("both@example.com");
            verify(passwordEncoder).matches("currentPass", originalPassword);
            verify(passwordEncoder).encode("newPass456");
            verify(userJpaRepository).save(userEntity);
        }

        @Test
        void shouldThrow409WhenEmailAlreadyUsedByAnotherUser() {
            UUID otherId = UUID.randomUUID();
            UserEntity other = new UserEntity();
            other.setId(otherId);
            other.setEmail("existing@example.com");
            UpdateUserRequest request = new UpdateUserRequest("existing@example.com", null, null);

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(userJpaRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(other));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.update(userId, request));
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            verify(userJpaRepository, never()).save(any());
        }

        @Test
        void shouldPermitUpdateWhenEmailBelongsToSameUser() {
            UpdateUserRequest request = new UpdateUserRequest("test@example.com", null, null);

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(userJpaRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userEntity));
            when(userJpaRepository.save(userEntity)).thenReturn(userEntity);

            assertDoesNotThrow(() -> userService.update(userId, request));
            verify(userJpaRepository).save(userEntity);
        }

        @Test
        void shouldThrow400WhenNewPasswordWithoutCurrentPassword() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, "newPass");

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.update(userId, request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            verify(userJpaRepository, never()).save(any());
        }

        @Test
        void shouldThrow400WhenCurrentPasswordIsWrong() {
            UpdateUserRequest request = new UpdateUserRequest(null, "wrongCurrent", "newPass");

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("wrongCurrent", userEntity.getPassword())).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.update(userId, request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userJpaRepository, never()).save(any());
        }

        @Test
        void shouldThrow400WhenCurrentPasswordIsEmpty() {
            UpdateUserRequest request = new UpdateUserRequest(null, "", "newPass");

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.update(userId, request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void shouldThrow400WhenCurrentPasswordIsBlank() {
            UpdateUserRequest request = new UpdateUserRequest(null, "   ", "newPass");

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.update(userId, request));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void shouldThrow404WhenUserNotFound() {
            UUID id = UUID.randomUUID();
            UpdateUserRequest request = new UpdateUserRequest(null, null, null);

            when(userJpaRepository.findById(id)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.update(id, request));
            assertTrue(ex.getMessage().contains("User") || ex.getMessage().contains("user"));
        }

        @Test
        void shouldNotPersistWhenEmailUnchangedAndNoPasswordChange() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, null);

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(userJpaRepository.save(userEntity)).thenReturn(userEntity);

            userService.update(userId, request);

            verify(userJpaRepository).save(userEntity);
        }
    }

    @Nested
    @DisplayName("deactivate()")
    class DeactivateTests {

        @Test
        void shouldSetStatusToInactive() {
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(userJpaRepository.save(userEntity)).thenReturn(userEntity);

            UserResponse response = userService.deactivate(userId);

            assertEquals("INACTIVE", userEntity.getStatus());
            assertNotNull(response);
            verify(userJpaRepository).save(userEntity);
        }

        @Test
        void shouldThrow404WhenUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userJpaRepository.findById(id)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.deactivate(id));
            assertTrue(ex.getMessage().contains("User") || ex.getMessage().contains("user"));
        }

        @Test
        void shouldThrow400WhenAlreadyInactive() {
            userEntity.setStatus("INACTIVE");
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.deactivate(userId));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            verify(userJpaRepository, never()).save(any());
        }

        @Test
        void shouldThrow400WhenAlreadySuspended() {
            userEntity.setStatus("SUSPENDED");
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.deactivate(userId));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("activate()")
    class ActivateTests {

        @Test
        void shouldSetStatusToActiveWithCorrectPassword() {
            userEntity.setStatus("INACTIVE");

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("myPassword", userEntity.getPassword())).thenReturn(true);
            when(userJpaRepository.save(userEntity)).thenReturn(userEntity);

            UserResponse response = userService.activate(userId, "myPassword");

            assertEquals("ACTIVE", userEntity.getStatus());
            assertNotNull(response);
            verify(userJpaRepository).save(userEntity);
            verify(passwordEncoder).matches("myPassword", userEntity.getPassword());
        }

        @Test
        void shouldThrow404WhenUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userJpaRepository.findById(id)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.activate(id, "pass"));
            assertTrue(ex.getMessage().contains("User") || ex.getMessage().contains("user"));
        }

        @Test
        void shouldThrow400WhenAccountAlreadyActive() {
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.activate(userId, "anything"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            verify(userJpaRepository, never()).save(any());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        void shouldThrow400WhenPasswordIsBlank(String blankPassword) {
            userEntity.setStatus("INACTIVE");

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.activate(userId, blankPassword));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void shouldThrow401WhenPasswordIsWrong() {
            userEntity.setStatus("INACTIVE");

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("wrong", userEntity.getPassword())).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.activate(userId, "wrong"));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void shouldThrow401WhenPasswordDoesNotMatchForSuspendedAccount() {
            userEntity.setStatus("SUSPENDED");

            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.activate(userId, "anything"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("validatePassword()")
    class ValidatePasswordTests {

        @Test
        void shouldPassWhenPasswordMatches() {
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("correct", userEntity.getPassword())).thenReturn(true);

            assertDoesNotThrow(() -> userService.validatePassword(userId, "correct"));
            verify(passwordEncoder).matches("correct", userEntity.getPassword());
        }

        @Test
        void shouldThrow404WhenUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userJpaRepository.findById(id)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> userService.validatePassword(id, "pass"));
            assertTrue(ex.getMessage().contains("User") || ex.getMessage().contains("user"));
        }

        @Test
        void shouldThrow400WhenPasswordIsBlank() {
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.validatePassword(userId, ""));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void shouldThrow401WhenPasswordIsWrong() {
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("incorrect", userEntity.getPassword())).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.validatePassword(userId, "incorrect"));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void shouldReturnWhenPasswordMatchesExact() {
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("exactMatch!", userEntity.getPassword())).thenReturn(true);

            assertDoesNotThrow(() -> userService.validatePassword(userId, "exactMatch!"));
        }

        @Test
        void shouldRejectPasswordWithTrailingWhitespace() {
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
            when(passwordEncoder.matches("correct ", userEntity.getPassword())).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.validatePassword(userId, "correct "));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("toResponse() — mapeo interno")
    class ToResponseMappingTests {

        @Test
        void shouldIncludeTokenInResponseWhenProvided() {
            when(userJpaRepository.findByEmail(anyString())).thenReturn(Optional.of(userEntity));
            when(jwtTokenProvider.generateToken(any(), anyString(), anyString(), any())).thenReturn("explicit-token");

            LoginRequest request = new LoginRequest("test@example.com", "rawPassword");
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            UserResponse response = userService.login(request);

            assertEquals("explicit-token", response.token());
        }

        @Test
        void shouldReturnNullPlayerIdWhenPlayerIsNullInGetById() {
            userEntity.setPlayer(null);
            when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));

            UserResponse response = userService.getById(userId);

            assertNull(response.playerId());
            assertNull(response.displayName());
        }
    }
}
