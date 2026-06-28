package ar.edu.utn.frc.tup.piii.engine.ports.impl;

import ar.edu.utn.frc.tup.piii.domain.decks.Deck;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationError;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.mappers.decks.DeckMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.DeckJpaRepository;
import ar.edu.utn.frc.tup.piii.services.decks.DeckValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckLoadAdapterTest {

    @Mock
    private DeckJpaRepository deckJpaRepository;
    @Mock
    private DeckValidator deckValidator;
    @Mock
    private DeckMapper deckMapper;

    private DeckLoadAdapter deckLoadAdapter;

    @BeforeEach
    void setUp() {
        deckLoadAdapter = new DeckLoadAdapter(deckJpaRepository, deckValidator, deckMapper);
    }

    @Test
    void shouldLoadValidDeck() {
        UUID id = UUID.randomUUID();
        DeckEntity entity = new DeckEntity();
        DeckValidationResult valid = new DeckValidationResult(true, List.of());
        Deck domainDeck = new Deck();

        when(deckJpaRepository.findByIdWithCards(id)).thenReturn(Optional.of(entity));
        when(deckValidator.validate(any())).thenReturn(valid);
        when(deckMapper.toDomain(entity)).thenReturn(domainDeck);

        Deck result = deckLoadAdapter.loadDeck(id);

        assertSame(domainDeck, result);
    }

    @Test
    void shouldThrowNotFoundForMissingDeck() {
        UUID id = UUID.randomUUID();
        when(deckJpaRepository.findByIdWithCards(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deckLoadAdapter.loadDeck(id));
    }

    @Test
    void shouldThrowValidationForInvalidDeck() {
        UUID id = UUID.randomUUID();
        DeckEntity entity = new DeckEntity();
        DeckValidationResult invalid = new DeckValidationResult(false,
                List.of(DeckValidationError.DECK_SIZE_INVALID));

        when(deckJpaRepository.findByIdWithCards(id)).thenReturn(Optional.of(entity));
        when(deckValidator.validate(any())).thenReturn(invalid);

        assertThrows(ValidationException.class, () -> deckLoadAdapter.loadDeck(id));
    }
}
