package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchDeckEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(SearchDeckEffect.class);
    private final String searchType;
    private final String energyType;
    private final int count;
    private final String pokemonType;

    public SearchDeckEffect(String searchType, int count) {
        this(searchType, null, count);
    }

    public SearchDeckEffect(String searchType, String energyType, int count) {
        this(searchType, energyType, count, null);
    }

    public SearchDeckEffect(String searchType, String energyType, int count, String pokemonType) {
        this.searchType = searchType != null ? searchType : "ANY";
        this.energyType = energyType;
        this.count = count;
        this.pokemonType = pokemonType;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState player = ctx.getPlayer(attacker.getOwnerPlayerId());
        if (player == null || player.getDeck() == null || player.getDeck().isEmpty()) return;

        List<CardInstance> matched = new ArrayList<>();
        for (CardInstance ci : player.getDeck()) {
            if (matched.size() >= count) break;
            CardDefinition def = ctx.getCardLookup().getCardById(ci.getCardDefinitionId());
            if (def == null) continue;
            if (matchesCriteria(def)) {
                matched.add(ci);
            }
        }

        if (matched.isEmpty()) return;

        for (CardInstance ci : matched) {
            player.getDeck().remove(ci);
            player.getHand().add(ci);
        }

        ctx.getRandomizer().shuffle(player.getDeck());

        log.warn("[searchDeck] Searched {} cards of type {} and added {} to hand", count, searchType, matched.size());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_SEARCHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Searched deck for " + searchType + " cards.",
                Map.of(
                        "playerId", player.getPlayerId().toString(),
                        "searchType", searchType,
                        "foundCount", matched.size()
                )
        ));
    }

    private boolean matchesCriteria(CardDefinition def) {
        return switch (searchType) {
            case "ANY" -> true;
            case "ENERGY" -> def instanceof EnergyCardDefinition ecd && (energyType == null || matchesEnergyType(ecd));
            case "SUPPORTER" -> "TRAINER".equalsIgnoreCase(def.getSupertype())
                    && def.getSubtypes() != null && def.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("SUPPORTER"));
            case "BASIC_POKEMON" -> "POKEMON".equalsIgnoreCase(def.getSupertype())
                    && def.getSubtypes() != null && def.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("BASIC"));
            case "POKEMON" -> {
                if (!(def instanceof PokemonCardDefinition pDef)) yield false;
                if (pokemonType == null) yield true;
                if (pDef.getTypes() == null) yield false;
                try {
                    EnergyType et = EnergyType.valueOf(pokemonType);
                    yield pDef.getTypes().contains(et);
                } catch (IllegalArgumentException e) {
                    yield false;
                }
            }
            default -> false;
        };
    }

    private boolean matchesEnergyType(EnergyCardDefinition ecd) {
        if (ecd.getEnergyCardType() == null) return false;
        try {
            EnergyType et = EnergyType.valueOf(energyType);
            return ecd.getProvides() != null && ecd.getProvides().contains(et);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
