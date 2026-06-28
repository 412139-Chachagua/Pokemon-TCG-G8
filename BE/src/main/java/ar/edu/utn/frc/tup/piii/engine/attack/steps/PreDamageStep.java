package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffectType;
import ar.edu.utn.frc.tup.piii.engine.attack.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class PreDamageStep extends AbstractAttackStep {

    private static final Logger log = LoggerFactory.getLogger(PreDamageStep.class);

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        var cardDef = ctx.getCardLookup().getCardById(attackCtx.getAttacker().getCardDefinitionId());
        if (cardDef instanceof PokemonCardDefinition pDef
                && pDef.getAttacks() != null
                && attackCtx.getAttackIndex() >= 0
                && attackCtx.getAttackIndex() < pDef.getAttacks().size()) {

            var attackDef = pDef.getAttacks().get(attackCtx.getAttackIndex());
            if (attackDef.getEffects() == null) return proceed(ctx, attackCtx);

            for (var effect : attackDef.getEffects()) {
                if (effect.getType() == AttackEffectType.COIN_FLIP_BEFORE_DAMAGE) {
                    Map<String, Object> params = effect.getParams() != null ? effect.getParams() : Map.of();
                    String subType = (String) params.get("effectType");
                    if ("CANCEL_ATTACK".equals(subType)) {
                        boolean heads = ctx.getRandomizer().nextInt(2) == 0;
                        log.warn("[predamage] Coin flip (cancel attack): {}", heads ? "HEADS" : "TAILS");
                        ctx.addEvent(new GameEvent(
                                GameEventType.COIN_FLIP_RESULT.name(),
                                ctx.getState().getMatchId(),
                                ctx.getState().getTurnNumber(),
                                Instant.now(),
                                heads ? "Cara" : "Cruz",
                                Map.of("result", heads ? "HEADS" : "TAILS", "source", "attack_cancel")
                        ));
                        if (!heads) {
                            log.warn("[predamage] Tails! Attack canceled.");
                            attackCtx.setAttackCanceled(true);
                            ctx.addEvent(new GameEvent(
                                    GameEventType.ATTACK_CANCELED.name(),
                                    ctx.getState().getMatchId(),
                                    ctx.getState().getTurnNumber(),
                                    Instant.now(),
                                    "The attack was canceled.",
                                    Map.of("reason", "coin_flip_tails")
                            ));
                            return proceed(ctx, attackCtx);
                        }
                    } else if ("DAMAGE_BONUS".equals(subType)) {
                        boolean heads = ctx.getRandomizer().nextInt(2) == 0;
                        log.warn("[predamage] Coin flip (damage bonus): {}", heads ? "HEADS" : "TAILS");
                        ctx.addEvent(new GameEvent(
                                GameEventType.COIN_FLIP_RESULT.name(),
                                ctx.getState().getMatchId(),
                                ctx.getState().getTurnNumber(),
                                Instant.now(),
                                heads ? "Cara" : "Cruz",
                                Map.of("result", heads ? "HEADS" : "TAILS", "source", "attack_effect")
                        ));
                        if (heads) {
                            try {
                                int bonus = Integer.parseInt(params.getOrDefault("effectParam", "0").toString());
                                attackCtx.setCoinFlipDamageBonus(bonus);
                                log.warn("[predamage] Heads! Damage bonus +{}", bonus);
                            } catch (NumberFormatException ignored) {}
                        }
                    } else if ("DAMAGE_MULTIPLIER".equals(subType)) {
                        // Only handle true multipliers (damage string contains "×") where
                        // PreDamageStep can determine the coin count from text.
                        // Skip for DamagePlus attacks ("50+", "20+") or special patterns
                        // (flip per energy, flip per damage counter, flip until tails)
                        // that are handled by the inline code in DeclareAttackHandler.
                        String damageStr = attackDef.getDamage() != null ? attackDef.getDamage() : "";
                        String attackText = attackDef.getText() != null ? attackDef.getText() : "";
                        int coinCount = extractCoinCount(attackText);
                        if (!damageStr.contains("×") || coinCount <= 0) {
                            String lowerText = attackText.toLowerCase();
                            // "Flip a coin until you get tails... more damage for each heads" → Spiny Rush style
                            if (lowerText.contains("flip a coin until") && lowerText.contains("tails")
                                    && lowerText.contains("for each heads") && lowerText.contains("more damage")) {
                                java.util.regex.Pattern bonusP = java.util.regex.Pattern.compile("(\\d+)\\s+more\\s+damage");
                                java.util.regex.Matcher bonusM = bonusP.matcher(lowerText);
                                int bonusPerHead = bonusM.find() ? Integer.parseInt(bonusM.group(1)) : 0;
                                int heads = 0;
                                int flipIdx = 0;
                                while (true) {
                                    boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                                    if (isHeads) heads++;
                                    attackCtx.addCoinFlipResult(isHeads);
                                    ctx.addEvent(new GameEvent(
                                            GameEventType.COIN_FLIP_RESULT.name(),
                                            ctx.getState().getMatchId(),
                                            ctx.getState().getTurnNumber(),
                                            Instant.now(),
                                            isHeads ? "Cara" : "Cruz",
                                            Map.of("result", isHeads ? "HEADS" : "TAILS", "source", "multi_coin_flip", "flipIndex", flipIdx, "totalFlips", -1)
                                    ));
                                    flipIdx++;
                                    if (!isHeads) break;
                                }
                                int baseDmg = DamageCalculator.parseIntDamage(damageStr);
                                int totalBaseDamage = baseDmg + bonusPerHead * heads;
                                attackCtx.setBaseDamageOverride(totalBaseDamage);
                                log.warn("[predamage] Flip-until-tails + for-each-heads: {} heads, base={}, bonus={}×{}={}, total={}",
                                        heads, baseDmg, bonusPerHead, heads, bonusPerHead * heads, totalBaseDamage);
                            } else {
                                // Register coin flips for all-heads tracking even if
                                // PreDamageStep doesn't calculate the multiplier itself
                                int recordedFlips = 0;
                                java.util.regex.Pattern genericFlipP = java.util.regex.Pattern.compile("flip\\s+(\\d+)\\s+coins?");
                                java.util.regex.Matcher genericFlipM = genericFlipP.matcher(lowerText);
                                if (genericFlipM.find()) {
                                    recordedFlips = Integer.parseInt(genericFlipM.group(1));
                                } else if (lowerText.contains("flip a coin")) {
                                    recordedFlips = 1;
                                }
                                for (int i = 0; i < recordedFlips; i++) {
                                    boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                                    attackCtx.addCoinFlipResult(isHeads);
                                }
                                log.warn("[predamage] DAMAGE_MULTIPLIER skipped (not a true multiplier or unhandled pattern): damageStr={}, coinCount={}", damageStr, coinCount);
                            }
                        } else {
                            String lowerText = attackText.toLowerCase();
                            boolean skipByInline = false;

                            // Skip patterns already handled by DeclareAttackHandler inline code:
                            // 1. "for each X energy" → flip per energy (handled inline at ~line 158)
                            if (lowerText.contains("for each") && lowerText.contains("energy")) {
                                String[] energyTypes = {"fighting", "fire", "water", "lightning", "grass", "psychic", "darkness", "metal", "fairy"};
                                for (String et : energyTypes) {
                                    if (lowerText.contains("for each " + et + " energy")) {
                                        skipByInline = true;
                                        break;
                                    }
                                }
                            }
                            // 2. "flip a coin until... tails" + true multiplier (handled inline at ~line 199)
                            if (!skipByInline && lowerText.contains("flip a coin until") && lowerText.contains("tails") && damageStr.contains("×")) {
                                skipByInline = true;
                            }

                            if (skipByInline) {
                                attackCtx.setFlipHandledByInline(true);
                                log.warn("[predamage] DAMAGE_MULTIPLIER skipped — handled by inline code: damageStr={}", damageStr);
                            } else {
                                int baseDamagePerHit;
                                try {
                                    baseDamagePerHit = Integer.parseInt(params.getOrDefault("effectParam", "0").toString());
                                } catch (NumberFormatException e) {
                                    baseDamagePerHit = 0;
                                }
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
                                int totalBaseDamage = baseDamagePerHit * heads;
                                attackCtx.setBaseDamageOverride(totalBaseDamage);
                                log.warn("[predamage] DAMAGE_MULTIPLIER: {} heads out of {}, damage={}×{}={}",
                                        heads, coinCount, baseDamagePerHit, heads, totalBaseDamage);
                            }
                        }
                    }
                }
            }
        }
        return proceed(ctx, attackCtx);
    }

    private static boolean hasVariableCoinCount(String lower) {
        // Coin count depends on attached energies (e.g. "for each Fighting Energy")
        if (lower.contains("for each") && lower.contains("energy")) {
            String[] energyTypes = {"fighting", "fire", "water", "lightning", "grass", "psychic", "darkness", "metal", "fairy"};
            for (String et : energyTypes) {
                if (lower.contains("for each " + et + " energy")) return true;
            }
        }
        // Coin count depends on damage counters (e.g. "for each damage counter on this Pokémon")
        if (lower.contains("for each") && lower.contains("damage counter")) return true;
        return false;
    }

    static int extractCoinCount(String text) {
        if (text == null) return 0;
        String lower = text.toLowerCase();
        // Fixed coin count: "Flip N coins" pattern
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("flip\\s+(\\d+)\\s+coins?");
        java.util.regex.Matcher m = p.matcher(lower);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        // "flip a coin" (singular) — but only if the count is fixed (not per-energy or per-counter)
        if ((lower.contains("flip a coin") || lower.contains("flip 1 coin")) && !hasVariableCoinCount(lower)) {
            return 1;
        }
        return 0;
    }
}
