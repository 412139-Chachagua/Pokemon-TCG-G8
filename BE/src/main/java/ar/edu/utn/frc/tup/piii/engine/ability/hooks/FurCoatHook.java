package ar.edu.utn.frc.tup.piii.engine.ability.hooks;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;

public class FurCoatHook {

    public static int reduceDamage(int damage, PokemonInPlay defender, CardLookupPort cardLookup) {
        if (defender == null || cardLookup == null) return damage;

        CardDefinition def = cardLookup.getCardById(defender.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) return damage;
        if (pkmDef.getAbilities() == null) return damage;

        boolean hasFurCoat = pkmDef.getAbilities().stream()
                .anyMatch(a -> "Fur Coat".equals(a.getName()));
        if (!hasFurCoat) return damage;

        return Math.max(damage - 20, 0);
    }
}
