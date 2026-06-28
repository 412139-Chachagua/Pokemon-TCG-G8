package ar.edu.utn.frc.tup.piii.dtos.cards;

import java.util.List;

public record AttackDto(
    Integer index,
    String name,
    List<String> cost,
    Integer convertedEnergyCost,
    String damage,
    String text,
    Integer baseDamage
) {}
