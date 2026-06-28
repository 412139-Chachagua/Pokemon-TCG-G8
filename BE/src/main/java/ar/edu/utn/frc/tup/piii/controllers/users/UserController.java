package ar.edu.utn.frc.tup.piii.controllers.users;

import ar.edu.utn.frc.tup.piii.dtos.users.CreateUserRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.UpdateUserRequest;
import ar.edu.utn.frc.tup.piii.dtos.users.UserResponse;
import ar.edu.utn.frc.tup.piii.services.users.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        UserResponse response = userService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listAll() {
        List<UserResponse> response = userService.listAll();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable UUID id) {
        UserResponse response = userService.deactivate(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activate(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        UserResponse response = userService.activate(id, body.get("password"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/validate-password")
    public ResponseEntity<Void> validatePassword(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        userService.validatePassword(id, body.get("password"));
        return ResponseEntity.ok().build();
    }
}
