package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.DestinyBurstHook;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.engine.victory.VictoryConditionChecker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KnockoutCheckStep extends AbstractAttackStep {

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        GameState state = ctx.getState();
        var opponent = ctx.getOpponent(attackCtx.getDefender().getOwnerPlayerId());
        if (opponent == null) {
            return proceed(ctx, attackCtx);
        }

        // 1. Check defender (main attack target)
        PokemonInPlay defender = attackCtx.getDefender();
        List<PokemonInPlay> allKOd = new ArrayList<>();
        if (isKOd(defender, ctx)) {
            allKOd.add(defender);
        }

        // 2. Check bench Pokémon of the defender's player (for bench damage effects)
        var player = ctx.getPlayer(attackCtx.getDefender().getOwnerPlayerId());
        checkBenchKOs(ctx, attackCtx, player, allKOd);

        // 3. Check bench Pokémon of the attacker's player (for self-bench damage effects)
        var attackerPlayer = ctx.getPlayer(attackCtx.getAttacker().getOwnerPlayerId());
        if (!attackerPlayer.getPlayerId().equals(player.getPlayerId())) {
            checkBenchKOs(ctx, attackCtx, attackerPlayer, allKOd);
        }

        if (allKOd.isEmpty()) {
            return proceed(ctx, attackCtx);
        }

        attackCtx.setKnockoutOccurred(true);
        state.setPendingPrizeOwnerPlayerId(null);
        state.setPendingPrizeCount(0);

        // Process each KO independently:
        // - The player who OWNS the KOd Pokémon handles discard
        // - The OPPONENT of that owner receives the Prize card
        for (PokemonInPlay pokemon : allKOd) {
            var owner = ctx.getPlayer(pokemon.getOwnerPlayerId());
            var prizeRecipient = ctx.getOpponent(pokemon.getOwnerPlayerId());
            processSingleKO(ctx, attackCtx, pokemon, owner, prizeRecipient);
        }

        return proceed(ctx, attackCtx);
    }

    private void checkBenchKOs(EngineContext ctx, AttackContext attackCtx,
                                PlayerState player, List<PokemonInPlay> allKOd) {
        if (player.getBench() == null) return;
        for (PokemonInPlay benched : new ArrayList<>(player.getBench())) {
            if (isKOd(benched, ctx)) {
                allKOd.add(benched);
            }
        }
    }

    private boolean isKOd(PokemonInPlay pokemon, EngineContext ctx) {
        CardDefinition def = ctx.getCardLookup().getCardById(pokemon.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pDef)) return false;
        return pokemon.getDamageCounters() * 10 >= pDef.getHp();
    }

    private int prizeValue(PokemonInPlay pokemon, EngineContext ctx) {
        CardDefinition def = ctx.getCardLookup().getCardById(pokemon.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pDef)) return 1;
        return pDef.isEx() ? 2 : 1;
    }

    private void processSingleKO(EngineContext ctx, AttackContext attackCtx,
                                  PokemonInPlay pokemon, PlayerState player, PlayerState opponent) {
        GameState state = ctx.getState();

        ctx.getEnergyService().detachAllEnergies(pokemon, player, ctx);

        if (pokemon.getToolCardInstanceId() != null) {
            CardInstance tool = pokemon.getAttachedTool();
            if (tool != null) {
                player.pushToDiscard(new CardInstance(tool.getInstanceId(), tool.getCardDefinitionId()));
            }
            pokemon.setToolCardInstanceId(null);
            pokemon.setAttachedTool(null);
        }

        player.pushToDiscard(new CardInstance(pokemon.getInstanceId(), pokemon.getCardDefinitionId()));

        // Prize goes to the opponent of whoever owned the KOd Pokémon
        int value = prizeValue(pokemon, ctx);
        if (state.getPendingPrizeOwnerPlayerId() == null) {
            state.setPendingPrizeOwnerPlayerId(opponent.getPlayerId());
            state.setPendingPrizeCount(value);
        } else if (state.getPendingPrizeOwnerPlayerId().equals(opponent.getPlayerId())) {
            state.setPendingPrizeCount(state.getPendingPrizeCount() + value);
        }

        boolean isActive = player.getActivePokemon() != null
                && player.getActivePokemon().getInstanceId().equals(pokemon.getInstanceId());
        if (isActive) {
            player.setActivePokemon(null);
            if (player.getBench() != null && !player.getBench().isEmpty()) {
                state.setPendingKOReplacement(true);
                state.setKnockedOutPlayerId(player.getPlayerId());
                List<String> candidates = player.getBench().stream()
                        .map(p -> p.getInstanceId().toString())
                        .toList();
                ctx.addEvent(new GameEvent(
                        GameEventType.KO_REPLACEMENT_REQUIRED.name(), state.getMatchId(),
                        state.getTurnNumber(), Instant.now(),
                        "KO replacement required.",
                        Map.of("knockedOutPlayerId", player.getPlayerId().toString(),
                                "candidates", candidates)
                ));
            } else {
                VictoryConditionChecker.VictoryCheckResult vr =
                        VictoryConditionChecker.check(state, opponent.getPlayerId());
                if (vr.finished()) {
                    if (vr.winnerPlayerId() != null) {
                        state.setWinnerPlayerId(vr.winnerPlayerId());
                        state.setFinishReason(vr.reason());
                        state.setStatus(MatchStatus.FINISHED);
                    } else if (vr.suddenDeath()) {
                        state.setSuddenDeath(true);
                        state.setStatus(MatchStatus.FINISHED);
                        state.setFinishReason(FinishReason.SUDDEN_DEATH);
                    }
                }
            }
        } else if (player.getBench() != null) {
            player.getBench().removeIf(p -> p.getInstanceId().equals(pokemon.getInstanceId()));
        }

        Map<String, Object> koPayload = new HashMap<>();
        koPayload.put("knockedOutPokemonInstanceId", pokemon.getInstanceId().toString());
        koPayload.put("ownerPlayerId", player.getPlayerId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.KNOCKOUT_OCCURRED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Knockout occurred.",
                koPayload
        ));

        DestinyBurstHook.onKnockout(pokemon, attackCtx.getAttacker(), ctx);
    }
}
