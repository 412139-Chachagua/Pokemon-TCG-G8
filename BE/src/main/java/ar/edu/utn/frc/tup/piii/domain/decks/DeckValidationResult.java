package ar.edu.utn.frc.tup.piii.domain.decks;

import java.util.List;

public class DeckValidationResult {
    private boolean valid;
    private List<DeckValidationError> errors;

    public DeckValidationResult() {}

    public DeckValidationResult(boolean valid, List<DeckValidationError> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public List<DeckValidationError> getErrors() { return errors; }
    public void setErrors(List<DeckValidationError> errors) { this.errors = errors; }
}
