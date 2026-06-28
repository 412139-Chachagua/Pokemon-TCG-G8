package ar.edu.utn.frc.tup.piii.repositories.jpa.api_card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.repositories.entities.api_card.PokemonCardResistanceEntity;

@Repository
@Deprecated
public interface PokemonCardResistanceJpaRepository extends JpaRepository<PokemonCardResistanceEntity, java.util.UUID> {
}
