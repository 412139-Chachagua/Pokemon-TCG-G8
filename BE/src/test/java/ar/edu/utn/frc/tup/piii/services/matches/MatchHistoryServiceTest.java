package ar.edu.utn.frc.tup.piii.services.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.MatchSummaryResponse;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchHistoryServiceTest {

    @Mock
    private MatchJpaRepository matchJpaRepository;

    private MatchHistoryService matchHistoryService;

    private UUID playerId;
    private UUID matchId;
    private MatchEntity finishedMatch;
    private MatchEntity expiredMatch;

    @BeforeEach
    void setUp() {
        matchHistoryService = new MatchHistoryService(matchJpaRepository);

        playerId = UUID.randomUUID();
        matchId = UUID.randomUUID();

        MatchPlayerEntity mp1 = new MatchPlayerEntity();
        mp1.setPlayerId(playerId);
        mp1.setSide("PLAYER_1");
        mp1.setDisplayName("Alice");

        MatchPlayerEntity mp2 = new MatchPlayerEntity();
        mp2.setPlayerId(UUID.randomUUID());
        mp2.setSide("PLAYER_2");
        mp2.setDisplayName("Bob");

        finishedMatch = new MatchEntity();
        finishedMatch.setId(matchId);
        finishedMatch.setStatus("FINISHED");
        finishedMatch.setFinishReason("KNOCKOUT");
        finishedMatch.setWinnerPlayerId(playerId);
        finishedMatch.setTurnNumber(10);
        finishedMatch.setCreatedAt(Instant.now().minusSeconds(3600));
        finishedMatch.setFinishedAt(Instant.now());
        finishedMatch.setPlayers(List.of(mp1, mp2));

        UUID otherId = UUID.randomUUID();
        expiredMatch = new MatchEntity();
        expiredMatch.setId(otherId);
        expiredMatch.setStatus("FINISHED");
        expiredMatch.setFinishReason("EXPIRED");
        expiredMatch.setWinnerPlayerId(UUID.randomUUID());
        expiredMatch.setTurnNumber(5);
        expiredMatch.setCreatedAt(Instant.now().minusSeconds(7200));
        expiredMatch.setFinishedAt(Instant.now().minusSeconds(1800));
        expiredMatch.setPlayers(List.of(mp1, mp2));
    }

    @Nested
    class GetHistoryByPlayer {

        @Test
        void shouldReturnFinishedMatchesExcludingExpired() {
            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch, expiredMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(1, result.size());
            assertEquals(matchId.toString(), result.get(0).id());
            assertEquals("Alice", result.get(0).winnerName());
            assertEquals("Bob", result.get(0).loserName());
            assertEquals(10, result.get(0).totalTurns());
            assertNotNull(result.get(0).durationSeconds());
        }

        @Test
        void shouldReturnEmptyListWhenNoMatches() {
            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of());

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnEmptyWhenAllMatchesExpired() {
            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(expiredMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldDetermineLoserCorrectlyWhenPlayerIsWinner() {
            MatchPlayerEntity winnerMp = new MatchPlayerEntity();
            winnerMp.setPlayerId(playerId);
            winnerMp.setSide("PLAYER_1");
            winnerMp.setDisplayName("Alice");

            UUID loserUuid = UUID.randomUUID();
            MatchPlayerEntity loserMp = new MatchPlayerEntity();
            loserMp.setPlayerId(loserUuid);
            loserMp.setSide("PLAYER_2");
            loserMp.setDisplayName("Bob");

            finishedMatch.setWinnerPlayerId(playerId);
            finishedMatch.setPlayers(List.of(winnerMp, loserMp));

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(1, result.size());
            assertEquals("Alice", result.get(0).winnerName());
            assertEquals("Bob", result.get(0).loserName());
        }

        @Test
        void shouldDetermineLoserCorrectlyWhenPlayerOneWins() {
            UUID p1Id = UUID.randomUUID();
            UUID p2Id = UUID.randomUUID();
            MatchPlayerEntity p1 = new MatchPlayerEntity();
            p1.setPlayerId(p1Id);
            p1.setSide("PLAYER_1");
            p1.setDisplayName("Winner");
            MatchPlayerEntity p2 = new MatchPlayerEntity();
            p2.setPlayerId(p2Id);
            p2.setSide("PLAYER_2");
            p2.setDisplayName("Loser");

            finishedMatch.setWinnerPlayerId(p1Id);
            finishedMatch.setPlayers(List.of(p1, p2));

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", p1Id))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(p1Id);

            assertEquals("Winner", result.get(0).winnerName());
            assertEquals("Loser", result.get(0).loserName());
        }

        @Test
        void shouldDetermineLoserCorrectlyWhenPlayerTwoWins() {
            UUID p1Id = UUID.randomUUID();
            UUID p2Id = UUID.randomUUID();
            MatchPlayerEntity p1 = new MatchPlayerEntity();
            p1.setPlayerId(p1Id);
            p1.setSide("PLAYER_1");
            p1.setDisplayName("Loser");
            MatchPlayerEntity p2 = new MatchPlayerEntity();
            p2.setPlayerId(p2Id);
            p2.setSide("PLAYER_2");
            p2.setDisplayName("Winner");

            finishedMatch.setWinnerPlayerId(p2Id);
            finishedMatch.setPlayers(List.of(p1, p2));

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", p2Id))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(p2Id);

            assertEquals("Winner", result.get(0).winnerName());
            assertEquals("Loser", result.get(0).loserName());
        }

        @Test
        void shouldHandleNullWinnerPlayerId() {
            finishedMatch.setWinnerPlayerId(null);

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(1, result.size());
            assertEquals("Alice", result.get(0).winnerName());
            assertEquals("Bob", result.get(0).loserName());
        }

        @Test
        void shouldHandleSinglePlayerInMatch() {
            MatchPlayerEntity singlePlayer = new MatchPlayerEntity();
            singlePlayer.setPlayerId(playerId);
            singlePlayer.setSide("PLAYER_1");
            singlePlayer.setDisplayName("Solo");
            finishedMatch.setPlayers(List.of(singlePlayer));

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(1, result.size());
            assertEquals("Solo", result.get(0).winnerName());
            assertEquals("Unknown", result.get(0).loserName());
        }

        @Test
        void shouldHandleEmptyPlayersList() {
            finishedMatch.setPlayers(Collections.emptyList());

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(1, result.size());
            assertEquals("Unknown", result.get(0).winnerName());
            assertEquals("Unknown", result.get(0).loserName());
        }

        @Test
        void shouldReturnMatchesOrderedByCreatedAtDesc() {
            MatchEntity older = new MatchEntity();
            older.setId(UUID.randomUUID());
            older.setStatus("FINISHED");
            older.setFinishReason("PRIZES");
            older.setWinnerPlayerId(playerId);
            older.setTurnNumber(8);
            older.setCreatedAt(Instant.now().minusSeconds(7200));
            older.setFinishedAt(Instant.now().minusSeconds(3600));

            MatchPlayerEntity op1 = new MatchPlayerEntity();
            op1.setPlayerId(playerId);
            op1.setSide("PLAYER_1");
            op1.setDisplayName("Alice");

            MatchPlayerEntity op2 = new MatchPlayerEntity();
            op2.setPlayerId(UUID.randomUUID());
            op2.setSide("PLAYER_2");
            op2.setDisplayName("Charlie");

            older.setPlayers(List.of(op1, op2));

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch, older));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(2, result.size());
            assertTrue(result.get(0).createdAt().isAfter(result.get(1).createdAt()));
        }

        @Test
        void shouldCalculateDurationCorrectly() {
            Instant start = Instant.parse("2024-01-01T10:00:00Z");
            Instant end = Instant.parse("2024-01-01T11:30:00Z");
            finishedMatch.setCreatedAt(start);
            finishedMatch.setFinishedAt(end);

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(5400L, result.get(0).durationSeconds());
        }

        @Test
        void shouldReturnNullDurationWhenCreatedAtIsNull() {
            finishedMatch.setCreatedAt(null);
            finishedMatch.setFinishedAt(Instant.now());

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertNull(result.get(0).durationSeconds());
        }

        @Test
        void shouldReturnNullDurationWhenFinishedAtIsNull() {
            finishedMatch.setCreatedAt(Instant.now());
            finishedMatch.setFinishedAt(null);

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertNull(result.get(0).durationSeconds());
        }

        @Test
        void shouldReturnNullDurationWhenBothDatesAreNull() {
            finishedMatch.setCreatedAt(null);
            finishedMatch.setFinishedAt(null);

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertNull(result.get(0).durationSeconds());
        }

        @Test
        void shouldHandleNullTurnNumber() {
            finishedMatch.setTurnNumber(null);

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(0, result.get(0).totalTurns());
        }

        @ParameterizedTest
        @CsvSource({
            "KNOCKOUT",
            "PRIZES",
            "DECK_OUT",
            "CONCEDE",
            "SUDDEN_DEATH"
        })
        void shouldIncludeNonExpiredFinishReasons(String finishReason) {
            finishedMatch.setFinishReason(finishReason);

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(1, result.size());
            assertEquals(finishReason, result.get(0).finishReason());
        }

        @Test
        void shouldPreservePlayersSideOrdering() {
            MatchPlayerEntity p1 = new MatchPlayerEntity();
            p1.setPlayerId(playerId);
            p1.setSide("PLAYER_2");
            p1.setDisplayName("Alice");
            MatchPlayerEntity p2 = new MatchPlayerEntity();
            p2.setPlayerId(UUID.randomUUID());
            p2.setSide("PLAYER_1");
            p2.setDisplayName("Bob");

            finishedMatch.setWinnerPlayerId(playerId);
            finishedMatch.setPlayers(List.of(p1, p2));

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals("Alice", result.get(0).winnerName());
            assertEquals("Bob", result.get(0).loserName());
        }

        @Test
        void shouldHandleMultipleMatches() {
            List<MatchEntity> manyMatches = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                MatchEntity m = new MatchEntity();
                m.setId(UUID.randomUUID());
                m.setStatus("FINISHED");
                m.setFinishReason("KNOCKOUT");
                m.setWinnerPlayerId(playerId);
                m.setTurnNumber(i + 1);

                MatchPlayerEntity mp = new MatchPlayerEntity();
                mp.setPlayerId(playerId);
                mp.setSide("PLAYER_1");
                mp.setDisplayName("P" + i);
                m.setPlayers(List.of(mp));

                manyMatches.add(m);
            }

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(manyMatches);

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(10, result.size());
        }

        @Test
        void shouldHandleNullPlayersList() {
            finishedMatch.setPlayers(null);

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(1, result.size());
            assertEquals("Unknown", result.get(0).winnerName());
            assertEquals("Unknown", result.get(0).loserName());
        }

        @Test
        void shouldHandleNullDisplayNameInPlayers() {
            MatchPlayerEntity nullName = new MatchPlayerEntity();
            nullName.setPlayerId(playerId);
            nullName.setSide("PLAYER_1");
            nullName.setDisplayName(null);
            finishedMatch.setPlayers(List.of(nullName));

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(1, result.size());
            assertNull(result.get(0).winnerName());
        }

        @Test
        void shouldHandleDurationOfZeroSeconds() {
            Instant now = Instant.now();
            finishedMatch.setCreatedAt(now);
            finishedMatch.setFinishedAt(now);

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertEquals(0L, result.get(0).durationSeconds());
        }

        @Test
        void shouldHandleNegativeDurationIfFinishedBeforeCreated() {
            finishedMatch.setCreatedAt(Instant.now());
            finishedMatch.setFinishedAt(Instant.now().minusSeconds(60));

            when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId))
                    .thenReturn(List.of(finishedMatch));

            List<MatchSummaryResponse> result = matchHistoryService.getHistoryByPlayer(playerId);

            assertTrue(result.get(0).durationSeconds() < 0);
        }
    }

    @Nested
    class GetHistoryDetail {

        @Test
        void shouldReturnMatchWhenFoundAndNotExpired() {
            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertEquals(matchId.toString(), result.get().id());
        }

        @Test
        void shouldReturnEmptyWhenMatchNotFound() {
            UUID id = UUID.randomUUID();
            when(matchJpaRepository.findById(id)).thenReturn(Optional.empty());

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(id);

            assertFalse(result.isPresent());
        }

        @Test
        void shouldReturnEmptyWhenMatchNotFinished() {
            MatchEntity activeMatch = new MatchEntity();
            activeMatch.setId(UUID.randomUUID());
            activeMatch.setStatus("ACTIVE");

            when(matchJpaRepository.findById(activeMatch.getId())).thenReturn(Optional.of(activeMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(activeMatch.getId());

            assertFalse(result.isPresent());
        }

        @Test
        void shouldReturnEmptyWhenMatchExpired() {
            when(matchJpaRepository.findById(expiredMatch.getId())).thenReturn(Optional.of(expiredMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(expiredMatch.getId());

            assertFalse(result.isPresent());
        }

        @Test
        void shouldReturnEmptyWhenStatusIsWaiting() {
            MatchEntity waiting = new MatchEntity();
            waiting.setId(UUID.randomUUID());
            waiting.setStatus("WAITING");

            when(matchJpaRepository.findById(waiting.getId())).thenReturn(Optional.of(waiting));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(waiting.getId());

            assertFalse(result.isPresent());
        }

        @Test
        void shouldReturnEmptyWhenStatusIsSetup() {
            MatchEntity setup = new MatchEntity();
            setup.setId(UUID.randomUUID());
            setup.setStatus("SETUP");

            when(matchJpaRepository.findById(setup.getId())).thenReturn(Optional.of(setup));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(setup.getId());

            assertFalse(result.isPresent());
        }

        @Test
        void shouldIncludeMatchWithPrizesFinishReason() {
            finishedMatch.setFinishReason("PRIZES");
            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertEquals("PRIZES", result.get().finishReason());
        }

        @Test
        void shouldIncludeMatchWithDeckOutFinishReason() {
            finishedMatch.setFinishReason("DECK_OUT");
            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertEquals("DECK_OUT", result.get().finishReason());
        }

        @Test
        void shouldIncludeMatchWithConcedeFinishReason() {
            finishedMatch.setFinishReason("CONCEDE");
            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertEquals("CONCEDE", result.get().finishReason());
        }

        @Test
        void shouldHandleNullWinnerPlayerIdInDetail() {
            finishedMatch.setWinnerPlayerId(null);
            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertNotNull(result.get().winnerName());
        }

        @Test
        void shouldHandleNullCreatedAtInDetail() {
            finishedMatch.setCreatedAt(null);
            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertNull(result.get().durationSeconds());
        }

        @Test
        void shouldHandleNullFinishedAtInDetail() {
            finishedMatch.setFinishedAt(null);
            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertNull(result.get().durationSeconds());
        }

        @Test
        void shouldHandleNullTurnNumberInDetail() {
            finishedMatch.setTurnNumber(null);
            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertEquals(0, result.get().totalTurns());
        }

        @Test
        void shouldHandleSinglePlayerInDetail() {
            MatchPlayerEntity solo = new MatchPlayerEntity();
            solo.setPlayerId(playerId);
            solo.setSide("PLAYER_1");
            solo.setDisplayName("Solo");
            finishedMatch.setPlayers(List.of(solo));

            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertEquals("Solo", result.get().winnerName());
            assertEquals("Unknown", result.get().loserName());
        }

        @Test
        void shouldHandleEmptyPlayersListInDetail() {
            finishedMatch.setPlayers(Collections.emptyList());

            when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(finishedMatch));

            Optional<MatchSummaryResponse> result = matchHistoryService.getHistoryDetail(matchId);

            assertTrue(result.isPresent());
            assertEquals("Unknown", result.get().winnerName());
            assertEquals("Unknown", result.get().loserName());
        }
    }
}
