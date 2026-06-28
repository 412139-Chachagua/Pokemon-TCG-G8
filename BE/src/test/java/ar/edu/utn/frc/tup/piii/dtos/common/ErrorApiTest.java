package ar.edu.utn.frc.tup.piii.dtos.common;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorApiTest {

    @Test
    void shouldCreateErrorApiUsingBuilder() {
        Map<String, String> details = new HashMap<>();
        details.put("field", "name");
        ErrorApi error = ErrorApi.builder()
                .timestamp("2025-01-01T00:00:00")
                .status(404)
                .error("Not Found")
                .code("NOT_FOUND")
                .message("Resource not found")
                .path("/api/resource")
                .details(details)
                .build();

        assertEquals("2025-01-01T00:00:00", error.getTimestamp());
        assertEquals(404, error.getStatus());
        assertEquals("Not Found", error.getError());
        assertEquals("NOT_FOUND", error.getCode());
        assertEquals("Resource not found", error.getMessage());
        assertEquals("/api/resource", error.getPath());
        assertEquals("name", error.getDetails().get("field"));
    }

    @Test
    void shouldCreateErrorApiUsingNoArgsConstructor() {
        ErrorApi error = new ErrorApi();
        assertNull(error.getTimestamp());
        assertNull(error.getStatus());
        assertNull(error.getError());
        assertNull(error.getCode());
        assertNull(error.getMessage());
        assertNull(error.getPath());
        assertNull(error.getDetails());
    }

    @Test
    void shouldCreateErrorApiUsingAllArgsConstructor() {
        Map<String, String> details = new HashMap<>();
        ErrorApi error = new ErrorApi("2025-01-01T00:00:00", 500, "Internal Server Error", "INTERNAL_ERROR", "Something went wrong", "/api/test", details);

        assertEquals("2025-01-01T00:00:00", error.getTimestamp());
        assertEquals(500, error.getStatus());
        assertEquals("Internal Server Error", error.getError());
        assertEquals("INTERNAL_ERROR", error.getCode());
        assertEquals("/api/test", error.getPath());
        assertEquals(details, error.getDetails());
    }

    @Test
    void shouldSetAndGetFields() {
        ErrorApi error = new ErrorApi();
        error.setTimestamp("2025-06-01T12:00:00");
        error.setStatus(400);
        error.setError("Bad Request");
        error.setCode("VALIDATION_ERROR");
        error.setMessage("Invalid input");
        error.setPath("/api/input");

        assertEquals("2025-06-01T12:00:00", error.getTimestamp());
        assertEquals(400, error.getStatus());
        assertEquals("Bad Request", error.getError());
        assertEquals("VALIDATION_ERROR", error.getCode());
        assertEquals("Invalid input", error.getMessage());
        assertEquals("/api/input", error.getPath());
    }

    @Test
    void shouldHandleNullDetails() {
        ErrorApi error = ErrorApi.builder().status(200).build();
        assertNull(error.getDetails());
        assertNull(error.getTimestamp());
        assertEquals(200, error.getStatus());
    }

    @Test
    void shouldTestEqualsAndHashCode() {
        ErrorApi error1 = ErrorApi.builder().status(404).code("NOT_FOUND").build();
        ErrorApi error2 = ErrorApi.builder().status(404).code("NOT_FOUND").build();
        ErrorApi error3 = ErrorApi.builder().status(500).code("INTERNAL_ERROR").build();

        assertEquals(error1, error2);
        assertEquals(error1.hashCode(), error2.hashCode());
        assertNotEquals(error1, error3);
    }

    @Test
    void shouldTestToString() {
        ErrorApi error = ErrorApi.builder().status(200).message("OK").build();
        String str = error.toString();
        assertTrue(str.contains("200"));
        assertTrue(str.contains("OK"));
    }
}
