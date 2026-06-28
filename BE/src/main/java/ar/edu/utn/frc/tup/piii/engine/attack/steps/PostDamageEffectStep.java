package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SpikyShieldHook;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffectType;
import ar.edu.utn.frc.tup.piii.engine.attack.effects.*;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PostDamageEffectStep extends AbstractAttackStep {

    private static final Logger log = LoggerFactory.getLogger(PostDamageEffectStep.class);

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        if (attackCtx.isAttackCanceled()) {
            return proceed(ctx, attackCtx);
        }

        SpikyShieldHook.afterDamageTaken(attackCtx.getDefender(), attackCtx.getAttacker(), ctx);

        var cardDef = ctx.getCardLookup().getCardById(attackCtx.getAttacker().getCardDefinitionId());
        if (cardDef instanceof PokemonCardDefinition pDef
                && pDef.getAttacks() != null
                && attackCtx.getAttackIndex() >= 0
                && attackCtx.getAttackIndex() < pDef.getAttacks().size()) {

            var attackDef = pDef.getAttacks().get(attackCtx.getAttackIndex());
            log.warn("[attack-step] Attack='{}' effectsList-size={}", attackDef.getName(),
                    attackDef.getEffects() != null ? attackDef.getEffects().size() : 0);
            List<PostDamageEffect> effects = buildEffects(attackDef.getEffects(), attackCtx);
            log.warn("[attack-step] Built {} PostDamageEffects", effects.size());
            for (PostDamageEffect effect : effects) {
                log.warn("[attack-step] Applying PostDamageEffect: {}", effect.getClass().getSimpleName());
                effect.apply(ctx, attackCtx);
                log.warn("[attack-step] PostDamageEffect applied: {}", effect.getClass().getSimpleName());
            }
            if (effects.isEmpty()) {
                log.warn("[attack-step] No PostDamageEffects to apply");
            }
        }

        return proceed(ctx, attackCtx);
    }

    private List<PostDamageEffect> buildEffects(List<ar.edu.utn.frc.tup.piii.engine.attack.AttackEffect> attackEffects, AttackContext attackCtx) {
        List<PostDamageEffect> effects = new ArrayList<>();
        if (attackEffects == null) return effects;

        for (var ae : attackEffects) {
            Map<String, Object> params = ae.getParams() != null ? ae.getParams() : Map.of();
            switch (ae.getType()) {
                case APPLY_SPECIAL_CONDITION:
                    String conditionStr = (String) params.get("condition");
                    if (conditionStr != null) {
                        if ("either".equals(params.get("choice"))) {
                            String chosen = attackCtx.getChosenCondition();
                            if (chosen == null || !chosen.equalsIgnoreCase(conditionStr)) {
                                break;
                            }
                        }
                        try {
                            SpecialCondition sc = SpecialCondition.valueOf(conditionStr);
                            String target = (String) params.getOrDefault("target", "defender");
                            effects.add(new ApplyConditionEffect(sc, target));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    break;
                case HEAL_USER:
                    int healCounters = parseIntParam(params.get("count"), 3);
                    boolean targetBench = Boolean.TRUE.equals(params.get("targetBench"));
                    boolean healAll = Boolean.TRUE.equals(params.get("healAll"));
                    boolean clearConditions = Boolean.TRUE.equals(params.get("clearConditions"));
                    boolean healFull = Boolean.TRUE.equals(params.get("healFull"));
                    boolean clearDefenderConditions = Boolean.TRUE.equals(params.get("clearDefenderConditions"));
                    effects.add(new HealUserEffect(healCounters, targetBench, healAll, clearConditions, healFull, clearDefenderConditions));
                    break;
                case DISCARD_ENERGY:
                    int discardCount = parseIntParam(params.get("count"), 1);
                    String discardTarget = (String) params.getOrDefault("target", "defender");
                    boolean discardOptional = Boolean.TRUE.equals(params.get("optional"));
                    effects.add(new DiscardEnergyEffect(discardCount, discardTarget, discardOptional));
                    break;
                case SWITCH_AFTER_DAMAGE:
                    boolean switchAttacker = Boolean.TRUE.equals(params.get("switchAttacker"));
                    boolean conditionalSwitch = Boolean.TRUE.equals(params.get("conditional"));
                    effects.add(new SwitchDefenderEffect(switchAttacker, conditionalSwitch));
                    break;
                case RECOIL:
                    int recoilCounters = parseIntParam(params.get("count"), 2);
                    effects.add(new RecoilEffect(recoilCounters));
                    break;
                case DAMAGE_BENCH:
                    int benchDamage = parseIntParam(params.get("damage"), 10);
                    boolean ownBench = Boolean.TRUE.equals(params.get("ownBench"));
                    effects.add(new DamageBenchEffect(benchDamage, ownBench));
                    break;
                case DRAW_CARDS:
                    int drawCount = parseIntParam(params.get("count"), 1);
                    effects.add(new DrawCardsEffect(drawCount));
                    break;
                case SEARCH_DECK:
                    String searchType = (String) params.getOrDefault("searchType", "ANY");
                    String searchEnergyType = (String) params.get("energyType");
                    String searchPokemonType = (String) params.get("pokemonType");
                    int searchCount = parseIntParam(params.get("count"), 1);
                    if (searchPokemonType != null) {
                        effects.add(new SearchDeckEffect(searchType, searchPokemonType, searchCount, searchPokemonType));
                    } else {
                        effects.add(new SearchDeckEffect(searchType, searchEnergyType, searchCount));
                    }
                    break;
                case ATTACH_ENERGY:
                    String attachSource = (String) params.getOrDefault("source", "deck");
                    String attachEnergyType = (String) params.get("energyType");
                    int attachCount = parseIntParam(params.get("count"), 1);
                    String attachTarget = (String) params.getOrDefault("target", "attacker");
                    effects.add(new AttachEnergyEffect(attachSource, attachEnergyType, attachCount, attachTarget));
                    break;
                case MOVE_ENERGY:
                    String meSource = (String) params.getOrDefault("sourcePokemon", "attacker");
                    String meTarget = (String) params.getOrDefault("targetPokemon", "ownBench");
                    int meCount = parseIntParam(params.get("count"), 1);
                    effects.add(new MoveEnergyEffect(meSource, meTarget, meCount));
                    break;
                case DAMAGE_PREVENTION:
                    Integer threshold = (Integer) params.get("threshold");
                    effects.add(new DamagePreventionEffect(true, threshold));
                    break;
                case CANNOT_ATTACK_NEXT_TURN:
                    String restrictedAttack = (String) params.get("attackName");
                    effects.add(new CanNotAttackNextTurnEffect(restrictedAttack));
                    break;
                case SUPPORTER_LOCK:
                    effects.add(new SupporterLockEffect());
                    break;
                case OPPONENT_DISCARD_HAND:
                    int odmCount = parseIntParam(params.get("count"), 1);
                    effects.add(new OpponentDiscardHandEffect(odmCount));
                    break;
                case NEXT_TURN_DAMAGE_BONUS:
                    int bonus = parseIntParam(params.get("bonus"), 40);
                    effects.add(new NextTurnDamageBonusEffect(bonus));
                    break;
                case RETREAT_LOCK:
                    effects.add(new RetreatLockEffect());
                    break;
                case DAMAGE_REDUCTION:
                    int reduction = parseIntParam(params.get("reduction"), 20);
                    effects.add(new DamageReductionEffect(reduction));
                    break;
                case DISCARD_OPPONENT_DECK:
                    int odmCount2 = parseIntParam(params.get("count"), 1);
                    String deckTarget = (String) params.getOrDefault("target", "opponent");
                    boolean multiplyDCs = "true".equals(params.get("multiplyByDamageCounters"));
                    String attachType = (String) params.get("attachIfEnergyType");
                    effects.add(new DiscardOpponentDeckEffect(odmCount2, deckTarget, multiplyDCs, attachType));
                    break;
                case SEARCH_DISCARD:
                    int sdCount = parseIntParam(params.get("count"), 2);
                    String cardType = (String) params.get("cardType");
                    effects.add(new SearchDiscardEffect(sdCount, cardType));
                    break;
                case RECYCLE_FROM_DISCARD:
                    effects.add(new RecycleFromDiscardEffect());
                    break;
                case OPPONENT_SHUFFLE_DRAW:
                    int osdCount = parseIntParam(params.get("count"), 4);
                    effects.add(new OpponentShuffleDrawEffect(osdCount));
                    break;
                case DAMAGE_ALL_BENCH:
                    int dabCounters = parseIntParam(params.get("damageCounters"), 2);
                    effects.add(new DamageAllBenchEffect(dabCounters));
                    break;
                case DEFENDER_CANNOT_ATTACK:
                    effects.add(new DefenderCannotAttackEffect());
                    break;
                case ABILITY_SUPPRESSION:
                    effects.add(new AbilitySuppressionEffect());
                    break;
                case DISCARD_TOOL:
                    effects.add(new DiscardToolEffect());
                    break;
                case REORDER_DECK:
                    int reorderCount = parseIntParam(params.get("count"), 3);
                    effects.add(new ReorderDeckEffect(reorderCount));
                    break;
                case PEEK_OPPONENT_DECK:
                    effects.add(new PeekOpponentDeckEffect());
                    break;
                case OPPONENT_RANDOM_DISCARD:
                    effects.add(new OpponentRandomDiscardEffect());
                    break;
                case SET_HP:
                    int targetHp = parseIntParam(params.get("targetHp"), 10);
                    effects.add(new SetHpEffect(targetHp));
                    break;
                case MENTAL_PANIC:
                    effects.add(new MentalPanicEffect());
                    break;
                case COIN_FLIP_BEFORE_DAMAGE:
                    PostDamageEffect beforeEffect = buildSubEffectFromParams(params);
                    if (beforeEffect != null) {
                        effects.add(new CoinFlipPostDamageEffect(beforeEffect));
                    }
                    break;
                case COIN_FLIP_AFTER_DAMAGE:
                    String subTypeStr = (String) params.get("effectType");
                    // FLIP_UNTIL_TAILS is self-contained — it does its own coin flips,
                    // so don't wrap it in CoinFlipPostDamageEffect (which would add an extra flip).
                    if ("FLIP_UNTIL_TAILS".equals(subTypeStr)) {
                        PostDamageEffect flipEffect = buildSubEffectFromParams(params);
                        if (flipEffect != null) effects.add(flipEffect);
                        break;
                    }
                    // Multi-flip for-each-heads attach energy (e.g., Lapras "Seafaring")
                    if ("ATTACH_ENERGY".equals(subTypeStr)) {
                        int coinCount = parseIntParam(params.get("coinCount"), 1);
                        boolean forEachHeads = "true".equals(params.get("forEachHeads"));
                        if (coinCount > 1 && forEachHeads) {
                            String energyType = (String) params.get("effectParam");
                            effects.add(createForEachHeadsAttachEffect(coinCount, energyType));
                            break;
                        }
                    }
                    PostDamageEffect afterEffect = buildSubEffectFromParams(params);
                    boolean applyOnHeads = !"false".equals(params.get("applyOnHeads"));
                    boolean allHeadsMode = "true".equals(params.get("allHeads"));
                    if (afterEffect != null) {
                        effects.add(new CoinFlipPostDamageEffect(afterEffect, applyOnHeads, allHeadsMode));
                    }
                    break;
            }
        }
        return effects;
    }

    private         PostDamageEffect buildSubEffectFromParams(Map<String, Object> params) {
        String subTypeStr = (String) params.get("effectType");
        if (subTypeStr == null) return null;

        // Handle non-standard effect types that are not part of AttackEffectType enum
        // e.g. "FLIP_UNTIL_TAILS" and "COMBINED_CONDITION_DISCARD" 
        // are parser-generated subtype strings
        if ("COMBINED_CONDITION_DISCARD".equals(subTypeStr)) {
            String conditionStr = (String) params.get("effectParam");
            String discardCountStr = (String) params.get("discardCount");
            int discardCount = 1;
            if (discardCountStr != null) { try { discardCount = Integer.parseInt(discardCountStr); } catch (NumberFormatException e) {} }
            String finalConditionStr = conditionStr;
            int finalDiscardCount = discardCount;
            return new PostDamageEffect() {
                @Override public void apply(EngineContext ctx, AttackContext attackCtx) {
                    if (finalConditionStr != null) {
                        try {
                            SpecialCondition sc = SpecialCondition.valueOf(finalConditionStr);
                            new ApplyConditionEffect(sc).apply(ctx, attackCtx);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    new DiscardEnergyEffect(finalDiscardCount, "defender").apply(ctx, attackCtx);
                }
                @Override public EffectTiming getTiming() { return EffectTiming.AFTER_DAMAGE; }
            };
        }

        if ("FLIP_UNTIL_TAILS".equals(subTypeStr)) {
            // Return a coin flip effect that flips until tails.
            // The actual flip-until-tails logic for damage is in DeclareAttackHandler inline code.
            // Here we just return a no-op effect so the pipeline doesn't crash.
            return new PostDamageEffect() {
                @Override public void apply(EngineContext ctx, AttackContext attackCtx) {
                    String text = (String) params.getOrDefault("text", "");
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage");
                    java.util.regex.Matcher m = p.matcher(text.toLowerCase());
                    int heads = 0;
                    while (true) {
                        boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                        ctx.addEvent(new GameEvent(
                                GameEventType.COIN_FLIP_RESULT.name(),
                                ctx.getState().getMatchId(),
                                ctx.getState().getTurnNumber(),
                                Instant.now(),
                                isHeads ? "Cara" : "Cruz",
                                Map.of("result", isHeads ? "HEADS" : "TAILS",
                                       "source", "multi_coin_flip",
                                       "flipIndex", heads,
                                       "totalFlips", -1)
                        ));
                        if (!isHeads) break;
                        heads++;
                    }
                }
                @Override public EffectTiming getTiming() { return EffectTiming.AFTER_DAMAGE; }
            };
        }

        AttackEffectType subType;
        try {
            subType = AttackEffectType.valueOf(subTypeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }

        String subParam = (String) params.get("effectParam");
        return switch (subType) {
            case APPLY_SPECIAL_CONDITION -> {
                if (subParam != null) {
                    try {
                        SpecialCondition sc = SpecialCondition.valueOf(subParam);
                        yield new ApplyConditionEffect(sc);
                    } catch (IllegalArgumentException ignored) {}
                }
                yield null;
            }
            case DISCARD_ENERGY -> {
                String subTarget = (String) params.getOrDefault("target", "defender");
                boolean subOptional = Boolean.TRUE.equals(params.get("optional"));
                yield new DiscardEnergyEffect(parseIntParam(subParam, 1), subTarget, subOptional);
            }
            case DAMAGE_BENCH -> new DamageBenchEffect(parseIntParam(subParam, 10), Boolean.TRUE.equals(params.get("ownBench")));
            case HEAL_USER -> new HealUserEffect(parseIntParam(subParam, 3), Boolean.TRUE.equals(params.get("targetBench")));
            case DRAW_CARDS -> new DrawCardsEffect(parseIntParam(subParam, 1));
            case RECOIL -> new RecoilEffect(parseIntParam(subParam, 2));
            case SWITCH_AFTER_DAMAGE -> new SwitchDefenderEffect(Boolean.parseBoolean(subParam));
            case DAMAGE_PREVENTION -> new DamagePreventionEffect();
            case CANNOT_ATTACK_NEXT_TURN -> new CanNotAttackNextTurnEffect();
            case SUPPORTER_LOCK -> new SupporterLockEffect();
            case OPPONENT_DISCARD_HAND -> new OpponentDiscardHandEffect(parseIntParam(subParam, 1));
            case SEARCH_DECK -> new SearchDeckEffect(subParam != null ? subParam : "SUPPORTER", null, 1);
            case ATTACH_ENERGY -> new AttachEnergyEffect("discard", subParam, 1, "bench");
            case DISCARD_OPPONENT_DECK -> new DiscardOpponentDeckEffect(parseIntParam(subParam, 1), "opponent", "true".equals(params.get("multiplyByDamageCounters")), (String) params.get("attachIfEnergyType"));
            default -> null;
        };
    }

    private int parseIntParam(Object value, int defaultVal) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    private PostDamageEffect createForEachHeadsAttachEffect(int coinCount, String energyType) {
        return new PostDamageEffect() {
            @Override
            public void apply(EngineContext ctx, AttackContext attackCtx) {
                PlayerState player = ctx.getPlayer(attackCtx.getAttacker().getOwnerPlayerId());
                if (player == null) return;

                int heads = 0;
                for (int i = 0; i < coinCount; i++) {
                    boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                    if (isHeads) heads++;
                    attackCtx.addCoinFlipResult(isHeads);
                    ctx.addEvent(new GameEvent(
                            GameEventType.COIN_FLIP_RESULT.name(),
                            ctx.getState().getMatchId(),
                            ctx.getState().getTurnNumber(),
                            Instant.now(),
                            isHeads ? "Cara" : "Cruz",
                            Map.of("result", isHeads ? "HEADS" : "TAILS", "source", "multi_coin_flip", "flipIndex", i, "totalFlips", coinCount)
                    ));
                }

                if (heads == 0) return;
                if (player.getBench() == null || player.getBench().isEmpty()) return;

                List<Map<String, Object>> benchTargets = attackCtx.getBenchTargets();
                int targetIdx = 0;
                                List<CardInstance> pool = new ArrayList<>(player.getDiscard());
                List<CardInstance> energies = new ArrayList<>();
                for (CardInstance ci : pool) {
                    CardDefinition def = ctx.getCardLookup().getCardById(ci.getCardDefinitionId());
                    if (def instanceof EnergyCardDefinition ecd && ecd.getProvides() != null) {
                        try {
                            EnergyType et = EnergyType.valueOf(energyType);
                            if (ecd.getProvides().contains(et)) {
                                energies.add(ci);
                            }
                        } catch (IllegalArgumentException e) {}
                    }
                }

                int attached = 0;
                for (int h = 0; h < heads && attached < energies.size(); h++) {
                    PokemonInPlay targetPkm = null;
                    if (benchTargets != null && targetIdx < benchTargets.size()) {
                        String idStr = (String) benchTargets.get(targetIdx).get("instanceId");
                        if (idStr != null) {
                            UUID targetId = UUID.fromString(idStr);
                            for (PokemonInPlay pkm : player.getBench()) {
                                if (pkm.getInstanceId().equals(targetId)) {
                                    targetPkm = pkm;
                                    break;
                                }
                            }
                        }
                        targetIdx++;
                    }
                    if (targetPkm == null) {
                        if (benchTargets != null && !benchTargets.isEmpty()) {
                            String lastId = (String) benchTargets.get(benchTargets.size() - 1).get("instanceId");
                            if (lastId != null) {
                                UUID tid = UUID.fromString(lastId);
                                for (PokemonInPlay pkm : player.getBench()) {
                                    if (pkm.getInstanceId().equals(tid)) {
                                        targetPkm = pkm;
                                        break;
                                    }
                                }
                            }
                        }
                        if (targetPkm == null && player.getBench() != null && !player.getBench().isEmpty()) {
                            targetPkm = player.getBench().get(0);
                        }
                    }

                    CardInstance energy = energies.get(attached);
                    player.removeFromDiscard(energy.getInstanceId());
                    player.getHand().add(energy);
                    ctx.getEnergyService().attachFromHand(energy, targetPkm, player, ctx);
                    attached++;
                }

                if (attached > 0) {
                    attackCtx.setEnergyAttachedThisAttack(true);
                }

                ctx.addEvent(new GameEvent(
                        GameEventType.ENERGY_ATTACHED.name(),
                        ctx.getState().getMatchId(),
                        ctx.getState().getTurnNumber(),
                        Instant.now(),
                        attached + " energy attached from discard via Seafaring.",
                        Map.of("count", attached, "source", "seafaring")
                ));
            }

            @Override
            public EffectTiming getTiming() {
                return EffectTiming.AFTER_DAMAGE;
            }
        };
    }
}
