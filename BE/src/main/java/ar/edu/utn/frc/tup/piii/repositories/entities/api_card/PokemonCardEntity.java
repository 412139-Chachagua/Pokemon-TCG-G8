package ar.edu.utn.frc.tup.piii.repositories.entities.api_card;

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
@Table(name = "pokemon_cards")
@Deprecated
public class PokemonCardEntity {

    @Id
    @Column(name = "id", length = 80)
    private String id;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "supertype", nullable = false, length = 30)
    private String supertype;

    @Column(name = "subtypes", columnDefinition = "TEXT")
    private String subtypes;

    @Column(name = "hp")
    private Integer hp;

    @Column(name = "pokemon_types", columnDefinition = "TEXT")
    private String pokemonTypes;

    @Column(name = "evolves_from", length = 160)
    private String evolvesFrom;

    @Column(name = "retreat_cost", columnDefinition = "TEXT")
    private String retreatCost;

    @Column(name = "converted_retreat_cost")
    private Integer convertedRetreatCost;

    @Column(name = "is_ex", nullable = false)
    private Boolean isEx = false;

    @Column(name = "is_mega", nullable = false)
    private Boolean isMega = false;

    @Column(name = "rules_text", columnDefinition = "TEXT")
    private String rulesText;

    @Column(name = "rarity", nullable = false, length = 30)
    private String rarity;

    @Column(name = "image_small_url", columnDefinition = "TEXT")
    private String imageSmallUrl;

    @Column(name = "image_large_url", columnDefinition = "TEXT")
    private String imageLargeUrl;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "pokemonCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PokemonCardAttackEntity> attacks = new ArrayList<>();

    @OneToMany(mappedBy = "pokemonCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PokemonCardWeaknessEntity> weaknesses = new ArrayList<>();

    @OneToMany(mappedBy = "pokemonCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PokemonCardResistanceEntity> resistances = new ArrayList<>();

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
