package ar.edu.utn.frc.tup.piii.repositories.jpa.api_card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.EnergyCardEntity;

@Repository
@Deprecated
public interface EnergyCardJpaRepository extends JpaRepository<EnergyCardEntity, String> {
}