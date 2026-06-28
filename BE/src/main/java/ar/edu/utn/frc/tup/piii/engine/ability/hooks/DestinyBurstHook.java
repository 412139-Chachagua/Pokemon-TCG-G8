package ar.edu.utn.frc.tup.piii.engine.ability.hooks;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DestinyBurstHook {

    private static final int DAMAGE_COUNTERS = 5;

    public static void onKnockout(PokemonInPlay knockedOut, PokemonInPlay attacker, EngineContext ctx) {
        if (knockedOut == null || attacker == null || ctx == null) return;

        CardLookupPort cardLookup = ctx.getCardLookup();
        CardDefinition def = cardLookup.getCardById(knockedOut.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) return;
        if (pkmDef.getAbilities() == null) return;

        boolean hasDestinyBurst = pkmDef.getAbilities().stream()
                .anyMatch(a -> "Destiny Burst".equals(a.getName()));
        if (!hasDestinyBurst) return;

        boolean heads = ctx.getRandomizer().nextInt(2) == 0;
        if (!heads) return;

        attacker.setDamageCounters(attacker.getDamageCounters() + DAMAGE_COUNTERS);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("knockedOutInstanceId", knockedOut.getInstanceId().toString());
        eventPayload.put("attackerInstanceId", attacker.getInstanceId().toString());
        eventPayload.put("damageCounters", DAMAGE_COUNTERS);
        eventPayload.put("coinResult", "heads");

        ctx.addEvent(new GameEvent(
                GameEventType.DAMAGE_APPLIED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Destiny Burst: placed " + DAMAGE_COUNTERS + " damage counters on attacker.",
                eventPayload
        ));
    }
}
