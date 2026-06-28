package ar.edu.utn.frc.tup.piii.controllers.players;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.dtos.players.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.players.UpdatePlayerRequest;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.services.players.AvatarStorageService;
import ar.edu.utn.frc.tup.piii.services.players.PlayerService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlayerService playerService;

    @Mock
    private AvatarStorageService avatarStorageService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlayerController(playerService, avatarStorageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // === listPlayers ===

    @Test
    void listPlayers_conDatos_retorna200() throws Exception {
        PlayerResponse player = new PlayerResponse(
                UUID.randomUUID().toString(), "Alice", null, Instant.now(), null);

        when(playerService.listAll()).thenReturn(List.of(player));

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Alice"));
    }

    @Test
    void listPlayers_vacio_retorna200Vacio() throws Exception {
        when(playerService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // === getById ===

    @Test
    void getById_existe_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        PlayerResponse response = new PlayerResponse(
                id.toString(), "Alice", null, Instant.now(), null);

        when(playerService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/players/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    void getById_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();

        when(playerService.getById(id))
                .thenThrow(new NotFoundException("Player not found"));

        mockMvc.perform(get("/api/players/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === updatePlayer ===

    @Test
    void updatePlayer_existe_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        UpdatePlayerRequest request = new UpdatePlayerRequest("NewName");
        PlayerResponse response = new PlayerResponse(
                id.toString(), "NewName", null, Instant.now(), null);

        when(playerService.update(eq(id), any(UpdatePlayerRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/players/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("NewName"));
    }

    @Test
    void updatePlayer_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        UpdatePlayerRequest request = new UpdatePlayerRequest("NewName");

        when(playerService.update(eq(id), any(UpdatePlayerRequest.class)))
                .thenThrow(new NotFoundException("Player not found"));

        mockMvc.perform(put("/api/players/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === uploadAvatar ===

    @Test
    void uploadAvatar_existe_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-png-data".getBytes());
        PlayerResponse response = new PlayerResponse(
                id.toString(), "Alice", null, Instant.now(), "avatars/abc123.png");

        when(avatarStorageService.store(any())).thenReturn("avatars/abc123.png");
        when(playerService.updateAvatar(eq(id), eq("avatars/abc123.png"))).thenReturn(response);

        mockMvc.perform(multipart("/api/players/{id}/avatar", id)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("avatars/abc123.png"));
    }

    @Test
    void uploadAvatar_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-png-data".getBytes());

        when(avatarStorageService.store(any())).thenReturn("avatars/abc123.png");
        when(playerService.updateAvatar(eq(id), eq("avatars/abc123.png")))
                .thenThrow(new NotFoundException("Player not found"));

        mockMvc.perform(multipart("/api/players/{id}/avatar", id)
                        .file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
