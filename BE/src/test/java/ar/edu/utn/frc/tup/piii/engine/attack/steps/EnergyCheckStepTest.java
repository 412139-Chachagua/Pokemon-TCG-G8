package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnergyCheckStepTest {

    @Mock
    private EngineContext ctx;

    @Mock
    private EnergyService energyService;

    @Mock
    private CardLookupPort cardLookup;

    private PokemonInPlay attacker;
    private PokemonInPlay defender;
    private AttackContext attackCtx;
    private EnergyCheckStep step;

    @BeforeEach
    void setUp() {
        step = new EnergyCheckStep();
        attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setCardDefinitionId("pkm-pikachu");
        defender = new PokemonInPlay();
        defender.setInstanceId(UUID.randomUUID());
    }

    @Test
    void shouldProceedWhenEnergyIsSufficient() {
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(energyService.checkAttackRequirements(attacker, cardLookup, 0)).thenReturn(true);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertTrue(attackCtx.isEnergyValid());
    }

    @Test
    void shouldStopChainWhenEnergyIsInsufficient() {
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(energyService.checkAttackRequirements(attacker, cardLookup, 0)).thenReturn(false);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN, result);
        assertFalse(attackCtx.isEnergyValid());
        assertEquals("INSUFFICIENT_ENERGY", attackCtx.getErrorMessage());
        ArgumentCaptor<GameError> errorCaptor = ArgumentCaptor.forClass(GameError.class);
        verify(ctx).setError(errorCaptor.capture());
        assertEquals("INSUFFICIENT_ENERGY", errorCaptor.getValue().getCode());
    }
}
