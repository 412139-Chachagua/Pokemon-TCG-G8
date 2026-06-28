package ar.edu.utn.frc.tup.piii.mappers.cards;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.AbilityType;
import ar.edu.utn.frc.tup.piii.dtos.cards.AbilityDto;
import ar.edu.utn.frc.tup.piii.dtos.cards.AttackDto;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardAbilityResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.PokemonTcgApiCardDto;
import ar.edu.utn.frc.tup.piii.dtos.cards.ResistanceDto;
import ar.edu.utn.frc.tup.piii.dtos.cards.WeaknessDto;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardDetailResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSummaryResponse;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffectType;
import ar.edu.utn.frc.tup.piii.engine.attack.TextEffectParser;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardAttackEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardResistanceEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardWeaknessEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CardMapper {

    private final AbilityRegistry abilityRegistry;

    public CardMapper(AbilityRegistry abilityRegistry) {
        this.abilityRegistry = abilityRegistry;
    }

    public CardEntity toCardEntity(PokemonTcgApiCardDto request) {
        CardEntity entity = new CardEntity();
        entity.setId(request.id());
        entity.setName(request.name());
        entity.setSupertype(normalizeSupertype(request.supertype()));
        entity.setSubtypes(listToCommaString(request.subtypes()));
        entity.setSetCode(request.set() != null ? request.set().id() : null);
        if (request.id() != null && request.id().contains("-")) {
            entity.setNumber(request.id().substring(request.id().indexOf("-") + 1));
        }
        entity.setRarity(request.rarity());
        entity.setImageSmallUrl(request.images() != null ? request.images().small() : null);
        entity.setImageLargeUrl(request.images() != null ? request.images().large() : null);
        entity.setHp(request.hp() != null ? Integer.parseInt(request.hp()) : null);
        entity.setPokemonStage(determineStage(request.subtypes()));
        entity.setEvolvesFrom(request.evolvesFrom());
        entity.setPokemonTypes(listToCommaString(request.types()));
        entity.setRetreatCost(listToCommaString(request.retreatCost()));
        entity.setConvertedRetreatCost(request.convertedRetreatCost());
        entity.setIsEx(request.subtypes() != null && request.subtypes().stream().anyMatch(s -> s.equalsIgnoreCase("EX")));
        entity.setIsMega(request.subtypes() != null && request.subtypes().stream().anyMatch(s -> s.equalsIgnoreCase("MEGA")));
        entity.setRulesText(request.rules() != null ? String.join("|", request.rules()) : null);

        entity.setEvolvesTo(listToCommaString(request.evolvesTo()));

        if (request.abilities() != null && !request.abilities().isEmpty()) {
            try {
                entity.setAbilities(new ObjectMapper().writeValueAsString(request.abilities()));
            } catch (JsonProcessingException e) {
                entity.setAbilities(null);
            }
        }

        if ("Energy".equalsIgnoreCase(request.supertype())) {
            boolean isBasic = request.subtypes() != null && request.subtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Basic"));
            entity.setEnergyCardType(isBasic ? "BASIC" : "SPECIAL");

            if (request.rules() != null && !request.rules().isEmpty()) {
                entity.setProvidesEnergyTypes(String.join(",", request.rules()));
            } else if (request.name() != null) {
                String energyName = request.name().replace(" Energy", "").toUpperCase();
                entity.setProvidesEnergyTypes(energyName);
            }

            if (request.name() != null) {
                String name = request.name().toLowerCase();
                if (name.contains("double colorless")) {
                    entity.setStrategyKey("DOUBLE_COLORLESS");
                } else if (name.contains("rainbow")) {
                    entity.setStrategyKey("RAINBOW");
                } else if (name.contains("strong")) {
                    entity.setStrategyKey("STRONG");
                } else {
                    entity.setStrategyKey("BASIC");
                }
            } else {
                entity.setStrategyKey("BASIC");
            }
        }

        if ("Trainer".equalsIgnoreCase(request.supertype())) {
            String subtype = request.subtypes() != null && !request.subtypes().isEmpty()
                    ? request.subtypes().get(0)
                    : null;
            if (subtype != null) {
                if (subtype.equalsIgnoreCase("SUPPORTER")) {
                    entity.setTrainerSubtype("SUPPORTER");
                } else if (subtype.equalsIgnoreCase("STADIUM")) {
                    entity.setTrainerSubtype("STADIUM");
                } else if (subtype.equalsIgnoreCase("ITEM")) {
                    entity.setTrainerSubtype("ITEM");
                } else if (subtype.equalsIgnoreCase("TOOL")) {
                    entity.setTrainerSubtype("POKEMON_TOOL");
                } else {
                    entity.setTrainerSubtype(subtype.toUpperCase());
                }
            }
            entity.setIsAceSpec(request.subtypes() != null && request.subtypes().stream().anyMatch(s -> s.equalsIgnoreCase("ACE SPEC")));
            entity.setEffectCode(generateTrainerEffectCode(request.name(), request.rules()));
        }

        if (request.attacks() != null) {
            List<CardAttackEntity> attacks = new ArrayList<>();
            for (int i = 0; i < request.attacks().size(); i++) {
                attacks.add(toAttackEntity(request.attacks().get(i), entity, i));
            }
            entity.setAttacks(attacks);
        }

        if (request.weaknesses() != null) {
            entity.setWeaknesses(request.weaknesses().stream()
                    .map(w -> toWeaknessEntity(w, entity))
                    .collect(Collectors.toList()));
        }

        if (request.resistances() != null) {
            entity.setResistances(request.resistances().stream()
                    .map(r -> toResistanceEntity(r, entity))
                    .collect(Collectors.toList()));
        }

        return entity;
    }

    public CardSummaryResponse toSummaryResponse(CardEntity entity) {
        List<String> subtypes = commaStringToList(entity.getSubtypes());
        return new CardSummaryResponse(
                entity.getId(),
                entity.getName(),
                entity.getSupertype(),
                entity.getSetCode(),
                entity.getNumber(),
                entity.getImageSmallUrl(),
                subtypes,
                entity.getPokemonStage()
        );
    }

    public CardDetailResponse toDetailResponse(CardEntity entity) {
        List<String> subtypes = commaStringToList(entity.getSubtypes());
        List<String> rulesText = entity.getRulesText() != null
                ? Arrays.asList(entity.getRulesText().split("\\|"))
                : List.of();
        List<String> types = commaStringToList(entity.getPokemonTypes());
        List<String> retreatCost = commaStringToList(entity.getRetreatCost());

        List<AttackDto> attacks = entity.getAttacks() != null
                ? entity.getAttacks().stream().map(this::toAttackDto).collect(Collectors.toList())
                : List.of();

        List<WeaknessDto> weaknesses = entity.getWeaknesses() != null
                ? entity.getWeaknesses().stream().map(this::toWeaknessDto).collect(Collectors.toList())
                : List.of();

        List<ResistanceDto> resistances = entity.getResistances() != null
                ? entity.getResistances().stream().map(this::toResistanceDto).collect(Collectors.toList())
                : List.of();

        List<CardAbilityResponse> abilities = entity.getAbilities() != null && !entity.getAbilities().isBlank()
                ? toCardAbilityResponseList(entity.getAbilities())
                : List.of();

        List<String> providesEnergyTypes = commaStringToList(entity.getProvidesEnergyTypes());

        return new CardDetailResponse(
                entity.getId(),
                entity.getName(),
                entity.getSupertype(),
                subtypes,
                entity.getSetCode(),
                entity.getNumber(),
                entity.getImageSmallUrl(),
                entity.getImageLargeUrl(),
                rulesText,
                entity.getHp(),
                entity.getPokemonStage(),
                entity.getEvolvesFrom(),
                types,
                attacks,
                weaknesses,
                resistances,
                retreatCost,
                entity.getIsEx(),
                entity.getIsMega(),
                abilities,
                providesEnergyTypes
        );
    }

    private CardAttackEntity toAttackEntity(AttackDto dto, CardEntity card, int index) {
        CardAttackEntity entity = new CardAttackEntity();
        entity.setCard(card);
        entity.setAttackIndex(index);
        entity.setName(dto.name());
        entity.setPrintedCost(listToCommaString(dto.cost()));
        entity.setConvertedEnergyCost(dto.convertedEnergyCost() != null ? dto.convertedEnergyCost() : 0);
        entity.setDamageText(dto.damage());
        entity.setEffectText(dto.text());
        entity.setBaseDamage(dto.baseDamage());
        entity.setEffectCode(generateEffectCode(dto.text()));
        return entity;
    }

    private String generateEffectCode(String effectText) {
        if (effectText == null || effectText.isBlank()) return null;
        List<AttackEffect> effects = TextEffectParser.parse(effectText);
        if (effects.isEmpty()) return null;
        return effects.stream()
                .map(CardMapper::formatEffectCode)
                .collect(Collectors.joining(";;"));
    }

    public static String formatEffectCode(AttackEffect effect) {
        AttackEffectType type = effect.getType();
        Map<String, Object> params = effect.getParams();
        if (params == null) return type.name();
        switch (type) {
            case APPLY_SPECIAL_CONDITION:
                Object condition = params.get("condition");
                Object condTarget = params.get("target");
                StringBuilder asc = new StringBuilder(type.name()).append(":").append(condition != null ? condition : "");
                if ("self".equals(condTarget) || "both".equals(condTarget)) asc.append(":").append(condTarget);
                return asc.toString();
            case DISCARD_ENERGY:
                Object count = params.get("count");
                Object target = params.get("target");
                StringBuilder de = new StringBuilder(type.name()).append(":").append(count != null ? count : 1);
                if ("attacker".equals(target)) de.append(":attacker");
                return de.toString();
            case DRAW_CARDS:
                Object drawCount = params.get("count");
                return type.name() + ":" + (drawCount != null ? drawCount : 1);
            case SEARCH_DECK:
                String st = (String) params.getOrDefault("searchType", "ANY");
                Object sc = params.get("count");
                String et = (String) params.get("energyType");
                StringBuilder sd = new StringBuilder(type.name()).append(":").append(st).append(":").append(sc != null ? sc : 1);
                if (et != null) sd.append(":").append(et);
                return sd.toString();
            case ATTACH_ENERGY:
                String aeSource = (String) params.getOrDefault("source", "deck");
                String aeEnergyType = (String) params.get("energyType");
                Object aeCount = params.get("count");
                String aeTarget = (String) params.getOrDefault("target", "attacker");
                StringBuilder ae = new StringBuilder(type.name()).append(":").append(aeSource);
                if (aeEnergyType != null) ae.append(":").append(aeEnergyType);
                ae.append(":").append(aeCount != null ? aeCount : 1);
                if (!"attacker".equals(aeTarget)) ae.append(":").append(aeTarget);
                return ae.toString();
            case MOVE_ENERGY:
                String meSrc = (String) params.getOrDefault("sourcePokemon", "attacker");
                String meTgt = (String) params.getOrDefault("targetPokemon", "ownBench");
                Object meCount = params.get("count");
                return type.name() + ":" + meSrc + ":" + meTgt + ":" + (meCount != null ? meCount : 1);
            case DAMAGE_PREVENTION:
                Object threshold = params.get("threshold");
                if (threshold != null) {
                    return type.name() + ":true:" + threshold;
                }
                return type.name() + ":true";
            case CANNOT_ATTACK_NEXT_TURN:
                Object attackName = params.get("attackName");
                if (attackName != null) {
                    return type.name() + ":true:" + attackName;
                }
                return type.name() + ":true";
            case SUPPORTER_LOCK:
                return type.name() + ":true";
            case OPPONENT_DISCARD_HAND:
                Object odmCount = params.get("count");
                return type.name() + ":" + (odmCount != null ? odmCount : 1);
            case NEXT_TURN_DAMAGE_BONUS:
                Object bonusVal = params.get("bonus");
                return type.name() + ":" + (bonusVal != null ? bonusVal : 40);
            case RETREAT_LOCK:
                return type.name() + ":true";
            case DAMAGE_REDUCTION:
                Object redVal = params.get("reduction");
                return type.name() + ":" + (redVal != null ? redVal : 20);
            case DISCARD_OPPONENT_DECK:
                Object dodCount = params.get("count");
                Object dodTarget = params.get("target");
                StringBuilder dod = new StringBuilder(type.name()).append(":").append(dodCount != null ? dodCount : 1);
                if ("self".equals(dodTarget)) dod.append(":self");
                return dod.toString();
            case SEARCH_DISCARD:
                Object sdc = params.get("count");
                return type.name() + ":" + (sdc != null ? sdc : 2);
            case RECYCLE_FROM_DISCARD:
                return type.name() + ":true";
            case OPPONENT_SHUFFLE_DRAW:
                Object osdc = params.get("count");
                return type.name() + ":" + (osdc != null ? osdc : 4);
            case DAMAGE_ALL_BENCH:
                Object dab = params.get("damageCounters");
                return type.name() + ":" + (dab != null ? dab : 2);
            case DEFENDER_CANNOT_ATTACK:
                return type.name() + ":true";
            case DAMAGE_BENCH:
                Object dmg = params.get("damage");
                Object ownBench = params.get("ownBench");
                StringBuilder db = new StringBuilder(type.name()).append(":").append(dmg != null ? dmg : 10);
                if (Boolean.TRUE.equals(ownBench)) db.append(":ownBench");
                return db.toString();
            case HEAL_USER:
                Object healCount = params.get("count");
                Object benchTarget = params.get("targetBench");
                Object healAllTarget = params.get("healAll");
                Object healFullTarget = params.get("healFull");
                Object clearCond = params.get("clearConditions");
                StringBuilder hu = new StringBuilder(type.name()).append(":").append(healCount != null ? healCount : 3);
                if (Boolean.TRUE.equals(benchTarget)) hu.append(":bench");
                else if (Boolean.TRUE.equals(healAllTarget)) hu.append(":all");
                else if (Boolean.TRUE.equals(healFullTarget)) hu.append(":full");
                else if (Boolean.TRUE.equals(clearCond)) hu.append(":clear");
                return hu.toString();
            case COIN_FLIP_BEFORE_DAMAGE:
            case COIN_FLIP_AFTER_DAMAGE:
                Object effectType = params.get("effectType");
                Object effectParam = params.get("effectParam");
                Object applyOnHeads = params.get("applyOnHeads");
                StringBuilder cf = new StringBuilder(type.name()).append(":").append(effectType != null ? effectType : "");
                if (effectParam != null) cf.append(":").append(effectParam);
                if (applyOnHeads != null && "false".equals(applyOnHeads.toString())) cf.append(":false");
                return cf.toString();
            case RECOIL:
                Object recoilCount = params.get("count");
                return type.name() + ":" + (recoilCount != null ? recoilCount : 2);
            case SWITCH_AFTER_DAMAGE:
                Object switchAttacker = params.get("switchAttacker");
                return type.name() + ":" + (Boolean.TRUE.equals(switchAttacker) ? "true" : "false");
            case ABILITY_SUPPRESSION:
                return type.name() + ":true";
            case DISCARD_TOOL:
                return type.name() + ":true";
            default:
                return type.name();
        }
    }

    private CardWeaknessEntity toWeaknessEntity(WeaknessDto dto, CardEntity card) {
        CardWeaknessEntity entity = new CardWeaknessEntity();
        entity.setCard(card);
        entity.setEnergyType(dto.type());
        if (dto.value() != null) {
            String numeric = dto.value().replaceAll("[^0-9]", "");
            entity.setMultiplier(numeric.isEmpty() ? 2 : Integer.parseInt(numeric));
        } else {
            entity.setMultiplier(2);
        }
        return entity;
    }

    private CardResistanceEntity toResistanceEntity(ResistanceDto dto, CardEntity card) {
        CardResistanceEntity entity = new CardResistanceEntity();
        entity.setCard(card);
        entity.setEnergyType(dto.type());
        try {
            entity.setValue(dto.value() != null ? Integer.parseInt(dto.value()) : -20);
        } catch (NumberFormatException e) {
            entity.setValue(-20);
        }
        return entity;
    }

    private AttackDto toAttackDto(CardAttackEntity entity) {
        return new AttackDto(
                entity.getAttackIndex() != null ? entity.getAttackIndex() : 0,
                entity.getName(),
                commaStringToList(entity.getPrintedCost()),
                entity.getConvertedEnergyCost(),
                entity.getDamageText(),
                entity.getEffectText(),
                entity.getBaseDamage()
        );
    }

    private WeaknessDto toWeaknessDto(CardWeaknessEntity entity) {
        return new WeaknessDto(
                entity.getEnergyType(),
                entity.getMultiplier() != null ? String.valueOf(entity.getMultiplier()) : null
        );
    }

    private ResistanceDto toResistanceDto(CardResistanceEntity entity) {
        return new ResistanceDto(
                entity.getEnergyType(),
                entity.getValue() != null ? String.valueOf(entity.getValue()) : null
        );
    }

    private String listToCommaString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }

    private List<String> commaStringToList(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeSupertype(String supertype) {
        if (supertype == null) return null;
        return supertype.replace('é', 'e').replace('É', 'E').toUpperCase();
    }

    public List<AbilityDefinition> toAbilityDefinitions(String jsonAbilities) {
        if (jsonAbilities == null || jsonAbilities.isBlank()) return List.of();
        try {
            ObjectMapper mapper = new ObjectMapper();
            AbilityDto[] dtos = mapper.readValue(jsonAbilities, AbilityDto[].class);
            List<AbilityDefinition> result = new ArrayList<>();
            for (AbilityDto dto : dtos) {
                AbilityType type = mapAbilityType(dto.type());
                result.add(new AbilityDefinition(dto.name(), dto.text(), type));
            }
            return result;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private AbilityType mapAbilityType(String apiType) {
        if (apiType == null) return AbilityType.ABILITY;
        if (apiType.contains("Power")) return AbilityType.POKEMON_POWER;
        if (apiType.contains("Body")) return AbilityType.POKEMON_BODY;
        return AbilityType.ABILITY;
    }

    private List<CardAbilityResponse> toCardAbilityResponseList(String jsonAbilities) {
        if (jsonAbilities == null || jsonAbilities.isBlank()) return List.of();
        try {
            ObjectMapper mapper = new ObjectMapper();
            AbilityDto[] dtos = mapper.readValue(jsonAbilities, AbilityDto[].class);
            return Arrays.stream(dtos)
                    .map(dto -> new CardAbilityResponse(
                            dto.name(),
                            dto.text(),
                            dto.type(),
                            abilityRegistry.has(dto.name())))
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String determineStage(List<String> subtypes) {
        if (subtypes == null) return null;
        for (String s : subtypes) {
            String normalized = s.toUpperCase().replace(" ", "_");
            if ("MEGA".equals(normalized)) return "MEGA";
            if ("STAGE_2".equals(normalized)) return "STAGE_2";
            if ("STAGE_1".equals(normalized)) return "STAGE_1";
            if ("BASIC".equals(normalized)) return "BASIC";
        }
        return null;
    }

    private String generateTrainerEffectCode(String name, List<String> rules) {
        if (name == null) return null;
        return switch (name) {
            case "Evosoda" -> "EVOSODA";
            case "Great Ball" -> "GREAT_BALL";
            case "Max Revive" -> "MAX_REVIVE";
            case "Professor's Letter" -> "PROFESSORS_LETTER";
            case "Red Card" -> "RED_CARD";
            case "Roller Skates" -> "COIN_FLIP_DRAW_3";
            case "Super Potion" -> "HEAL_60_DISCARD_1";
            case "Cassius" -> "CASSIUS";
            case "Professor Sycamore" -> "DISCARD_HAND_DRAW_7";
            case "Shauna" -> "SHUFFLE_DRAW_5";
            case "Team Flare Grunt" -> "TEAM_FLARE_GRUNT";
            case "Fairy Garden" -> "FAIRY_GARDEN";
            case "Shadow Circle" -> "SHADOW_CIRCLE";
            case "Hard Charm" -> "ATTACH_TOOL";
            case "Muscle Band" -> "ATTACH_TOOL";
            default -> null;
        };
    }
}
