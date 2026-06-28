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
@Table(name = "match_players")
public class MatchPlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "player_kind", nullable = false, length = 20)
    private String playerKind;

    @Column(name = "side", nullable = false, length = 30)
    private String side;

    @Column(name = "deck_id")
    private UUID deckId;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = Instant.now();
    }
}
