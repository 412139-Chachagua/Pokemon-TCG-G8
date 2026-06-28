package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealResolverTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private PlayerState player;
    @Mock
    private TrainerCardDefinition card;
    @Mock
    private GameState state;

    @Captor
    private ArgumentCaptor<GameEvent> eventCaptor;

    private HealResolver resolver;
    private PokemonInPlay target;
    private UUID targetId;
    private Map<String, Integer> effectHealCounts;

    @BeforeEach
    void setUp() {
        effectHealCounts = new HashMap<>();
        effectHealCounts.put("heal40", 4);
        resolver = new HealResolver(effectHealCounts);

        targetId = UUID.randomUUID();
        target = new PokemonInPlay();
        target.setInstanceId(targetId);
        target.setDamageCounters(6);
        target.setAttachedEnergies(new ArrayList<>());
        target.setSpecialConditions(new ArrayList<>());

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
    }

    @Test
    void resolve_shouldHealSpecifiedCountersFromEffectCode() {
        when(card.getEffectCode()).thenReturn("heal40");
        when(player.getActivePokemon()).thenReturn(target);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, target.getDamageCounters());
    }

    @Test
    void resolve_shouldHealCountFromPayloadOverridingConfig() {
        when(card.getEffectCode()).thenReturn("heal40");
        when(player.getActivePokemon()).thenReturn(target);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());
        payload.put("count", 2);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(4, target.getDamageCounters());
    }

    @Test
    void resolve_shouldHealMoreThanCurrentDamage() {
        when(card.getEffectCode()).thenReturn("heal40");
        target.setDamageCounters(2);
        when(player.getActivePokemon()).thenReturn(target);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertEquals(0, target.getDamageCounters());
    }

    @Test
    void resolve_shouldHealMoreThanCurrentDamageAndRecordCorrectCountersRemoved() {
        when(card.getEffectCode()).thenReturn("heal40");
        target.setDamageCounters(2);
        when(player.getActivePokemon()).thenReturn(target);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        verify(ctx).addEvent(eventCaptor.capture());
        assertEquals(2, eventCaptor.getValue().getPayload().get("countersRemoved"));
        assertEquals(0, ((Number) eventCaptor.getValue().getPayload().get("remainingDamageCounters")).intValue());
    }

    @Test
    void resolve_withNoDamageToHeal_shouldStillPublishEventWithZero() {
        when(card.getEffectCode()).thenReturn("heal40");
        target.setDamageCounters(0);
        when(player.getActivePokemon()).thenReturn(target);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertEquals(0, target.getDamageCounters());
        verify(ctx).addEvent(eventCaptor.capture());
        assertEquals(0, eventCaptor.getValue().getPayload().get("countersRemoved"));
    }

    @Test
    void resolve_shouldUseDefaultWhenEffectCodeNotConfigured() {
        when(card.getEffectCode()).thenReturn("unknown");
        target.setDamageCounters(5);
        when(player.getActivePokemon()).thenReturn(target);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertEquals(3, target.getDamageCounters());
    }

    @Test
    void resolve_withNullTargetPokemonInstanceId_shouldReturnEarly() {
        when(card.getEffectCode()).thenReturn("heal40");
        Map<String, Object> payload = new HashMap<>();

        resolver.resolve(ctx, player, card, payload);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void resolve_withTargetNotFound_shouldReturnEarly() {
        when(card.getEffectCode()).thenReturn("heal40");
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>());
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void resolve_shouldPublishHealEvent() {
        when(card.getEffectCode()).thenReturn("heal40");
        when(player.getActivePokemon()).thenReturn(target);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        verify(ctx).addEvent(eventCaptor.capture());
        GameEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.POKEMON_HEALED.name(), event.getType());
        assertEquals(targetId.toString(), event.getPayload().get("targetPokemonInstanceId"));
        assertEquals(4, event.getPayload().get("countersRemoved"));
        assertEquals(2, event.getPayload().get("remainingDamageCounters"));
    }

    @Test
    void resolve_withBenchTarget_shouldHealBenchPokemon() {
        PokemonInPlay benchTarget = new PokemonInPlay();
        UUID benchId = UUID.randomUUID();
        benchTarget.setInstanceId(benchId);
        benchTarget.setDamageCounters(5);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>(java.util.List.of(benchTarget)));
        when(card.getEffectCode()).thenReturn("heal40");
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", benchId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertEquals(1, benchTarget.getDamageCounters());
    }

    @Test
    void getType_shouldReturnHEAL() {
        assertEquals(EffectType.HEAL, resolver.getType());
    }
}
