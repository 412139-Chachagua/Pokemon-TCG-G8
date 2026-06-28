package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckValidationResponse;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RandomDeckService")
class RandomDeckServiceTest {

    @Mock
    private CardJpaRepository cardJpaRepository;
    @Mock
    private CardLookupPort cardLookupPort;
    @Mock
    private DeckValidator deckValidator;

    private RandomDeckService randomDeckService;

    private CardEntity basicPokemon;
    private CardEntity basicPokemon2;
    private CardEntity stage1;
    private CardEntity stage2;
    private CardEntity basicEnergy;
    private CardEntity trainer;

    @BeforeEach
    void setUp() {
        randomDeckService = new RandomDeckService(cardJpaRepository, cardLookupPort, deckValidator);

        basicPokemon = new CardEntity();
        basicPokemon.setId("xy1-1");
        basicPokemon.setName("Pikachu");
        basicPokemon.setSupertype("POKEMON");
        basicPokemon.setPokemonStage("BASIC");
        basicPokemon.setSetCode("xy1");
        basicPokemon.setPokemonTypes("FIGHTING");

        basicEnergy = new CardEntity();
        basicEnergy.setId("xy1-137");
        basicEnergy.setName("Fighting Energy");
        basicEnergy.setSupertype("ENERGY");
        basicEnergy.setEnergyCardType("BASIC");
        basicEnergy.setSetCode("xy1");
        basicEnergy.setProvidesEnergyTypes("FIGHTING");

        trainer = new CardEntity();
        trainer.setId("xy1-127");
        trainer.setName("Professor Sycamore");
        trainer.setSupertype("TRAINER");
        trainer.setTrainerSubtype("SUPPORTER");
        trainer.setSetCode("xy1");

        basicPokemon2 = new CardEntity();
        basicPokemon2.setId("xy1-4");
        basicPokemon2.setName("Machop");
        basicPokemon2.setSupertype("POKEMON");
        basicPokemon2.setPokemonStage("BASIC");
        basicPokemon2.setSetCode("xy1");
        basicPokemon2.setPokemonTypes("FIGHTING");

        stage1 = new CardEntity();
        stage1.setId("xy1-2");
        stage1.setName("Raichu");
        stage1.setSupertype("POKEMON");
        stage1.setPokemonStage("STAGE_1");
        stage1.setEvolvesFrom("Pikachu");
        stage1.setSetCode("xy1");
        stage1.setPokemonTypes("FIGHTING");

        stage2 = new CardEntity();
        stage2.setId("xy1-3");
        stage2.setName("Alolan Raichu");
        stage2.setSupertype("POKEMON");
        stage2.setPokemonStage("STAGE_2");
        stage2.setEvolvesFrom("Raichu");
        stage2.setSetCode("xy1");
        stage2.setPokemonTypes("FIGHTING");
    }

    @Nested
    @DisplayName("generateRandomDeck() — éxito")
    class SuccessTests {

        @Test
        void shouldGenerateValidDeck() {
            when(cardJpaRepository.findAll()).thenReturn(List.of(basicPokemon, basicPokemon2, stage1, stage2, basicEnergy, trainer));
            when(deckValidator.validate(any())).thenReturn(new DeckValidationResult(true, List.of()));
            CardDefinition cd = mock(CardDefinition.class);
            when(cardLookupPort.getCardById(anyString())).thenReturn(cd);

            DeckResponse result = randomDeckService.generateRandomDeck();

            assertAll("generated deck",
                    () -> assertNotNull(result),
                    () -> assertEquals("Random Deck", result.name()),
                    () -> assertEquals("RANDOM", result.source()),
                    () -> assertTrue(result.valid()),
                    () -> assertTrue(result.totalCards() > 0)
            );
        }

        @Test
        void shouldPassValidation() {
            when(cardJpaRepository.findAll()).thenReturn(List.of(basicPokemon, basicPokemon2, stage1, stage2, basicEnergy, trainer));
            when(deckValidator.validate(any())).thenReturn(new DeckValidationResult(true, List.of()));
            when(cardLookupPort.getCardById(anyString())).thenReturn(mock(CardDefinition.class));

            DeckResponse result = randomDeckService.generateRandomDeck();

            assertTrue(result.valid());
            verify(deckValidator, atLeastOnce()).validate(any());
        }
    }

    @Nested
    @DisplayName("generateRandomDeck() — errores")
    class ErrorTests {

        @Test
        void shouldThrowWhenNoBasicPokemonAvailable() {
            when(cardJpaRepository.findAll()).thenReturn(List.of(basicEnergy, trainer));

            ValidationException ex = assertThrows(ValidationException.class, () -> randomDeckService.generateRandomDeck());
            assertTrue(ex.getMessage().toLowerCase().contains("pokémon") || ex.getMessage().toLowerCase().contains("pokemon") || ex.getMessage().toLowerCase().contains("basic"));
        }

        @Test
        void shouldThrowWhenOnlyStage1Pokemon() {
            CardEntity stage1 = new CardEntity();
            stage1.setSupertype("POKEMON");
            stage1.setPokemonStage("STAGE_1");
            stage1.setSetCode("xy1");

            when(cardJpaRepository.findAll()).thenReturn(List.of(stage1, basicEnergy));

            assertThrows(ValidationException.class, () -> randomDeckService.generateRandomDeck());
        }

        @Test
        void shouldThrowWhenNoBasicEnergy() {
            when(cardJpaRepository.findAll()).thenReturn(List.of(basicPokemon, trainer));

            assertThrows(ValidationException.class, () -> randomDeckService.generateRandomDeck());
        }

        @Test
        void shouldThrowWhenNoCardsAtAll() {
            when(cardJpaRepository.findAll()).thenReturn(List.of());

            assertThrows(ValidationException.class, () -> randomDeckService.generateRandomDeck());
        }

        @Test
        void shouldThrowAfterMaxRetriesOnValidationFailure() {
            lenient().when(cardJpaRepository.findAll()).thenReturn(List.of(basicPokemon, basicEnergy, trainer));
            lenient().when(deckValidator.validate(any())).thenReturn(new DeckValidationResult(false, List.of()));

            assertThrows(ValidationException.class, () -> randomDeckService.generateRandomDeck());
        }

        @Test
        void shouldRetryWhenValidationFails() {
            lenient().when(cardJpaRepository.findAll()).thenReturn(List.of(basicPokemon, basicEnergy, trainer));
            lenient().when(deckValidator.validate(any())).thenReturn(new DeckValidationResult(false, List.of()));

            assertThrows(ValidationException.class, () -> randomDeckService.generateRandomDeck());
        }
    }
}
