package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.dtos.decks.*;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.decks.DeckMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.DeckJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
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
class DeckServiceTest {

    @Mock
    private DeckJpaRepository deckJpaRepository;
    @Mock
    private DeckValidator deckValidator;
    @Mock
    private DeckMapper deckMapper;
    @Mock
    private PlayerJpaRepository playerJpaRepository;
    @Mock
    private PdfImportService pdfImportService;
    @Mock
    private PdfExportService pdfExportService;
    @Mock
    private CardJpaRepository cardJpaRepository;

    private DeckService deckService;

    @BeforeEach
    void setUp() {
        deckService = new DeckService(deckJpaRepository, deckValidator, deckMapper, playerJpaRepository, pdfImportService, pdfExportService, cardJpaRepository);
    }

    @Test
    void shouldCreateDeck() {
        String playerId = UUID.randomUUID().toString();
        CreateDeckRequest request = new CreateDeckRequest("Test", playerId, List.of());
        DeckEntity entity = new DeckEntity();
        DeckValidationResult validation = new DeckValidationResult(true, List.of());
        DeckResponse expected = new DeckResponse("id", "Test", null, "USER", 0, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(playerJpaRepository.existsById(UUID.fromString(playerId))).thenReturn(true);
        when(deckMapper.toEntity(request)).thenReturn(entity);
        when(deckValidator.validate(any())).thenReturn(validation);
        when(deckJpaRepository.save(entity)).thenReturn(entity);
        when(deckMapper.toResponse(entity, validation)).thenReturn(expected);

        DeckResponse result = deckService.createDeck(request);

        assertEquals(expected, result);
        verify(deckJpaRepository).save(entity);
    }

    @Test
    void shouldGetDeck() {
        UUID id = UUID.randomUUID();
        DeckEntity entity = new DeckEntity();
        DeckValidationResult validation = new DeckValidationResult(true, List.of());
        DeckResponse expected = new DeckResponse(id.toString(), "Test", null, "USER", 0, true, List.of(),
                new DeckValidationResponse(true, List.of()), null);

        when(deckJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(deckValidator.validate(any())).thenReturn(validation);
        when(deckMapper.toResponse(entity, validation)).thenReturn(expected);

        DeckResponse result = deckService.getDeck(id);

        assertEquals(expected, result);
    }

    @Test
    void shouldThrowNotFoundForMissingDeck() {
        UUID id = UUID.randomUUID();
        when(deckJpaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> deckService.getDeck(id));
    }

    @Test
    void shouldDeleteDeck() {
        UUID id = UUID.randomUUID();
        DeckEntity entity = new DeckEntity();
        when(deckJpaRepository.findById(id)).thenReturn(Optional.of(entity));

        deckService.deleteDeck(id);

        verify(deckJpaRepository).delete(entity);
    }

    @Test
    void shouldValidateDeck() {
        UUID id = UUID.randomUUID();
        DeckEntity entity = new DeckEntity();
        DeckValidationResult validation = new DeckValidationResult(false, List.of());
        DeckValidationResponse expected = new DeckValidationResponse(false, List.of());

        when(deckJpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(deckValidator.validate(any())).thenReturn(validation);
        when(deckMapper.toValidationResponse(validation)).thenReturn(expected);

        DeckValidationResponse result = deckService.validateDeck(id);

        assertEquals(expected, result);
    }
}