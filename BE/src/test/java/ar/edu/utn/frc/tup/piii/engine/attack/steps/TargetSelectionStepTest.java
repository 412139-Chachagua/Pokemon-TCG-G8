package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TargetSelectionStepTest {

    @Mock
    private EngineContext ctx;

    @Mock
    private PokemonInPlay defender;

    private PokemonInPlay attacker;
    private AttackContext attackCtx;
    private TargetSelectionStep step;

    @BeforeEach
    void setUp() {
        step = new TargetSelectionStep();
        attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
    }

    @Test
    void shouldStopChainWhenDefenderIsNull() {
        attackCtx = new AttackContext(attacker, null, 0, Map.of(), null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN, result);
    }

    @Test
    void shouldProceedWhenDefenderIsPresent() {
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
    }
}
