package ar.edu.utn.frc.tup.piii.controllers.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.MatchSummaryResponse;
import ar.edu.utn.frc.tup.piii.services.matches.MatchHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MatchHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MatchHistoryService matchHistoryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MatchHistoryController(matchHistoryService))
                .build();
    }

    @Test
    void listHistory_conPlayerId_retorna200() throws Exception {
        UUID playerId = UUID.randomUUID();
        MatchSummaryResponse summary = new MatchSummaryResponse(
                UUID.randomUUID().toString(), "Player1", "Player2",
                5, Instant.now(), 300L, "SURRENDER");

        when(matchHistoryService.getHistoryByPlayer(playerId)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/matches/history")
                        .param("playerId", playerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].winnerName").value("Player1"))
                .andExpect(jsonPath("$[0].totalTurns").value(5));
    }

    @Test
    void listHistory_sinResultados_retorna200Vacio() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(matchHistoryService.getHistoryByPlayer(playerId)).thenReturn(List.of());

        mockMvc.perform(get("/api/matches/history")
                        .param("playerId", playerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getHistoryDetail_existe_retorna200() throws Exception {
        UUID matchId = UUID.randomUUID();
        MatchSummaryResponse detail = new MatchSummaryResponse(
                matchId.toString(), "Winner", "Loser",
                10, Instant.now(), 600L, "ALL_PRIZES");

        when(matchHistoryService.getHistoryDetail(matchId)).thenReturn(Optional.of(detail));

        mockMvc.perform(get("/api/matches/history/{id}", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerName").value("Winner"))
                .andExpect(jsonPath("$.totalTurns").value(10));
    }

    @Test
    void getHistoryDetail_noExiste_retorna404() throws Exception {
        UUID matchId = UUID.randomUUID();

        when(matchHistoryService.getHistoryDetail(matchId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/matches/history/{id}", matchId))
                .andExpect(status().isNotFound());
    }
}
