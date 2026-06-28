package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.dtos.decks.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.decks.ImportDeckRequest;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfImportService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final int MAX_PAGES = 3;
    private static final int DECK_TOTAL_CARDS = 60;

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private static final Pattern DECK_NAME_PATTERN = Pattern.compile("^#+\\s+(.+)$");

    private static final Pattern CARDID_PATTERN = Pattern.compile("^([a-z0-9]+-\\d+):(\\d+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern READABLE_PATTERN =
            Pattern.compile("^(\\d+)\\s+(.+?)\\s+([a-zA-Z0-9]+)\\s+(\\d+)$");

    private static final Pattern NAME_ONLY_PATTERN =
            Pattern.compile("^(\\d+)\\s+(.+)$");

    private final CardJpaRepository cardJpaRepository;

    public PdfImportService(CardJpaRepository cardJpaRepository) {
        this.cardJpaRepository = cardJpaRepository;
    }

    public List<ImportDeckRequest> extractDecksFromPdf(MultipartFile file) {
        byte[] bytes = validateFile(file);
        PDDocument document = openPdfSecurely(bytes);
        try {
            validatePageCount(document);
            String text = extractText(document);
            return parseDeckContent(text);
        } finally {
            try {
                document.close();
            } catch (IOException ignored) {
            }
        }
    }

    private byte[] validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("No file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File exceeds maximum size of 2MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ValidationException("Could not read file");
        }
        if (bytes.length < PDF_MAGIC.length) {
            throw new ValidationException("File is not a valid PDF");
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                throw new ValidationException("File is not a valid PDF");
            }
        }
        return bytes;
    }

    private PDDocument openPdfSecurely(byte[] bytes) {
        PDDocument document;
        try {
            document = Loader.loadPDF(bytes);
        } catch (IOException e) {
            throw new ValidationException("Could not read PDF content");
        }
        if (document.isEncrypted()) {
            try {
                document.close();
            } catch (IOException ignored) {
            }
            throw new ValidationException("PDF is password-protected");
        }
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        if (catalog.getAcroForm() != null) {
            try {
                document.close();
            } catch (IOException ignored) {
            }
            throw new ValidationException("PDF contains forms which are not allowed");
        }
        if (catalog.getNames() != null && catalog.getNames().getEmbeddedFiles() != null) {
            try {
                document.close();
            } catch (IOException ignored) {
            }
            throw new ValidationException("PDF contains embedded files which are not allowed");
        }
        return document;
    }

    private void validatePageCount(PDDocument document) {
        int pageCount = document.getNumberOfPages();
        if (pageCount == 0) {
            try {
                document.close();
            } catch (IOException ignored) {
            }
            throw new ValidationException("PDF has no pages");
        }
        if (pageCount > MAX_PAGES) {
            try {
                document.close();
            } catch (IOException ignored) {
            }
            throw new ValidationException("PDF exceeds maximum of 3 pages");
        }
    }

    private String extractText(PDDocument document) {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(false);
        try {
            return stripper.getText(document);
        } catch (IOException e) {
            throw new ValidationException("Could not extract text from PDF");
        }
    }

    private List<ImportDeckRequest> parseDeckContent(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("No deck content found in PDF");
        }
        String[] lines = text.split("\\r?\\n");
        String currentName = "Imported Deck";
        List<CreateDeckRequest.DeckCardRequest> currentCards = new ArrayList<>();
        List<ImportDeckRequest> decks = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher nameMatcher = DECK_NAME_PATTERN.matcher(trimmed);
            if (nameMatcher.matches()) {
                if (!currentCards.isEmpty()) {
                    decks.add(new ImportDeckRequest(currentName, currentCards));
                    currentCards = new ArrayList<>();
                }
                currentName = nameMatcher.group(1).trim();
                continue;
            }
            Matcher cardIdMatcher = CARDID_PATTERN.matcher(trimmed);
            if (cardIdMatcher.matches()) {
                String cardId = cardIdMatcher.group(1).toLowerCase();
                int quantity = parseIntSafely(cardIdMatcher.group(2), 1);
                currentCards.add(new CreateDeckRequest.DeckCardRequest(cardId, quantity));
                continue;
            }
            Matcher readableMatcher = READABLE_PATTERN.matcher(trimmed);
            if (readableMatcher.matches()) {
                int quantity = parseIntSafely(readableMatcher.group(1), 1);
                String setCode = readableMatcher.group(3).toLowerCase();
                String number = readableMatcher.group(4);
                String cardId = setCode + "-" + number;
                currentCards.add(new CreateDeckRequest.DeckCardRequest(cardId, quantity));
                continue;
            }
            Matcher nameOnlyMatcher = NAME_ONLY_PATTERN.matcher(trimmed);
            if (nameOnlyMatcher.matches()) {
                int quantity = parseIntSafely(nameOnlyMatcher.group(1), 1);
                String name = nameOnlyMatcher.group(2).trim();
                List<CardEntity> cards = cardJpaRepository.findByNameIgnoreCase(name);
                if (cards.isEmpty()) {
                    throw new ValidationException("Card not found: " + name);
                }
                String cardId = cards.get(0).getId();
                currentCards.add(new CreateDeckRequest.DeckCardRequest(cardId, quantity));
            }
        }
        if (!currentCards.isEmpty()) {
            decks.add(new ImportDeckRequest(currentName, currentCards));
        }
        if (decks.isEmpty()) {
            throw new ValidationException("No valid cards found in PDF");
        }
        for (ImportDeckRequest deck : decks) {
            int total = deck.cards().stream().mapToInt(CreateDeckRequest.DeckCardRequest::quantity).sum();
            if (total != DECK_TOTAL_CARDS) {
                throw new ValidationException(
                        "Deck '" + deck.name() + "' must have exactly " + DECK_TOTAL_CARDS
                                + " cards (found " + total + ")");
            }
        }
        return decks;
    }

    private int parseIntSafely(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
