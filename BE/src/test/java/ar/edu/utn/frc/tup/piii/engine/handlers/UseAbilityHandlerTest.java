package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.AbilityType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityResolver;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UseAbilityHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private AbilityRegistry abilityRegistry;
    @Mock
    private AbilityResolver abilityResolver;

    private UseAbilityHandler handler;
    private UUID playerId;
    private UUID pokemonInstanceId;
    private PokemonInPlay pokemon;
    private PokemonCardDefinition pokemonDef;
    private AbilityDefinition abilityDef;

    @BeforeEach
    void setUp() {
        handler = new UseAbilityHandler(abilityRegistry);
        playerId = UUID.randomUUID();
        pokemonInstanceId = UUID.randomUUID();

        pokemon = new PokemonInPlay();
        pokemon.setInstanceId(pokemonInstanceId);
        pokemon.setCardDefinitionId("pkm-lunatone");
        pokemon.setSpecialConditions(new ArrayList<>());

        pokemonDef = new PokemonCardDefinition();
        abilityDef = new AbilityDefinition("Lunar Shield", "Prevents all damage", AbilityType.POKEMON_POWER);
        pokemonDef.setAbilities(List.of(abilityDef));

        pokemonDef.setAbilities(List.of(abilityDef));
    }

    private GameAction createAction(String pokemonIdStr, String abilityName) {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        if (pokemonIdStr != null) payload.put("pokemonInstanceId", pokemonIdStr);
        if (abilityName != null) payload.put("abilityName", abilityName);
        action.setPayload(payload);
        return action;
    }

    @Test
    void shouldUseAbility() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(3);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);

        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(pokemonDef);
        when(abilityRegistry.get("Lunar Shield")).thenReturn(abilityResolver);
        when(ctx.getError()).thenReturn(null);
        when(player.getPlayerId()).thenReturn(playerId);

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        verify(abilityResolver).resolve(eq(ctx), eq(player), eq(pokemon), eq(abilityDef), anyMap());
        assertTrue(pokemon.getAbilitiesUsedThisTurn().contains("Lunar Shield"));
    }

    @Test
    void shouldDoNothingWhenPokemonInstanceIdIsNull() {
        handler.handle(ctx, createAction(null, "Lunar Shield"));

        verifyNoInteractions(player, cardLookup, abilityRegistry);
    }

    @Test
    void shouldDoNothingWhenAbilityNameIsNull() {
        handler.handle(ctx, createAction(pokemonInstanceId.toString(), null));

        verifyNoInteractions(player, cardLookup, abilityRegistry);
    }

    @Test
    void shouldDoNothingWhenPokemonNotFound() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        verify(cardLookup, never()).getCardById(any());
    }

    @Test
    void shouldDoNothingWhenCardDefIsNotPokemon() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);

        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(null);

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        verify(abilityRegistry, never()).get(any());
    }

    @Test
    void shouldSetErrorWhenAbilityNotFoundOnCard() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);

        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(pokemonDef);

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "NonExistentAbility"));

        verify(ctx).setError(argThat(e -> "ABILITY_NOT_FOUND".equals(e.getCode())));
    }

    @Test
    void shouldSetErrorWhenAbilitiesSuppressed() {
        pokemon.setAbilitiesSuppressedNextTurn(true);

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);

        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(pokemonDef);

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        verify(ctx).setError(argThat(e -> "ABILITIES_SUPPRESSED".equals(e.getCode())));
    }

    @Test
    void shouldSetErrorWhenPokemonIsAsleep() {
        pokemon.getSpecialConditions().add(SpecialCondition.ASLEEP);

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);

        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(pokemonDef);

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        verify(ctx).setError(argThat(e -> "POKEMON_CANNOT_USE_ABILITY".equals(e.getCode())));
    }

    @Test
    void shouldSetErrorWhenPokemonIsParalyzed() {
        pokemon.getSpecialConditions().add(SpecialCondition.PARALYZED);

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);
        lenient().when(player.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(pokemonDef);

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        verify(ctx).setError(argThat(e -> "POKEMON_CANNOT_USE_ABILITY".equals(e.getCode())));
    }

    @Test
    void shouldSetErrorWhenAbilityAlreadyUsedThisTurn() {
        pokemon.getAbilitiesUsedThisTurn().add("Lunar Shield");

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);

        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(pokemonDef);

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        verify(ctx).setError(argThat(e -> "ABILITY_ALREADY_USED".equals(e.getCode())));
    }

    @Test
    void shouldSetErrorWhenNoResolverRegistered() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);
        lenient().when(player.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(pokemonDef);
        when(abilityRegistry.get("Lunar Shield")).thenReturn(null);

        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        verify(ctx).setError(argThat(e -> "ABILITY_NOT_FOUND".equals(e.getCode())));
    }

    @Test
    void shouldNotAddToUsedAbilitiesWhenResolverSetsError() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(pokemon);
        lenient().when(player.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-lunatone")).thenReturn(pokemonDef);
        when(abilityRegistry.get("Lunar Shield")).thenReturn(abilityResolver);
        when(ctx.getError()).thenReturn(new ar.edu.utn.frc.tup.piii.engine.action.GameError("SOME_ERROR", "Error"));
        when(player.getPlayerId()).thenReturn(playerId);
        when(ctx.getState()).thenReturn(state);
        handler.handle(ctx, createAction(pokemonInstanceId.toString(), "Lunar Shield"));

        assertFalse(pokemon.getAbilitiesUsedThisTurn().contains("Lunar Shield"));
    }
}


