package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
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
class DrawCardsEffectTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private AttackContext attackCtx;
    @Mock
    private GameState state;

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
    void apply_draw1Card_drawsFromDeck() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        CardInstance card = new CardInstance(UUID.randomUUID(), "card-1");
        player.getDeck().add(card);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DrawCardsEffect effect = new DrawCardsEffect(1);
        effect.apply(ctx, attackCtx);

        assertEquals(0, player.getDeck().size());
        assertEquals(1, player.getHand().size());
        assertEquals(card, player.getHand().get(0));
    }

    @Test
    void apply_draw3Cards_drawsMultiple() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-2"));
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-3"));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DrawCardsEffect effect = new DrawCardsEffect(3);
        effect.apply(ctx, attackCtx);

        assertEquals(0, player.getDeck().size());
        assertEquals(3, player.getHand().size());
    }

    @Test
    void apply_drawMoreThanDeckSize_drawsOnlyAvailable() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DrawCardsEffect effect = new DrawCardsEffect(5);
        effect.apply(ctx, attackCtx);

        assertEquals(0, player.getDeck().size());
        assertEquals(1, player.getHand().size());
    }

    @Test
    void apply_emptyDeck_drawsZero() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});

        DrawCardsEffect effect = new DrawCardsEffect(1);
        effect.apply(ctx, attackCtx);

        assertEquals(0, player.getHand().size());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_playerNotFound_noAction() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState other = createPlayer(UUID.randomUUID());
        other.setActivePokemon(createPokemon(other.getPlayerId()));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{other});

        DrawCardsEffect effect = new DrawCardsEffect(1);
        effect.apply(ctx, attackCtx);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_firesCardDrawnEvent() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = createPlayer(playerId);
        player.setActivePokemon(attacker);
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, createPlayer(UUID.randomUUID())});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DrawCardsEffect effect = new DrawCardsEffect(1);
        effect.apply(ctx, attackCtx);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(captor.capture());
        assertEquals(GameEventType.CARDS_DRAWN.name(), captor.getValue().getType());
        assertEquals(1, captor.getValue().getPayload().get("count"));
    }

    @Test
    void getTiming_returnsAfterDamage() {
        DrawCardsEffect effect = new DrawCardsEffect(1);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }
}
