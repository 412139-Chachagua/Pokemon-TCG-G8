package ar.edu.utn.frc.tup.piii.domain.cards;

import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffect;

import java.util.List;

public class PokemonCardDefinition extends CardDefinition {
    private int hp;
    private String stage;
    private String evolvesFrom;
    private List<EnergyType> types;
    private List<AttackDefinition> attacks;
    private List<WeaknessDefinition> weaknesses;
    private List<ResistanceDefinition> resistances;
    private List<EnergyType> retreatCost;
    private boolean isEx;
    private boolean isMega;
    private List<AbilityDefinition> abilities;

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getEvolvesFrom() { return evolvesFrom; }
    public void setEvolvesFrom(String evolvesFrom) { this.evolvesFrom = evolvesFrom; }
    public List<EnergyType> getTypes() { return types; }
    public void setTypes(List<EnergyType> types) { this.types = types; }
    public List<AttackDefinition> getAttacks() { return attacks; }
    public void setAttacks(List<AttackDefinition> attacks) { this.attacks = attacks; }
    public List<WeaknessDefinition> getWeaknesses() { return weaknesses; }
    public void setWeaknesses(List<WeaknessDefinition> weaknesses) { this.weaknesses = weaknesses; }
    public List<ResistanceDefinition> getResistances() { return resistances; }
    public void setResistances(List<ResistanceDefinition> resistances) { this.resistances = resistances; }
    public List<EnergyType> getRetreatCost() { return retreatCost; }
    public void setRetreatCost(List<EnergyType> retreatCost) { this.retreatCost = retreatCost; }
    public boolean isEx() { return isEx; }
    public void setEx(boolean ex) { isEx = ex; }
    public boolean isMega() { return isMega; }
    public void setMega(boolean mega) { isMega = mega; }
    public List<AbilityDefinition> getAbilities() { return abilities; }
    public void setAbilities(List<AbilityDefinition> abilities) { this.abilities = abilities; }

    public static class AttackDefinition {
        private int index;
        private String name;
        private List<EnergyType> cost;
        private String damage;
        private String text;
        private List<AttackEffect> effects;

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<EnergyType> getCost() { return cost; }
        public void setCost(List<EnergyType> cost) { this.cost = cost; }
        public String getDamage() { return damage; }
        public void setDamage(String damage) { this.damage = damage; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public List<AttackEffect> getEffects() { return effects; }
        public void setEffects(List<AttackEffect> effects) { this.effects = effects; }
    }

    public static class WeaknessDefinition {
        private EnergyType type;
        private String value;

        public EnergyType getType() { return type; }
        public void setType(EnergyType type) { this.type = type; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class ResistanceDefinition {
        private EnergyType type;
        private String value;

        public EnergyType getType() { return type; }
        public void setType(EnergyType type) { this.type = type; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
