package ar.edu.utn.frc.tup.piii.dtos.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(min = 6, max = 100) String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 50) String displayName
) {}
