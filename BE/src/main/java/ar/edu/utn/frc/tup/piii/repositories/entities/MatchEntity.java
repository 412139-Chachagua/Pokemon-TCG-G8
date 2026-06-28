package ar.edu.utn.frc.tup.piii.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "current_phase", length = 30)
    private String currentPhase;

    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber = 0;

    @Column(name = "current_player_id")
    private UUID currentPlayerId;

    @Column(name = "first_player_id")
    private UUID firstPlayerId;

    @Column(name = "winner_player_id")
    private UUID winnerPlayerId;

    @Column(name = "finish_reason", length = 60)
    private String finishReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "hand_size")
    private Integer handSize = 7;

    @Column(name = "latest_state_version")
    private Long latestStateVersion;

    @Column(name = "last_resumed_player_id")
    private UUID lastResumedPlayerId;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MatchPlayerEntity> players = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MatchStateEntity> states = new ArrayList<>();

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
