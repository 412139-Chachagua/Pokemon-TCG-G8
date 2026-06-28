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
@Table(name = "trainer_cards")
@Deprecated
public class TrainerCardEntity {

    @Id
    @Column(name = "id", length = 80)
    private String id;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "supertype", nullable = false, length = 30)
    private String supertype;

    @Column(name = "subtypes", columnDefinition = "TEXT")
    private String subtypes;

    @Column(name = "rules_text", columnDefinition = "TEXT")
    private String rulesText;

    @Column(name = "rarity", nullable = false, length = 30)
    private String rarity;

    @Column(name = "image_small_url", columnDefinition = "TEXT")
    private String imageSmallUrl;

    @Column(name = "image_large_url", columnDefinition = "TEXT")
    private String imageLargeUrl;

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
