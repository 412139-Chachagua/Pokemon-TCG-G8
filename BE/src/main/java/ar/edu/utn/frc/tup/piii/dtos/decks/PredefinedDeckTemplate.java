package ar.edu.utn.frc.tup.piii.dtos.decks;

import java.util.List;
import java.util.UUID;

public record PredefinedDeckTemplate(
        UUID id,
        String name,
        String mainCardId,
        List<PredefinedDeckCardEntry> cards
) {
}
