package ar.edu.utn.frc.tup.piii.controllers.ranking;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.dtos.ranking.PlayerStatsResponse;
import ar.edu.utn.frc.tup.piii.dtos.ranking.RankingEntryResponse;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.services.ranking.PlayerStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RankingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlayerStatsService playerStatsService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RankingController(playerStatsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnRanking() throws Exception {
        List<RankingEntryResponse> ranking = List.of(
                new RankingEntryResponse(1, UUID.randomUUID().toString(), "Alice", 10, 2, 83.33, 5, 5),
                new RankingEntryResponse(2, UUID.randomUUID().toString(), "Bob", 5, 3, 62.5, 2, 3)
        );
        when(playerStatsService.getRanking()).thenReturn(ranking);

        mockMvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].displayName").value("Alice"))
                .andExpect(jsonPath("$[0].totalWins").value(10))
                .andExpect(jsonPath("$[1].displayName").value("Bob"));
    }

    @Test
    void shouldReturnPlayerStats() throws Exception {
        UUID playerId = UUID.randomUUID();
        PlayerStatsResponse stats = new PlayerStatsResponse(
                playerId.toString(), "Charlie", 7, 3, 2, 4);
        when(playerStatsService.getPlayerStats(playerId)).thenReturn(stats);

        mockMvc.perform(get("/api/players/{id}/stats", playerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.playerId").value(playerId.toString()))
                .andExpect(jsonPath("$.totalWins").value(7))
                .andExpect(jsonPath("$.currentWinStreak").value(2))
                .andExpect(jsonPath("$.maxWinStreak").value(4));
    }

    @Test
    void getRanking_vacio_retorna200Vacio() throws Exception {
        when(playerStatsService.getRanking()).thenReturn(List.of());

        mockMvc.perform(get("/api/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPlayerStats_noExiste_retorna404() throws Exception {
        UUID playerId = UUID.randomUUID();

        when(playerStatsService.getPlayerStats(playerId))
                .thenThrow(new NotFoundException("Player not found"));

        mockMvc.perform(get("/api/players/{id}/stats", playerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
