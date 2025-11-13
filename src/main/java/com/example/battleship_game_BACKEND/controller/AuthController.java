package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.ChangePasswordRequest;
import com.example.battleship_game_BACKEND.dto.JwtResponse;
import com.example.battleship_game_BACKEND.dto.LoginRequest;
import com.example.battleship_game_BACKEND.dto.SignupRequest;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<JwtResponse> registerUser(@RequestBody SignupRequest signUpRequest) {
        try {
            JwtResponse jwtResponse = authService.registerUser(signUpRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build(); // Nickname already taken
        }
    }

    // Пример защищенного эндпоинта - требует валидный JWT в заголовке Authorization
    @GetMapping("/me")
    public ResponseEntity<Player> getCurrentUser() {
        // Здесь можно получить текущего пользователя из SecurityContext или из токена
        // Пока возвращаем примерный ответ
        return ResponseEntity.ok().build(); // Реализация зависит от вашей логики
    }

    // AuthController.java - измените метод changePassword
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request,
                                            @AuthenticationPrincipal Player player) {
        try {
            System.out.println("Changing password for player: " + player.getNickname());

            authService.changePassword(player, request.getOldPassword(), request.getNewPassword());

            System.out.println("Password changed successfully for player: " + player.getNickname());

            // Возвращаем JSON объект вместо plain text
            Map<String, String> response = new HashMap<>();
            response.put("message", "Пароль успешно изменен");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("Error changing password: " + e.getMessage());

            // Возвращаем JSON объект с ошибкой
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            System.out.println("Error changing password: " + e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Ошибка при смене пароля: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
