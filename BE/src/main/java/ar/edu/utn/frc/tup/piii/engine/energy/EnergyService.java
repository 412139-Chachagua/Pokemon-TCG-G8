package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import java.util.*;

public class EnergyService {

    private final EnergyMatchingEngine matchingEngine;
    private final EnergyStrategyRegistry registry;

    public EnergyService(EnergyMatchingEngine matchingEngine, EnergyStrategyRegistry registry) {
        this.matchingEngine = matchingEngine;
        this.registry = registry;
    }

    public List<EnergySource> buildPool(PokemonInPlay pokemon, CardLookupPort cardLookup) {
        return buildPool(pokemon, cardLookup, null);
    }

    public List<EnergySource> buildPool(PokemonInPlay pokemon, CardLookupPort cardLookup, List<UUID> filterIds) {
        if (pokemon.getAttachedEnergies() == null) return List.of();
        List<EnergySource> pool = new ArrayList<>();
        Set<UUID> filterSet = filterIds != null && !filterIds.isEmpty() ? new HashSet<>(filterIds) : null;
        for (CardInstance ci : pokemon.getAttachedEnergies()) {
            if (filterSet != null && !filterSet.contains(ci.getInstanceId())) continue;
            CardDefinition def = cardLookup.getCardById(ci.getCardDefinitionId());
            if (!(def instanceof EnergyCardDefinition energyDef)) continue;
            EnergyResolutionStrategy strategy = registry.getStrategy(energyDef);
            pool.add(strategy.resolve(cardLookup, ci, energyDef));
        }
        return pool;
    }

    public boolean checkAttackRequirements(PokemonInPlay attacker, CardLookupPort cardLookup, int attackIndex) {
        CardDefinition def = cardLookup.getCardById(attacker.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) return false;
        if (pkmDef.getAttacks() == null || attackIndex < 0 || attackIndex >= pkmDef.getAttacks().size())
            return false;
        List<EnergyType> cost = pkmDef.getAttacks().get(attackIndex).getCost();
        if (cost == null || cost.isEmpty()) return true;
        List<EnergySource> pool = buildPool(attacker, cardLookup);
        return matchingEngine.selectPayment(pool, cost).canPay();
    }

    public int calculateDamageBonus(PokemonInPlay attacker, PokemonInPlay defender,
                                    CardLookupPort cardLookup, int baseDamage) {
        if (attacker.getAttachedEnergies() == null) return 0;
        int total = 0;
        for (CardInstance ci : attacker.getAttachedEnergies()) {
            CardDefinition def = cardLookup.getCardById(ci.getCardDefinitionId());
            if (!(def instanceof EnergyCardDefinition energyDef)) continue;
            EnergyResolutionStrategy strategy = registry.getStrategy(energyDef);
            List<DamageModifier> mods = strategy.getDamageModifiers(ci, attacker, cardLookup);
            for (DamageModifier mod : mods) {
                total = mod.applyTo(total, baseDamage, attacker, defender, cardLookup);
            }
        }
        return total;
    }

    public EnergyPaymentResult validateAndPayRetreat(PokemonInPlay active, List<UUID> clientProvidedIds,
                                                      CardLookupPort cardLookup) {
        if (clientProvidedIds == null) clientProvidedIds = List.of();

        Set<UUID> validIds = new HashSet<>();
        if (active.getAttachedEnergies() != null) {
            for (CardInstance ci : active.getAttachedEnergies()) {
                validIds.add(ci.getInstanceId());
            }
        }
        for (UUID id : clientProvidedIds) {
            if (!validIds.contains(id)) {
                return new EnergyPaymentResult(false, List.of(), "Invalid energy instance ID: " + id);
            }
        }

        if (clientProvidedIds.size() != new HashSet<>(clientProvidedIds).size()) {
            return new EnergyPaymentResult(false, List.of(), "Duplicate energy instance IDs");
        }

        List<EnergySource> pool = buildPool(active, cardLookup, clientProvidedIds);

        CardDefinition def = cardLookup.getCardById(active.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) {
            return new EnergyPaymentResult(false, List.of(), "Not a Pokemon");
        }
        List<EnergyType> retreatCost = pkmDef.getRetreatCost();
        if (retreatCost == null || retreatCost.isEmpty()) {
            return new EnergyPaymentResult(true, List.of(), null);
        }

        return matchingEngine.selectPayment(pool, retreatCost);
    }

    public void attachFromHand(CardInstance card, PokemonInPlay target,
                                PlayerState player, EngineContext ctx) {
        target.getAttachedEnergies().add(card);
        player.getHand().remove(card);
        triggerOnAttach(card, target, ctx,
            new AttachmentContext(AttachmentOrigin.FROM_HAND, player.getPlayerId(),
                ctx.getState().getTurnNumber()));
    }

    public void attachFromDeck(CardInstance card, PokemonInPlay target,
                               PlayerState player, EngineContext ctx) {
        target.getAttachedEnergies().add(card);
        player.getDeck().remove(card);
        ctx.getRandomizer().shuffle(player.getDeck());
        triggerOnAttach(card, target, ctx,
            new AttachmentContext(AttachmentOrigin.FROM_DECK, player.getPlayerId(),
                ctx.getState().getTurnNumber()));
    }

    public void detachEnergies(PokemonInPlay pokemon, PlayerState owner,
                                List<CardInstance> energies, EngineContext ctx) {
        if (energies == null || energies.isEmpty()) return;
        for (CardInstance e : energies) {
            triggerOnDetach(e, pokemon, ctx);
        }
        pokemon.getAttachedEnergies().removeAll(energies);
        owner.pushManyToDiscard(energies);
    }

    public void detachAllEnergies(PokemonInPlay pokemon, PlayerState owner, EngineContext ctx) {
        if (pokemon.getAttachedEnergies() == null || pokemon.getAttachedEnergies().isEmpty()) return;
        detachEnergies(pokemon, owner, new ArrayList<>(pokemon.getAttachedEnergies()), ctx);
    }

    public void transferEnergy(CardInstance energyCard, PokemonInPlay source,
                                PokemonInPlay target, PlayerState player, EngineContext ctx) {
        triggerOnDetach(energyCard, source, ctx);
        source.getAttachedEnergies().remove(energyCard);
        target.getAttachedEnergies().add(energyCard);
        triggerOnAttach(energyCard, target, ctx,
            new AttachmentContext(AttachmentOrigin.VIA_ABILITY, player.getPlayerId(),
                ctx.getState().getTurnNumber()));
    }

    private void triggerOnAttach(CardInstance card, PokemonInPlay target,
                                 EngineContext ctx, AttachmentContext attachCtx) {
        CardDefinition def = ctx.getCardLookup().getCardById(card.getCardDefinitionId());
        if (!(def instanceof EnergyCardDefinition energyDef)) return;
        EnergyResolutionStrategy strategy = registry.getStrategy(energyDef);
        strategy.onAttach(card, target, ctx, attachCtx);
    }

    private void triggerOnDetach(CardInstance card, PokemonInPlay source, EngineContext ctx) {
        CardDefinition def = ctx.getCardLookup().getCardById(card.getCardDefinitionId());
        if (!(def instanceof EnergyCardDefinition energyDef)) return;
        EnergyResolutionStrategy strategy = registry.getStrategy(energyDef);
        strategy.onDetach(card, source, ctx);
    }
}
