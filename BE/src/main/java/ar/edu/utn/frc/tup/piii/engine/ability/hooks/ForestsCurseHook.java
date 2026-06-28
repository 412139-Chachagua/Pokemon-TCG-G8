package ar.edu.utn.frc.tup.piii.engine.ability.hooks;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;

public class ForestsCurseHook {

    public static boolean isItemBlocked(PlayerState playerWhoseItemIsBlocked, GameState state, CardLookupPort cardLookup) {
        if (playerWhoseItemIsBlocked == null || state == null || cardLookup == null) return false;

        PlayerState opponent = null;
        for (PlayerState p : state.getPlayers()) {
            if (!p.getPlayerId().equals(playerWhoseItemIsBlocked.getPlayerId())) {
                opponent = p;
                break;
            }
        }
        if (opponent == null) return false;

        PokemonInPlay opponentActive = opponent.getActivePokemon();
        if (opponentActive == null) return false;

        CardDefinition def = cardLookup.getCardById(opponentActive.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) return false;
        if (pkmDef.getAbilities() == null) return false;

        return pkmDef.getAbilities().stream()
                .anyMatch(a -> "Forest's Curse".equals(a.getName()));
    }
}
