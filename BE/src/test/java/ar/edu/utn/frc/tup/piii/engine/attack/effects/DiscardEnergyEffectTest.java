package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscardEnergyEffectTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private AttackContext attackCtx;
    @Mock
    private GameState state;
    @Mock
    private EnergyService energyService;

    private PokemonInPlay createPokemon(UUID ownerId, List<CardInstance> energies) {
        PokemonInPlay p = new PokemonInPlay();
        p.setInstanceId(UUID.randomUUID());
        p.setOwnerPlayerId(ownerId);
        p.setAttachedEnergies(energies != null ? energies : new ArrayList<>());
        return p;
    }

    @Test
    void apply_discard1FromDefender_removesOneEnergy() {
        UUID ownerId = UUID.randomUUID();
        CardInstance energy1 = new CardInstance(UUID.randomUUID(), "en-1");
        CardInstance energy2 = new CardInstance(UUID.randomUUID(), "en-2");
        PokemonInPlay defender = createPokemon(ownerId, new ArrayList<>(List.of(energy1, energy2)));
        when(attackCtx.getDefender()).thenReturn(defender);
        when(attackCtx.getDiscardEnergyInstanceIds()).thenReturn(null);

        PlayerState owner = new PlayerState();
        owner.setPlayerId(ownerId);
        when(ctx.getPlayer(ownerId)).thenReturn(owner);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DiscardEnergyEffect effect = new DiscardEnergyEffect(1);
        effect.apply(ctx, attackCtx);

        verify(energyService).detachEnergies(eq(defender), eq(owner), anyList(), eq(ctx));
    }

    @Test
    void apply_discardAllEnergy_count99_removesAll() {
        UUID ownerId = UUID.randomUUID();
        List<CardInstance> energies = new ArrayList<>();
        energies.add(new CardInstance(UUID.randomUUID(), "en-1"));
        energies.add(new CardInstance(UUID.randomUUID(), "en-2"));
        energies.add(new CardInstance(UUID.randomUUID(), "en-3"));
        PokemonInPlay defender = createPokemon(ownerId, energies);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(attackCtx.getDiscardEnergyInstanceIds()).thenReturn(null);

        PlayerState owner = new PlayerState();
        owner.setPlayerId(ownerId);
        when(ctx.getPlayer(ownerId)).thenReturn(owner);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DiscardEnergyEffect effect = new DiscardEnergyEffect(99);
        effect.apply(ctx, attackCtx);

        verify(energyService).detachEnergies(eq(defender), eq(owner), argThat(list -> list.size() == 3), eq(ctx));
    }

    @Test
    void apply_discardFromAttacker_usesAttacker() {
        UUID ownerId = UUID.randomUUID();
        CardInstance energy = new CardInstance(UUID.randomUUID(), "en-1");
        PokemonInPlay attacker = createPokemon(ownerId, new ArrayList<>(List.of(energy)));
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDiscardEnergyInstanceIds()).thenReturn(null);

        PlayerState owner = new PlayerState();
        owner.setPlayerId(ownerId);
        when(ctx.getPlayer(ownerId)).thenReturn(owner);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DiscardEnergyEffect effect = new DiscardEnergyEffect(1, "attacker");
        effect.apply(ctx, attackCtx);

        verify(energyService).detachEnergies(eq(attacker), eq(owner), anyList(), eq(ctx));
    }

    @Test
    void apply_noEnergyAttached_noAction() {
        PokemonInPlay defender = createPokemon(UUID.randomUUID(), null);
        when(attackCtx.getDefender()).thenReturn(defender);

        DiscardEnergyEffect effect = new DiscardEnergyEffect(1);
        effect.apply(ctx, attackCtx);

        verify(energyService, never()).detachEnergies(any(), any(), any(), any());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_optionalNoIds_skips() {
        PokemonInPlay defender = createPokemon(UUID.randomUUID(),
                new ArrayList<>(List.of(new CardInstance(UUID.randomUUID(), "en-1"))));
        when(attackCtx.getDefender()).thenReturn(defender);
        when(attackCtx.getDiscardEnergyInstanceIds()).thenReturn(null);

        DiscardEnergyEffect effect = new DiscardEnergyEffect(1, "defender", true);
        effect.apply(ctx, attackCtx);

        verify(energyService, never()).detachEnergies(any(), any(), any(), any());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_optionalWithSpecificIds_discardsSpecified() {
        UUID ownerId = UUID.randomUUID();
        CardInstance energy1 = new CardInstance(UUID.randomUUID(), "en-1");
        CardInstance energy2 = new CardInstance(UUID.randomUUID(), "en-2");
        PokemonInPlay defender = createPokemon(ownerId, new ArrayList<>(List.of(energy1, energy2)));
        when(attackCtx.getDefender()).thenReturn(defender);
        when(attackCtx.getDiscardEnergyInstanceIds()).thenReturn(List.of(energy1.getInstanceId()));

        PlayerState owner = new PlayerState();
        owner.setPlayerId(ownerId);
        when(ctx.getPlayer(ownerId)).thenReturn(owner);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DiscardEnergyEffect effect = new DiscardEnergyEffect(1, "defender", true);
        effect.apply(ctx, attackCtx);

        verify(energyService).detachEnergies(eq(defender), eq(owner),
                argThat(list -> list.size() == 1 && list.get(0).getInstanceId().equals(energy1.getInstanceId())),
                eq(ctx));
    }

    @Test
    void apply_emptyEnergyList_noAction() {
        PokemonInPlay defender = createPokemon(UUID.randomUUID(), new ArrayList<>());
        when(attackCtx.getDefender()).thenReturn(defender);

        DiscardEnergyEffect effect = new DiscardEnergyEffect(1);
        effect.apply(ctx, attackCtx);

        verify(energyService, never()).detachEnergies(any(), any(), any(), any());
    }

    @Test
    void getTiming_returnsAfterDamage() {
        DiscardEnergyEffect effect = new DiscardEnergyEffect(1);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }
}
