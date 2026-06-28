package ar.edu.utn.frc.tup.piii.exceptions;

public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
