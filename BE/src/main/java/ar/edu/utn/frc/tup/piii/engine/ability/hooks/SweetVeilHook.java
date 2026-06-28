package ar.edu.utn.frc.tup.piii.engine.ability.hooks;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;

import java.util.ArrayList;
import java.util.List;

public class SweetVeilHook {

    public static boolean isImmune(PokemonInPlay target, PlayerState owner, CardLookupPort cardLookup) {
        if (target == null || owner == null || cardLookup == null) return false;

        if (!hasFairyEnergy(target, cardLookup)) return false;
        if (!ownerHasSweetVeil(owner, cardLookup)) return false;

        return true;
    }

    public static void syncImmunity(PlayerState player, CardLookupPort cardLookup) {
        if (player == null || cardLookup == null) return;
        if (!ownerHasSweetVeil(player, cardLookup)) return;

        for (PokemonInPlay pkm : getAllPlayerPokemon(player)) {
            if (hasFairyEnergy(pkm, cardLookup)
                    && pkm.getSpecialConditions() != null
                    && !pkm.getSpecialConditions().isEmpty()) {
                pkm.getSpecialConditions().clear();
            }
        }
    }

    public static boolean hasFairyEnergy(PokemonInPlay target, CardLookupPort cardLookup) {
        if (target.getAttachedEnergies() == null) return false;

        return target.getAttachedEnergies().stream().anyMatch(ci -> {
            CardDefinition def = cardLookup.getCardById(ci.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition energyDef) {
                return energyDef.getProvides() != null
                        && energyDef.getProvides().contains(EnergyType.FAIRY);
            }
            return false;
        });
    }

    public static boolean ownerHasSweetVeil(PlayerState owner, CardLookupPort cardLookup) {
        for (PokemonInPlay pkm : getAllPlayerPokemon(owner)) {
            CardDefinition def = cardLookup.getCardById(pkm.getCardDefinitionId());
            if (def instanceof PokemonCardDefinition pkmDef && pkmDef.getAbilities() != null) {
                boolean hasSweetVeil = pkmDef.getAbilities().stream()
                        .anyMatch(a -> "Sweet Veil".equals(a.getName()));
                if (hasSweetVeil) return true;
            }
        }
        return false;
    }

    private static List<PokemonInPlay> getAllPlayerPokemon(PlayerState player) {
        List<PokemonInPlay> all = new ArrayList<>();
        if (player.getActivePokemon() != null) all.add(player.getActivePokemon());
        all.addAll(player.getBench());
        return all;
    }
}
