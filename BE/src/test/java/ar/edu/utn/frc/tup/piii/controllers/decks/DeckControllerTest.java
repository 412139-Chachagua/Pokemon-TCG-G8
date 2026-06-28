package ar.edu.utn.frc.tup.piii.controllers.decks;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.dtos.decks.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckValidationResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.UpdateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.decks.ValidateDeckRequest;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.services.decks.DeckService;
import ar.edu.utn.frc.tup.piii.services.decks.PredefinedDeckService;
import ar.edu.utn.frc.tup.piii.services.decks.RandomDeckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DeckControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeckService deckService;

    @Mock
    private RandomDeckService randomDeckService;

    @Mock
    private PredefinedDeckService predefinedDeckService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DeckController(deckService, randomDeckService, predefinedDeckService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateDeck() throws Exception {
        CreateDeckRequest request = new CreateDeckRequest("Test Deck", "p1", List.of());
        DeckResponse response = new DeckResponse("id", "Test Deck", "p1", "USER", 0, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(deckService.createDeck(any())).thenReturn(response);

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Deck"));
    }

    @Test
    void shouldGetDeck() throws Exception {
        UUID id = UUID.randomUUID();
        DeckResponse response = new DeckResponse(id.toString(), "Test Deck", null, "USER", 60, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(deckService.getDeck(id)).thenReturn(response);

        mockMvc.perform(get("/api/decks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void shouldUpdateDeck() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateDeckRequest request = new UpdateDeckRequest("Updated", List.of());
        DeckResponse response = new DeckResponse(id.toString(), "Updated", null, "USER", 60, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(deckService.updateDeck(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/decks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void shouldDeleteDeck() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/decks/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldListDecksByPlayer() throws Exception {
        UUID playerId = UUID.randomUUID();
        when(deckService.listDecksByPlayer(playerId)).thenReturn(List.of());

        mockMvc.perform(get("/api/decks").param("playerId", playerId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldValidateDeck() throws Exception {
        UUID id = UUID.randomUUID();
        DeckValidationResponse response = new DeckValidationResponse(true, List.of());

        when(deckService.validateDeck(id)).thenReturn(response);

        mockMvc.perform(post("/api/decks/{id}/validate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void exportDeck_existe_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] pdfBytes = "fake-pdf-content".getBytes();

        when(deckService.exportDeckPdf(id)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/decks/{id}/export", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + id + ".pdf\""));
    }

    @Test
    void exportDeck_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();

        when(deckService.exportDeckPdf(id))
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.NotFoundException("Deck not found"));

        mockMvc.perform(get("/api/decks/{id}/export", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void listPredefined_retorna200() throws Exception {
        DeckResponse predefined = new DeckResponse("pre-1", "Destruction Rush", null, "PREDEFINED", 60, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(predefinedDeckService.getAllAsResponse()).thenReturn(List.of(predefined));

        mockMvc.perform(get("/api/decks/predefined"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Destruction Rush"))
                .andExpect(jsonPath("$[0].source").value("PREDEFINED"));
    }

    @Test
    void copyDeck_existe_retorna201() throws Exception {
        UUID deckId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        DeckResponse response = new DeckResponse("new-deck", "Destruction Rush Copy", playerId.toString(), "USER", 60, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(predefinedDeckService.copyToPlayer(eq(deckId), eq(playerId))).thenReturn(response);

        mockMvc.perform(post("/api/decks/{id}/copy", deckId)
                        .param("playerId", playerId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Destruction Rush Copy"));
    }

    @Test
    void copyDeck_noExiste_retorna404() throws Exception {
        UUID deckId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        when(predefinedDeckService.copyToPlayer(eq(deckId), eq(playerId)))
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.NotFoundException("Deck not found"));

        mockMvc.perform(post("/api/decks/{id}/copy", deckId)
                        .param("playerId", playerId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void validateCards_bodyValido_retorna200() throws Exception {
        ValidateDeckRequest request = new ValidateDeckRequest(List.of());
        DeckValidationResponse response = new DeckValidationResponse(true, List.of());

        when(deckService.validateCards(any(ValidateDeckRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/decks/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void randomDeck_retorna200() throws Exception {
        DeckResponse response = new DeckResponse("random-1", "Random Deck", null, "RANDOM", 60, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(randomDeckService.generateRandomDeck()).thenReturn(response);

        mockMvc.perform(post("/api/decks/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RANDOM"));
    }

    @Test
    void randomDeck_errorRetorna422() throws Exception {
        when(randomDeckService.generateRandomDeck())
                .thenThrow(new ValidationException("Could not generate valid deck"));

        mockMvc.perform(post("/api/decks/random"))
                .andExpect(status().isUnprocessableEntity());
    }

    // === importDecks ===

    @Test
    void importDeck_pdf_retorna201() throws Exception {
        UUID playerId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "decks.pdf", "application/pdf", "fake-pdf".getBytes());

        DeckResponse deck = new DeckResponse("deck-1", "Imported", playerId.toString(), "USER", 60, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(deckService.importPdfDecks(any(), eq(playerId))).thenReturn(List.of(deck));

        mockMvc.perform(multipart("/api/decks/import")
                        .file(file)
                        .param("playerId", playerId.toString())
                        .param("format", "pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("Imported"));
    }

    @Test
    void importDeck_texto_retorna201() throws Exception {
        UUID playerId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "decks.json", "application/json", "[{\"name\":\"Test\"}]".getBytes());

        DeckResponse deck = new DeckResponse("deck-1", "Imported", playerId.toString(), "USER", 60, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(deckService.importDecks(any(String.class), eq("json"), eq(playerId), any()))
                .thenReturn(List.of(deck));

        mockMvc.perform(multipart("/api/decks/import")
                        .file(file)
                        .param("playerId", playerId.toString())
                        .param("format", "json"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("Imported"));
    }

    @Test
    void importDeck_ioException_retorna400() throws Exception {
        UUID playerId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "decks.txt", "text/plain", "content".getBytes());

        when(deckService.importDecks(any(String.class), eq("txt"), eq(playerId), any()))
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.ValidationException("Error reading file"));

        mockMvc.perform(multipart("/api/decks/import")
                        .file(file)
                        .param("playerId", playerId.toString())
                        .param("format", "txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // === error cases for existing endpoints ===

    @Test
    void createDeck_errorValidacion_retorna400() throws Exception {
        CreateDeckRequest request = new CreateDeckRequest("Test Deck", "p1", List.of());

        when(deckService.createDeck(any()))
                .thenThrow(new ValidationException("Invalid deck"));

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getDeck_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();

        when(deckService.getDeck(id))
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.NotFoundException("Deck not found"));

        mockMvc.perform(get("/api/decks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void updateDeck_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateDeckRequest request = new UpdateDeckRequest("Updated", List.of());

        when(deckService.updateDeck(any(), any()))
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.NotFoundException("Deck not found"));

        mockMvc.perform(put("/api/decks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void validateDeck_invalido_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        DeckValidationResponse response = new DeckValidationResponse(false,
                List.of(new DeckValidationResponse.DeckValidationError("INVALID_SIZE", "Deck must have 60 cards", null)));

        when(deckService.validateDeck(id)).thenReturn(response);

        mockMvc.perform(post("/api/decks/{id}/validate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_SIZE"));
    }

    @Test
    void validateCards_invalido_retorna200() throws Exception {
        ValidateDeckRequest request = new ValidateDeckRequest(List.of());
        DeckValidationResponse response = new DeckValidationResponse(false,
                List.of(new DeckValidationResponse.DeckValidationError("TOO_FEW", "Need more cards", null)));

        when(deckService.validateCards(any(ValidateDeckRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/decks/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
