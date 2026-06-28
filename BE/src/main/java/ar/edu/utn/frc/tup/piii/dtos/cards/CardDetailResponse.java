package ar.edu.utn.frc.tup.piii.dtos.cards;

import java.util.List;

public record CardDetailResponse(
        String id,
        String name,
        String supertype,
        List<String> subtypes,
        String setCode,
        String number,
        String imageSmallUrl,
        String imageLargeUrl,
        List<String> rulesText,
        Integer hp,
        String stage,
        String evolvesFrom,
        List<String> types,
        List<AttackDto> attacks,
        List<WeaknessDto> weaknesses,
        List<ResistanceDto> resistances,
        List<String> retreatCost,
        Boolean isEx,
        Boolean isMega,
        List<CardAbilityResponse> abilities,
        List<String> providesEnergyTypes
) {
}
