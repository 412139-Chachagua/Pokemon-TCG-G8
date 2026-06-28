package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfusionCheckStepTest {

    @Mock
    private EngineContext ctx;

    @Mock
    private RandomizerPort randomizer;

    private PokemonInPlay attacker;
    private PokemonInPlay defender;
    private AttackContext attackCtx;
    private ConfusionCheckStep step;

    @BeforeEach
    void setUp() {
        step = new ConfusionCheckStep();
        attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setSpecialConditions(new ArrayList<>());
        defender = new PokemonInPlay();
        defender.setInstanceId(UUID.randomUUID());
    }

    @Test
    void shouldProceedWhenNotConfused() {
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertFalse(attackCtx.isConfusedSelfHit());
    }

    @Test
    void shouldSelfHitWhenConfusedAndTails() {
        attacker.getSpecialConditions().add(SpecialCondition.CONFUSED);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        // nextInt(2) == 0 → self-hit triggers
        when(randomizer.nextInt(2)).thenReturn(0);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN_END_TURN, result);
        assertTrue(attackCtx.isConfusedSelfHit());
        assertEquals(3, attackCtx.getSelfDamageCounters());
    }

    @Test
    void shouldProceedWhenConfusedAndHeads() {
        attacker.getSpecialConditions().add(SpecialCondition.CONFUSED);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        // nextInt(2) == 1 → no self-hit
        when(randomizer.nextInt(2)).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertFalse(attackCtx.isConfusedSelfHit());
    }
}
