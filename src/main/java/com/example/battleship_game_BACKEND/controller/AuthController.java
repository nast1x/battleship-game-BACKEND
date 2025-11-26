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
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /** Запрос для авторизации */
    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        System.out.println("Получен запрос на вход: " + loginRequest.getNickname());
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
            System.out.println("Успешный вход для: " + loginRequest.getNickname());
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            System.out.println("Ошибка входа: " + e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    /** Запрос для регистрации */
    @PostMapping("/signup")
    public ResponseEntity<JwtResponse> registerUser(@RequestBody SignupRequest signUpRequest) {
        System.out.println("Получен запрос на регистрацию: " + signUpRequest.getNickname());
        try {
            JwtResponse jwtResponse = authService.registerUser(signUpRequest);
            System.out.println("Успешная регистрация для: " + signUpRequest.getNickname());
            return ResponseEntity.ok(jwtResponse);
        } catch (RuntimeException e) {
            System.out.println("Ошибка регистрации: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Player> getCurrentUser() {
        return ResponseEntity.ok().build();
    }


    /** Смена пароля */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest,
                                            @AuthenticationPrincipal Player player) {
        try {
            System.out.println("Получен запрос на смену пароля для пользователя: " +
                    (player != null ? player.getNickname() : "null"));
            System.out.println("Старый пароль: " + changePasswordRequest.getOldPassword());
            System.out.println("Новый пароль: " + changePasswordRequest.getNewPassword());

            if (player == null) {
                System.out.println("Ошибка: пользователь не аутентифицирован");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Пользователь не аутентифицирован");
                return ResponseEntity.status(401).body(errorResponse);
            }

            authService.changePassword(player, changePasswordRequest.getOldPassword(), changePasswordRequest.getNewPassword());

            System.out.println("Пароль успешно изменен для пользователя: " + player.getNickname());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Пароль успешно изменен");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.out.println("Ошибка при смене пароля: " + e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            System.out.println("Неожиданная ошибка при смене пароля: " + e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Ошибка при смене пароля: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}