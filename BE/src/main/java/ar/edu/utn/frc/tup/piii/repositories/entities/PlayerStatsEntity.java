package ar.edu.utn.frc.tup.piii.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_stats")
public class PlayerStatsEntity {

    @Id
    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "total_wins", nullable = false)
    private int totalWins = 0;

    @Column(name = "total_losses", nullable = false)
    private int totalLosses = 0;

    @Column(name = "current_win_streak", nullable = false)
    private int currentWinStreak = 0;

    @Column(name = "max_win_streak", nullable = false)
    private int maxWinStreak = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
