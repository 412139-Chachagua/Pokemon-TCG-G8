package ar.edu.utn.frc.tup.piii.services.ranking;

import ar.edu.utn.frc.tup.piii.dtos.ranking.PlayerStatsResponse;
import ar.edu.utn.frc.tup.piii.dtos.ranking.RankingEntryResponse;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerStatsEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchPlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerStatsJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerStatsServiceTest {

    @Mock
    private PlayerStatsJpaRepository playerStatsJpaRepository;

    @Mock
    private PlayerJpaRepository playerJpaRepository;

    @Mock
    private MatchPlayerJpaRepository matchPlayerJpaRepository;

    private PlayerStatsService playerStatsService;

    private UUID winnerId;
    private UUID loserId;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        playerStatsService = new PlayerStatsService(
                playerStatsJpaRepository, playerJpaRepository, matchPlayerJpaRepository);
        winnerId = UUID.randomUUID();
        loserId = UUID.randomUUID();
        matchId = UUID.randomUUID();
    }

    @Test
    void shouldRecordWin() {
        MatchPlayerEntity mp1 = new MatchPlayerEntity();
        mp1.setPlayerId(winnerId);
        MatchPlayerEntity mp2 = new MatchPlayerEntity();
        mp2.setPlayerId(loserId);
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(mp1, mp2));
        when(playerStatsJpaRepository.findByPlayerId(winnerId)).thenReturn(Optional.empty());
        when(playerStatsJpaRepository.findByPlayerId(loserId)).thenReturn(Optional.empty());

        playerStatsService.recordMatchResult(matchId, winnerId, FinishReason.KNOCKOUT);

        verify(playerStatsJpaRepository, times(2)).save(any());
    }

    @Test
    void shouldIncrementWinStreakOnConsecutiveWins() {
        UUID playerId = UUID.randomUUID();
        PlayerStatsEntity existing = new PlayerStatsEntity();
        existing.setPlayerId(playerId);
        existing.setTotalWins(3);
        existing.setTotalLosses(1);
        existing.setCurrentWinStreak(2);
        existing.setMaxWinStreak(2);

        UUID opponentId = UUID.randomUUID();
        MatchPlayerEntity mp1 = new MatchPlayerEntity();
        mp1.setPlayerId(playerId);
        MatchPlayerEntity mp2 = new MatchPlayerEntity();
        mp2.setPlayerId(opponentId);

        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(mp1, mp2));
        when(playerStatsJpaRepository.findByPlayerId(playerId)).thenReturn(Optional.of(existing));
        when(playerStatsJpaRepository.findByPlayerId(opponentId)).thenReturn(Optional.empty());

        playerStatsService.recordMatchResult(matchId, playerId, FinishReason.PRIZES);

        assertEquals(4, existing.getTotalWins());
        assertEquals(3, existing.getCurrentWinStreak());
        assertEquals(3, existing.getMaxWinStreak());
        verify(playerStatsJpaRepository, times(2)).save(any());
    }

    @Test
    void shouldResetWinStreakOnLoss() {
        UUID playerId = UUID.randomUUID();
        PlayerStatsEntity existing = new PlayerStatsEntity();
        existing.setPlayerId(playerId);
        existing.setTotalWins(5);
        existing.setTotalLosses(2);
        existing.setCurrentWinStreak(3);
        existing.setMaxWinStreak(5);

        UUID winnerId2 = UUID.randomUUID();
        MatchPlayerEntity mp1 = new MatchPlayerEntity();
        mp1.setPlayerId(playerId);
        MatchPlayerEntity mp2 = new MatchPlayerEntity();
        mp2.setPlayerId(winnerId2);

        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(mp1, mp2));
        when(playerStatsJpaRepository.findByPlayerId(playerId)).thenReturn(Optional.of(existing));
        when(playerStatsJpaRepository.findByPlayerId(winnerId2)).thenReturn(Optional.empty());

        playerStatsService.recordMatchResult(matchId, winnerId2, FinishReason.DECK_OUT);

        assertEquals(3, existing.getTotalLosses());
        assertEquals(0, existing.getCurrentWinStreak());
        assertEquals(5, existing.getMaxWinStreak());
        verify(playerStatsJpaRepository, times(2)).save(any());
    }

    @Test
    void shouldIgnoreSuddenDeath() {
        playerStatsService.recordMatchResult(matchId, null, FinishReason.SUDDEN_DEATH);
        verifyNoInteractions(matchPlayerJpaRepository, playerStatsJpaRepository);
    }

    @Test
    void shouldReturnRankingOrderedByWins() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        PlayerStatsEntity s1 = new PlayerStatsEntity(p1, 10, 2, 5, 5, null);
        PlayerStatsEntity s2 = new PlayerStatsEntity(p2, 5, 3, 2, 3, null);

        when(playerStatsJpaRepository.findAllByOrderByTotalWinsDescMaxWinStreakDesc())
                .thenReturn(List.of(s1, s2));
        when(playerJpaRepository.findById(p1)).thenReturn(Optional.of(createPlayer(p1, "Alice")));
        when(playerJpaRepository.findById(p2)).thenReturn(Optional.of(createPlayer(p2, "Bob")));

        List<RankingEntryResponse> ranking = playerStatsService.getRanking();

        assertEquals(2, ranking.size());
        assertEquals(1, ranking.get(0).rank());
        assertEquals("Alice", ranking.get(0).displayName());
        assertEquals(10, ranking.get(0).totalWins());
        assertEquals(2, ranking.get(1).rank());
        assertEquals("Bob", ranking.get(1).displayName());
    }

    @Test
    void shouldReturnPlayerStats() {
        UUID playerId = UUID.randomUUID();
        PlayerStatsEntity stats = new PlayerStatsEntity(playerId, 7, 3, 2, 4, null);

        when(playerJpaRepository.findById(playerId))
                .thenReturn(Optional.of(createPlayer(playerId, "Charlie")));
        when(playerStatsJpaRepository.findByPlayerId(playerId)).thenReturn(Optional.of(stats));

        PlayerStatsResponse response = playerStatsService.getPlayerStats(playerId);

        assertEquals(playerId.toString(), response.playerId());
        assertEquals("Charlie", response.displayName());
        assertEquals(7, response.totalWins());
        assertEquals(3, response.totalLosses());
        assertEquals(2, response.currentWinStreak());
        assertEquals(4, response.maxWinStreak());
    }

    @Test
    void shouldReturnEmptyStatsWhenNoGamesPlayed() {
        UUID playerId = UUID.randomUUID();
        when(playerJpaRepository.findById(playerId))
                .thenReturn(Optional.of(createPlayer(playerId, "Diana")));
        when(playerStatsJpaRepository.findByPlayerId(playerId)).thenReturn(Optional.empty());

        PlayerStatsResponse response = playerStatsService.getPlayerStats(playerId);

        assertEquals(0, response.totalWins());
        assertEquals(0, response.totalLosses());
        assertEquals(0, response.currentWinStreak());
        assertEquals(0, response.maxWinStreak());
    }

    private PlayerEntity createPlayer(UUID id, String displayName) {
        PlayerEntity p = new PlayerEntity();
        p.setId(id);
        p.setDisplayName(displayName);
        return p;
    }
}
