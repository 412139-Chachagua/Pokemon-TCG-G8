package ar.edu.utn.frc.tup.piii.controllers.players;

import ar.edu.utn.frc.tup.piii.dtos.players.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.players.UpdatePlayerRequest;
import ar.edu.utn.frc.tup.piii.services.players.AvatarStorageService;
import ar.edu.utn.frc.tup.piii.services.players.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final AvatarStorageService avatarStorageService;

    public PlayerController(PlayerService playerService, AvatarStorageService avatarStorageService) {
        this.playerService = playerService;
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping
    public ResponseEntity<List<PlayerResponse>> listPlayers() {
        return ResponseEntity.ok(playerService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getById(@PathVariable UUID id) {
        PlayerResponse response = playerService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponse> updatePlayer(@PathVariable UUID id, @RequestBody UpdatePlayerRequest request) {
        PlayerResponse response = playerService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(
        value = "/{id}/avatar",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlayerResponse> uploadAvatar(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        String avatarFileName = avatarStorageService.store(file);
        PlayerResponse response = playerService.updateAvatar(id, avatarFileName);
        return ResponseEntity.ok(response);
    }
}