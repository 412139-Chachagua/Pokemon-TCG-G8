package ar.edu.utn.frc.tup.piii.controllers.decks;

import ar.edu.utn.frc.tup.piii.dtos.decks.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckValidationResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.UpdateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.decks.ValidateDeckRequest;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.services.decks.DeckService;
import ar.edu.utn.frc.tup.piii.services.decks.PredefinedDeckService;
import ar.edu.utn.frc.tup.piii.services.decks.RandomDeckService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;
    private final RandomDeckService randomDeckService;
    private final PredefinedDeckService predefinedDeckService;

    public DeckController(DeckService deckService, RandomDeckService randomDeckService,
                          PredefinedDeckService predefinedDeckService) {
        this.deckService = deckService;
        this.randomDeckService = randomDeckService;
        this.predefinedDeckService = predefinedDeckService;
    }

    @PostMapping
    public ResponseEntity<DeckResponse> createDeck(@RequestBody CreateDeckRequest request) {
        DeckResponse response = deckService.createDeck(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeckResponse> getDeck(@PathVariable UUID id) {
        DeckResponse response = deckService.getDeck(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeckResponse> updateDeck(@PathVariable UUID id, @RequestBody UpdateDeckRequest request) {
        DeckResponse response = deckService.updateDeck(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(@PathVariable UUID id) {
        deckService.deleteDeck(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportDeck(@PathVariable UUID id) {
        byte[] pdf = deckService.exportDeckPdf(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping
    public ResponseEntity<List<DeckResponse>> listDecksByPlayer(@RequestParam UUID playerId) {
        List<DeckResponse> decks = deckService.listDecksByPlayer(playerId);
        return ResponseEntity.ok(decks);
    }

    @GetMapping("/predefined")
    public ResponseEntity<List<DeckResponse>> listPredefinedDecks() {
        List<DeckResponse> decks = predefinedDeckService.getAllAsResponse();
        return ResponseEntity.ok(decks);
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<DeckResponse> copyDeck(@PathVariable UUID id, @RequestParam UUID playerId) {
        DeckResponse response = predefinedDeckService.copyToPlayer(id, playerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<DeckValidationResponse> validateDeck(@PathVariable UUID id) {
        DeckValidationResponse response = deckService.validateDeck(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<DeckValidationResponse> validateCards(@RequestBody ValidateDeckRequest request) {
        DeckValidationResponse response = deckService.validateCards(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/random")
    public ResponseEntity<DeckResponse> generateRandomDeck() {
        try {
            DeckResponse response = randomDeckService.generateRandomDeck();
            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
    }

    @PostMapping("/import")
    public ResponseEntity<List<DeckResponse>> importDecks(
            @RequestParam("file") MultipartFile file,
            @RequestParam("playerId") UUID playerId,
            @RequestParam("format") String format) {

        if ("pdf".equalsIgnoreCase(format)) {
            List<DeckResponse> decks = deckService.importPdfDecks(file, playerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(decks);
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String filename = file.getOriginalFilename();
            List<DeckResponse> decks = deckService.importDecks(content, format, playerId, filename);
            return ResponseEntity.status(HttpStatus.CREATED).body(decks);
        } catch (IOException e) {
            throw new ValidationException("Error reading file: " + e.getMessage());
        }
    }
}
