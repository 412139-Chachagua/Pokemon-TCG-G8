package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealWithDiscardResolverTest {

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

    private HealWithDiscardResolver resolver;
    private PokemonInPlay target;
    private UUID targetId;
    private List<CardInstance> attachedEnergies;
    private List<CardInstance> discard;

    @BeforeEach
    void setUp() {
        resolver = new HealWithDiscardResolver();

        targetId = UUID.randomUUID();
        target = new PokemonInPlay();
        target.setInstanceId(targetId);
        target.setDamageCounters(8);

        attachedEnergies = new ArrayList<>();
        attachedEnergies.add(createEnergyCard("energy-0"));
        attachedEnergies.add(createEnergyCard("energy-1"));
        target.setAttachedEnergies(attachedEnergies);

        discard = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(player.getDiscard()).thenReturn(discard);
        when(player.getActivePokemon()).thenReturn(target);
        when(player.getBench()).thenReturn(new ArrayList<>());
    }

    @Test
    void resolve_shouldHealUpTo6CountersAndDiscardEnergyAtIndex() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());
        payload.put("energyIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, target.getDamageCounters());
        assertEquals(1, attachedEnergies.size());
        assertEquals("energy-1", attachedEnergies.get(0).getCardDefinitionId());
        assertEquals(1, discard.size());
        assertEquals("energy-0", discard.get(0).getCardDefinitionId());
    }

    @Test
    void resolve_shouldHealUpToDamageCountersWhenLessThan6() {
        target.setDamageCounters(3);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());
        payload.put("energyIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(0, target.getDamageCounters());
        assertEquals(1, attachedEnergies.size());
    }

    @Test
    void resolve_withTargetNotFound_shouldReturnEarly() {
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>());

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

        resolver.resolve(ctx, player, card, payload);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void resolve_withNoDamageToHeal_shouldReturnEarly() {
        target.setDamageCounters(0);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void resolve_withEnergyIndexOutOfBounds_shouldNotDiscardEnergy() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());
        payload.put("energyIndex", 10);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, target.getDamageCounters());
        assertEquals(2, attachedEnergies.size());
        assertTrue(discard.isEmpty());
    }

    @Test
    void resolve_withNegativeEnergyIndex_shouldNotDiscardEnergy() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());
        payload.put("energyIndex", -1);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, target.getDamageCounters());
        assertEquals(2, attachedEnergies.size());
        assertTrue(discard.isEmpty());
    }

    @Test
    void resolve_shouldHealMax6Counters() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, target.getDamageCounters());
    }

    @Test
    void resolve_shouldPublishHealEvent() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());
        payload.put("energyIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        verify(ctx, times(2)).addEvent(eventCaptor.capture());
        List<GameEvent> events = eventCaptor.getAllValues();

        GameEvent healEvent = events.get(0);
        assertEquals(GameEventType.POKEMON_HEALED.name(), healEvent.getType());
        assertEquals(targetId.toString(), healEvent.getPayload().get("pokemonInstanceId"));
        assertEquals(6, healEvent.getPayload().get("healed"));
    }

    @Test
    void resolve_shouldPublishEnergyDiscardedEvent() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());
        payload.put("energyIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        verify(ctx, times(2)).addEvent(eventCaptor.capture());
        List<GameEvent> events = eventCaptor.getAllValues();

        GameEvent energyEvent = events.get(1);
        assertEquals(GameEventType.ENERGY_DISCARDED.name(), energyEvent.getType());
        assertEquals(1, energyEvent.getPayload().get("count"));
        assertEquals(targetId.toString(), energyEvent.getPayload().get("pokemonInstanceId"));
    }

    @Test
    void resolve_shouldUseDefaultEnergyIndex0() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, target.getDamageCounters());
        assertEquals(1, attachedEnergies.size());
        assertEquals("energy-1", attachedEnergies.get(0).getCardDefinitionId());
    }

    @Test
    void resolve_withNullAttachedEnergies_shouldSkipDiscard() {
        target.setAttachedEnergies(null);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", targetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, target.getDamageCounters());
        assertTrue(discard.isEmpty());
    }

    @Test
    void getType_shouldReturnHEAL_WITH_DISCARD() {
        assertEquals(EffectType.HEAL_WITH_DISCARD, resolver.getType());
    }

    private CardInstance createEnergyCard(String cardDefId) {
        CardInstance ci = new CardInstance();
        ci.setInstanceId(UUID.randomUUID());
        ci.setCardDefinitionId(cardDefId);
        return ci;
    }
}
