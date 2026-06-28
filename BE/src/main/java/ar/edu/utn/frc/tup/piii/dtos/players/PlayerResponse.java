package ar.edu.utn.frc.tup.piii.dtos.players;

import java.time.Instant;

public record PlayerResponse(
        String id,
        String displayName,
        String userId,
        Instant createdAt,
        String avatarUrl
) {}