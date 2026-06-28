package ar.edu.utn.frc.tup.piii.controllers.debug;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.exceptions.ConflictException;
import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DebugControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MatchApplicationService matchApplicationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DebugController(matchApplicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void forceSuddenDeath_exitoso_retorna200() throws Exception {
        UUID matchId = UUID.randomUUID();

        doNothing().when(matchApplicationService).forceSuddenDeath(matchId);

        mockMvc.perform(post("/api/debug/matches/{id}/force-sudden-death", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sudden death triggered. End your turn to start the sudden death match."))
                .andExpect(jsonPath("$.matchId").value(matchId.toString()));
    }

    @Test
    void forceSuddenDeath_noExiste_retorna404() throws Exception {
        UUID matchId = UUID.randomUUID();

        doThrow(new ar.edu.utn.frc.tup.piii.exceptions.NotFoundException("Match not found"))
                .when(matchApplicationService).forceSuddenDeath(matchId);

        mockMvc.perform(post("/api/debug/matches/{id}/force-sudden-death", matchId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void forceSuddenDeath_estadoInvalido_retorna409() throws Exception {
        UUID matchId = UUID.randomUUID();

        doThrow(new ConflictException("Match not in correct state"))
                .when(matchApplicationService).forceSuddenDeath(matchId);

        mockMvc.perform(post("/api/debug/matches/{id}/force-sudden-death", matchId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
}
