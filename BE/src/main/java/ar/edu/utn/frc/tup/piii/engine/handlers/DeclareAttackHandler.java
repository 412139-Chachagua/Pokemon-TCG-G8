package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackChainBuilder;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.ConditionCheckStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.ConfusionCheckStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.DamageStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.EnergyCheckStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.KnockoutCheckStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.ModifierStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.PostDamageEffectStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.PreDamageStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.PrerequisiteStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.TargetSelectionStep;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.engine.victory.VictoryConditionChecker;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeclareAttackHandler implements GameHandler {

    private static final Logger log = LoggerFactory.getLogger(DeclareAttackHandler.class);
    private final TurnManager turnManager;
    private final AttackStep attackChain;

    public DeclareAttackHandler(TurnManager turnManager) {
        this.turnManager = turnManager;
        this.attackChain = AbstractAttackStep.buildChain(
                new ConditionCheckStep(),
                new EnergyCheckStep(),
                new ConfusionCheckStep(),
                new PreDamageStep(),
                new TargetSelectionStep(),
                new PrerequisiteStep(),
                new ModifierStep(),
                new DamageStep(),
                new PostDamageEffectStep(),
                new KnockoutCheckStep()
        );
    }

    @SuppressWarnings("unchecked")
    public void handle(EngineContext ctx, GameAction action) {
        var state = ctx.getState();
        var player = ctx.getPlayer(action.getPlayerId());
        var opponent = ctx.getOpponent(action.getPlayerId());

        log.warn("[attack] handle() called: playerId={}, payloadKeys={}",
                action.getPlayerId(), action.getPayload() != null ? action.getPayload().keySet() : "null");

        if (state.getTurnFlags().hasAttacked()) {
            log.warn("[attack] REJECTED: hasAttacked is already true for this turn");
            return;
        }

        Integer attackIndex = action.getPayloadInt("attackIndex");
        if (attackIndex == null) {
            log.warn("[attack] REJECTED: attackIndex is null");
            return;
        }
        String targetIdStr = action.getPayloadString("targetPokemonInstanceId");
        if (targetIdStr == null) {
            log.warn("[attack] REJECTED: targetPokemonInstanceId is null");
            return;
        }
        UUID targetPokemonInstanceId = UUID.fromString(targetIdStr);
        PokemonInPlay attacker = player.getActivePokemon();
        PokemonInPlay defender = HandlerHelper.findPokemon(opponent, targetPokemonInstanceId);

        if (attacker == null) {
            log.warn("[attack] REJECTED: attacker is null (active Pokemon not found for player {})", action.getPlayerId());
            return;
        }
        if (defender == null) {
            log.warn("[attack] REJECTED: defender is null (target {} not found in opponent board)", targetPokemonInstanceId);
            return;
        }

        log.warn("[attack] DECLARE_ATTACK: attacker={}, attackIndex={}, target={}",
                attacker.getCardDefinitionId(), attackIndex, targetPokemonInstanceId);
        // Log the attack definition's effects if available
        CardDefinition cardDefForLog = ctx.getCardLookup().getCardById(attacker.getCardDefinitionId());
        if (cardDefForLog instanceof PokemonCardDefinition pDefLog
                && pDefLog.getAttacks() != null && attackIndex >= 0 && attackIndex < pDefLog.getAttacks().size()) {
            var atkDefLog = pDefLog.getAttacks().get(attackIndex);
            log.warn("[attack] Attack name='{}', damage='{}', effects={}",
                    atkDefLog.getName(), atkDefLog.getDamage(),
                    atkDefLog.getEffects() != null ? atkDefLog.getEffects().size() : 0);
            if (atkDefLog.getEffects() != null) {
                for (var ae : atkDefLog.getEffects()) {
                    log.warn("[attack]   effect type={}, params={}", ae.getType(), ae.getParams());
                }
            }
        }

        List<UUID> discardEnergyIds = null;
        if (action.getPayload().containsKey("energyCardInstanceIdsToDiscard")) {
            List<String> rawIds = (List<String>) action.getPayload().get("energyCardInstanceIdsToDiscard");
            discardEnergyIds = rawIds.stream().map(UUID::fromString).toList();
        }

        List<Map<String, Object>> benchTargets = null;
        if (action.getPayload().containsKey("benchTargets")) {
            benchTargets = (List<Map<String, Object>>) action.getPayload().get("benchTargets");
        }

        List<UUID> moveEnergyIds = null;
        if (action.getPayload().containsKey("energyCardInstanceIdsToMove")) {
            List<String> rawIds = (List<String>) action.getPayload().get("energyCardInstanceIdsToMove");
            moveEnergyIds = rawIds.stream().map(UUID::fromString).toList();
        }

        CardDefinition attackerCardDef = ctx.getCardLookup().getCardById(attacker.getCardDefinitionId());
        int attackBaseDamage = 0;
        int damageMultiplierValue = 1;
        if (attackerCardDef instanceof PokemonCardDefinition pDef
                && pDef.getAttacks() != null && attackIndex >= 0 && attackIndex < pDef.getAttacks().size()) {
            String damageStr = pDef.getAttacks().get(attackIndex).getDamage();
            var parsed = DamageCalculator.parseDamage(damageStr);
            if (parsed.isMultiplier()) {
                String lowerText = pDef.getAttacks().get(attackIndex).getText() != null
                        ? pDef.getAttacks().get(attackIndex).getText().toLowerCase() : "";
                // "times the number of your Benched Pokémon" → count attacker's bench
                if (lowerText.contains("benched") && !lowerText.contains("opponent")) {
                    damageMultiplierValue = player.getBench() != null ? player.getBench().size() : 0;
                    if (damageMultiplierValue < 1) damageMultiplierValue = 1;
                }
                // "times the amount of Energy attached to this Pokémon" (Balloon Barrage)
                if (lowerText.contains("energy attached to this") || lowerText.contains("energy attached to this pok")) {
                    damageMultiplierValue = attacker.getAttachedEnergies() != null ? attacker.getAttachedEnergies().size() : 0;
                    if (damageMultiplierValue < 1) damageMultiplierValue = 1;
                }
                // "Flip a coin for each Fighting Energy attached" (Rock Black) — flip per energy
                for (String type : new String[]{"fighting", "fire", "water", "lightning", "grass", "psychic", "darkness", "metal", "fairy"}) {
                    String pattern = "for each " + type + " energy";
                    if (lowerText.contains(pattern)) {
                        int totalCoins = countEnergyOfType(attacker, type.toUpperCase(), ctx);
                        int heads = 0;
                        for (int i = 0; i < totalCoins; i++) {
                            boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                            if (isHeads) heads++;
                            ctx.addEvent(new GameEvent(
                                    GameEventType.COIN_FLIP_RESULT.name(),
                                    state.getMatchId(),
                                    state.getTurnNumber(),
                                    Instant.now(),
                                    isHeads ? "Cara" : "Cruz",
                                    Map.of("result", isHeads ? "HEADS" : "TAILS", "source", "multi_coin_flip", "flipIndex", i, "totalFlips", totalCoins)
                            ));
                        }
                        damageMultiplierValue = heads;
                        break;
                    }
                }
                // "Flip a coin for each damage counter on this Pokémon" (Seething Anger) — flip per counter
                if (lowerText.contains("damage counter on this") && lowerText.contains("flip a coin")) {
                    int totalCoins = attacker.getDamageCounters();
                    int heads = 0;
                    for (int i = 0; i < totalCoins; i++) {
                        boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                        if (isHeads) heads++;
                        ctx.addEvent(new GameEvent(
                                GameEventType.COIN_FLIP_RESULT.name(),
                                state.getMatchId(),
                                state.getTurnNumber(),
                                Instant.now(),
                                isHeads ? "Cara" : "Cruz",
                                Map.of("result", isHeads ? "HEADS" : "TAILS", "source", "multi_coin_flip", "flipIndex", i, "totalFlips", totalCoins)
                        ));
                    }
                    damageMultiplierValue = heads;
                }
                // Continuous Tumble style: "Flip a coin until you get tails. This attack does N damage times the number of heads."
                // REMOVED: Standard "Flip X coins" multiplier is now handled by PreDamageStep via the effect pipeline.
                if (lowerText.contains("flip a coin until") && lowerText.contains("tails")) {
                    int heads = 0;
                    while (true) {
                        boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                        ctx.addEvent(new GameEvent(
                                GameEventType.COIN_FLIP_RESULT.name(),
                                state.getMatchId(),
                                state.getTurnNumber(),
                                Instant.now(),
                                isHeads ? "Cara" : "Cruz",
                                Map.of("result", isHeads ? "HEADS" : "TAILS", "source", "multi_coin_flip", "flipIndex", heads, "totalFlips", -1)
                        ));
                        if (!isHeads) break;
                        heads++;
                    }
                    damageMultiplierValue = heads;
                }
                attackBaseDamage = parsed.baseValue() * damageMultiplierValue;
            } else {
                attackBaseDamage = parsed.baseValue();
            }
        }

        Map<String, Object> damageMods = new HashMap<>();
        int energyBonus = ctx.getEnergyService().calculateDamageBonus(
                attacker, defender, ctx.getCardLookup(), attackBaseDamage);
        if (energyBonus != 0) {
            String key = attacker.getInstanceId().toString();
            damageMods.merge(key, energyBonus,
                (existing, bonus) -> ((Number) existing).intValue() + (int) bonus);
        }

        AttackContext attackCtx = new AttackContext(
                attacker, defender, attackIndex, damageMods, targetPokemonInstanceId
        );
        attackCtx.setBaseDamageOverride(attackBaseDamage);
        attackCtx.setDiscardEnergyInstanceIds(discardEnergyIds);
        attackCtx.setBenchTargets(benchTargets);

        // ── Conditional damage bonus (DamagePlus without coin flip) ──────────
        if (attackerCardDef instanceof PokemonCardDefinition pDef2
                && pDef2.getAttacks() != null && attackIndex >= 0 && attackIndex < pDef2.getAttacks().size()) {
            String atkText = pDef2.getAttacks().get(attackIndex).getText();
            if (atkText != null) {
                String lowerAtk = atkText.toLowerCase();
                int conditionalBonus = computeConditionalDamageBonus(lowerAtk, attacker, defender, player, opponent, ctx, action.getPayload());
                if (conditionalBonus != 0) {
                    String key = attacker.getInstanceId().toString();
                    damageMods.merge(key, conditionalBonus,
                        (existing, bonus) -> ((Number) existing).intValue() + (int) bonus);
                }
                if (lowerAtk.contains("you may do") && lowerAtk.contains("more damage")) {
                    attackCtx.setOptionalBonusApplied(conditionalBonus > 0);
                }
                if (lowerAtk.contains("isn't affected by weakness") || lowerAtk.contains("not affected by weakness")) {
                    attackCtx.setBypassWeakness(true);
                }
                if (lowerAtk.contains("isn't affected by resistance") || lowerAtk.contains("not affected by resistance")) {
                    attackCtx.setBypassResistance(true);
                }
            }
        }
        attackCtx.setMoveEnergyInstanceIds(moveEnergyIds);

        // Magma Mantle style: "You may discard the top card of your deck. If that card is a Fire Energy card, this attack does 50 more damage."
        if (attackerCardDef instanceof PokemonCardDefinition pDef3
                && pDef3.getAttacks() != null && attackIndex >= 0 && attackIndex < pDef3.getAttacks().size()) {
            String atkText = pDef3.getAttacks().get(attackIndex).getText();
            if (atkText != null) {
                String lowerAtk2 = atkText.toLowerCase();
                if (lowerAtk2.contains("you may") && lowerAtk2.contains("discard the top")
                        && lowerAtk2.contains("if that card") && lowerAtk2.contains("energy")
                        && lowerAtk2.contains("more damage")) {
                    boolean useBonus = action.getPayload() == null || !Boolean.FALSE.equals(action.getPayload().get("useOptionalBonus"));
                    if (useBonus && player.getDeck() != null && !player.getDeck().isEmpty()) {
                        CardInstance topCard = player.getDeck().remove(0);
                        CardDefinition cardDef = ctx.getCardLookup().getCardById(topCard.getCardDefinitionId());
                        if (cardDef instanceof EnergyCardDefinition ecd && ecd.getProvides() != null) {
                            for (EnergyType et : ecd.getProvides()) {
                                if (et == EnergyType.FIRE) {
                                    attackCtx.setCoinFlipDamageBonus(50 + attackCtx.getCoinFlipDamageBonus());
                                    log.warn("[attack] Magma Mantle: top card was Fire Energy, +50 damage bonus");
                                    break;
                                }
                            }
                        }
                        player.pushToDiscard(topCard);
                        log.warn("[attack] Magma Mantle: discarded top card {}", topCard.getCardDefinitionId());
                        ctx.addEvent(new GameEvent(
                                GameEventType.ENERGY_DISCARDED.name(),
                                state.getMatchId(),
                                state.getTurnNumber(),
                                java.time.Instant.now(),
                                "Discarded top card of own deck.",
                                java.util.Map.of("count", 1, "target", "self")
                        ));
                    }
                }
            }
        }

        String healTargetStr = action.getPayloadString("healTargetId");
        if (healTargetStr != null) {
            attackCtx.setHealTargetId(UUID.fromString(healTargetStr));
        }

        String restrictedAttackStr = action.getPayloadString("restrictedAttackName");
        if (restrictedAttackStr != null) {
            attackCtx.setRestrictedAttackName(restrictedAttackStr);
        }

        String chosenConditionStr = action.getPayloadString("specialCondition");
        if (chosenConditionStr != null) {
            attackCtx.setChosenCondition(chosenConditionStr.toUpperCase());
        }

        AttackStep.AttackStepResult chainResult = AttackChainBuilder.executeChain(attackChain, ctx, attackCtx);

        if (chainResult == AttackStep.AttackStepResult.STOP_CHAIN) {
            log.warn("[attack] Chain stopped with STOP_CHAIN (reason: {})", attackCtx.getErrorMessage() != null ? attackCtx.getErrorMessage() : "unknown");
            return;
        }
        if (chainResult == AttackStep.AttackStepResult.STOP_CHAIN_END_TURN) {
            log.warn("[attack] Chain stopped with STOP_CHAIN_END_TURN");
        }

        if (attackCtx.isConfusedSelfHit()) {
            handleConfusionSelfHit(ctx, player, attacker, opponent);
            state.getTurnFlags().setHasAttacked(true);
            if (state.isPendingKOReplacement()) {
                turnManager.endTurn(ctx);
                turnManager.startTurn(ctx);
                return;
            }
            turnManager.endTurn(ctx);
            turnManager.startTurn(ctx);
            return;
        }

        if (attackCtx.getDamageCalc() != null) {
            int bonusDmg = attackCtx.getCoinFlipDamageBonus();
            int finalDmg = attackCtx.getDamageCalc().finalDamage();
            int countersAdded = attackCtx.getDamageCalc().damageCountersAdded();
            if (bonusDmg > 0) {
                finalDmg += bonusDmg;
                countersAdded += bonusDmg / 10;
            }
            Map<String, Object> dmgPayload = new HashMap<>();
            dmgPayload.put("attackerPokemonInstanceId", attacker.getInstanceId().toString());
            dmgPayload.put("defenderPokemonInstanceId", targetPokemonInstanceId.toString());
            dmgPayload.put("finalDamage", finalDmg);
            dmgPayload.put("damageCountersAdded", countersAdded);
            dmgPayload.put("weaknessApplied", attackCtx.getDamageCalc().weaknessApplied());
            dmgPayload.put("weaknessMultiplier", attackCtx.getDamageCalc().weaknessMultiplier());
            dmgPayload.put("resistanceApplied", attackCtx.getDamageCalc().resistanceApplied());
            dmgPayload.put("resistanceValue", attackCtx.getDamageCalc().resistanceValue());

            ctx.addEvent(new GameEvent(
                    GameEventType.DAMAGE_APPLIED.name(),
                    state.getMatchId(),
                    state.getTurnNumber(),
                    Instant.now(),
                    "Damage applied.",
                    dmgPayload
            ));
        }

        state.getTurnFlags().setHasAttacked(true);

        String attackName = null;
        if (attackerCardDef instanceof PokemonCardDefinition pDef
                && pDef.getAttacks() != null && attackIndex >= 0 && attackIndex < pDef.getAttacks().size()) {
            attackName = pDef.getAttacks().get(attackIndex).getName();
        }

        ctx.addEvent(new GameEvent(
                GameEventType.ATTACK_DECLARED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                attackName != null ? "Attack: " + attackName : "Attack declared.",
                Map.of("attackIndex", attackIndex, "attackName", attackName != null ? attackName : "")
        ));

        if (state.isPendingKOReplacement()) {
            turnManager.endTurn(ctx);
            turnManager.startTurn(ctx);
            return;
        }

        if (state.getStatus() == MatchStatus.FINISHED) return;

        turnManager.endTurn(ctx);
        turnManager.startTurn(ctx);
    }

    private void handleConfusionSelfHit(EngineContext ctx,
                                         ar.edu.utn.frc.tup.piii.engine.model.PlayerState player,
                                         PokemonInPlay attacker,
                                         ar.edu.utn.frc.tup.piii.engine.model.PlayerState opponent) {
        var state = ctx.getState();

        ctx.addEvent(new GameEvent(
                GameEventType.CONFUSION_SELF_HIT.name(),
                state.getMatchId(), state.getTurnNumber(), Instant.now(),
                "Confused Pokemon hit itself.",
                Map.of("attackerPokemonInstanceId", attacker.getInstanceId().toString(),
                        "selfDamageCounters", 3)
        ));
        ctx.addEvent(new GameEvent(
                GameEventType.DAMAGE_APPLIED.name(),
                state.getMatchId(), state.getTurnNumber(), Instant.now(),
                "Self-inflicted damage from confusion.",
                Map.of("attackerPokemonInstanceId", attacker.getInstanceId().toString(),
                        "damageCountersAdded", 3)
        ));

        attacker.setDamageCounters(attacker.getDamageCounters() + 3);

        if (isPokemonKOd(attacker, ctx)) {
            handleKODuringConfusion(ctx, player, attacker, opponent);
        }
    }

    private boolean isPokemonKOd(PokemonInPlay pokemon, EngineContext ctx) {
        CardDefinition def = ctx.getCardLookup().getCardById(pokemon.getCardDefinitionId());
        int hp = (def instanceof PokemonCardDefinition p) ? p.getHp() : 0;
        return pokemon.getDamageCounters() * 10 >= hp;
    }

    private void handleKODuringConfusion(EngineContext ctx,
                                          ar.edu.utn.frc.tup.piii.engine.model.PlayerState player,
                                          PokemonInPlay attacker,
                                          ar.edu.utn.frc.tup.piii.engine.model.PlayerState opponent) {
        var state = ctx.getState();

        if (attacker.getAttachedEnergies() != null) {
            for (var e : attacker.getAttachedEnergies()) {
                ctx.addEvent(new GameEvent(
                        GameEventType.ENERGY_DISCARDED.name(),
                        state.getMatchId(),
                        state.getTurnNumber(),
                        Instant.now(),
                        "Energy discarded during confusion KO.",
                        Map.of(
                                "pokemonInstanceId", attacker.getInstanceId().toString(),
                                "energyInstanceId", e.getInstanceId().toString(),
                                "reason", "CONFUSION_KO"
                        )
                ));
            }
            ctx.getEnergyService().detachAllEnergies(attacker, player, ctx);
        }

        if (attacker.getToolCardInstanceId() != null) {
            var tool = attacker.getAttachedTool();
            if (tool != null) {
                player.pushToDiscard(tool);
            }
            attacker.setToolCardInstanceId(null);
            attacker.setAttachedTool(null);
        }

        player.pushToDiscard(
                new ar.edu.utn.frc.tup.piii.engine.model.CardInstance(attacker.getInstanceId(), attacker.getCardDefinitionId()));

        CardDefinition cardDef = ctx.getCardLookup().getCardById(attacker.getCardDefinitionId());
        if (cardDef instanceof PokemonCardDefinition pokemonDef) {
            state.setPendingPrizeOwnerPlayerId(opponent.getPlayerId());
            state.setPendingPrizeCount(pokemonDef.isEx() ? 2 : 1);
        }

        if (player.getActivePokemon() != null
                && player.getActivePokemon().getInstanceId().equals(attacker.getInstanceId())) {
            player.setActivePokemon(null);
            if (player.getBench() != null && !player.getBench().isEmpty()) {
                state.setPendingKOReplacement(true);
                state.setKnockedOutPlayerId(player.getPlayerId());
                java.util.List<String> candidates = player.getBench().stream()
                        .map(p -> p.getInstanceId().toString())
                        .toList();
                ctx.addEvent(new GameEvent(
                        GameEventType.KO_REPLACEMENT_REQUIRED.name(), state.getMatchId(),
                        state.getTurnNumber(), Instant.now(),
                        "KO replacement required after confusion self-hit.",
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
        }
    }

    private int computeConditionalDamageBonus(String lowerText, PokemonInPlay attacker, PokemonInPlay defender,
                                                PlayerState player, PlayerState opponent, EngineContext ctx,
                                                Map<String, Object> payload) {
        int bonus = 0;

        // "times the amount of Energy attached to both Active Pokémon" (Evil Ball)
        if (lowerText.contains("energy attached to both active")) {
            int totalEnergy = (attacker.getAttachedEnergies() != null ? attacker.getAttachedEnergies().size() : 0)
                            + (defender.getAttachedEnergies() != null ? defender.getAttachedEnergies().size() : 0);
            if (totalEnergy > 0) {
                int basePerEnergy = 20;
                if (lowerText.contains("20")) basePerEnergy = 20;
                bonus = basePerEnergy * totalEnergy;
            }
        }

        // "for each Fire Energy attached to this Pokémon" (Blaze Ball)
        if (lowerText.contains("for each") && lowerText.contains("energy attached")) {
            String[] energyTypes = {"fire", "water", "lightning", "grass", "psychic", "fighting", "darkness", "metal", "fairy"};
            for (String et : energyTypes) {
                if (lowerText.contains(et + " energy")) {
                    int count = countEnergyOfType(attacker, et.toUpperCase(), ctx);
                    int perEnergy = 20;
                    if (lowerText.contains("30") && !lowerText.contains("20")) perEnergy = 30;
                    bonus = perEnergy * count;
                    break;
                }
            }
        }

        // "for each different type of basic Energy attached" (Colorful Wind)
        if (lowerText.contains("different type") && lowerText.contains("energy")) {
            bonus = countDistinctEnergyTypes(attacker, ctx) * 30;
        }

        // "for each damage counter on your opponent's Active Pokémon" (Second Bite)
        if (lowerText.contains("damage counter on your opponent")) {
            bonus = defender.getDamageCounters() * 10;
        }

        // "for each damage counter on this Pokémon" (Rage)
        if (lowerText.contains("damage counter on this")) {
            bonus = attacker.getDamageCounters() * 10;
        }

        // "If Lunatone is on your Bench" (Cosmic Spin)
        if (lowerText.contains("lunatone") && lowerText.contains("bench")) {
            if (player.getBench() != null) {
                boolean hasLunatone = player.getBench().stream()
                        .anyMatch(p -> p.getCardDefinitionId() != null && p.getCardDefinitionId().contains("lunatone"));
                if (hasLunatone) bonus = 30;
            }
        }

        // "If your opponent's Active Pokémon is affected by a Special Condition" (Wake-Up Slap)
        if (lowerText.contains("special condition") && lowerText.contains("opponent")) {
            if (defender.getSpecialConditions() != null && !defender.getSpecialConditions().isEmpty()) {
                bonus = 60;
            }
        }

        // "If your opponent's Active Pokémon already has any damage counters" (Tailspin Piledriver)
        if (lowerText.contains("damage counter") && lowerText.contains("your opponent") && !lowerText.contains("for each")) {
            if (defender.getDamageCounters() > 0) bonus = 40;
        }

        // "If your opponent's Active Pokémon is a Pokémon-EX" (Bite Off)
        if (lowerText.contains("pokémon-ex") || lowerText.contains("pokemon-ex")) {
            CardDefinition def = ctx.getCardLookup().getCardById(defender.getCardDefinitionId());
            if (def instanceof PokemonCardDefinition pDef && pDef.isEx()) bonus = 60;
        }

        // "If your opponent's Active Pokémon is a Grass Pokémon" (Leaf Munch)
        if (lowerText.contains("grass pokémon") || lowerText.contains("grass pokemon")) {
            CardDefinition def = ctx.getCardLookup().getCardById(defender.getCardDefinitionId());
            if (def instanceof PokemonCardDefinition pDef && pDef.getTypes() != null
                    && pDef.getTypes().contains(EnergyType.GRASS)) bonus = 20;
        }

        // "If this Pokémon has any Psychic Energy attached" (Core Splash)
        if (lowerText.contains("psychic energy") && lowerText.contains("this")) {
            if (countEnergyOfType(attacker, "PSYCHIC", ctx) > 0) bonus = 30;
        }

        // "Flip X coins. This attack does Y more damage for each heads." (DamagePlus style)
        // e.g. Dodrio Endeavor, Mr. Mime Slap Down, Scolipede Random Peck
        if (lowerText.contains("for each heads") && lowerText.contains("more damage")) {
            java.util.regex.Pattern flipP = java.util.regex.Pattern.compile("flip\\s+(\\d+)\\s+coins?");
            java.util.regex.Matcher flipM = flipP.matcher(lowerText);
            if (flipM.find()) {
                int coinCount = Integer.parseInt(flipM.group(1));
                int heads = 0;
                for (int i = 0; i < coinCount; i++) {
                    boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                    if (isHeads) heads++;
                    ctx.addEvent(new GameEvent(
                            GameEventType.COIN_FLIP_RESULT.name(),
                            ctx.getState().getMatchId(),
                            ctx.getState().getTurnNumber(),
                            java.time.Instant.now(),
                            isHeads ? "Cara" : "Cruz",
                            java.util.Map.of("result", isHeads ? "HEADS" : "TAILS", "source", "multi_coin_flip", "flipIndex", i, "totalFlips", coinCount)
                    ));
                }
                java.util.regex.Pattern bonusP = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage");
                java.util.regex.Matcher bonusM = bonusP.matcher(lowerText);
                if (bonusM.find()) {
                    bonus = Integer.parseInt(bonusM.group(1)) * heads;
                }
            }
        }

        // "You may discard/remove N. If you do, this attack does N more damage" (Electron Crush style)
        if ((lowerText.contains("you may") && lowerText.contains("more damage"))
                && (lowerText.contains("if you do") || lowerText.contains("if you discard"))) {
            // If the bonus requires discarding, only apply if discard IDs were actually provided
            boolean requiresDiscard = lowerText.contains("discard");
            boolean hasDiscardIds = payload != null && payload.containsKey("energyCardInstanceIdsToDiscard")
                    && payload.get("energyCardInstanceIdsToDiscard") instanceof List && !((List<?>) payload.get("energyCardInstanceIdsToDiscard")).isEmpty();
            if (!requiresDiscard || hasDiscardIds) {
                boolean useOptional = payload == null || !Boolean.FALSE.equals(payload.get("useOptionalBonus"));
                if (useOptional) {
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage");
                    java.util.regex.Matcher m = p.matcher(lowerText);
                    if (m.find()) bonus += Integer.parseInt(m.group(1));
                }
            }
        }

        // "You may do N more damage" — optional bonus (check payload flag)
        if (lowerText.contains("you may do") && lowerText.contains("more damage")) {
            boolean useOptional = payload == null || !Boolean.FALSE.equals(payload.get("useOptionalBonus"));
            if (useOptional) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage");
                java.util.regex.Matcher m = p.matcher(lowerText);
                if (m.find()) bonus += Integer.parseInt(m.group(1));
            }
        }

        if (bonus != 0) {
            log.warn("[attack] Conditional damage bonus: +{} (text: {})", bonus,
                    lowerText.substring(0, Math.min(80, lowerText.length())));
        }
        return bonus;
    }

    private int countEnergyOfType(PokemonInPlay pokemon, String energyType, EngineContext ctx) {
        if (pokemon.getAttachedEnergies() == null) return 0;
        int count = 0;
        for (CardInstance ci : pokemon.getAttachedEnergies()) {
            CardDefinition def = ctx.getCardLookup().getCardById(ci.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition ecd && ecd.getProvides() != null) {
                try {
                    EnergyType et = EnergyType.valueOf(energyType);
                    if (ecd.getProvides().contains(et)) count++;
                } catch (IllegalArgumentException e) { }
            }
        }
        return count;
    }

    private int countDistinctEnergyTypes(PokemonInPlay pokemon, EngineContext ctx) {
        if (pokemon.getAttachedEnergies() == null) return 0;
        java.util.Set<String> types = new java.util.HashSet<>();
        for (CardInstance ci : pokemon.getAttachedEnergies()) {
            CardDefinition def = ctx.getCardLookup().getCardById(ci.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition ecd && ecd.getProvides() != null
                    && ecd.getEnergyCardType() == EnergyCardType.BASIC) {
                for (EnergyType et : ecd.getProvides()) {
                    types.add(et.name());
                }
            }
        }
        return types.size();
    }
}
