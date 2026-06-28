package ar.edu.utn.frc.tup.piii.services.matches;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchQueryService {
    private static final Logger log = LoggerFactory.getLogger(MatchQueryService.class);

    private final CardLookupPort cardLookupPort;

    public MatchQueryService(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    public PublicGameState buildPublicState(GameState state) {
        if (state == null) return null;

        PublicGameState.PublicPlayerState[] publicPlayers = new PublicGameState.PublicPlayerState[2];
        for (int i = 0; i < 2; i++) {
            PlayerState ps = state.getPlayers()[i];
            if (ps == null) continue;

            PublicGameState.PublicPokemonSlot activeSlot = null;
            if (ps.getActivePokemon() != null) {
                activeSlot = toPublicSlot(ps.getActivePokemon());
            }

            PublicGameState.PublicPokemonSlot[] benchSlots = new PublicGameState.PublicPokemonSlot[ps.getBench() != null ? ps.getBench().size() : 0];
            if (ps.getBench() != null) {
                for (int j = 0; j < ps.getBench().size(); j++) {
                    benchSlots[j] = toPublicSlot(ps.getBench().get(j));
                }
            }

            String[] prizes = new String[ps.getPrizes() != null ? ps.getPrizes().size() : 0];
            Arrays.fill(prizes, "FACE_DOWN");

            publicPlayers[i] = new PublicGameState.PublicPlayerState(
                    ps.getPlayerId(),
                    ps.getSide() != null ? ps.getSide().name() : null,
                    activeSlot,
                    benchSlots,
                    prizes,
                    ps.isSetupConfirmed()
            );
            publicPlayers[i].setMulliganCount(ps.getMulliganCount());
            publicPlayers[i].setTotalPrizeCount(state.getPrizeCountPerPlayer());
            publicPlayers[i].setDiscardCount(ps.getDiscard().size());

            List<PublicDiscardCard> discardCards = ps.getDiscard().stream()
                    .map(card -> new PublicDiscardCard(
                            card.getInstanceId().toString(),
                            card.getCardDefinitionId()
                    ))
                    .collect(Collectors.toList());
            publicPlayers[i].setDiscard(discardCards);

            publicPlayers[i].setMulliganRevealedCards(ps.getMulliganRevealedCards());
            publicPlayers[i].setDisplayName(ps.getDisplayName());
            publicPlayers[i].setFirstTurnCompleted(state.hasPlayerCompletedFirstTurn(ps.getPlayerId()));
        }

        PublicGameState pgs = new PublicGameState(
                state.getMatchId(),
                state.getStatus() != null ? state.getStatus().name() : null,
                state.getPhase() != null ? state.getPhase().name() : null,
                state.getTurnNumber(),
                state.getCurrentPlayerId(),
                state.getFirstPlayerId(),
                publicPlayers
        );
        pgs.setMulliganDrawPending(state.isMulliganDrawPending());
        pgs.setMulliganDrawDeadline(state.getMulliganDrawDeadline() != null ? state.getMulliganDrawDeadline().toString() : null);
        Set<UUID> pending = state.getPendingInitialMulliganPlayers();
        pgs.setPendingInitialMulliganPlayers(pending != null && !pending.isEmpty() ? pending.toArray(new UUID[0]) : null);
        pgs.setPendingKOReplacement(state.isPendingKOReplacement());
        pgs.setPendingPrizeOwnerPlayerId(state.getPendingPrizeOwnerPlayerId() != null ? state.getPendingPrizeOwnerPlayerId().toString() : null);
        pgs.setKnockedOutPlayerId(state.getKnockedOutPlayerId() != null ? state.getKnockedOutPlayerId().toString() : null);
        pgs.setWinnerPlayerId(state.getWinnerPlayerId());
        pgs.setFinishReason(state.getFinishReason() != null ? state.getFinishReason().name() : null);
        pgs.setStadiumCardInstanceId(state.getStadiumCardInstanceId() != null ? state.getStadiumCardInstanceId().toString() : null);
        pgs.setStadiumCardDefinitionId(state.getStadiumCardDefinitionId());
        pgs.setStadiumOwnerPlayerId(state.getStadiumOwnerPlayerId() != null ? state.getStadiumOwnerPlayerId().toString() : null);
        pgs.setHasPlayedSupporter(state.getTurnFlags() != null && state.getTurnFlags().hasPlayedSupporter());
        pgs.setHasPlayedStadium(state.getTurnFlags() != null && state.getTurnFlags().hasPlayedStadium());
        pgs.setHasAttachedEnergy(state.getTurnFlags() != null && state.getTurnFlags().hasAttachedEnergy());
        pgs.setHasRetreated(state.getTurnFlags() != null && state.getTurnFlags().hasRetreated());
        return pgs;
    }

    public PrivatePlayerState buildPrivateState(GameState state, UUID playerId) {
        if (state == null) return null;

        PlayerState playerState = null;
        for (PlayerState ps : state.getPlayers()) {
            if (ps != null && ps.getPlayerId().equals(playerId)) {
                playerState = ps;
                break;
            }
        }
        if (playerState == null) return null;

        List<PrivatePlayerState.PrivateHandCard> handCards = new ArrayList<>();
        if (playerState.getHand() != null) {
            for (CardInstance ci : playerState.getHand()) {
                CardDefinition def = cardLookupPort.getCardById(ci.getCardDefinitionId());
                String effectCode = null;
                if (def instanceof TrainerCardDefinition trainerDef) {
                    effectCode = trainerDef.getEffectCode();
                }
                handCards.add(new PrivatePlayerState.PrivateHandCard(
                        ci.getInstanceId().toString(),
                        ci.getCardDefinitionId(),
                        def != null ? def.getName() : "Unknown",
                        def != null ? def.getSupertype() : "Unknown",
                        effectCode
                ));
            }
        }

        List<PrivatePlayerState.PrizeSlot> prizeSlots = new ArrayList<>();
        if (playerState.getPrizes() != null) {
            for (int i = 0; i < playerState.getPrizes().size(); i++) {
                CardInstance ci = playerState.getPrizes().get(i);
                prizeSlots.add(new PrivatePlayerState.PrizeSlot(
                        i + 1,
                        false,
                        null
                ));
            }
        }

        List<PrivatePlayerState.PrivateHandCard> deckCards = new ArrayList<>();
        if (playerState.getDeck() != null) {
            for (CardInstance ci : playerState.getDeck()) {
                CardDefinition def = cardLookupPort.getCardById(ci.getCardDefinitionId());
                deckCards.add(new PrivatePlayerState.PrivateHandCard(
                        ci.getInstanceId().toString(),
                        ci.getCardDefinitionId(),
                        def != null ? def.getName() : "Unknown",
                        def != null ? def.getSupertype() : "Unknown"
                ));
            }
        }

        PrivatePlayerState pps = new PrivatePlayerState(
                playerState.getPlayerId(),
                handCards,
                playerState.getDeck() != null ? playerState.getDeck().size() : 0,
                playerState.getDiscard().size(),
                prizeSlots
        );
        pps.setDeck(deckCards);
        int pendingDrawCount = 0;
        if (state.isMulliganDrawPending() && state.getMulliganDrawCounts() != null) {
            boolean alreadyResolved = state.getMulliganDrawResolved() != null
                && state.getMulliganDrawResolved().contains(playerId);
            if (!alreadyResolved) {
                pendingDrawCount = state.getMulliganDrawCounts().getOrDefault(playerId, 0);
            }
        }
        pps.setPendingMulliganDrawCount(pendingDrawCount);
        return pps;
    }

    private PublicGameState.PublicPokemonSlot toPublicSlot(PokemonInPlay pkm) {
        String[] energyTypes;
        String[] energyInstanceIds;
        if (pkm.getAttachedEnergies() != null) {
            List<String> types = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (CardInstance ci : pkm.getAttachedEnergies()) {
                ids.add(ci.getInstanceId().toString());
                CardDefinition def = cardLookupPort.getCardById(ci.getCardDefinitionId());
                log.warn("[DEBUG] toPublicSlot: cardInstanceId={} cardDefId={} def==null?{} instanceofEnergyCardDef?{}",
                        ci.getInstanceId(), ci.getCardDefinitionId(), def == null, def instanceof EnergyCardDefinition);
                if (def instanceof EnergyCardDefinition ed) {
                    log.warn("[DEBUG] toPublicSlot: ed.getProvides()={} ed.getStrategyKey()={}", ed.getProvides(), ed.getStrategyKey());
                    List<EnergyType> provides = ed.getProvides();
                    String sk = ed.getStrategyKey();
                    // If getProvides() is empty (non-parseable data like "DOUBLE COLORLESS"),
                    // fall back to known type from strategyKey
                    if (provides == null || provides.isEmpty()) {
                        if ("DOUBLE_COLORLESS".equals(sk)) {
                            provides = List.of(EnergyType.COLORLESS);
                        } else if ("RAINBOW".equals(sk)) {
                            provides = List.of(EnergyType.COLORLESS);
                        } else {
                            provides = List.of();
                        }
                    }
                    if (provides != null && !provides.isEmpty()) {
                        int units = 1;
                        if ("DOUBLE_COLORLESS".equals(sk)) units = 2;
                        for (int i = 0; i < units; i++) {
                            for (EnergyType et : provides) {
                                types.add(et.name());
                            }
                        }
                    }
                } else {
                    log.warn("[DEBUG] toPublicSlot: cardDefId={} is NOT an EnergyCardDefinition, def.class={}",
                            ci.getCardDefinitionId(), def != null ? def.getClass().getSimpleName() : "N/A");
                }
            }
            energyTypes = types.toArray(String[]::new);
            energyInstanceIds = ids.toArray(String[]::new);
            log.warn("[DEBUG] toPublicSlot: pokemon={} attachedCards={}", pkm.getInstanceId(), Arrays.toString(energyTypes));
        } else {
            energyTypes = new String[0];
            energyInstanceIds = new String[0];
            log.warn("[DEBUG] toPublicSlot: pokemon={} attachedEnergies is NULL", pkm.getInstanceId());
        }

        return new PublicGameState.PublicPokemonSlot(
                pkm.getInstanceId().toString(),
                pkm.getCardDefinitionId(),
                pkm.getDamageCounters(),
                pkm.getSpecialConditions() != null
                        ? pkm.getSpecialConditions().stream().map(Enum::name).toArray(String[]::new)
                        : new String[0],
                energyTypes,
                pkm.isEvolvedThisTurn(),
                pkm.getEnteredTurnNumber(),
                energyInstanceIds,
                pkm.getToolCardInstanceId() != null ? pkm.getToolCardInstanceId().toString() : null,
                pkm.getAttachedTool() != null ? pkm.getAttachedTool().getCardDefinitionId() : null
        );
    }
}