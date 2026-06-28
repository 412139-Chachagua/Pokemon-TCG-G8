package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.repositories.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class PdfExportService {

    private final CardJpaRepository cardJpaRepository;

    public PdfExportService(CardJpaRepository cardJpaRepository) {
        this.cardJpaRepository = cardJpaRepository;
    }

    public byte[] export(DeckEntity deck) {
        Map<String, String> cardNames = new HashMap<>();
        for (DeckCardEntity dce : deck.getCards()) {
            cardJpaRepository.findById(dce.getCardId())
                    .ifPresent(c -> cardNames.put(c.getId(), c.getName()));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            cs.newLineAtOffset(50, 750);
            cs.showText("# " + deck.getName());
            cs.endText();

            int y = 720;
            int lineHeight = 14;

            for (DeckCardEntity card : deck.getCards()) {
                if (y < 60) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = 750;
                }

                String name = cardNames.getOrDefault(card.getCardId(), card.getCardId());
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(50, y);
                cs.showText(card.getQuantity() + " " + name);
                cs.endText();
                y -= lineHeight;
            }

            cs.close();
            doc.save(baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }
}
