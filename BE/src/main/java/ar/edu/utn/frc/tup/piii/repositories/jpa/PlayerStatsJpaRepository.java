package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerStatsJpaRepository extends JpaRepository<PlayerStatsEntity, UUID> {

    Optional<PlayerStatsEntity> findByPlayerId(UUID playerId);

    @Query("SELECT s FROM PlayerStatsEntity s ORDER BY s.totalWins DESC, s.maxWinStreak DESC")
    List<PlayerStatsEntity> findAllByOrderByTotalWinsDescMaxWinStreakDesc();
}
