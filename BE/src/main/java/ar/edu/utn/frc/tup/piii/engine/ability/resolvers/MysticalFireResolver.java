package ar.edu.utn.frc.tup.piii.engine.ability.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityResolver;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MysticalFireResolver implements AbilityResolver {

    private static final int TARGET_HAND_SIZE = 6;

    @Override
    public void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon,
                        AbilityDefinition ability, Map<String, Object> payload) {
        int cardsToDraw = TARGET_HAND_SIZE - player.getHand().size();
        if (cardsToDraw <= 0) return;

        int actualDraw = Math.min(cardsToDraw, player.getDeck().size());
        for (int i = 0; i < actualDraw; i++) {
            player.getHand().add(player.getDeck().remove(0));
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", pokemon.getInstanceId().toString());
        eventPayload.put("count", actualDraw);
        eventPayload.put("playerId", player.getPlayerId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.CARDS_DRAWN.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Mystical Fire: drew " + actualDraw + " cards.",
                eventPayload
        ));
    }
}
