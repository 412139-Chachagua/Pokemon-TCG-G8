package ar.edu.utn.frc.tup.piii.controllers.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionResponse;
import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class GameActionController {

    private final MatchApplicationService matchApplicationService;

    public GameActionController(MatchApplicationService matchApplicationService) {
        this.matchApplicationService = matchApplicationService;
    }

    @PostMapping("/{id}/actions")
    public ResponseEntity<GameActionResponse> executeAction(@PathVariable UUID id,
                                                             @Valid @RequestBody GameActionRequest request) {
        GameActionResponse response = matchApplicationService.executeAction(id, request);
        return ResponseEntity.ok(response);
    }
}
