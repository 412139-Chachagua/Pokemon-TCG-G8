package ar.edu.utn.frc.tup.piii.dtos.matches;

import java.util.Map;

public record GameEventDto(
        String type,
        String message,
        Map<String, Object> payload,
        Integer turnNumber
) {}
