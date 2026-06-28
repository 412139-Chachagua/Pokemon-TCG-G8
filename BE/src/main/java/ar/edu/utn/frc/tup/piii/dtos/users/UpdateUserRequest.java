package ar.edu.utn.frc.tup.piii.dtos.users;

public record UpdateUserRequest(String email, String currentPassword, String newPassword) {}
