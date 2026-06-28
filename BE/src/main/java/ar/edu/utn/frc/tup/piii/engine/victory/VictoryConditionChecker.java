package ar.edu.utn.frc.tup.piii.engine.victory;

import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.util.UUID;

public class VictoryConditionChecker {

    public record VictoryCheckResult(
            boolean finished,
            UUID winnerPlayerId,
            FinishReason reason,
            boolean suddenDeath
    ) {}

    public static VictoryCheckResult check(GameState state, UUID currentPlayerId) {
        PlayerState[] ps = state.getPlayers();
        PlayerState p1 = ps[0];
        PlayerState p2 = ps[1];

        boolean p1PrizeWin = p1.getPrizes() != null && p1.getPrizes().isEmpty();
        boolean p2PrizeWin = p2.getPrizes() != null && p2.getPrizes().isEmpty();

        boolean p1NoPokemon = p1.getActivePokemon() == null
                && (p1.getBench() == null || p1.getBench().isEmpty());
        boolean p2NoPokemon = p2.getActivePokemon() == null
                && (p2.getBench() == null || p2.getBench().isEmpty());

        boolean p1DeckOut = p1.getDeck() == null || p1.getDeck().isEmpty();
        boolean p2DeckOut = p2.getDeck() == null || p2.getDeck().isEmpty();

        boolean p1Wins = p1PrizeWin || p2NoPokemon || p2DeckOut;
        boolean p2Wins = p2PrizeWin || p1NoPokemon || p1DeckOut;

        if (p1Wins && p2Wins) {
            int p1Count = (p1PrizeWin ? 1 : 0) + (p2NoPokemon ? 1 : 0) + (p2DeckOut ? 1 : 0);
            int p2Count = (p2PrizeWin ? 1 : 0) + (p1NoPokemon ? 1 : 0) + (p1DeckOut ? 1 : 0);
            if (p1Count > p2Count) {
                FinishReason reason = p1PrizeWin ? FinishReason.PRIZES
                        : p2NoPokemon ? FinishReason.KNOCKOUT : FinishReason.DECK_OUT;
                return new VictoryCheckResult(true, p1.getPlayerId(), reason, false);
            }
            if (p2Count > p1Count) {
                FinishReason reason = p2PrizeWin ? FinishReason.PRIZES
                        : p1NoPokemon ? FinishReason.KNOCKOUT : FinishReason.DECK_OUT;
                return new VictoryCheckResult(true, p2.getPlayerId(), reason, false);
            }
            return new VictoryCheckResult(true, null, null, true);
        }
        if (p1Wins) {
            FinishReason reason = p1PrizeWin ? FinishReason.PRIZES
                    : p2NoPokemon ? FinishReason.KNOCKOUT : FinishReason.DECK_OUT;
            return new VictoryCheckResult(true, p1.getPlayerId(), reason, false);
        }
        if (p2Wins) {
            FinishReason reason = p2PrizeWin ? FinishReason.PRIZES
                    : p1NoPokemon ? FinishReason.KNOCKOUT : FinishReason.DECK_OUT;
            return new VictoryCheckResult(true, p2.getPlayerId(), reason, false);
        }
        return new VictoryCheckResult(false, null, null, false);
    }
}
