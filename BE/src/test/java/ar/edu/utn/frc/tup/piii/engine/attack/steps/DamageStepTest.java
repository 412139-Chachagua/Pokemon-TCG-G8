package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DamageStepTest {

    @Mock
    private EngineContext ctx;

    @Mock
    private GameState state;

    @Mock
    private CardLookupPort cardLookup;

    private PokemonInPlay attacker;
    private PokemonInPlay defender;
    private PokemonCardDefinition attackerDef;
    private PokemonCardDefinition.AttackDefinition attackDef;
    private AttackContext attackCtx;
    private DamageStep step;

    @BeforeEach
    void setUp() {
        step = new DamageStep();
        attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setCardDefinitionId("pkm-pikachu");
        attacker.setAttachedEnergies(new ArrayList<>());
        defender = new PokemonInPlay();
        defender.setInstanceId(UUID.randomUUID());
        defender.setCardDefinitionId("pkm-charmander");
        defender.setDamageCounters(0);
        defender.setAttachedEnergies(new ArrayList<>());

        attackDef = new PokemonCardDefinition.AttackDefinition();
        attackDef.setName("Thunderbolt");
        attackDef.setDamage("50");

        attackerDef = new PokemonCardDefinition();
        attackerDef.setHp(60);
        attackerDef.setAttacks(List.of(attackDef));
        attackerDef.setTypes(new ArrayList<>());
    }

    @Test
    void shouldProceedWithoutDamageWhenAttackCanceled() {
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        attackCtx.setAttackCanceled(true);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertNull(attackCtx.getDamageCalc());
    }

    @Test
    void shouldCalculateDamageAndApplyCounters() {
        attackCtx = new AttackContext(attacker, defender, 0, new HashMap<>(), null);
        when(ctx.getState()).thenReturn(state);
        when(ctx.getCardLookup()).thenReturn(cardLookup);


        PokemonCardDefinition defenderDef = mock(PokemonCardDefinition.class);


        var expectedResult = new DamageCalculator.DamageCalculatorResult(
                50, 1, 0, 50, 5, false, false
        );

        try (MockedStatic<DamageCalculator> mockedCalc = mockStatic(DamageCalculator.class)) {
            mockedCalc.when(() -> DamageCalculator.calculate(
                    eq(attacker), eq(defender), eq(cardLookup), eq(0),
                    anyMap(), isNull(), eq(false), eq(false), isNull()
            )).thenReturn(expectedResult);

            var result = step.execute(ctx, attackCtx);

            assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
            assertNotNull(attackCtx.getDamageCalc());
            assertEquals(50, attackCtx.getDamageCalc().finalDamage());
            assertEquals(5, defender.getDamageCounters());
        }
    }

    @Test
    void shouldIncludeCoinFlipDamageBonus() {
        attackCtx = new AttackContext(attacker, defender, 0, new HashMap<>(), null);
        attackCtx.setCoinFlipDamageBonus(20);
        when(ctx.getState()).thenReturn(state);
        when(ctx.getCardLookup()).thenReturn(cardLookup);


        PokemonCardDefinition defenderDef = mock(PokemonCardDefinition.class);


        var expectedResult = new DamageCalculator.DamageCalculatorResult(
                50, 1, 0, 50, 5, false, false
        );

        try (MockedStatic<DamageCalculator> mockedCalc = mockStatic(DamageCalculator.class)) {
            mockedCalc.when(() -> DamageCalculator.calculate(
                    eq(attacker), eq(defender), eq(cardLookup), eq(0),
                    anyMap(), isNull(), eq(false), eq(false), isNull()
            )).thenReturn(expectedResult);

            var result = step.execute(ctx, attackCtx);

            assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
            // bonus = 20 → 20/10 = 2 extra damage counters
            // base counters = 5, + 2 bonus = 7
            assertEquals(7, defender.getDamageCounters());
        }
    }

    @Test
    void shouldResolveStadiumEffectCode() {
        attackCtx = new AttackContext(attacker, defender, 0, new HashMap<>(), null);
        when(ctx.getState()).thenReturn(state);
        when(ctx.getCardLookup()).thenReturn(cardLookup);


        PokemonCardDefinition defenderDef = mock(PokemonCardDefinition.class);


        String stadiumDefId = "stadium-shadow-circle";
        when(state.getStadiumCardDefinitionId()).thenReturn(stadiumDefId);
        TrainerCardDefinition stadiumDef = mock(TrainerCardDefinition.class);
        when(stadiumDef.getEffectCode()).thenReturn("SHADOW_CIRCLE");
        when(cardLookup.getCardById(stadiumDefId)).thenReturn(stadiumDef);

        var expectedResult = new DamageCalculator.DamageCalculatorResult(
                50, 1, 0, 50, 5, false, false
        );

        try (MockedStatic<DamageCalculator> mockedCalc = mockStatic(DamageCalculator.class)) {
            mockedCalc.when(() -> DamageCalculator.calculate(
                    eq(attacker), eq(defender), eq(cardLookup), eq(0),
                    anyMap(), isNull(), eq(false), eq(false), eq("SHADOW_CIRCLE")
            )).thenReturn(expectedResult);

            var result = step.execute(ctx, attackCtx);

            assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
            assertNotNull(attackCtx.getDamageCalc());
        }
    }
}
