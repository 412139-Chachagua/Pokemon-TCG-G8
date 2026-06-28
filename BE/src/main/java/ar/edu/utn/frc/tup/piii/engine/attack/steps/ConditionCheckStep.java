package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class ConditionCheckStep extends AbstractAttackStep {

    private static final Logger log = LoggerFactory.getLogger(ConditionCheckStep.class);

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();

        if (attacker.isCannotAttackNextTurn()) {
            String restricted = attacker.getRestrictedAttackName();
            boolean blocked = true;
            if (restricted != null) {
                // Check if the current attack is the restricted one
                var cardDef = ctx.getCardLookup().getCardById(attacker.getCardDefinitionId());
                if (cardDef instanceof PokemonCardDefinition pDef
                        && pDef.getAttacks() != null && attackCtx.getAttackIndex() >= 0
                        && attackCtx.getAttackIndex() < pDef.getAttacks().size()) {
                    String attackName = pDef.getAttacks().get(attackCtx.getAttackIndex()).getName();
                    blocked = attackName != null && attackName.equalsIgnoreCase(restricted);
                }
            }
            attacker.setCannotAttackNextTurn(false);
            attacker.setRestrictedAttackName(null);
            if (blocked) {
                log.warn("[condition] Attacker cannot attack this turn");
                ctx.addEvent(new GameEvent(
                        GameEventType.ATTACK_CANCELED.name(),
                        ctx.getState().getMatchId(),
                        ctx.getState().getTurnNumber(),
                        Instant.now(),
                        "El Pokémon no puede atacar este turno.",
                        Map.of("reason", "cannot_attack_next_turn")
                ));
                return AttackStepResult.STOP_CHAIN;
            }
        }

        if (attacker.getSpecialConditions() != null
                && attacker.getSpecialConditions().contains(SpecialCondition.ASLEEP)) {
            log.warn("[condition] Attacker is ASLEEP — cannot attack");
            ctx.addEvent(new GameEvent(
                    GameEventType.ATTACK_CANCELED.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    "El Pokémon está Dormido y no puede atacar.",
                    Map.of("reason", "asleep")
            ));
            return AttackStepResult.STOP_CHAIN;
        }

        if (attacker.getSpecialConditions() != null
                && attacker.getSpecialConditions().contains(SpecialCondition.PARALYZED)) {
            log.warn("[condition] Attacker is PARALYZED — cannot attack");
            ctx.addEvent(new GameEvent(
                    GameEventType.ATTACK_CANCELED.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    "El Pokémon está Paralizado y no puede atacar.",
                    Map.of("reason", "paralyzed")
            ));
            return AttackStepResult.STOP_CHAIN;
        }

        // Mental Panic: defender must flip a coin to attack.
        // If tails, the attack does nothing.
        if (attacker.isMustFlipToAttackNextTurn()) {
            attacker.setMustFlipToAttackNextTurn(false);
            boolean canAttack = ctx.getRandomizer().nextInt(2) == 0;
            ctx.addEvent(new GameEvent(
                    GameEventType.COIN_FLIP_RESULT.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    canAttack ? "Cara" : "Cruz",
                    Map.of("result", canAttack ? "HEADS" : "TAILS", "source", "mental_panic")
            ));
            if (!canAttack) {
                log.warn("[condition] Mental Panic: tails! Attack does nothing.");
                ctx.addEvent(new GameEvent(
                        GameEventType.ATTACK_CANCELED.name(),
                        ctx.getState().getMatchId(),
                        ctx.getState().getTurnNumber(),
                        Instant.now(),
                        "El ataque no hizo nada debido a Pánico Mental.",
                        Map.of("reason", "mental_panic_tails")
                ));
                return AttackStepResult.STOP_CHAIN;
            }
        }

        return proceed(ctx, attackCtx);
    }
}
