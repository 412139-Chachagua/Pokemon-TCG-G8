package ar.edu.utn.frc.tup.piii.exceptions;

public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
