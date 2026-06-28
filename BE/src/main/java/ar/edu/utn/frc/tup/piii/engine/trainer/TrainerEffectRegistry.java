package ar.edu.utn.frc.tup.piii.engine.trainer;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrainerEffectRegistry {

    private final Map<String, EffectType> effectCodeMapping = new HashMap<>();
    private final Map<EffectType, TrainerEffectResolver> resolvers = new HashMap<>();

    public void registerEffectCode(String effectCode, EffectType effectType) {
        effectCodeMapping.put(effectCode, effectType);
    }

    public void registerResolver(TrainerEffectResolver resolver) {
        resolvers.put(resolver.getType(), resolver);
    }

    public EffectType getEffectType(String effectCode) {
        return effectCodeMapping.get(effectCode);
    }

    public boolean isEffectCodeKnown(String effectCode) {
        return effectCodeMapping.containsKey(effectCode);
    }

    public List<String> getRequiredTargetKeys(EffectType effectType) {
        return switch (effectType) {
            case HEAL, CONDITION_REMOVE -> List.of("targetPokemonInstanceId");
            case SWITCH_POKEMON -> List.of("targetPokemonInstanceId");
            case SEARCH_BASIC_POKEMON, SEARCH_ENERGY, EVOLVE_SEARCH, REVIVE ->
                List.of("targetCardIndex");
            case ATTACH_EXTRA_ENERGY -> List.of("targetPokemonInstanceId", "handIndex");
            case DAMAGE_MODIFY -> List.of("targetPokemonInstanceId");
            case TOOL_ATTACH -> List.of("targetPokemonInstanceId", "handIndex");
            case STADIUM_PLAY -> List.of();
            case DRAW_CARDS, DISCARD_AND_DRAW, SHUFFLE_HAND_INTO_DECK -> List.of();
            case EVOLVE_DIRECT -> List.of("targetPokemonInstanceId");
            case LOOK_TOP_SEARCH -> List.of("targetCardIndex");
            case REVIVE_TO_DECK -> List.of("targetCardIndex");
            case SEARCH_ENERGY_TO_HAND -> List.of("targetCardIndexes");
            case OPPONENT_SHUFFLE_HAND_DRAW, COIN_FLIP_DRAW -> List.of();
            case HEAL_WITH_DISCARD -> List.of("targetPokemonInstanceId");
            case RETURN_POKEMON_TO_DECK -> List.of("targetPokemonInstanceId");
            case DISCARD_OPPONENT_ENERGY -> List.of("targetPokemonInstanceId");
        };
    }

    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        String effectCode = card.getEffectCode();
        if (effectCode == null) return;

        EffectType effectType = effectCodeMapping.get(effectCode);
        if (effectType == null) return;

        TrainerEffectResolver resolver = resolvers.get(effectType);
        if (resolver == null) return;

        resolver.resolve(ctx, player, card, payload);
    }
}
