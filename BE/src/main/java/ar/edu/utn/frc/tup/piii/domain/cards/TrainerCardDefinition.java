package ar.edu.utn.frc.tup.piii.domain.cards;

public class TrainerCardDefinition extends CardDefinition {
    private TrainerSubtype trainerSubtype;
    private boolean isAceSpec;
    private String effectCode;

    public TrainerSubtype getTrainerSubtype() { return trainerSubtype; }
    public void setTrainerSubtype(TrainerSubtype trainerSubtype) { this.trainerSubtype = trainerSubtype; }
    public boolean isAceSpec() { return isAceSpec; }
    public void setAceSpec(boolean aceSpec) { isAceSpec = aceSpec; }
    public String getEffectCode() { return effectCode; }
    public void setEffectCode(String effectCode) { this.effectCode = effectCode; }
}
