package ar.edu.utn.frc.tup.piii.controllers.cards;

import ar.edu.utn.frc.tup.piii.dtos.cards.CardDetailResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSearchRequest;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSearchResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSummaryResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSyncResponse;
import ar.edu.utn.frc.tup.piii.services.cards.CardCacheSyncService;
import ar.edu.utn.frc.tup.piii.services.cards.CardCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Endpoints for card catalog operations")
public class CardController {

    private final CardCatalogService cardCatalogService;
    private final CardCacheSyncService cardCacheSyncService;

    @GetMapping
    @Operation(summary = "Search cards with filters", description = "Search the local card catalog with optional filters and pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful search",
                    content = @Content(schema = @Schema(implementation = CardSearchResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CardSearchResponse> searchCards(
            @Parameter(description = "Search query (name LIKE)")
            @RequestParam(required = false) String query,
            @Parameter(description = "Card supertype filter (POKEMON, TRAINER, ENERGY)")
            @RequestParam(required = false) String supertype,
            @Parameter(description = "Set code filter")
            @RequestParam(required = false) String setCode,
            @Parameter(description = "Pokemon stage filter (BASIC, STAGE_1, STAGE_2, etc.)")
            @RequestParam(required = false) String stage,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        CardSearchRequest request = new CardSearchRequest(query, supertype, setCode, stage, page, size);
        CardSearchResponse result = cardCatalogService.searchCards(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card by ID", description = "Returns full card details including attacks, weaknesses, and resistances")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card found",
                    content = @Content(schema = @Schema(implementation = CardDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Card not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CardDetailResponse> getCardById(
            @Parameter(description = "Card ID (e.g., xy1-1)")
            @PathVariable String id) {
        CardDetailResponse result = cardCatalogService.getCardById(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync")
    @Operation(summary = "Manual card sync", description = "Triggers a manual synchronization of all cards from the Pokemon TCG API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed successfully",
                    content = @Content(schema = @Schema(implementation = CardSyncResponse.class))),
            @ApiResponse(responseCode = "500", description = "Sync failed")
    })
    public ResponseEntity<CardSyncResponse> syncCards() {
        CardSyncResponse response = cardCacheSyncService.syncAll();
        return ResponseEntity.ok(response);
    }
}
