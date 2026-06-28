package ar.edu.utn.frc.tup.piii.controllers.debug;

import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Profile("dev")
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private static final Logger log = LoggerFactory.getLogger(DebugController.class);
    private final MatchApplicationService matchApplicationService;

    public DebugController(MatchApplicationService matchApplicationService) {
        this.matchApplicationService = matchApplicationService;
    }

    @PostMapping("/matches/{id}/force-sudden-death")
    public ResponseEntity<Map<String, Object>> forceSuddenDeath(@PathVariable UUID id) {
        log.warn("[DEBUG] forceSuddenDeath requested for match {}", id);
        matchApplicationService.forceSuddenDeath(id);
        return ResponseEntity.ok(Map.of(
                "message", "Sudden death triggered. End your turn to start the sudden death match.",
                "matchId", id.toString()
        ));
    }
}
