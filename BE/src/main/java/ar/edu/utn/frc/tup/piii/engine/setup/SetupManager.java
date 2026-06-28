package ar.edu.utn.frc.tup.piii.engine.setup;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.decks.Deck;
import ar.edu.utn.frc.tup.piii.engine.PlayerSide;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.DeckLoadPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SetupManager {

    private final DeckLoadPort deckLoadPort;
    private final CardLookupPort cardLookupPort;
    private final RandomizerPort randomizerPort;
    private final EventPublisherPort eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(SetupManager.class);

    public SetupManager(DeckLoadPort deckLoadPort, CardLookupPort cardLookupPort, RandomizerPort randomizerPort, EventPublisherPort eventPublisher) {
        this.deckLoadPort = deckLoadPort;
        this.cardLookupPort = cardLookupPort;
        this.randomizerPort = randomizerPort;
        this.eventPublisher = eventPublisher;
    }

    public GameState setup(UUID matchId, UUID playerOneId, UUID playerTwoId, UUID deckOneId, UUID deckTwoId,
                           String playerOneName, String playerTwoName) {
        return setup(matchId, playerOneId, playerTwoId, deckOneId, deckTwoId, 6, playerOneName, playerTwoName);
    }

    public GameState setup(UUID matchId, UUID playerOneId, UUID playerTwoId, UUID deckOneId, UUID deckTwoId,
                           int prizeCount, String playerOneName, String playerTwoName) {
        return setup(matchId, playerOneId, playerTwoId, deckOneId, deckTwoId, prizeCount, playerOneName, playerTwoName, 7);
    }

    public GameState setup(UUID matchId, UUID playerOneId, UUID playerTwoId, UUID deckOneId, UUID deckTwoId,
                           int prizeCount, String playerOneName, String playerTwoName, int handSize) {
        log.warn("[setup] starting setup for match {} with prizeCount={} handSize={}", matchId, prizeCount, handSize);
        Deck deck1 = deckLoadPort.loadDeck(deckOneId);
        Deck deck2 = deckLoadPort.loadDeck(deckTwoId);
        log.warn("[setup] decks loaded for match {}", matchId);

        List<CardInstance> deck1Cards = expandDeck(deck1);
        List<CardInstance> deck2Cards = expandDeck(deck2);
        log.warn("[setup] decks expanded for match {} (p1={} p2={})", matchId, deck1Cards.size(), deck2Cards.size());

        validateDeck(deck1Cards, playerOneId);
        validateDeck(deck2Cards, playerTwoId);
        log.warn("[setup] decks validated for match {}", matchId);

        deck1Cards = shuffleDeck(deck1Cards);
        deck2Cards = shuffleDeck(deck2Cards);
        log.warn("[setup] decks shuffled for match {}", matchId);

        PlayerState playerOneState = createPlayerState(playerOneId, PlayerSide.PLAYER_ONE, playerOneName);
        PlayerState playerTwoState = createPlayerState(playerTwoId, PlayerSide.PLAYER_TWO, playerTwoName);
        log.warn("[setup] player states created for match {}", matchId);

        dealHand(playerOneState, deck1Cards, handSize);
        dealHand(playerTwoState, deck2Cards, handSize);
        log.warn("[setup] initial hands dealt for match {} (handSize={})", matchId, handSize);

        GameState gameState = new GameState();
        gameState.setMatchId(matchId);
        gameState.setStatus(MatchStatus.SETUP);
        gameState.setPhase(null);
        gameState.setTurnNumber(0);
        gameState.setCurrentPlayerId(playerOneId);
        gameState.setFirstPlayerId(playerOneId);
        gameState.setPlayers(new PlayerState[]{playerOneState, playerTwoState});
        gameState.setSuddenDeath(prizeCount == 1);
        gameState.setPrizeCountPerPlayer(prizeCount);
        gameState.setHandSize(handSize);
        gameState.addPlayerDeckId(playerOneId, deckOneId);
        gameState.addPlayerDeckId(playerTwoId, deckTwoId);

        TurnFlags turnFlags = new TurnFlags();
        gameState.setTurnFlags(turnFlags);

        playerOneState.setDeck(new ArrayList<>(deck1Cards));
        playerTwoState.setDeck(new ArrayList<>(deck2Cards));

        Set<UUID> pendingSet = new HashSet<>();
        if (!hasBasicPokemon(playerOneState.getHand())) {
            pendingSet.add(playerOneId);
            log.warn("[setup] player {} needs initial mulligan decision (no Basic in hand)", playerOneId);
        } else {
            playerOneState.setInitialMulliganResolved(true);
        }
        if (!hasBasicPokemon(playerTwoState.getHand())) {
            pendingSet.add(playerTwoId);
            log.warn("[setup] player {} needs initial mulligan decision (no Basic in hand)", playerTwoId);
        } else {
            playerTwoState.setInitialMulliganResolved(true);
        }

        if (!pendingSet.isEmpty()) {
            gameState.setPendingInitialMulliganPlayers(pendingSet);
            for (UUID pid : pendingSet) {
                GameEvent mulliganNeededEvent = new GameEvent(
                        GameEventType.INITIAL_MULLIGAN_NEEDED.name(),
                        matchId, 0, Instant.now(),
                        "Jugador necesita decidir mulligan inicial",
                        Map.of("playerId", pid.toString())
                );
                eventPublisher.publishEvents(matchId, List.of(mulliganNeededEvent));
            }
            log.warn("[setup] initial mulligan pending for players: {}", pendingSet);
        } else {
            assignPrizes(playerOneState, deck1Cards, prizeCount);
            assignPrizes(playerTwoState, deck2Cards, prizeCount);
            createMulliganDrawPendingState(gameState, playerOneState, playerTwoState, matchId);
            log.warn("[setup] prizes assigned and mulligan draw state created for match {}", matchId);
        }

        if (playerOneState.getPrizes() != null && playerTwoState.getPrizes() != null
                && (playerOneState.getPrizes().size() != prizeCount || playerTwoState.getPrizes().size() != prizeCount)) {
            throw new IllegalStateException("Both players must have exactly " + prizeCount + " prizes");
        }

        Instant now = Instant.now();
        gameState.setCreatedAt(now);
        gameState.setUpdatedAt(now);

        log.warn("[setup] setup complete for match {}, prizeCount={}", matchId, prizeCount);
        return gameState;
    }

    private PlayerState createPlayerState(UUID playerId, PlayerSide side, String displayName) {
        PlayerState state = new PlayerState();
        state.setPlayerId(playerId);
        state.setSide(side);
        state.setDisplayName(displayName);
        state.setDiscard(new ArrayList<>());
        state.setBench(new ArrayList<>());
        state.setSetupConfirmed(false);
        return state;
    }

    private List<CardInstance> expandDeck(Deck deck) {
        List<CardInstance> result = new ArrayList<>();
        for (var deckCard : deck.getCards()) {
            for (int i = 0; i < deckCard.getQuantity(); i++) {
                result.add(new CardInstance(UUID.randomUUID(), deckCard.getCardId()));
            }
        }
        return result;
    }

    // Mariano si lees esto, santi me obligo
    private void validateDeck(List<CardInstance> deckCards, UUID playerId) {
        if (deckCards.size() < 60) {
            throw new IllegalStateException(
                    "Player " + playerId + " deck has " + deckCards.size() + " cards, minimum is 60");
        }
        boolean hasBasic = false;
        for (CardInstance card : deckCards) {
            CardDefinition def = cardLookupPort.getCardById(card.getCardDefinitionId());
            if (def == null) {
                throw new IllegalStateException("Card definition not found: " + card.getCardDefinitionId());
            }
            if (def instanceof PokemonCardDefinition pkmn && (pkmn.getStage() == null || "BASIC".equalsIgnoreCase(pkmn.getStage()))) {
                hasBasic = true;
                break;
            }
        }
        if (!hasBasic) {
            throw new IllegalStateException(
                    "Player " + playerId + " deck has no Basic Pokemon");
        }
    }

    private List<CardInstance> shuffleDeck(List<CardInstance> deck) {
        List<CardInstance> mutableCopy = new ArrayList<>(deck);
        randomizerPort.shuffle(mutableCopy);
        return mutableCopy;
    }

    private void dealHand(PlayerState playerState, List<CardInstance> deckCards, int count) {
        List<CardInstance> hand = new ArrayList<>(deckCards.subList(0, count));
        deckCards.subList(0, count).clear();
        playerState.setHand(hand);
    }

    private void resolveMulligan(PlayerState playerState, List<CardInstance> deckCards, UUID matchId) {
        int maxMulligans = 20;
        while (!hasBasicPokemon(playerState.getHand())) {
            if (playerState.getMulliganCount() >= maxMulligans) {
                throw new IllegalStateException(
                        "Player " + playerState.getPlayerId() + " has no basic Pokemon after " + maxMulligans + " mulligans");
            }

            log.warn("[resolveMulligan] player {} mulligan #{}", playerState.getPlayerId(),
                    playerState.getMulliganCount() + 1);

            List<String> revealedCardIds = playerState.getHand().stream()
                    .map(CardInstance::getCardDefinitionId)
                    .toList();
            GameEvent revealEvent = new GameEvent(
                    GameEventType.MULLIGAN_REVEALED.name(),
                    matchId,
                    0,
                    Instant.now(),
                    "Mano revelada por mulligan",
                    Map.of("playerId", playerState.getPlayerId().toString(), "revealedCardIds", revealedCardIds)
            );
            eventPublisher.publishEvents(matchId, List.of(revealEvent));

            deckCards.addAll(playerState.getHand());
            playerState.getHand().clear();

            List<CardInstance> shuffled = shuffleDeck(deckCards);
            deckCards.clear();
            deckCards.addAll(shuffled);

            List<CardInstance> hand = new ArrayList<>(deckCards.subList(0, 7));
            deckCards.subList(0, 7).clear();
            playerState.setHand(hand);

            playerState.setMulliganCount(playerState.getMulliganCount() + 1);
        }
    }

    private boolean hasBasicPokemon(List<CardInstance> hand) {
        for (CardInstance card : hand) {
            CardDefinition def = cardLookupPort.getCardById(card.getCardDefinitionId());
            if (def == null) {
                throw new IllegalStateException("Card definition not found: " + card.getCardDefinitionId());
            }
            if (def instanceof PokemonCardDefinition pkmn && (pkmn.getStage() == null || "BASIC".equalsIgnoreCase(pkmn.getStage()))) {
                return true;
            }
        }
        return false;
    }

    private void drawExtraCards(PlayerState playerState, List<CardInstance> deckCards, int extraCount) {
        for (int i = 0; i < extraCount; i++) {
            if (!deckCards.isEmpty()) {
                playerState.getHand().add(deckCards.remove(0));
            }
        }
    }

    private void createMulliganDrawPendingState(GameState gs, PlayerState p1, PlayerState p2, UUID matchId) {
        Map<UUID, Integer> draws = new HashMap<>();
        if (p2.getMulliganCount() > 0) draws.put(p1.getPlayerId(), p2.getMulliganCount());
        if (p1.getMulliganCount() > 0) draws.put(p2.getPlayerId(), p1.getMulliganCount());

        if (!draws.isEmpty()) {
            gs.setMulliganDrawPending(true);
            gs.setMulliganDrawCounts(draws);
            gs.setMulliganDrawResolved(new HashSet<>());
            gs.setMulliganDrawDeadline(Instant.now().plusSeconds(30));

            for (var entry : draws.entrySet()) {
                GameEvent event = new GameEvent(
                    GameEventType.MULLIGAN_DRAW_OPPORTUNITY.name(),
                    matchId, 0, Instant.now(),
                    "Puedes robar " + entry.getValue() + " carta(s) por mulligan del oponente",
                    Map.of("playerId", entry.getKey().toString(), "count", entry.getValue())
                );
                eventPublisher.publishEvents(matchId, List.of(event));
            }
        }
    }

    private void selectActivePokemon(PlayerState playerState, UUID playerId) {
        List<CardInstance> hand = playerState.getHand();
        for (int i = 0; i < hand.size(); i++) {
            CardInstance card = hand.get(i);
            CardDefinition def = cardLookupPort.getCardById(card.getCardDefinitionId());
            if (def == null) {
                throw new IllegalStateException("Card definition not found: " + card.getCardDefinitionId());
            }
            if (def instanceof PokemonCardDefinition pkmn && (pkmn.getStage() == null || "BASIC".equalsIgnoreCase(pkmn.getStage()))) {
                CardInstance removed = hand.remove(i);

                PokemonInPlay active = createFaceDownPokemon(removed, playerId);

                playerState.setActivePokemon(active);
                return;
            }
        }
        throw new IllegalStateException("No Basic Pokemon found for active selection");
    }

    private void fillBenchWithBasics(PlayerState playerState, UUID playerId) {
        List<PokemonInPlay> bench = new ArrayList<>();
        List<CardInstance> hand = playerState.getHand();
        for (int i = hand.size() - 1; i >= 0 && bench.size() < 5; i--) {
            CardInstance card = hand.get(i);
            CardDefinition def = cardLookupPort.getCardById(card.getCardDefinitionId());
            if (def instanceof PokemonCardDefinition pkmn && (pkmn.getStage() == null || "BASIC".equalsIgnoreCase(pkmn.getStage()))) {
                bench.add(createFaceDownPokemon(card, playerId));
                hand.remove(i);
            }
        }
        playerState.setBench(bench);
    }

    private PokemonInPlay createFaceDownPokemon(CardInstance card, UUID playerId) {
        PokemonInPlay pkm = new PokemonInPlay();
        pkm.setInstanceId(card.getInstanceId());
        pkm.setCardDefinitionId(card.getCardDefinitionId());
        pkm.setOwnerPlayerId(playerId);
        pkm.setEnteredTurnNumber(0);
        pkm.setEvolvedThisTurn(false);
        pkm.setFaceDown(true);
        pkm.setDamageCounters(0);
        pkm.setSpecialConditions(new ArrayList<>());
        pkm.setAttachedEnergies(new ArrayList<>());
        return pkm;
    }

    public static void revealAllPokemon(GameState state) {
        for (PlayerState player : state.getPlayers()) {
            if (player.getActivePokemon() != null) {
                player.getActivePokemon().setFaceDown(false);
            }
            if (player.getBench() != null) {
                for (PokemonInPlay pkm : player.getBench()) {
                    pkm.setFaceDown(false);
                }
            }
        }
    }

    private void assignPrizes(PlayerState playerState, List<CardInstance> deckCards) {
        assignPrizes(playerState, deckCards, 6);
    }

    private void assignPrizes(PlayerState playerState, List<CardInstance> deckCards, int prizeCount) {
        List<CardInstance> prizes = new ArrayList<>(deckCards.subList(0, prizeCount));
        deckCards.subList(0, prizeCount).clear();
        playerState.setPrizes(prizes);
    }
}
