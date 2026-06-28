package ar.edu.utn.frc.tup.piii.mappers.cards;

import ar.edu.utn.frc.tup.piii.dtos.cards.AttackDto;
import ar.edu.utn.frc.tup.piii.dtos.cards.PokemonTcgApiCardDto;
import ar.edu.utn.frc.tup.piii.dtos.cards.ResistanceDto;
import ar.edu.utn.frc.tup.piii.dtos.cards.WeaknessDto;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.EnergyCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardAttackEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardResistanceEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardWeaknessEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.TrainerCardEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Deprecated
public class ApiCardMapper {

    public PokemonCardEntity toPokemonCardEntity(PokemonTcgApiCardDto dto) {
        PokemonCardEntity entity = new PokemonCardEntity();
        entity.setId(dto.id());
        entity.setName(dto.name());
        entity.setSupertype(dto.supertype());
        entity.setSubtypes(listToCommaString(dto.subtypes()));
        entity.setHp(dto.hp() != null ? Integer.parseInt(dto.hp()) : null);
        entity.setPokemonTypes(listToCommaString(dto.types()));
        entity.setEvolvesFrom(dto.evolvesFrom());
        entity.setRetreatCost(listToCommaString(dto.retreatCost()));
        entity.setConvertedRetreatCost(dto.convertedRetreatCost());
        entity.setIsEx(dto.subtypes() != null && dto.subtypes().stream().anyMatch(s -> s.equalsIgnoreCase("EX")));
        entity.setIsMega(dto.subtypes() != null && dto.subtypes().stream().anyMatch(s -> s.equalsIgnoreCase("MEGA")));
        entity.setRulesText(dto.rules() != null ? String.join("|", dto.rules()) : null);
        entity.setRarity(dto.rarity());
        entity.setImageSmallUrl(dto.images() != null ? dto.images().small() : null);
        entity.setImageLargeUrl(dto.images() != null ? dto.images().large() : null);

        if (dto.attacks() != null) {
            List<PokemonCardAttackEntity> attacks = new ArrayList<>();
            for (int i = 0; i < dto.attacks().size(); i++) {
                attacks.add(toPokemonCardAttackEntity(dto.attacks().get(i), entity, i));
            }
            entity.setAttacks(attacks);
        }

        if (dto.weaknesses() != null) {
            entity.setWeaknesses(dto.weaknesses().stream()
                    .map(w -> toPokemonCardWeaknessEntity(w, entity))
                    .collect(Collectors.toList()));
        }

        if (dto.resistances() != null) {
            entity.setResistances(dto.resistances().stream()
                    .map(r -> toPokemonCardResistanceEntity(r, entity))
                    .collect(Collectors.toList()));
        }

        return entity;
    }

    public TrainerCardEntity toTrainerCardEntity(PokemonTcgApiCardDto dto) {
        TrainerCardEntity entity = new TrainerCardEntity();
        entity.setId(dto.id());
        entity.setName(dto.name());
        entity.setSupertype(dto.supertype());
        entity.setSubtypes(listToCommaString(dto.subtypes()));
        entity.setRulesText(dto.rules() != null ? String.join("|", dto.rules()) : null);
        entity.setRarity(dto.rarity());
        entity.setImageSmallUrl(dto.images() != null ? dto.images().small() : null);
        entity.setImageLargeUrl(dto.images() != null ? dto.images().large() : null);
        return entity;
    }

    public EnergyCardEntity toEnergyCardEntity(PokemonTcgApiCardDto dto) {
        EnergyCardEntity entity = new EnergyCardEntity();
        entity.setId(dto.id());
        entity.setName(dto.name());
        entity.setSupertype(dto.supertype());
        entity.setSubtypes(listToCommaString(dto.subtypes()));
        entity.setRulesText(dto.rules() != null ? String.join("|", dto.rules()) : null);
        entity.setImageSmallUrl(dto.images() != null ? dto.images().small() : null);
        entity.setImageLargeUrl(dto.images() != null ? dto.images().large() : null);
        return entity;
    }

    private PokemonCardAttackEntity toPokemonCardAttackEntity(AttackDto dto, PokemonCardEntity card, int index) {
        PokemonCardAttackEntity entity = new PokemonCardAttackEntity();
        entity.setPokemonCard(card);
        entity.setAttackIndex(index);
        entity.setName(dto.name());
        entity.setPrintedCost(listToCommaString(dto.cost()));
        entity.setConvertedEnergyCost(dto.convertedEnergyCost() != null ? dto.convertedEnergyCost() : 0);
        entity.setDamageText(dto.damage());
        entity.setEffectText(dto.text());
        return entity;
    }

    private PokemonCardWeaknessEntity toPokemonCardWeaknessEntity(WeaknessDto dto, PokemonCardEntity card) {
        PokemonCardWeaknessEntity entity = new PokemonCardWeaknessEntity();
        entity.setPokemonCard(card);
        entity.setWeaknessType(dto.type());
        entity.setWeaknessValue(dto.value());
        return entity;
    }

    private PokemonCardResistanceEntity toPokemonCardResistanceEntity(ResistanceDto dto, PokemonCardEntity card) {
        PokemonCardResistanceEntity entity = new PokemonCardResistanceEntity();
        entity.setPokemonCard(card);
        entity.setResistanceType(dto.type());
        entity.setResistanceValue(dto.value());
        return entity;
    }

    private String listToCommaString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }
}
