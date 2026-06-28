package ar.edu.utn.frc.tup.piii.services.players;

import ar.edu.utn.frc.tup.piii.dtos.players.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.players.UpdatePlayerRequest;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerJpaRepository playerJpaRepository;

    public PlayerService(PlayerJpaRepository playerJpaRepository) {
        this.playerJpaRepository = playerJpaRepository;
    }

    public boolean existsById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Player ID cannot be null");
        }
        return playerJpaRepository.existsById(id);
    }

    public List<PlayerResponse> listAll() {
        return playerJpaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PlayerResponse getById(UUID id) {
        PlayerEntity entity = playerJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Player not found: " + id));
        return toResponse(entity);
    }

    public PlayerResponse update(UUID id, UpdatePlayerRequest request) {
        PlayerEntity entity = playerJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Player not found: " + id));
        entity.setDisplayName(request.displayName());
        if (entity.getUser() != null) {
            entity.getUser().setUsername(request.displayName());
        }
        entity = playerJpaRepository.save(entity);
        return toResponse(entity);
    }

    public PlayerResponse updateAvatar(UUID id, String avatarUrl) {
        PlayerEntity entity = playerJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Player not found: " + id));
        entity.setAvatarUrl(avatarUrl);
        entity = playerJpaRepository.save(entity);
        return toResponse(entity);
    }

    private PlayerResponse toResponse(PlayerEntity entity) {
        return new PlayerResponse(
                entity.getId().toString(),
                entity.getDisplayName(),
                entity.getUser() != null ? entity.getUser().getId().toString() : null,
                entity.getCreatedAt(),
                entity.getAvatarUrl()
        );
    }
}