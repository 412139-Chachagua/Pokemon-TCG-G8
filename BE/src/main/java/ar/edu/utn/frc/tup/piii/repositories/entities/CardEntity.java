package ar.edu.utn.frc.tup.piii.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cards")
public class CardEntity {

    @Id
    @Column(name = "id", length = 80)
    private String id;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "supertype", nullable = false, length = 30)
    private String supertype;

    @Column(name = "subtypes", columnDefinition = "TEXT")
    private String subtypes;

    @Column(name = "set_code", nullable = false, length = 30)
    private String setCode;

    @Column(name = "number", length = 30)
    private String number;

    @Column(name = "rarity", length = 80)
    private String rarity;

    @Column(name = "image_small_url", columnDefinition = "TEXT")
    private String imageSmallUrl;

    @Column(name = "image_large_url", columnDefinition = "TEXT")
    private String imageLargeUrl;

    @Column(name = "hp")
    private Integer hp;

    @Column(name = "pokemon_stage", length = 30)
    private String pokemonStage;

    @Column(name = "evolves_from", length = 160)
    private String evolvesFrom;

    @Column(name = "evolves_to", columnDefinition = "TEXT")
    private String evolvesTo;

    @Column(name = "abilities", columnDefinition = "TEXT")
    private String abilities;

    @Column(name = "pokemon_types", columnDefinition = "TEXT")
    private String pokemonTypes;

    @Column(name = "retreat_cost", columnDefinition = "TEXT")
    private String retreatCost;

    @Column(name = "converted_retreat_cost")
    private Integer convertedRetreatCost;

    @Column(name = "is_ex", nullable = false)
    private Boolean isEx = false;

    @Column(name = "is_mega", nullable = false)
    private Boolean isMega = false;

    @Column(name = "energy_card_type", length = 30)
    private String energyCardType;

    @Column(name = "provides_energy_types", columnDefinition = "TEXT")
    private String providesEnergyTypes;

    @Column(name = "trainer_subtype", length = 30)
    private String trainerSubtype;

    @Column(name = "effect_code", length = 60)
    private String effectCode;

    @Column(name = "is_ace_spec", nullable = false)
    private Boolean isAceSpec = false;

    @Column(name = "strategy_key", length = 30)
    private String strategyKey;

    @Column(name = "rules_text", columnDefinition = "TEXT")
    private String rulesText;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CardAttackEntity> attacks = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CardWeaknessEntity> weaknesses = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CardResistanceEntity> resistances = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
