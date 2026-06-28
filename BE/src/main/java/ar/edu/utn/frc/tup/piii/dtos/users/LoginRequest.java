package ar.edu.utn.frc.tup.piii.dtos.users;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {}
