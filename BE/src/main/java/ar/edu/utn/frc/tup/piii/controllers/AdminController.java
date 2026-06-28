package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.services.CardAdminService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final CardAdminService cardAdminService;

    public AdminController(CardAdminService cardAdminService) {
        this.cardAdminService = cardAdminService;
    }

    @Operation(summary = "Regenerate all attack effect codes from effect text",
            description = "Iterates all cards, re-parses effectText with current TextEffectParser, and updates effectCode if changed.")
    @PostMapping("/regenerate-effect-codes")
    public ResponseEntity<Map<String, Object>> regenerateEffectCodes() {
        log.warn("[admin] Starting regeneration of all effect codes...");
        CardAdminService.RegenerateResult result = cardAdminService.regenerateAllEffectCodes();
        log.warn("[admin] Regeneration complete: total={}, updated={}, skipped={}, errors={}",
                result.totalAttacks, result.updatedAttacks, result.skippedAttacks, result.errorAttacks);
        return ResponseEntity.ok(Map.of(
                "totalAttacks", result.totalAttacks,
                "updatedAttacks", result.updatedAttacks,
                "skippedAttacks", result.skippedAttacks,
                "errorAttacks", result.errorAttacks,
                "errors", result.errors
        ));
    }
}
