package ar.edu.utn.frc.tup.piii.engine.trainer;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerEffectRegistryTest {

    private TrainerEffectRegistry registry;

    @Mock
    private TrainerEffectResolver mockResolver;
    @Mock
    private EngineContext ctx;
    @Mock
    private PlayerState player;
    @Mock
    private TrainerCardDefinition card;

    @BeforeEach
    void setUp() {
        registry = new TrainerEffectRegistry();
    }

    @Test
    void registerEffectCodeAndGetEffectType_shouldReturnRegisteredType() {
        registry.registerEffectCode("draw3", EffectType.DRAW_CARDS);
        assertEquals(EffectType.DRAW_CARDS, registry.getEffectType("draw3"));
    }

    @Test
    void registerEffectCodeAndGetEffectType_withDifferentCode_shouldReturnCorrectType() {
        registry.registerEffectCode("heal40", EffectType.HEAL);
        registry.registerEffectCode("draw3", EffectType.DRAW_CARDS);
        assertEquals(EffectType.HEAL, registry.getEffectType("heal40"));
        assertEquals(EffectType.DRAW_CARDS, registry.getEffectType("draw3"));
    }

    @Test
    void getEffectType_withUnknownCode_shouldReturnNull() {
        assertNull(registry.getEffectType("unknown"));
    }

    @Test
    void registerResolver_shouldStoreResolver() {
        when(mockResolver.getType()).thenReturn(EffectType.DRAW_CARDS);
        registry.registerResolver(mockResolver);
    }

    @Test
    void isEffectCodeKnown_withRegisteredCode_shouldReturnTrue() {
        registry.registerEffectCode("draw3", EffectType.DRAW_CARDS);
        assertTrue(registry.isEffectCodeKnown("draw3"));
    }

    @Test
    void isEffectCodeKnown_withUnknownCode_shouldReturnFalse() {
        assertFalse(registry.isEffectCodeKnown("unknown"));
    }

    @Test
    void isEffectCodeKnown_withNullCode_shouldReturnFalse() {
        assertFalse(registry.isEffectCodeKnown(null));
    }

    @Test
    void getRequiredTargetKeys_forHeal_shouldReturnTargetPokemonInstanceId() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.HEAL);
        assertEquals(List.of("targetPokemonInstanceId"), keys);
    }

    @Test
    void getRequiredTargetKeys_forConditionRemove_shouldReturnTargetPokemonInstanceId() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.CONDITION_REMOVE);
        assertEquals(List.of("targetPokemonInstanceId"), keys);
    }

    @Test
    void getRequiredTargetKeys_forSwitchPokemon_shouldReturnTargetPokemonInstanceId() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.SWITCH_POKEMON);
        assertEquals(List.of("targetPokemonInstanceId"), keys);
    }

    @Test
    void getRequiredTargetKeys_forSearchBasicPokemon_shouldReturnTargetCardIndex() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.SEARCH_BASIC_POKEMON);
        assertEquals(List.of("targetCardIndex"), keys);
    }

    @Test
    void getRequiredTargetKeys_forSearchEnergy_shouldReturnTargetCardIndex() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.SEARCH_ENERGY);
        assertEquals(List.of("targetCardIndex"), keys);
    }

    @Test
    void getRequiredTargetKeys_forEvolveSearch_shouldReturnTargetCardIndex() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.EVOLVE_SEARCH);
        assertEquals(List.of("targetCardIndex"), keys);
    }

    @Test
    void getRequiredTargetKeys_forRevive_shouldReturnTargetCardIndex() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.REVIVE);
        assertEquals(List.of("targetCardIndex"), keys);
    }

    @Test
    void getRequiredTargetKeys_forAttachExtraEnergy_shouldReturnTargetPokemonInstanceIdAndHandIndex() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.ATTACH_EXTRA_ENERGY);
        assertEquals(List.of("targetPokemonInstanceId", "handIndex"), keys);
    }

    @Test
    void getRequiredTargetKeys_forDamageModify_shouldReturnTargetPokemonInstanceId() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.DAMAGE_MODIFY);
        assertEquals(List.of("targetPokemonInstanceId"), keys);
    }

    @Test
    void getRequiredTargetKeys_forToolAttach_shouldReturnTargetPokemonInstanceIdAndHandIndex() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.TOOL_ATTACH);
        assertEquals(List.of("targetPokemonInstanceId", "handIndex"), keys);
    }

    @Test
    void getRequiredTargetKeys_forStadiumPlay_shouldReturnEmpty() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.STADIUM_PLAY);
        assertTrue(keys.isEmpty());
    }

    @Test
    void getRequiredTargetKeys_forDrawCards_shouldReturnEmpty() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.DRAW_CARDS);
        assertTrue(keys.isEmpty());
    }

    @Test
    void getRequiredTargetKeys_forDiscardAndDraw_shouldReturnEmpty() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.DISCARD_AND_DRAW);
        assertTrue(keys.isEmpty());
    }

    @Test
    void getRequiredTargetKeys_forShuffleHandIntoDeck_shouldReturnEmpty() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.SHUFFLE_HAND_INTO_DECK);
        assertTrue(keys.isEmpty());
    }

    @Test
    void getRequiredTargetKeys_forEvolveDirect_shouldReturnTargetPokemonInstanceId() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.EVOLVE_DIRECT);
        assertEquals(List.of("targetPokemonInstanceId"), keys);
    }

    @Test
    void getRequiredTargetKeys_forLookTopSearch_shouldReturnTargetCardIndex() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.LOOK_TOP_SEARCH);
        assertEquals(List.of("targetCardIndex"), keys);
    }

    @Test
    void getRequiredTargetKeys_forReviveToDeck_shouldReturnTargetCardIndex() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.REVIVE_TO_DECK);
        assertEquals(List.of("targetCardIndex"), keys);
    }

    @Test
    void getRequiredTargetKeys_forSearchEnergyToHand_shouldReturnTargetCardIndexes() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.SEARCH_ENERGY_TO_HAND);
        assertEquals(List.of("targetCardIndexes"), keys);
    }

    @Test
    void getRequiredTargetKeys_forOpponentShuffleHandDraw_shouldReturnEmpty() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.OPPONENT_SHUFFLE_HAND_DRAW);
        assertTrue(keys.isEmpty());
    }

    @Test
    void getRequiredTargetKeys_forCoinFlipDraw_shouldReturnEmpty() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.COIN_FLIP_DRAW);
        assertTrue(keys.isEmpty());
    }

    @Test
    void getRequiredTargetKeys_forHealWithDiscard_shouldReturnTargetPokemonInstanceId() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.HEAL_WITH_DISCARD);
        assertEquals(List.of("targetPokemonInstanceId"), keys);
    }

    @Test
    void getRequiredTargetKeys_forReturnPokemonToDeck_shouldReturnTargetPokemonInstanceId() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.RETURN_POKEMON_TO_DECK);
        assertEquals(List.of("targetPokemonInstanceId"), keys);
    }

    @Test
    void getRequiredTargetKeys_forDiscardOpponentEnergy_shouldReturnTargetPokemonInstanceId() {
        List<String> keys = registry.getRequiredTargetKeys(EffectType.DISCARD_OPPONENT_ENERGY);
        assertEquals(List.of("targetPokemonInstanceId"), keys);
    }

    @Test
    void resolve_withNullEffectCode_shouldReturnEarly() {
        when(card.getEffectCode()).thenReturn(null);

        registry.resolve(ctx, player, card, new HashMap<>());

        verifyNoInteractions(ctx);
    }

    @Test
    void resolve_withUnknownEffectCode_shouldReturnEarly() {
        when(card.getEffectCode()).thenReturn("unknown");

        registry.resolve(ctx, player, card, new HashMap<>());

        verifyNoInteractions(ctx);
    }

    @Test
    void resolve_withNoResolverRegistered_shouldReturnEarly() {
        registry.registerEffectCode("draw3", EffectType.DRAW_CARDS);
        when(card.getEffectCode()).thenReturn("draw3");

        registry.resolve(ctx, player, card, new HashMap<>());

        verifyNoInteractions(ctx);
    }

    @Test
    void resolve_withRegisteredResolver_shouldDelegate() {
        registry.registerEffectCode("draw3", EffectType.DRAW_CARDS);
        when(mockResolver.getType()).thenReturn(EffectType.DRAW_CARDS);
        registry.registerResolver(mockResolver);
        when(card.getEffectCode()).thenReturn("draw3");

        Map<String, Object> payload = new HashMap<>();
        payload.put("count", 3);
        registry.resolve(ctx, player, card, payload);

        verify(mockResolver).resolve(ctx, player, card, payload);
    }

    @Test
    void resolve_withMultipleResolvers_shouldUseCorrectOne() {
        TrainerEffectResolver healResolver = mock(TrainerEffectResolver.class);
        when(healResolver.getType()).thenReturn(EffectType.HEAL);

        registry.registerEffectCode("heal40", EffectType.HEAL);
        registry.registerEffectCode("draw3", EffectType.DRAW_CARDS);
        when(mockResolver.getType()).thenReturn(EffectType.DRAW_CARDS);

        registry.registerResolver(healResolver);
        registry.registerResolver(mockResolver);
        when(card.getEffectCode()).thenReturn("heal40");

        Map<String, Object> payload = new HashMap<>();
        registry.resolve(ctx, player, card, payload);

        verify(healResolver).resolve(ctx, player, card, payload);
        verify(mockResolver, never()).resolve(any(), any(), any(), any());
    }

    @Test
    void resolve_withPayload_shouldPassPayloadToResolver() {
        registry.registerEffectCode("heal40", EffectType.HEAL);
        when(mockResolver.getType()).thenReturn(EffectType.HEAL);
        registry.registerResolver(mockResolver);
        when(card.getEffectCode()).thenReturn("heal40");

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", "uuid");
        payload.put("count", 40);
        registry.resolve(ctx, player, card, payload);

        verify(mockResolver).resolve(ctx, player, card, payload);
    }
}
