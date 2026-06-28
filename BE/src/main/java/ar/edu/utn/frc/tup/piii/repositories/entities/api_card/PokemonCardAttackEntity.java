package ar.edu.utn.frc.tup.piii.repositories.entities.api_card;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pokemon_card_attacks")
@Deprecated
public class PokemonCardAttackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pokemon_card_id", nullable = false)
    private PokemonCardEntity pokemonCard;

    @Column(name = "attack_index", nullable = false)
    private Integer attackIndex;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "printed_cost", columnDefinition = "TEXT")
    private String printedCost;

    @Column(name = "converted_energy_cost", nullable = false)
    private Integer convertedEnergyCost = 0;

    @Column(name = "damage_text", length = 40)
    private String damageText;

    @Column(name = "effect_text", columnDefinition = "TEXT")
    private String effectText;

    @Column(name = "effect_code", length = 80)
    private String effectCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
