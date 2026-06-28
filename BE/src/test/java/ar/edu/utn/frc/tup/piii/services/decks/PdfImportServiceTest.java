package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.dtos.decks.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.decks.ImportDeckRequest;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PdfImportService")
class PdfImportServiceTest {

    @Mock
    private CardJpaRepository cardJpaRepository;

    private PdfImportService pdfImportService;

    @BeforeEach
    void setUp() {
        pdfImportService = new PdfImportService(cardJpaRepository);
    }

    private byte[] createMinimalPdfBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\nendobj\n");
        sb.append("xref\n0 4\n");
        sb.append("0000000000 65535 f \n");
        sb.append("0000000009 00000 n \n");
        sb.append("0000000058 00000 n \n");
        sb.append("0000000115 00000 n \n");
        sb.append("trailer\n<< /Size 4 /Root 1 0 R >>\n");
        sb.append("startxref\n190\n%%EOF");
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private MultipartFile mockPdf(byte[] content) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) content.length);
        when(file.getBytes()).thenReturn(content);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        return file;
    }

    @Nested
    @DisplayName("Validaciones de archivo")
    class FileValidationTests {

        @Test
        void shouldThrowWhenFileIsNull() {
            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(null));
        }

        @Test
        void shouldThrowWhenFileIsEmpty() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenFileSizeIsZero() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(0L);

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenFileExceedsMaxSize() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(3L * 1024 * 1024);

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldAcceptFileAtMaxSize() throws IOException {
            byte[] content = createMinimalPdfBytes();
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(2L * 1024 * 1024);
            when(file.getBytes()).thenReturn(content);
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));

            assertDoesNotThrow(() -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenFileIsTooSmall() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(4L);
            when(file.getBytes()).thenReturn(new byte[]{0, 0, 0, 0});

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenNotAPdf() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(10L);
            when(file.getBytes()).thenReturn("NotAPDF!!!".getBytes());

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenFileReadFails() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(100L);
            when(file.getBytes()).thenThrow(new IOException("Cannot read"));

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenPdfIsEncrypted() throws IOException {
            String encrypted = "%PDF-1.4\n" +
                    "1 0 obj\n<< /Filter /Crypt >>\nendobj\n" +
                    "%%EOF";
            MultipartFile file = mockPdf(encrypted.getBytes(StandardCharsets.US_ASCII));

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenPdfHasAcroForm() throws IOException {
            String withForm = "%PDF-1.4\n" +
                    "1 0 obj\n<< /Type /Catalog /AcroForm 2 0 R >>\nendobj\n" +
                    "%%EOF";
            MultipartFile file = mockPdf(withForm.getBytes(StandardCharsets.US_ASCII));

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }
    }

    @Nested
    @DisplayName("Parseo de contenido CARDID")
    class CardIdParsingTests {

        @Test
        void shouldParseCardIdFormat() throws IOException {
            String content = "# Mi Deck\nxy1-78:2\nxy1-1:3\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            CardEntity card1 = new CardEntity();
            card1.setId("xy1-78");
            card1.setName("Dark Pokemon");
            CardEntity card2 = new CardEntity();
            card2.setId("xy1-1");
            card2.setName("Pikachu");

            when(cardJpaRepository.findById("xy1-78")).thenReturn(Optional.of(card1));
            when(cardJpaRepository.findById("xy1-1")).thenReturn(Optional.of(card2));

            List<ImportDeckRequest> result = pdfImportService.extractDecksFromPdf(file);

            assertAll("cardid format",
                    () -> assertEquals("Mi Deck", result.get(0).name()),
                    () -> assertEquals(2, result.get(0).cards().size())
            );
            assertEquals(5, result.get(0).cards().stream().mapToInt(CreateDeckRequest.DeckCardRequest::quantity).sum());
        }

        @Test
        void shouldParseCardIdWithMultipleQuantities() throws IOException {
            String content = "# Mi Deck\nxy1-78:4\nxy1-1:1\nxy1-2:2\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findById(anyString())).thenReturn(Optional.of(new CardEntity()));

            List<ImportDeckRequest> result = pdfImportService.extractDecksFromPdf(file);

            assertEquals(3, result.get(0).cards().size());
            assertEquals(7, result.get(0).cards().stream().mapToInt(CreateDeckRequest.DeckCardRequest::quantity).sum());
        }
    }

    @Nested
    @DisplayName("Parseo de contenido READABLE")
    class ReadableParsingTests {

        @Test
        void shouldParseReadableFormat() throws IOException {
            String content = "# Energy Deck\n2 Dark Pokemon xy1 78\n3 Pikachu xy1 1\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findById("xy1-78")).thenReturn(Optional.of(new CardEntity()));
            when(cardJpaRepository.findById("xy1-1")).thenReturn(Optional.of(new CardEntity()));

            List<ImportDeckRequest> result = pdfImportService.extractDecksFromPdf(file);

            assertNotNull(result.get(0));
            assertEquals(2, result.get(0).cards().size());
        }

        @Test
        void shouldHandleReadableWithLongName() throws IOException {
            String content = "# Test\n1 Long Card Name Here xy1 99\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findById("xy1-99")).thenReturn(Optional.of(new CardEntity()));

            assertDoesNotThrow(() -> pdfImportService.extractDecksFromPdf(file));
        }
    }

    @Nested
    @DisplayName("Parseo de contenido NAME_ONLY")
    class NameOnlyParsingTests {

        @Test
        void shouldParseNameOnlyFormat() throws IOException {
            String content = "# Pokedex\n4 Pikachu\n2 Raichu\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findByNameIgnoreCase("Pikachu")).thenReturn(List.of(createCard("xy1-1", "Pikachu")));
            when(cardJpaRepository.findByNameIgnoreCase("Raichu")).thenReturn(List.of(createCard("xy1-2", "Raichu")));

            List<ImportDeckRequest> result = pdfImportService.extractDecksFromPdf(file);

            assertNotNull(result.get(0));
            assertEquals(2, result.get(0).cards().size());
        }

        @Test
        void shouldThrowWhenNameNotFound() throws IOException {
            String content = "# Unknown Deck\n4 MissingNo\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findByNameIgnoreCase("MissingNo")).thenReturn(List.of());

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldUseFirstResultWhenMultipleCardsMatchName() throws IOException {
            String content = "# Multi Match\n4 Pikachu\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findByNameIgnoreCase("Pikachu"))
                    .thenReturn(List.of(createCard("xy1-1", "Pikachu"), createCard("xy1-55", "Pikachu")));

            assertDoesNotThrow(() -> pdfImportService.extractDecksFromPdf(file));
        }
    }

    @Nested
    @DisplayName("Validaciones de carta")
    class CardValidationTests {

        @Test
        void shouldThrowWhenTotalCardsNot60() throws IOException {
            String content = "# Short Deck\nxy1-78:5\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findById("xy1-78")).thenReturn(Optional.of(new CardEntity()));

            ValidationException ex = assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
            assertTrue(ex.getMessage().toLowerCase().contains("60") || ex.getMessage().toLowerCase().contains("sesenta"));
        }

        @Test
        void shouldThrowWhenNoValidCardsFound() throws IOException {
            String content = "# Empty\n%% comment only\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenCardNotFoundInDb() throws IOException {
            String content = "# Mixed\nxy1-999:60\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findById("xy1-999")).thenReturn(Optional.empty());

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldThrowWhenQuantityIsZero() throws IOException {
            String content = "# Zero\nxy1-1:0\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @ParameterizedTest
        @ValueSource(strings = {"   ", "invalid-format-no-colon", "abc:def", ":60", "xy1-1:"})
        void shouldThrowForMalformedLines(String line) throws IOException {
            String content = "# Bad\n" + line + "\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }

        @Test
        void shouldSkipLinesWithComments() throws IOException {
            String content = "# Comments\n% just a comment\n%% another\nxy1-78:2\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findById("xy1-78")).thenReturn(Optional.of(new CardEntity()));

            assertThrows(ValidationException.class, () -> pdfImportService.extractDecksFromPdf(file));
        }
    }

    @Nested
    @DisplayName("Extracción de nombre de deck")
    class DeckNameExtractionTests {

        @Test
        void shouldExtractDeckNameFromHashtagLine() throws IOException {
            String content = "#MyDeck\nxy1-78:2\nxy1-1:1\nxy1-2:2\nxy1-3:55\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findById(anyString())).thenReturn(Optional.of(new CardEntity()));

            List<ImportDeckRequest> result = pdfImportService.extractDecksFromPdf(file);

            assertEquals("MyDeck", result.get(0).name());
        }

        @Test
        void shouldUseDefaultNameWhenNoHashtagLine() throws IOException {
            String content = "xy1-78:20\nxy1-1:20\nxy1-2:20\n";
            byte[] pdfBytes = embedInPdf(content);
            MultipartFile file = mockPdf(pdfBytes);

            when(cardJpaRepository.findById(anyString())).thenReturn(Optional.of(new CardEntity()));

            List<ImportDeckRequest> result = pdfImportService.extractDecksFromPdf(file);

            assertNotNull(result.get(0).name());
        }
    }

    private CardEntity createCard(String id, String name) {
        CardEntity c = new CardEntity();
        c.setId(id);
        c.setName(name);
        c.setSupertype("POKEMON");
        return c;
    }

    private byte[] embedInPdf(String textContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n");
        sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]");
        sb.append(" /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");
        sb.append("4 0 obj\n<< /Length ").append(textContent.length()).append(" >>\nstream\n");
        sb.append(textContent).append("\nendstream\nendobj\n");
        sb.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n");
        sb.append("xref\n0 6\n");
        sb.append("0000000000 65535 f \n");
        sb.append("0000000009 00000 n \n");
        sb.append("0000000058 00000 n \n");
        sb.append("0000000115 00000 n \n");
        sb.append("0000000200 00000 n \n");
        sb.append("0000000320 00000 n \n");
        sb.append("trailer\n<< /Size 6 /Root 1 0 R >>\n");
        sb.append("startxref\n390\n%%EOF");
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
