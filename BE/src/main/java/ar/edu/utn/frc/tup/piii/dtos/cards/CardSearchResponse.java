package ar.edu.utn.frc.tup.piii.dtos.cards;

import java.util.List;

public record CardSearchResponse(
        List<CardSummaryResponse> items,
        int page,
        int size,
        long totalItems
) {}
