package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModifierStepTest {

    @Mock
    private EngineContext ctx;

    @Mock
    private GameState state;

    private PokemonInPlay attacker;
    private PokemonInPlay defender;
    private AttackContext attackCtx;
    private TurnFlags turnFlags;
    private ModifierStep step;

    @BeforeEach
    void setUp() {
        step = new ModifierStep();
        attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        defender = new PokemonInPlay();
        defender.setInstanceId(UUID.randomUUID());
        turnFlags = new TurnFlags();
    }

    @Test
    void shouldProceedWhenNoTurnModifiers() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(turnFlags);
        attackCtx = new AttackContext(attacker, defender, 0, new HashMap<>(), null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertTrue(attackCtx.getDamageModifiers().isEmpty());
    }

    @Test
    void shouldMergeTurnModifiersIntoAttackContext() {
        Map<String, Object> turnMods = new HashMap<>();
        turnMods.put("mod1", 20);
        turnMods.put("mod2", 30);
        turnFlags.setDamageModifiers(turnMods);

        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(turnFlags);
        attackCtx = new AttackContext(attacker, defender, 0, new HashMap<>(), null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertEquals(20, attackCtx.getDamageModifiers().get("mod1"));
        assertEquals(30, attackCtx.getDamageModifiers().get("mod2"));
        assertNull(turnFlags.getDamageModifiers());
    }

    @Test
    void shouldSumModifiersWhenKeyAlreadyExistsInAttackContext() {
        Map<String, Object> turnMods = new HashMap<>();
        turnMods.put("shared", 15);
        turnFlags.setDamageModifiers(turnMods);

        Map<String, Object> existingMods = new HashMap<>();
        existingMods.put("shared", 10);
        existingMods.put("other", 5);

        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(turnFlags);
        attackCtx = new AttackContext(attacker, defender, 0, existingMods, null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertEquals(25, attackCtx.getDamageModifiers().get("shared")); // 10 + 15
        assertEquals(5, attackCtx.getDamageModifiers().get("other"));
        assertNull(turnFlags.getDamageModifiers());
    }
}
