package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.util.UUID;

public class HandlerHelper {
    public static PokemonInPlay findPokemon(PlayerState player, UUID instanceId) {
        PokemonInPlay active = player.getActivePokemon();
        if (active != null && active.getInstanceId().equals(instanceId)) return active;
        for (PokemonInPlay pkm : player.getBench()) {
            if (pkm.getInstanceId().equals(instanceId)) return pkm;
        }
        return null;
    }
}
