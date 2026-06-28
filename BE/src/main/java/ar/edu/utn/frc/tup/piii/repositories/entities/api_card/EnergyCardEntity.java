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
@Table(name = "energy_cards")
@Deprecated
public class EnergyCardEntity {

    @Id
    @Column(name = "id", length = 80)
    private String id;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "supertype", nullable = false, length = 30)
    private String supertype;

    @Column(name = "subtypes", columnDefinition = "TEXT")
    private String subtypes;

    @Column(name = "energy_card_type", length = 30)
    private String energyCardType;

    @Column(name = "provides_energy_types", columnDefinition = "TEXT")
    private String providesEnergyTypes;

    @Column(name = "rules_text", columnDefinition = "TEXT")
    private String rulesText;
    
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
