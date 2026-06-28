package ar.edu.utn.frc.tup.piii.engine.attack;

import java.util.Map;

public class AttackEffect {
    private AttackEffectType type;
    private Map<String, Object> params;

    public AttackEffect() {}

    public AttackEffect(AttackEffectType type, Map<String, Object> params) {
        this.type = type;
        this.params = params;
    }

    public AttackEffectType getType() { return type; }
    public void setType(AttackEffectType type) { this.type = type; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
