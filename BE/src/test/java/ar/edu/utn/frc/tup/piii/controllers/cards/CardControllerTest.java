package ar.edu.utn.frc.tup.piii.controllers.cards;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.dtos.cards.*;
import ar.edu.utn.frc.tup.piii.services.cards.CardCacheSyncService;
import ar.edu.utn.frc.tup.piii.services.cards.CardCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CardCatalogService cardCatalogService;

    @Mock
    private CardCacheSyncService cardCacheSyncService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CardController(cardCatalogService, cardCacheSyncService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchCards_conResultados_retorna200() throws Exception {
        CardSummaryResponse summary = new CardSummaryResponse(
                "xy1-1", "Pikachu", "POKEMON", "xy1", "1", "http://img/small",
                List.of("Basic"), "BASIC");
        CardSearchResponse response = new CardSearchResponse(
                List.of(summary), 0, 20, 1);

        when(cardCatalogService.searchCards(any(CardSearchRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("xy1-1"))
                .andExpect(jsonPath("$.items[0].name").value("Pikachu"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void searchCards_sinResultados_retorna200Vacio() throws Exception {
        CardSearchResponse response = new CardSearchResponse(List.of(), 0, 20, 0);

        when(cardCatalogService.searchCards(any(CardSearchRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/cards")
                        .param("query", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void searchCards_conFiltros_retorna200() throws Exception {
        CardSearchResponse response = new CardSearchResponse(List.of(), 0, 20, 0);

        when(cardCatalogService.searchCards(any(CardSearchRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/cards")
                        .param("query", "Pikachu")
                        .param("supertype", "POKEMON")
                        .param("setCode", "xy1")
                        .param("stage", "BASIC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getCardById_existe_retorna200() throws Exception {
        CardDetailResponse response = new CardDetailResponse(
                "xy1-1", "Pikachu", "POKEMON", List.of("Basic"), "xy1", "1",
                "http://img/small", "http://img/large", List.of(), 60, "BASIC",
                null, List.of("Lightning"), List.of(), List.of(), List.of(),
                List.of(), false, false, List.of(), List.of());

        when(cardCatalogService.getCardById("xy1-1")).thenReturn(response);

        mockMvc.perform(get("/api/cards/{id}", "xy1-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("xy1-1"))
                .andExpect(jsonPath("$.name").value("Pikachu"));
    }

    @Test
    void getCardById_noExiste_retorna404() throws Exception {
        when(cardCatalogService.getCardById("nonexistent"))
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.NotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/{id}", "nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void syncCards_exitoso_retorna200() throws Exception {
        CardSyncResponse response = new CardSyncResponse(true, "Sync completed", 10, 5);

        when(cardCacheSyncService.syncAll()).thenReturn(response);

        mockMvc.perform(post("/api/cards/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newCards").value(10))
                .andExpect(jsonPath("$.updatedCards").value(5));
    }

    @Test
    void searchCards_errorServidor_retorna500() throws Exception {
        when(cardCatalogService.searchCards(any(CardSearchRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void syncCards_errorServidor_retorna500() throws Exception {
        when(cardCacheSyncService.syncAll())
                .thenThrow(new RuntimeException("API unavailable"));

        mockMvc.perform(post("/api/cards/sync"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
