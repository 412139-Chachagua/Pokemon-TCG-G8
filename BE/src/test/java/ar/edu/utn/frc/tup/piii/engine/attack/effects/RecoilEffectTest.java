package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoilEffectTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private AttackContext attackCtx;
    @Mock
    private GameState state;

    @Test
    void apply_recoilDamage_addsCounters() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setDamageCounters(3);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        RecoilEffect effect = new RecoilEffect(2);
        effect.apply(ctx, attackCtx);

        assertEquals(5, attacker.getDamageCounters());
    }

    @Test
    void apply_zeroRecoil_noChange() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setDamageCounters(3);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        RecoilEffect effect = new RecoilEffect(0);
        effect.apply(ctx, attackCtx);

        assertEquals(3, attacker.getDamageCounters());
    }

    @Test
    void apply_highRecoil_addsManyCounters() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setDamageCounters(0);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        RecoilEffect effect = new RecoilEffect(10);
        effect.apply(ctx, attackCtx);

        assertEquals(10, attacker.getDamageCounters());
    }

    @Test
    void apply_firesRecoilEvent() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setDamageCounters(0);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        RecoilEffect effect = new RecoilEffect(3);
        effect.apply(ctx, attackCtx);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(captor.capture());
        GameEvent event = captor.getValue();
        assertEquals(GameEventType.RECOIL_OCCURRED.name(), event.getType());
        assertEquals(30, event.getPayload().get("damage"));
        assertEquals(3, event.getPayload().get("damageCounters"));
    }

    @Test
    void getTiming_returnsAfterDamage() {
        RecoilEffect effect = new RecoilEffect(1);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }
}
