package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class CoinFlipPostDamageEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(CoinFlipPostDamageEffect.class);
    private final PostDamageEffect subEffect;
    private final boolean applyOnHeads;
    private final boolean allHeadsMode;

    public CoinFlipPostDamageEffect(PostDamageEffect subEffect) {
        this(subEffect, true, false);
    }

    public CoinFlipPostDamageEffect(PostDamageEffect subEffect, boolean applyOnHeads) {
        this(subEffect, applyOnHeads, false);
    }

    public CoinFlipPostDamageEffect(PostDamageEffect subEffect, boolean applyOnHeads, boolean allHeadsMode) {
        this.subEffect = subEffect;
        this.applyOnHeads = applyOnHeads;
        this.allHeadsMode = allHeadsMode;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        // allHeadsMode: used when multiple coin flips must ALL be heads.
        // Two cases:
        // 1. BEFORE_DAMAGE: PreDamageStep already recorded flips (e.g. Flash Needle, 3 coins)
        //    → attackCtx.allCoinFlipsHeads() has the answer.
        // 2. AFTER_DAMAGE: No flips recorded yet (e.g. Mad Mountain, 2 coins)
        //    → do the multi-flip here.
        if (allHeadsMode) {
            if (attackCtx.allCoinFlipsHeads()) {
                // PreDamageStep already recorded all heads - apply sub-effect
                if (subEffect != null) {
                    log.warn("[coinflip] allHeadsMode: ALL flips were heads. Applying sub-effect.");
                    subEffect.apply(ctx, attackCtx);
                }
            } else if (attackCtx.getTotalCoinFlips() > 0 || attackCtx.isFlipHandledByInline()) {
                // PreDamageStep already recorded some flips (or handled by inline code) — skip.
                log.warn("[coinflip] allHeadsMode: {} flips (or inline), not all heads. Skipping.",
                        attackCtx.getTotalCoinFlips() > 0 ? attackCtx.getTotalCoinFlips() + " recorded" : "inline-handled");
            } else {
                // No flips were recorded yet (AFTER_DAMAGE, e.g. Mad Mountain).
                // Do the multi-flip here.
                int coinCount = 2;
                boolean allHeads = true;
                for (int i = 0; i < coinCount; i++) {
                    boolean isHeads = ctx.getRandomizer().nextInt(2) == 0;
                    attackCtx.addCoinFlipResult(isHeads);
                    ctx.addEvent(new GameEvent(
                            GameEventType.COIN_FLIP_RESULT.name(),
                            ctx.getState().getMatchId(),
                            ctx.getState().getTurnNumber(),
                            Instant.now(),
                            isHeads ? "Cara" : "Cruz",
                            Map.of("result", isHeads ? "HEADS" : "TAILS", "source", "multi_coin_flip", "flipIndex", i, "totalFlips", coinCount)
                    ));
                    if (!isHeads) allHeads = false;
                }
                if (allHeads && subEffect != null) {
                    log.warn("[coinflip] allHeadsMode: Both flips were heads. Applying sub-effect.");
                    subEffect.apply(ctx, attackCtx);
                } else {
                    log.warn("[coinflip] allHeadsMode: Not all flips were heads. Skipping sub-effect.");
                }
            }
            return;
        }

        boolean heads = ctx.getRandomizer().nextInt(2) == 0;
        boolean trigger = applyOnHeads ? heads : !heads;

        log.warn("[coinflip] Result: {}, applyOnHeads={}, trigger={}", heads ? "HEADS" : "TAILS", applyOnHeads, trigger);

        ctx.addEvent(new GameEvent(
                GameEventType.COIN_FLIP_RESULT.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                heads ? "Cara" : "Cruz",
                Map.of(
                        "result", heads ? "HEADS" : "TAILS",
                        "source", "attack_effect"
                )
        ));

        if (trigger && subEffect != null) {
            log.warn("[coinflip] Trigger! Applying sub-effect: {}", subEffect.getClass().getSimpleName());
            subEffect.apply(ctx, attackCtx);
        } else {
            log.warn("[coinflip] No trigger or no sub-effect — skipping");
        }
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
