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

public class SpikyShieldHook {

    private static final int DAMAGE_COUNTERS = 3;

    public static void afterDamageTaken(PokemonInPlay defender, PokemonInPlay attacker, EngineContext ctx) {
        if (defender == null || attacker == null || ctx == null) return;

        CardLookupPort cardLookup = ctx.getCardLookup();
        CardDefinition def = cardLookup.getCardById(defender.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) return;
        if (pkmDef.getAbilities() == null) return;

        boolean hasSpikyShield = pkmDef.getAbilities().stream()
                .anyMatch(a -> "Spiky Shield".equals(a.getName()));
        if (!hasSpikyShield) return;

        attacker.setDamageCounters(attacker.getDamageCounters() + DAMAGE_COUNTERS);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("defenderInstanceId", defender.getInstanceId().toString());
        eventPayload.put("attackerInstanceId", attacker.getInstanceId().toString());
        eventPayload.put("damageCounters", DAMAGE_COUNTERS);

        ctx.addEvent(new GameEvent(
                GameEventType.DAMAGE_APPLIED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Spiky Shield: placed " + DAMAGE_COUNTERS + " damage counters on attacker.",
                eventPayload
        ));
    }
}
