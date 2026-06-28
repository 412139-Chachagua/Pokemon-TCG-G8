package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class RainbowEnergyStrategy implements EnergyResolutionStrategy {

    private static final Set<EnergyType> ALL_TYPES = EnumSet.allOf(EnergyType.class);

    @Override
    public EnergyStrategyKey getKey() {
        return EnergyStrategyKey.RAINBOW;
    }

    @Override
    public EnergySource resolve(CardLookupPort cardLookup, CardInstance card, EnergyCardDefinition def) {
        return new EnergySource(
            card.getInstanceId(),
            card.getCardDefinitionId(),
            1,
            ALL_TYPES,
            MatchBehavior.FLEXIBLE
        );
    }

    @Override
    public void onAttach(CardInstance card, PokemonInPlay target, EngineContext ctx, AttachmentContext attachCtx) {
        if (attachCtx.origin() == AttachmentOrigin.FROM_HAND) {
            target.setDamageCounters(target.getDamageCounters() + 1);
            ctx.addEvent(new GameEvent(
                GameEventType.DAMAGE_APPLIED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Rainbow Energy: 1 damage counter placed",
                Map.of(
                    "pokemonInstanceId", target.getInstanceId().toString(),
                    "damageCountersAdded", 1,
                    "reason", "RAINBOW_ENERGY_ATTACH"
                )
            ));
        }
    }
}
