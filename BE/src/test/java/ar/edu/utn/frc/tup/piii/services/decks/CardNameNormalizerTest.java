package ar.edu.utn.frc.tup.piii.services.decks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CardNameNormalizer")
class CardNameNormalizerTest {

    @Nested
    @DisplayName("normalize() — casos nulos y vacíos")
    class NullAndEmptyTests {

        @Test
        void shouldReturnNullForNullInput() {
            assertNull(CardNameNormalizer.normalize(null));
        }

        @Test
        void shouldReturnEmptyForBlankInput() {
            assertEquals("", CardNameNormalizer.normalize("   "));
        }

        @Test
        void shouldReturnEmptyForEmptyInput() {
            assertEquals("", CardNameNormalizer.normalize(""));
        }

        @Test
        void shouldReturnEmptyForTabInput() {
            assertEquals("", CardNameNormalizer.normalize("\t"));
        }

        @Test
        void shouldReturnEmptyForNewlineInput() {
            assertEquals("", CardNameNormalizer.normalize("\n"));
        }
    }

    @Nested
    @DisplayName("normalize() — Nv. suffix")
    class NvSuffixTests {

        @ParameterizedTest
        @CsvSource({
            "Pikachu Nv.10, Pikachu",
            "Charizard Nv.99, Charizard",
            "Mewtwo Nv.100, Mewtwo",
            "Blastoise Nv.5, Blastoise",
            "Pikachu Nv., Pikachu"
        })
        void shouldRemoveNvSuffix(String input, String expected) {
            assertEquals(expected, CardNameNormalizer.normalize(input));
        }

        @Test
        void shouldNotRemoveNvInMiddleOfName() {
            String result = CardNameNormalizer.normalize("Nv Pokémon");
            assertEquals("Nv Pokémon", result);
        }

        @Test
        void shouldHandleMultipleSpacesBeforeNv() {
            assertEquals("Pikachu", CardNameNormalizer.normalize("Pikachu   Nv.10"));
        }
    }

    @Nested
    @DisplayName("normalize() — Team Plasma")
    class TeamPlasmaTests {

        @ParameterizedTest
        @CsvSource({
            "Zapdos del Equipo Plasma, Zapdos",
            "Zapdos Del Equipo Plasma, Zapdos",
            "Zapdos EQUIPO PLASMA, Zapdos",
            "Deoxys Equipo Plasma, Deoxys",
            "Deoxys equipo plasma, Deoxys"
        })
        void shouldRemoveTeamPlasmaVariants(String input, String expected) {
            assertEquals(expected, CardNameNormalizer.normalize(input));
        }

        @Test
        void shouldRemoveTeamPlasmaWithExtraSpaces() {
            assertEquals("Tornadus", CardNameNormalizer.normalize("Tornadus   Equipo   Plasma"));
        }

        @Test
        void shouldNotAlterNonTeamPlasmaNames() {
            assertEquals("Team Rocket", CardNameNormalizer.normalize("Team Rocket"));
        }
    }

    @Nested
    @DisplayName("normalize() — δ symbol")
    class DeltaSymbolTests {

        @ParameterizedTest
        @CsvSource({
            "Gardevoir δ, Gardevoir",
            "Gardevoir δ, Gardevoir",
            " δ, ''",
            "Gardevoirδ, Gardevoir"
        })
        void shouldRemoveDeltaSymbol(String input, String expected) {
            assertEquals(expected, CardNameNormalizer.normalize(input));
        }

        @Test
        void shouldRemoveDeltaWithSurroundingSpaces() {
            assertEquals("Gardevoir", CardNameNormalizer.normalize("  Gardevoir  δ  "));
        }
    }

    @Nested
    @DisplayName("normalize() — trimming")
    class TrimmingTests {

        @ParameterizedTest
        @CsvSource({
            "  Charizard, Charizard",
            "Charizard  , Charizard",
            "  Charizard  , Charizard",
            "'\tCharizard\n', Charizard"
        })
        void shouldTrimWhitespace(String input, String expected) {
            assertEquals(expected, CardNameNormalizer.normalize(input));
        }
    }

    @Nested
    @DisplayName("normalize() — combinaciones")
    class CombinedTests {

        @Test
        void shouldApplyMultipleTransformations() {
            assertEquals("Mewtwo", CardNameNormalizer.normalize("  Mewtwo del Equipo Plasma Nv.100  "));
        }

        @Test
        void shouldHandleAllTransformationsAtOnce() {
            assertEquals("Gardevoir", CardNameNormalizer.normalize("Gardevoir δ Equipo Plasma Nv.50"));
        }

        @Test
        void shouldNotModifyCleanName() {
            assertEquals("Bulbasaur", CardNameNormalizer.normalize("Bulbasaur"));
        }

        @Test
        void shouldPreserveInternalSpacesWhenNotRemoving() {
            assertEquals("Mr. Mime", CardNameNormalizer.normalize("Mr. Mime"));
        }

        @ParameterizedTest
        @CsvSource({
            "Farfetch'd, Farfetch'd",
            "Jigglypuff Lv. 99, Jigglypuff Lv. 99",
            "Porygon-Z, Porygon-Z"
        })
        void shouldPreserveSpecialCharacters(String input, String expected) {
            assertEquals(expected, CardNameNormalizer.normalize(input));
        }
    }
}
