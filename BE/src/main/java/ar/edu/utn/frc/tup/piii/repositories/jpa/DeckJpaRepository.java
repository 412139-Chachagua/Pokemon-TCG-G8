package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeckJpaRepository extends JpaRepository<DeckEntity, UUID> {
    List<DeckEntity> findByOwnerPlayerId(UUID ownerPlayerId);

    @Query("SELECT d FROM DeckEntity d LEFT JOIN FETCH d.cards WHERE d.id = :id")
    Optional<DeckEntity> findByIdWithCards(@Param("id") UUID id);

    @Query("SELECT d FROM DeckEntity d LEFT JOIN FETCH d.cards WHERE d.ownerPlayer IS NULL")
    List<DeckEntity> findPredefinedDecks();
}