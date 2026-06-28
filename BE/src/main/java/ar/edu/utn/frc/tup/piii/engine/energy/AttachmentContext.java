package ar.edu.utn.frc.tup.piii.engine.energy;

import java.util.UUID;

public record AttachmentContext(
    AttachmentOrigin origin,
    UUID playerId,
    int turnNumber
) {}
