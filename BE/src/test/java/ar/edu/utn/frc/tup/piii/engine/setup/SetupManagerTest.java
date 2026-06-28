package ar.edu.utn.frc.tup.piii.engine.setup;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.DeckLoadPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.domain.decks.Deck;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckCard;
import static org.mockito.ArgumentMatchers.anyString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupManagerTest {

    @Mock private DeckLoadPort deckLoadPort;
    @Mock private CardLookupPort cardLookupPort;
    @Mock private RandomizerPort randomizerPort;
    @Mock private EventPublisherPort eventPublisher;

    private SetupManager setupManager;
    private UUID matchId;
    private UUID p1Id;
    private UUID p2Id;
    private UUID deck1Id;
    private UUID deck2Id;

    @BeforeEach
    void setUp() {
        setupManager = new SetupManager(deckLoadPort, cardLookupPort, randomizerPort, eventPublisher);
        matchId = UUID.randomUUID();
        p1Id = UUID.randomUUID();
        p2Id = UUID.randomUUID();
        deck1Id = UUID.randomUUID();
        deck2Id = UUID.randomUUID();
    }

    private DeckCard createDeckCard(String cardId, int quantity) {
        DeckCard dc = new DeckCard();
        dc.setCardId(cardId);
        dc.setQuantity(quantity);
        return dc;
    }

    @Test
    void shouldSetupWithCustomPrizeCount() {
        Deck bigDeck = new Deck();
        bigDeck.setCards(List.of(createDeckCard("basic-1", 60)));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(bigDeck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(bigDeck);

        CardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setName("Basic Pokemon");
        ((PokemonCardDefinition) basicDef).setStage("BASIC");
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicDef);

        var state = setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, 1, "Player 1", "Player 2");

        assertNotNull(state);
        assertTrue(state.isSuddenDeath());
        assertEquals(1, state.getPrizeCountPerPlayer());
        assertEquals(1, state.getPlayers()[0].getPrizes().size());
        assertEquals(1, state.getPlayers()[1].getPrizes().size());
        assertEquals(matchId, state.getMatchId());
    }

    @Test
    void shouldSetupWithDefault6Prizes() {
        Deck bigDeck = new Deck();
        bigDeck.setCards(List.of(createDeckCard("basic-1", 60)));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(bigDeck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(bigDeck);

        CardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setName("Basic Pokemon");
        ((PokemonCardDefinition) basicDef).setStage("BASIC");
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicDef);

        var state = setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, "Player 1", "Player 2");

        assertNotNull(state);
        assertFalse(state.isSuddenDeath());
        assertEquals(6, state.getPrizeCountPerPlayer());
        assertEquals(6, state.getPlayers()[0].getPrizes().size());
        assertEquals(6, state.getPlayers()[1].getPrizes().size());
    }

    @Test
    void shouldHandleCustomHandSize() {
        Deck bigDeck = new Deck();
        bigDeck.setCards(List.of(createDeckCard("basic-1", 60)));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(bigDeck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(bigDeck);

        CardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setName("Basic Pokemon");
        ((PokemonCardDefinition) basicDef).setStage("BASIC");
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicDef);

        var state = setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, 6, "Player 1", "Player 2", 10);

        assertNotNull(state);
        assertEquals(10, state.getPlayers()[0].getHand().size());
        assertEquals(10, state.getPlayers()[1].getHand().size());
    }

    @Test
    void shouldThrowWhenDeckHasLessThan60Cards() {
        Deck smallDeck = new Deck();
        smallDeck.setCards(List.of(createDeckCard("basic-1", 30)));
        Deck bigDeck = new Deck();
        bigDeck.setCards(List.of(createDeckCard("basic-1", 60)));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(smallDeck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(bigDeck);

        assertThrows(RuntimeException.class, () ->
                setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, "Player 1", "Player 2"));
    }

    @Test
    void shouldThrowWhenDeckCardDefinitionNotFound() {
        Deck deck = new Deck();
        deck.setCards(List.of(createDeckCard("missing-card", 60)));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(deck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(deck);

        assertThrows(RuntimeException.class, () ->
                setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, "Player 1", "Player 2"));
    }

    @Test
    void shouldThrowWhenDeckHasNoBasicPokemon() {
        Deck deck = new Deck();
        deck.setCards(List.of(createDeckCard("stage1-pokemon", 60)));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(deck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(deck);
        CardDefinition stage1 = new PokemonCardDefinition();
        stage1.setName("Stage 1 Pokemon");
        ((PokemonCardDefinition) stage1).setStage("STAGE1");
        when(cardLookupPort.getCardById("stage1-pokemon")).thenReturn(stage1);

        assertThrows(RuntimeException.class, () ->
                setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, "Player 1", "Player 2"));
    }

    @Test
    void shouldTriggerMulliganWhenPlayerHandLacksBasic() {
        Deck mixedDeck = new Deck();
        mixedDeck.setCards(List.of(
                createDeckCard("trainer-1", 7),
                createDeckCard("basic-1", 53)
        ));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(mixedDeck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(mixedDeck);

        CardDefinition trainerDef = mock(CardDefinition.class);
        CardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setName("Basic Pokemon");
        ((PokemonCardDefinition) basicDef).setStage("BASIC");
        when(cardLookupPort.getCardById("trainer-1")).thenReturn(trainerDef);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicDef);
        doNothing().when(randomizerPort).shuffle(any());

        var state = setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, 6, "Player 1", "Player 2");

        assertNotNull(state);
        assertNotNull(state.getPendingInitialMulliganPlayers());
        assertFalse(state.getPendingInitialMulliganPlayers().isEmpty());
        assertNull(state.getPlayers()[0].getPrizes());
        assertNull(state.getPlayers()[1].getPrizes());
    }

    @Test
    void shouldResolveBothMulliganWhenBothLackBasic() {
        Deck nonBasicDeck = new Deck();
        nonBasicDeck.setCards(List.of(
                createDeckCard("trainer-1", 7),
                createDeckCard("basic-1", 53)
        ));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(nonBasicDeck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(nonBasicDeck);

        CardDefinition trainerDef = mock(CardDefinition.class);
        CardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setName("Basic Pokemon");
        ((PokemonCardDefinition) basicDef).setStage("BASIC");
        when(cardLookupPort.getCardById("trainer-1")).thenReturn(trainerDef);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicDef);
        doNothing().when(randomizerPort).shuffle(any());

        var state = setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, 6, "Player 1", "Player 2");

        assertNotNull(state);
        assertNotNull(state.getPendingInitialMulliganPlayers());
        assertEquals(2, state.getPendingInitialMulliganPlayers().size());
    }

    @Test
    void revealAllPokemon_shouldHandleNullBench() {
        GameState state = new GameState();
        PlayerState p1 = new PlayerState();
        PokemonInPlay active = new PokemonInPlay();
        active.setFaceDown(true);
        p1.setActivePokemon(active);
        p1.setBench(null);
        PlayerState p2 = new PlayerState();
        p2.setActivePokemon(null);
        p2.setBench(null);
        state.setPlayers(new PlayerState[]{p1, p2});

        SetupManager.revealAllPokemon(state);

        assertFalse(p1.getActivePokemon().isFaceDown());
    }

    @Test
    void revealAllPokemon_shouldMakeBenchPokemonFaceUp() {
        GameState state = new GameState();
        PlayerState p1 = new PlayerState();
        PokemonInPlay active = new PokemonInPlay();
        active.setFaceDown(true);
        p1.setActivePokemon(active);
        PokemonInPlay bench1 = new PokemonInPlay();
        bench1.setFaceDown(true);
        p1.setBench(new ArrayList<>(List.of(bench1)));
        PlayerState p2 = new PlayerState();
        p2.setActivePokemon(null);
        p2.setBench(new ArrayList<>());
        state.setPlayers(new PlayerState[]{p1, p2});

        SetupManager.revealAllPokemon(state);

        assertFalse(p1.getActivePokemon().isFaceDown());
        assertFalse(bench1.isFaceDown());
    }

    @Test
    void shouldReturnStateWithMatchStatusSetup() {
        Deck bigDeck = new Deck();
        bigDeck.setCards(List.of(createDeckCard("basic-1", 60)));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(bigDeck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(bigDeck);
        CardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setName("Basic Pokemon");
        ((PokemonCardDefinition) basicDef).setStage("BASIC");
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicDef);

        var state = setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, 6, "Player 1", "Player 2");

        assertEquals(MatchStatus.SETUP, state.getStatus());
        assertNotNull(state.getTurnFlags());
        assertNotNull(state.getPlayers()[0].getHand());
        assertNotNull(state.getPlayers()[1].getHand());
    }

    @Test
    void shouldCallEventPublisherOnMulligan() {
        Deck mixedDeck = new Deck();
        mixedDeck.setCards(List.of(
                createDeckCard("trainer-1", 7),
                createDeckCard("basic-1", 53)
        ));

        when(deckLoadPort.loadDeck(deck1Id)).thenReturn(mixedDeck);
        when(deckLoadPort.loadDeck(deck2Id)).thenReturn(mixedDeck);

        CardDefinition trainerDef = mock(CardDefinition.class);
        CardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setName("Basic Pokemon");
        ((PokemonCardDefinition) basicDef).setStage("BASIC");
        when(cardLookupPort.getCardById("trainer-1")).thenReturn(trainerDef);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicDef);
        doNothing().when(randomizerPort).shuffle(any());

        setupManager.setup(matchId, p1Id, p2Id, deck1Id, deck2Id, 6, "Player 1", "Player 2");

        verify(eventPublisher, atLeastOnce()).publishEvents(eq(matchId), anyList());
    }
}
