package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.JwtResponse;
import com.example.battleship_game_BACKEND.dto.LoginRequest;
import com.example.battleship_game_BACKEND.dto.SignupRequest;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
