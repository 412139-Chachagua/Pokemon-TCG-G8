package ar.edu.utn.frc.tup.piii.engine.ports.impl;

import ar.edu.utn.frc.tup.piii.domain.cards.*;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.engine.attack.TextEffectParser;
import ar.edu.utn.frc.tup.piii.dtos.cards.AbilityDto;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardAttackEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardResistanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardWeaknessEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.services.cards.CardCacheSyncService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CardLookupAdapter implements CardLookupPort {

    private static final Logger log = LoggerFactory.getLogger(CardLookupAdapter.class);
    private final CardJpaRepository cardJpaRepository;
    private final CardCacheSyncService cardCacheSyncService;

    public CardLookupAdapter(CardJpaRepository cardJpaRepository, CardCacheSyncService cardCacheSyncService) {
        this.cardJpaRepository = cardJpaRepository;
        this.cardCacheSyncService = cardCacheSyncService;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cards", key = "#cardId")
    public CardDefinition getCardById(String cardId) {
        Optional<CardEntity> optional = cardJpaRepository.findById(cardId);
        if (optional.isEmpty()) {
            log.warn("Card {} not found in DB, attempting fallback sync from external API", cardId);
            boolean synced = cardCacheSyncService.syncCardById(cardId);
            if (synced) {
                optional = cardJpaRepository.findById(cardId);
            }
        }
        if (optional.isEmpty()) {
            return null;
        }
        CardEntity entity = optional.get();
        CardSupertype supertype = resolveSupertype(entity.getSupertype());
        return switch (supertype) {
            case POKEMON -> toPokemon(entity);
            case TRAINER -> toTrainer(entity);
            case ENERGY -> toEnergy(entity);
        };
    }

    private CardSupertype resolveSupertype(String value) {
        if (value == null) return CardSupertype.POKEMON;
        return switch (value) {
            case "Pokémon", "Pokemon", "POKEMON" -> CardSupertype.POKEMON;
            case "Trainer", "TRAINER" -> CardSupertype.TRAINER;
            case "Energy", "ENERGY" -> CardSupertype.ENERGY;
            default -> CardSupertype.POKEMON;
        };
    }

    private PokemonCardDefinition toPokemon(CardEntity e) {
        PokemonCardDefinition d = new PokemonCardDefinition();
        mapBase(e, d);
        d.setHp(e.getHp() != null ? e.getHp() : 0);
        d.setStage(e.getPokemonStage());
        d.setEvolvesFrom(e.getEvolvesFrom());
        d.setTypes(splitEnumList(e.getPokemonTypes(), EnergyType.class));
        d.setRetreatCost(splitEnumList(e.getRetreatCost(), EnergyType.class));
        d.setEx(e.getIsEx() != null && e.getIsEx());
        d.setMega(e.getIsMega() != null && e.getIsMega());
        if (e.getAttacks() != null) {
            d.setAttacks(e.getAttacks().stream()
                    .map(this::toAttack)
                    .collect(Collectors.toList()));
        }
        if (e.getWeaknesses() != null) {
            d.setWeaknesses(e.getWeaknesses().stream()
                    .map(this::toWeakness)
                    .collect(Collectors.toList()));
        }
        if (e.getResistances() != null) {
            d.setResistances(e.getResistances().stream()
                    .map(this::toResistance)
                    .collect(Collectors.toList()));
        }
        d.setAbilities(hydrateAbilities(e.getAbilities()));
        return d;
    }

    private TrainerCardDefinition toTrainer(CardEntity e) {
        TrainerCardDefinition d = new TrainerCardDefinition();
        mapBase(e, d);
        TrainerSubtype subtype = resolveTrainerSubtype(e.getTrainerSubtype());
        if (subtype == null) {
            subtype = resolveTrainerSubtypeFromSubtypes(e.getSubtypes());
        }
        d.setTrainerSubtype(subtype);
        d.setAceSpec(e.getIsAceSpec() != null && e.getIsAceSpec());
        d.setEffectCode(e.getEffectCode());
        return d;
    }

    private TrainerSubtype resolveTrainerSubtypeFromSubtypes(String subtypes) {
        if (subtypes == null) return null;
        List<String> list = splitList(subtypes);
        for (String s : list) {
            String upper = s.toUpperCase()
                    .replace("É", "E")
                    .replace(" ", "_");
            if ("TOOL".equals(upper) || "POKEMON_TOOL".equals(upper)) {
                return TrainerSubtype.POKEMON_TOOL;
            }
            if ("SUPPORTER".equals(upper)) return TrainerSubtype.SUPPORTER;
            if ("STADIUM".equals(upper)) return TrainerSubtype.STADIUM;
            if ("ACE_SPEC".equals(upper)) return TrainerSubtype.ACE_SPEC;
        }
        if (list.stream().anyMatch(s -> s.equalsIgnoreCase("ITEM"))) {
            return TrainerSubtype.ITEM;
        }
        return null;
    }

    private EnergyCardDefinition toEnergy(CardEntity e) {
        EnergyCardDefinition d = new EnergyCardDefinition();
        mapBase(e, d);
        d.setEnergyCardType(resolveEnergyCardType(e.getEnergyCardType()));
        d.setProvides(splitEnumList(e.getProvidesEnergyTypes(), EnergyType.class));
        d.setStrategyKey(e.getStrategyKey());
        return d;
    }

    private void mapBase(CardEntity e, CardDefinition d) {
        d.setId(e.getId());
        d.setName(e.getName());
        d.setSupertype(e.getSupertype());
        d.setSubtypes(splitList(e.getSubtypes()));
        d.setSetCode(e.getSetCode());
        d.setNumber(e.getNumber());
        d.setImageSmallUrl(e.getImageSmallUrl());
        d.setImageLargeUrl(e.getImageLargeUrl());
        d.setRulesText(splitList(e.getRulesText()));
    }

    private PokemonCardDefinition.AttackDefinition toAttack(CardAttackEntity a) {
        PokemonCardDefinition.AttackDefinition ad = new PokemonCardDefinition.AttackDefinition();
        try {
            ad.setIndex(a.getAttackIndex() != null ? a.getAttackIndex() : 0);
        } catch (Exception ex) {
            ad.setIndex(0);
        }
        ad.setName(a.getName());
        ad.setCost(splitEnumList(a.getPrintedCost(), EnergyType.class));
        ad.setDamage(a.getDamageText());
        ad.setText(a.getEffectText());
        ad.setEffects(parseAttackEffects(a.getEffectCode(), a.getEffectText()));
        return ad;
    }

    private List<AttackEffect> parseAttackEffects(String effectCode, String effectText) {
        if (effectText != null && !effectText.isBlank()) {
            List<AttackEffect> parsed = TextEffectParser.parse(effectText);
            return parsed;
        }
        log.warn("[attack-effects] effectText is null/blank — no effects");
        return List.of();
    }

    private PokemonCardDefinition.WeaknessDefinition toWeakness(CardWeaknessEntity w) {
        PokemonCardDefinition.WeaknessDefinition wd = new PokemonCardDefinition.WeaknessDefinition();
        wd.setType(parseEnergyType(w.getEnergyType()));
        wd.setValue(w.getMultiplier() != null ? "×" + w.getMultiplier() : null);
        return wd;
    }

    private PokemonCardDefinition.ResistanceDefinition toResistance(CardResistanceEntity r) {
        PokemonCardDefinition.ResistanceDefinition rd = new PokemonCardDefinition.ResistanceDefinition();
        rd.setType(parseEnergyType(r.getEnergyType()));
        rd.setValue(r.getValue() != null ? String.valueOf(r.getValue()) : null);
        return rd;
    }

    private List<AbilityDefinition> hydrateAbilities(String jsonAbilities) {
        if (jsonAbilities == null || jsonAbilities.isBlank()) return List.of();
        try {
            ObjectMapper mapper = new ObjectMapper();
            AbilityDto[] dtos = mapper.readValue(jsonAbilities, AbilityDto[].class);
            List<AbilityDefinition> result = new ArrayList<>();
            for (AbilityDto dto : dtos) {
                AbilityType type = AbilityType.ABILITY;
                if (dto.type() != null) {
                    if (dto.type().contains("Power")) type = AbilityType.POKEMON_POWER;
                    else if (dto.type().contains("Body")) type = AbilityType.POKEMON_BODY;
                }
                result.add(new AbilityDefinition(dto.name(), dto.text(), type));
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private EnergyType parseEnergyType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return EnergyType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TrainerSubtype resolveTrainerSubtype(String value) {
        if (value == null) return null;
        return switch (value) {
            case "ITEM" -> TrainerSubtype.ITEM;
            case "SUPPORTER" -> TrainerSubtype.SUPPORTER;
            case "STADIUM" -> TrainerSubtype.STADIUM;
            case "ACE_SPEC" -> TrainerSubtype.ACE_SPEC;
            case "POKEMON_TOOL" -> TrainerSubtype.POKEMON_TOOL;
            default -> null;
        };
    }

    private EnergyCardType resolveEnergyCardType(String value) {
        if (value == null) return null;
        return switch (value) {
            case "BASIC" -> EnergyCardType.BASIC;
            case "SPECIAL" -> EnergyCardType.SPECIAL;
            default -> null;
        };
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private <T extends Enum<T>> List<T> splitEnumList(String value, Class<T> enumClass) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Enum.valueOf(enumClass, s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(e -> e != null)
                .collect(Collectors.toList());
    }
}
