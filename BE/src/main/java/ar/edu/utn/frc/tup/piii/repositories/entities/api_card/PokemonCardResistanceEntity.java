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
@Table(name = "pokemon_card_resistances")
@Deprecated
public class PokemonCardResistanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pokemon_card_id", nullable = false)
    private PokemonCardEntity pokemonCard;

    @Column(name = "resistance_type", nullable = false, length = 30)
    private String resistanceType;

    @Column(name = "resistance_value", columnDefinition = "TEXT")
    private String resistanceValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
