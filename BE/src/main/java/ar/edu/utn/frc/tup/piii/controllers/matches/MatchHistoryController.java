package ar.edu.utn.frc.tup.piii.controllers.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.MatchSummaryResponse;
import ar.edu.utn.frc.tup.piii.services.matches.MatchHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches/history")
public class MatchHistoryController {

    private final MatchHistoryService matchHistoryService;

    public MatchHistoryController(MatchHistoryService matchHistoryService) {
        this.matchHistoryService = matchHistoryService;
    }

    @GetMapping
    public ResponseEntity<List<MatchSummaryResponse>> listHistory(@RequestParam UUID playerId) {
        List<MatchSummaryResponse> history = matchHistoryService.getHistoryByPlayer(playerId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchSummaryResponse> getHistoryDetail(@PathVariable UUID id) {
        return matchHistoryService.getHistoryDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
