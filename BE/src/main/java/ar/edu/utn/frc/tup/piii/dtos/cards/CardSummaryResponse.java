package ar.edu.utn.frc.tup.piii.dtos.cards;

import java.util.List;

public record CardSummaryResponse(
        String id,
        String name,
        String supertype,
        String setCode,
        String number,
        String imageSmallUrl,
        List<String> subtypes,
        String stage
) {
}
