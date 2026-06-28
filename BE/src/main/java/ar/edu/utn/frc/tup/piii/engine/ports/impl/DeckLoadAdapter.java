package ar.edu.utn.frc.tup.piii.engine.ports.impl;

import ar.edu.utn.frc.tup.piii.domain.decks.Deck;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.engine.ports.DeckLoadPort;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.mappers.decks.DeckMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.DeckJpaRepository;
import ar.edu.utn.frc.tup.piii.services.decks.DeckValidator;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DeckLoadAdapter implements DeckLoadPort {

    private final DeckJpaRepository deckJpaRepository;
    private final DeckValidator deckValidator;
    private final DeckMapper deckMapper;

    public DeckLoadAdapter(DeckJpaRepository deckJpaRepository, DeckValidator deckValidator, DeckMapper deckMapper) {
        this.deckJpaRepository = deckJpaRepository;
        this.deckValidator = deckValidator;
        this.deckMapper = deckMapper;
    }

    @Override
    public Deck loadDeck(UUID deckId) {
        DeckEntity entity = deckJpaRepository.findByIdWithCards(deckId)
                .orElseThrow(() -> new NotFoundException("Deck not found: " + deckId));

        DeckValidationResult validation = deckValidator.validate(
                entity.getCards().stream().map(deckMapper::toDomainCard).collect(Collectors.toList()));

        if (!validation.isValid()) {
            throw new ValidationException("Deck is invalid and cannot be loaded: " + deckId);
        }

        return deckMapper.toDomain(entity);
    }
}
