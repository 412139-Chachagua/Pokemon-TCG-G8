package ar.edu.utn.frc.tup.piii.dtos.users;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDtosTest {

    @Test
    void shouldCreateCreateUserRequest() {
        CreateUserRequest request = new CreateUserRequest("ash@pokemon.com", "password123", "Ash Ketchum");

        assertEquals("ash@pokemon.com", request.email());
        assertEquals("password123", request.password());
        assertEquals("Ash Ketchum", request.displayName());
    }

    @Test
    void shouldCreateCreateUserRequestWithNullFields() {
        CreateUserRequest request = new CreateUserRequest(null, null, null);

        assertNull(request.email());
        assertNull(request.password());
        assertNull(request.displayName());
    }

    @Test
    void shouldCreateLoginRequest() {
        LoginRequest request = new LoginRequest("ash@pokemon.com", "password123");

        assertEquals("ash@pokemon.com", request.email());
        assertEquals("password123", request.password());
    }

    @Test
    void shouldCreateLoginRequestWithNullFields() {
        LoginRequest request = new LoginRequest(null, null);

        assertNull(request.email());
        assertNull(request.password());
    }

    @Test
    void shouldCreateUpdateUserRequest() {
        UpdateUserRequest request = new UpdateUserRequest("new@email.com", "oldPass", "newPass");

        assertEquals("new@email.com", request.email());
        assertEquals("oldPass", request.currentPassword());
        assertEquals("newPass", request.newPassword());
    }

    @Test
    void shouldCreateUpdateUserRequestWithNulls() {
        UpdateUserRequest request = new UpdateUserRequest(null, null, null);

        assertNull(request.email());
        assertNull(request.currentPassword());
        assertNull(request.newPassword());
    }

    @Test
    void shouldCreateUserResponse() {
        UserResponse response = new UserResponse("u1", "ash@pokemon.com", "Ash Ketchum", "p1", "token-123");

        assertEquals("u1", response.id());
        assertEquals("ash@pokemon.com", response.email());
        assertEquals("Ash Ketchum", response.displayName());
        assertEquals("p1", response.playerId());
        assertEquals("token-123", response.token());
    }

    @Test
    void shouldCreateUserResponseWithNulls() {
        UserResponse response = new UserResponse(null, null, null, null, null);

        assertNull(response.id());
        assertNull(response.email());
        assertNull(response.token());
    }
}
