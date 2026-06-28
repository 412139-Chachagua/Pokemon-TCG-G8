package ar.edu.utn.frc.tup.piii.controllers.users;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.dtos.users.*;
import ar.edu.utn.frc.tup.piii.exceptions.ConflictException;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.services.users.UserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // === register ===

    @Test
    void register_dataValida_retorna201() throws Exception {
        CreateUserRequest request = new CreateUserRequest("test@mail.com", "password123", "TestUser");
        UserResponse response = new UserResponse("uuid-1", "test@mail.com", "TestUser", null, "token-123");

        when(userService.register(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.displayName").value("TestUser"));
    }

    @Test
    void register_emailDuplicado_retorna409() throws Exception {
        CreateUserRequest request = new CreateUserRequest("test@mail.com", "password123", "TestUser");

        when(userService.register(any(CreateUserRequest.class)))
                .thenThrow(new ConflictException("Email already registered"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void register_bodyInvalido_retorna400() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emailVacio_retorna400() throws Exception {
        String invalidJson = "{\"email\":\"\",\"password\":\"password123\",\"displayName\":\"TestUser\"}";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // === login ===

    @Test
    void login_credencialesValidas_retorna200() throws Exception {
        LoginRequest request = new LoginRequest("test@mail.com", "password123");
        UserResponse response = new UserResponse("uuid-1", "test@mail.com", "TestUser", null, "token-123");

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"));
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        LoginRequest request = new LoginRequest("test@mail.com", "wrongpassword");

        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.ValidationException("Invalid credentials"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_bodyInvalido_retorna400() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // === getById ===

    @Test
    void getById_existe_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(id.toString(), "test@mail.com", "TestUser", null, null);

        when(userService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"));
    }

    @Test
    void getById_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();

        when(userService.getById(id)).thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === listAll ===

    @Test
    void listAll_retorna200() throws Exception {
        UserResponse user1 = new UserResponse("uuid-1", "a@mail.com", "UserA", null, null);
        UserResponse user2 = new UserResponse("uuid-2", "b@mail.com", "UserB", null, null);

        when(userService.listAll()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("a@mail.com"))
                .andExpect(jsonPath("$[1].email").value("b@mail.com"));
    }

    @Test
    void listAll_vacia_retorna200Vacio() throws Exception {
        when(userService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // === update ===

    @Test
    void update_existe_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest("new@mail.com", null, null);
        UserResponse response = new UserResponse(id.toString(), "new@mail.com", "TestUser", null, null);

        when(userService.update(eq(id), any(UpdateUserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@mail.com"));
    }

    @Test
    void update_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest("new@mail.com", null, null);

        when(userService.update(eq(id), any(UpdateUserRequest.class)))
                .thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === deactivate ===

    @Test
    void deactivate_existe_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(id.toString(), "test@mail.com", "TestUser", null, null);

        when(userService.deactivate(id)).thenReturn(response);

        mockMvc.perform(put("/api/users/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"));
    }

    @Test
    void deactivate_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();

        when(userService.deactivate(id)).thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(put("/api/users/{id}/deactivate", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === activate ===

    @Test
    void activate_existe_retorna200() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(id.toString(), "test@mail.com", "TestUser", null, null);

        when(userService.activate(eq(id), any(String.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/{id}/activate", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"));
    }

    @Test
    void activate_noExiste_retorna404() throws Exception {
        UUID id = UUID.randomUUID();

        when(userService.activate(eq(id), any(String.class)))
                .thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(put("/api/users/{id}/activate", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === validatePassword ===

    @Test
    void validatePassword_valida_retorna200() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/users/{id}/validate-password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        verify(userService).validatePassword(eq(id), eq("password123"));
    }

    @Test
    void validatePassword_invalida_retorna400() throws Exception {
        UUID id = UUID.randomUUID();

        org.mockito.Mockito.doThrow(new ar.edu.utn.frc.tup.piii.exceptions.ValidationException("Invalid password"))
                .when(userService).validatePassword(eq(id), eq("wrongpassword"));

        mockMvc.perform(post("/api/users/{id}/validate-password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrongpassword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void update_bodyInvalido_retorna400() throws Exception {
        UUID id = UUID.randomUUID();

        when(userService.update(eq(id), any(UpdateUserRequest.class)))
                .thenThrow(new ar.edu.utn.frc.tup.piii.exceptions.ValidationException("Invalid data"));

        UpdateUserRequest request = new UpdateUserRequest(null, null, null);

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
