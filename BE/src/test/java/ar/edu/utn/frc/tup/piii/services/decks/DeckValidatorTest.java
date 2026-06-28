package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.domain.cards.*;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckCard;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationError;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeckValidatorTest {

    private CardLookupPort cardLookupPort;
    private DeckValidator deckValidator;

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        deckValidator = new DeckValidator(cardLookupPort);
    }

    @Test
    void shouldReturnValidForValidDeck() {
        String[] cardIds = new String[15];
        for (int i = 0; i < 15; i++) {
            cardIds[i] = "xy1-" + (i + 1);
            PokemonCardDefinition basic = new PokemonCardDefinition();
            basic.setStage("BASIC");
            basic.setName("Pokemon " + i);
            when(cardLookupPort.getCardById(cardIds[i])).thenReturn(basic);
        }
        List<DeckCard> cards = java.util.Arrays.stream(cardIds)
                .map(id -> deckCard(id, 4))
                .collect(java.util.stream.Collectors.toList());
        DeckValidationResult result = deckValidator.validate(cards);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldFailWhenLessThan60Cards() {
        List<DeckCard> cards = List.of(deckCard("xy1-1", 30));
        DeckValidationResult result = deckValidator.validate(cards);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(DeckValidationError.DECK_SIZE_INVALID));
    }

    @Test
    void shouldFailWhenMoreThan60Cards() {
        List<DeckCard> cards = List.of(deckCard("xy1-1", 61));
        DeckValidationResult result = deckValidator.validate(cards);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(DeckValidationError.DECK_SIZE_INVALID));
    }

    @Test
    void shouldFailWhenMoreThan4Copies() {
        List<DeckCard> cards = List.of(
                deckCard("xy1-1", 5),
                deckCard("xy1-2", 55)
        );
        PokemonCardDefinition basic = new PokemonCardDefinition();
        basic.setStage("BASIC");
        when(cardLookupPort.getCardById("xy1-2")).thenReturn(basic);

        DeckValidationResult result = deckValidator.validate(cards);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(DeckValidationError.MORE_THAN_4_COPIES));
    }

    @Test
    void shouldFailWhenNoBasicPokemon() {
        PokemonCardDefinition stage1 = new PokemonCardDefinition();
        stage1.setStage("STAGE_1");
        when(cardLookupPort.getCardById("xy1-1")).thenReturn(stage1);

        List<DeckCard> cards = List.of(deckCard("xy1-1", 60));
        DeckValidationResult result = deckValidator.validate(cards);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(DeckValidationError.MISSING_BASIC_POKEMON));
    }

    // ---- Special Energy tests ----

    private EnergyCardDefinition specialEnergy(String name) {
        var e = new EnergyCardDefinition();
        e.setEnergyCardType(EnergyCardType.SPECIAL);
        e.setName(name);
        return e;
    }

    private EnergyCardDefinition basicEnergy() {
        var e = new EnergyCardDefinition();
        e.setEnergyCardType(EnergyCardType.BASIC);
        e.setName("Fighting Energy");
        return e;
    }

    @Test
    void shouldPassWith4SpecialEnergySameName() {
        var energy = specialEnergy("Double Colorless Energy");
        when(cardLookupPort.getCardById("special-1")).thenReturn(energy);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicEnergy());

        List<DeckCard> cards = List.of(
                deckCard("special-1", 4),
                deckCard("basic-1", 56)
        );

        DeckValidationResult result = deckValidator.validate(cards);
        assertFalse(result.getErrors().contains(DeckValidationError.MORE_THAN_4_COPIES));
    }

    @Test
    void shouldFailWith5SpecialEnergySameName() {
        var energy = specialEnergy("Double Colorless Energy");
        when(cardLookupPort.getCardById("special-1")).thenReturn(energy);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicEnergy());

        List<DeckCard> cards = List.of(
                deckCard("special-1", 5),
                deckCard("basic-1", 55)
        );

        DeckValidationResult result = deckValidator.validate(cards);
        assertTrue(result.getErrors().contains(DeckValidationError.MORE_THAN_4_COPIES));
    }

    @Test
    void shouldAggregateSpecialEnergyByNameAcrossDifferentIds() {
        var specialA = specialEnergy("Double Colorless Energy");
        var specialB = specialEnergy("Double Colorless Energy");
        // Different IDs, same name
        when(cardLookupPort.getCardById("special-a")).thenReturn(specialA);
        when(cardLookupPort.getCardById("special-b")).thenReturn(specialB);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicEnergy());

        List<DeckCard> cards = List.of(
                deckCard("special-a", 2),
                deckCard("special-b", 3),
                deckCard("basic-1", 55)
        );

        DeckValidationResult result = deckValidator.validate(cards);
        assertTrue(result.getErrors().contains(DeckValidationError.MORE_THAN_4_COPIES));
    }

    @Test
    void shouldNotAggregateSpecialEnergyWithDifferentNames() {
        var specialA = specialEnergy("Double Colorless Energy");
        var specialB = specialEnergy("Speed Lightning Energy");
        when(cardLookupPort.getCardById("special-a")).thenReturn(specialA);
        when(cardLookupPort.getCardById("special-b")).thenReturn(specialB);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicEnergy());

        List<DeckCard> cards = List.of(
                deckCard("special-a", 4),
                deckCard("special-b", 4),
                deckCard("basic-1", 52)
        );

        DeckValidationResult result = deckValidator.validate(cards);
        assertFalse(result.getErrors().contains(DeckValidationError.MORE_THAN_4_COPIES));
    }

    // ---- Ace Spec tests ----

    private TrainerCardDefinition aceSpec() {
        var t = new TrainerCardDefinition();
        t.setAceSpec(true);
        return t;
    }

    private TrainerCardDefinition nonAceSpec() {
        var t = new TrainerCardDefinition();
        t.setAceSpec(false);
        return t;
    }

    @Test
    void shouldPassWithoutAceSpec() {
        var trainer = nonAceSpec();
        when(cardLookupPort.getCardById("trainer-1")).thenReturn(trainer);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicEnergy());

        List<DeckCard> cards = List.of(
                deckCard("trainer-1", 4),
                deckCard("basic-1", 56)
        );

        DeckValidationResult result = deckValidator.validate(cards);
        assertFalse(result.getErrors().contains(DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED));
    }

    @Test
    void shouldPassWith1AceSpec() {
        var ace = aceSpec();
        when(cardLookupPort.getCardById("ace-1")).thenReturn(ace);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicEnergy());

        List<DeckCard> cards = List.of(
                deckCard("ace-1", 1),
                deckCard("basic-1", 59)
        );

        DeckValidationResult result = deckValidator.validate(cards);
        assertFalse(result.getErrors().contains(DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED));
    }

    @Test
    void shouldFailWith2AceSpec() {
        var ace1 = aceSpec();
        var ace2 = aceSpec();
        when(cardLookupPort.getCardById("ace-1")).thenReturn(ace1);
        when(cardLookupPort.getCardById("ace-2")).thenReturn(ace2);
        when(cardLookupPort.getCardById("basic-1")).thenReturn(basicEnergy());

        List<DeckCard> cards = List.of(
                deckCard("ace-1", 1),
                deckCard("ace-2", 1),
                deckCard("basic-1", 58)
        );

        DeckValidationResult result = deckValidator.validate(cards);
        assertTrue(result.getErrors().contains(DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED));
    }

    private DeckCard deckCard(String cardId, int quantity) {
        DeckCard card = new DeckCard();
        card.setCardId(cardId);
        card.setQuantity(quantity);
        return card;
    }
}
