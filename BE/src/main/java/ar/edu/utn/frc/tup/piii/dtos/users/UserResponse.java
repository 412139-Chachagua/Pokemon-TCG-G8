package ar.edu.utn.frc.tup.piii.dtos.users;

public record UserResponse(
        String id,
        String email,
        String displayName,
        String playerId,
        String token
) {}