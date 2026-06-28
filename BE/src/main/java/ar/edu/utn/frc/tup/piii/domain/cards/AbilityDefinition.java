package ar.edu.utn.frc.tup.piii.domain.cards;

public class AbilityDefinition {
    private String name;
    private String text;
    private AbilityType type;

    public AbilityDefinition() {
    }

    public AbilityDefinition(String name, String text, AbilityType type) {
        this.name = name;
        this.text = text;
        this.type = type;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public AbilityType getType() { return type; }
    public void setType(AbilityType type) { this.type = type; }
}
