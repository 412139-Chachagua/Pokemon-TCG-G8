package ar.edu.utn.frc.tup.piii.controllers.ranking;

import ar.edu.utn.frc.tup.piii.dtos.ranking.PlayerStatsResponse;
import ar.edu.utn.frc.tup.piii.dtos.ranking.RankingEntryResponse;
import ar.edu.utn.frc.tup.piii.services.ranking.PlayerStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RankingController {

    private final PlayerStatsService playerStatsService;

    public RankingController(PlayerStatsService playerStatsService) {
        this.playerStatsService = playerStatsService;
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<RankingEntryResponse>> getRanking() {
        return ResponseEntity.ok(playerStatsService.getRanking());
    }

    @GetMapping("/players/{id}/stats")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(@PathVariable UUID id) {
        return ResponseEntity.ok(playerStatsService.getPlayerStats(id));
    }
}
