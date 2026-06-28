package ar.edu.utn.frc.tup.piii.dtos.cards;

import java.util.List;

public record PokemonTcgApiCardDto(
    String id,
    String name,
    String supertype,
    List<String> subtypes,
    String hp,
    List<String> types,
    List<String> rules,
    String evolvesFrom,
    List<String> evolvesTo,
    List<AbilityDto> abilities,
    List<AttackDto> attacks,
    List<WeaknessDto> weaknesses,
    List<ResistanceDto> resistances,
    List<String> retreatCost,
    Integer convertedRetreatCost,
    SetInfoDto set,
    ImagesDto images,
    String rarity
) {}
