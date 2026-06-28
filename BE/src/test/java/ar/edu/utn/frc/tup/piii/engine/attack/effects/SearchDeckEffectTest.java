package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchDeckEffectTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private AttackContext attackCtx;
    @Mock
    private GameState state;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private RandomizerPort randomizer;

    private PokemonInPlay createPokemon(UUID ownerId) {
        PokemonInPlay p = new PokemonInPlay();
        p.setInstanceId(UUID.randomUUID());
        p.setOwnerPlayerId(ownerId);
        return p;
    }

    private PlayerState createPlayer(UUID playerId) {
        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setDeck(new ArrayList<>());
        player.setHand(new ArrayList<>());
        return player;
    }

    @Test
    void apply_searchAny_returnsAllCards() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        CardInstance card1 = new CardInstance(UUID.randomUUID(), "card-1");
        CardInstance card2 = new CardInstance(UUID.randomUUID(), "card-2");
        player.getDeck().add(card1);
        player.getDeck().add(card2);

        CardDefinition def = mock(CardDefinition.class);
        when(cardLookup.getCardById(anyString())).thenReturn(def);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SearchDeckEffect effect = new SearchDeckEffect("ANY", 2);
        effect.apply(ctx, attackCtx);

        assertEquals(0, player.getDeck().size());
        assertEquals(2, player.getHand().size());
        verify(randomizer).shuffle(anyList());
    }

    @Test
    void apply_searchEnergy_returnsOnlyEnergyCards() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        CardInstance energyCard = new CardInstance(UUID.randomUUID(), "energy-1");
        CardInstance otherCard = new CardInstance(UUID.randomUUID(), "other-1");
        player.getDeck().add(energyCard);
        player.getDeck().add(otherCard);

        EnergyCardDefinition energyDef = new EnergyCardDefinition();
        energyDef.setEnergyCardType(EnergyCardType.BASIC);
        energyDef.setProvides(List.of(EnergyType.FIRE));
        CardDefinition otherDef = mock(CardDefinition.class);

        when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
        when(cardLookup.getCardById("other-1")).thenReturn(otherDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SearchDeckEffect effect = new SearchDeckEffect("ENERGY", "FIRE", 1);
        effect.apply(ctx, attackCtx);

        assertEquals(1, player.getDeck().size());
        assertEquals(1, player.getHand().size());
        assertEquals(energyCard, player.getHand().get(0));
    }

    @Test
    void apply_searchSupporter_returnsOnlySupporterCards() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        CardInstance supporterCard = new CardInstance(UUID.randomUUID(), "sup-1");
        CardInstance otherCard = new CardInstance(UUID.randomUUID(), "other-1");
        player.getDeck().add(supporterCard);
        player.getDeck().add(otherCard);

        CardDefinition supporterDef = mock(CardDefinition.class);
        when(supporterDef.getSupertype()).thenReturn("TRAINER");
        when(supporterDef.getSubtypes()).thenReturn(List.of("SUPPORTER"));
        CardDefinition otherDef = mock(CardDefinition.class);
        when(otherDef.getSupertype()).thenReturn("POKEMON");

        when(cardLookup.getCardById("sup-1")).thenReturn(supporterDef);
        when(cardLookup.getCardById("other-1")).thenReturn(otherDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SearchDeckEffect effect = new SearchDeckEffect("SUPPORTER", 1);
        effect.apply(ctx, attackCtx);

        assertEquals(1, player.getDeck().size());
        assertEquals(1, player.getHand().size());
    }

    @Test
    void apply_searchBasicPokemon_returnsOnlyBasicPokemon() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        CardInstance basic = new CardInstance(UUID.randomUUID(), "basic-1");
        player.getDeck().add(basic);

        PokemonCardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setSupertype("POKEMON");
        basicDef.setSubtypes(List.of("BASIC"));
        basicDef.setTypes(List.of(EnergyType.COLORLESS));

        when(cardLookup.getCardById("basic-1")).thenReturn(basicDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SearchDeckEffect effect = new SearchDeckEffect("BASIC_POKEMON", 1);
        effect.apply(ctx, attackCtx);

        assertEquals(0, player.getDeck().size());
        assertEquals(1, player.getHand().size());
    }

    @Test
    void apply_searchPokemonByType_returnsMatchingType() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        CardInstance firePkm = new CardInstance(UUID.randomUUID(), "fire-1");
        CardInstance waterPkm = new CardInstance(UUID.randomUUID(), "water-1");
        player.getDeck().add(firePkm);
        player.getDeck().add(waterPkm);

        PokemonCardDefinition fireDef = new PokemonCardDefinition();
        fireDef.setSupertype("POKEMON");
        fireDef.setTypes(List.of(EnergyType.FIRE));
        PokemonCardDefinition waterDef = new PokemonCardDefinition();
        waterDef.setSupertype("POKEMON");
        waterDef.setTypes(List.of(EnergyType.WATER));

        when(cardLookup.getCardById("fire-1")).thenReturn(fireDef);
        when(cardLookup.getCardById("water-1")).thenReturn(waterDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SearchDeckEffect effect = new SearchDeckEffect("POKEMON", null, 1, "FIRE");
        effect.apply(ctx, attackCtx);

        assertEquals(1, player.getDeck().size());
        assertEquals(1, player.getHand().size());
        assertEquals(firePkm, player.getHand().get(0));
    }

    @Test
    void apply_noMatchesInDeck_returnsEmpty() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        CardInstance card = new CardInstance(UUID.randomUUID(), "card-1");
        player.getDeck().add(card);

        CardDefinition def = mock(CardDefinition.class);
        when(cardLookup.getCardById("card-1")).thenReturn(def);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});

        SearchDeckEffect effect = new SearchDeckEffect("SUPPORTER", 1);
        effect.apply(ctx, attackCtx);

        assertEquals(1, player.getDeck().size());
        assertEquals(0, player.getHand().size());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_emptyDeck_noAction() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});

        SearchDeckEffect effect = new SearchDeckEffect("ANY", 1);
        effect.apply(ctx, attackCtx);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_nullCardDefinitions_skipped() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "null-card"));

        when(cardLookup.getCardById("null-card")).thenReturn(null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});

        SearchDeckEffect effect = new SearchDeckEffect("ANY", 1);
        effect.apply(ctx, attackCtx);

        assertEquals(1, player.getDeck().size());
        assertEquals(0, player.getHand().size());
    }

    @Test
    void apply_moreMatchesThanCount_limitsResults() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-2"));
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-3"));

        when(cardLookup.getCardById(anyString())).thenReturn(mock(CardDefinition.class));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SearchDeckEffect effect = new SearchDeckEffect("ANY", 2);
        effect.apply(ctx, attackCtx);

        assertEquals(1, player.getDeck().size());
        assertEquals(2, player.getHand().size());
    }

    @Test
    void apply_firesSearchedEvent() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));

        when(cardLookup.getCardById("card-1")).thenReturn(mock(CardDefinition.class));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SearchDeckEffect effect = new SearchDeckEffect("ANY", 1);
        effect.apply(ctx, attackCtx);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(captor.capture());
        assertEquals(GameEventType.POKEMON_SEARCHED.name(), captor.getValue().getType());
    }

    @Test
    void getTiming_returnsAfterDamage() {
        SearchDeckEffect effect = new SearchDeckEffect("ANY", 1);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }
}
