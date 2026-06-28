package ar.edu.utn.frc.tup.piii.services.users;

import ar.edu.utn.frc.tup.piii.dtos.users.CreateUserRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.UpdateUserRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.UserResponse;
import ar.edu.utn.frc.tup.piii.exceptions.ConflictException;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.UserEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.UserJpaRepository;
import ar.edu.utn.frc.tup.piii.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserJpaRepository userJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserJpaRepository userJpaRepository, PlayerJpaRepository playerJpaRepository,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userJpaRepository = userJpaRepository;
        this.playerJpaRepository = playerJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public UserResponse register(CreateUserRequest request) {
        if (userJpaRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("El email ya está registrado");
        }
        if (userJpaRepository.findByUsername(request.displayName()).isPresent()) {
            throw new ConflictException("El nombre de usuario ya está en uso");
        }
        UserEntity entity = new UserEntity();
        entity.setUsername(request.displayName());
        entity.setEmail(request.email());
        entity.setPassword(passwordEncoder.encode(request.password()));
        entity.setRole("PLAYER");
        entity.setStatus("ACTIVE");

        PlayerEntity player = new PlayerEntity();
        player.setUser(entity);
        player.setDisplayName(request.displayName());
        entity.setPlayer(player);

        entity = userJpaRepository.save(entity);

        String token = jwtTokenProvider.generateToken(
                entity.getId(), entity.getEmail(), entity.getRole(), entity.getPlayer().getId());
        return toResponse(entity, token);
    }

    public UserResponse login(LoginRequest request) {
        UserEntity entity = userJpaRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if ("INACTIVE".equals(entity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Account deactivated|" + entity.getId().toString());
        }
        if (!passwordEncoder.matches(request.password(), entity.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtTokenProvider.generateToken(
                entity.getId(), entity.getEmail(), entity.getRole(), entity.getPlayer().getId());
        return toResponse(entity, token);
    }

    public UserResponse getById(UUID id) {
        UserEntity entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        return toResponse(entity, null);
    }

    public List<UserResponse> listAll() {
        return userJpaRepository.findAll().stream()
                .map(e -> toResponse(e, null))
                .collect(Collectors.toList());
    }

    public boolean existsById(UUID id) {
        return userJpaRepository.existsById(id);
    }

    public UserResponse update(UUID id, UpdateUserRequest request) {
        UserEntity entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        if (request.email() != null && !request.email().isBlank()) {
            userJpaRepository.findByEmail(request.email())
                    .filter(found -> !found.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                    });
            entity.setEmail(request.email());
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required");
            }
            if (!passwordEncoder.matches(request.currentPassword(), entity.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }
            entity.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        entity = userJpaRepository.save(entity);
        return toResponse(entity, null);
    }

    public UserResponse deactivate(UUID id) {
        UserEntity entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        if (!"ACTIVE".equals(entity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not active");
        }
        entity.setStatus("INACTIVE");
        entity = userJpaRepository.save(entity);
        return toResponse(entity, null);
    }

    public UserResponse activate(UUID id, String password) {
        UserEntity entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        if (!"INACTIVE".equals(entity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not inactive");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (!passwordEncoder.matches(password, entity.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }
        entity.setStatus("ACTIVE");
        entity = userJpaRepository.save(entity);
        return toResponse(entity, null);
    }

    public void validatePassword(UUID id, String password) {
        UserEntity entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (!passwordEncoder.matches(password, entity.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }
    }

    private UserResponse toResponse(UserEntity entity, String token) {
        return new UserResponse(
                entity.getId().toString(),
                entity.getEmail(),
                entity.getPlayer() != null ? entity.getPlayer().getDisplayName() : null,
                entity.getPlayer() != null ? entity.getPlayer().getId().toString() : null,
                token
        );
    }
}