package ar.edu.utn.frc.tup.piii.security;

import java.util.UUID;

public record JwtUserDetails(UUID userId, String role, UUID playerId) {
}
