package ar.edu.utn.frc.tup.piii.engine.trainer;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.util.Map;

public interface TrainerEffectResolver {
    void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload);
    EffectType getType();
}
