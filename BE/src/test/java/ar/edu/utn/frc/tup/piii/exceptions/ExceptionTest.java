package ar.edu.utn.frc.tup.piii.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void shouldCreateDomainException() {
        DomainException ex = new DomainException("SOME_CODE", "Some message");

        assertEquals("SOME_CODE", ex.getCode());
        assertEquals("Some message", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void shouldCreateConflictException() {
        ConflictException ex = new ConflictException("Resource already exists");

        assertEquals("CONFLICT", ex.getCode());
        assertEquals("Resource already exists", ex.getMessage());
        assertInstanceOf(DomainException.class, ex);
    }

    @Test
    void shouldCreateNotFoundException() {
        NotFoundException ex = new NotFoundException("User not found");

        assertEquals("NOT_FOUND", ex.getCode());
        assertEquals("User not found", ex.getMessage());
        assertInstanceOf(DomainException.class, ex);
    }

    @Test
    void shouldCreateValidationException() {
        ValidationException ex = new ValidationException("Invalid input");

        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertEquals("Invalid input", ex.getMessage());
        assertInstanceOf(DomainException.class, ex);
    }

    @Test
    void shouldCreateValidationExceptionWithDetails() {
        ValidationException ex = new ValidationException("Invalid input", "details object");

        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertEquals("Invalid input", ex.getMessage());
    }

    @Test
    void shouldCreateStorageException() {
        StorageException ex = new StorageException("Storage error");

        assertEquals("Storage error", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
        assertNull(ex.getCause());
    }

    @Test
    void shouldCreateStorageExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        StorageException ex = new StorageException("Storage error", cause);

        assertEquals("Storage error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void shouldThrowDomainException() {
        assertThrows(DomainException.class, () -> { throw new ConflictException("test"); });
        assertThrows(DomainException.class, () -> { throw new NotFoundException("test"); });
        assertThrows(DomainException.class, () -> { throw new ValidationException("test"); });
    }

    @Test
    void shouldThrowRuntimeException() {
        assertThrows(RuntimeException.class, () -> { throw new StorageException("test"); });
        assertThrows(RuntimeException.class, () -> { throw new ConflictException("test"); });
    }
}
