package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.services.CardAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CardAdminService cardAdminService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminController(cardAdminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void regenerateEffectCodes_exitoso_retorna200() throws Exception {
        CardAdminService.RegenerateResult result = new CardAdminService.RegenerateResult();
        result.totalAttacks = 100;
        result.updatedAttacks = 10;
        result.skippedAttacks = 5;
        result.errorAttacks = 2;
        result.errors = List.of("card1/attack1: parse error");

        when(cardAdminService.regenerateAllEffectCodes()).thenReturn(result);

        mockMvc.perform(post("/api/admin/regenerate-effect-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttacks").value(100))
                .andExpect(jsonPath("$.updatedAttacks").value(10))
                .andExpect(jsonPath("$.skippedAttacks").value(5))
                .andExpect(jsonPath("$.errorAttacks").value(2))
                .andExpect(jsonPath("$.errors[0]").value("card1/attack1: parse error"));
    }

    @Test
    void regenerateEffectCodes_sinErrores_retorna200() throws Exception {
        CardAdminService.RegenerateResult result = new CardAdminService.RegenerateResult();
        result.totalAttacks = 50;
        result.updatedAttacks = 0;
        result.skippedAttacks = 50;
        result.errorAttacks = 0;
        result.errors = List.of();

        when(cardAdminService.regenerateAllEffectCodes()).thenReturn(result);

        mockMvc.perform(post("/api/admin/regenerate-effect-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttacks").value(50))
                .andExpect(jsonPath("$.updatedAttacks").value(0))
                .andExpect(jsonPath("$.errorAttacks").value(0));
    }

    @Test
    void regenerateEffectCodes_errorServidor_retorna500() throws Exception {
        when(cardAdminService.regenerateAllEffectCodes())
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.StorageException("Database error"));

        mockMvc.perform(post("/api/admin/regenerate-effect-codes"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("STORAGE_ERROR"));
    }
}
