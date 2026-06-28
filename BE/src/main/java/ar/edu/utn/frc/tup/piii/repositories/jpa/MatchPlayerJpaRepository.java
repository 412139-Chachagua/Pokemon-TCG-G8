package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchPlayerJpaRepository extends JpaRepository<MatchPlayerEntity, UUID> {
    List<MatchPlayerEntity> findByMatch_Id(UUID matchId);
}
