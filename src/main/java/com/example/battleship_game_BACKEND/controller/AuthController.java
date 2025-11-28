package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.ChangePasswordRequest;
import com.example.battleship_game_BACKEND.dto.JwtResponse;
import com.example.battleship_game_BACKEND.dto.LoginRequest;
import com.example.battleship_game_BACKEND.dto.SignupRequest;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import com.example.battleship_game_BACKEND.security.JwtTokenUtil;
import com.example.battleship_game_BACKEND.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    /** Запрос для авторизации */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getNickname(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            Player player = (Player) authentication.getPrincipal();
            Player playerToUpdate = playerRepository.findByNickname(player.getNickname())
                    .orElseThrow(() -> new RuntimeException("Player not found after auth"));

            playerToUpdate.setStatus(true);
            playerRepository.save(playerToUpdate);

            String jwt = jwtTokenUtil.generateToken(player);
            JwtResponse response = new JwtResponse(
                    jwt, "Bearer",
                    player.getPlayerId(),
                    player.getNickname(),
                    player.getAvatarUrl() != null ? player.getAvatarUrl() : Player.DEFAULT_AVATAR
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Ошибка входа: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Неверный логин или пароль"));
        }
    }

    /** Запрос для регистрации */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        try {
            System.out.println("Получен запрос на регистрацию: " + signUpRequest.getNickname());
            if (playerRepository.existsByNickname(signUpRequest.getNickname())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Игрок с таким никнеймом уже существует"));
            }
            Player player = new Player();
            player.setNickname(signUpRequest.getNickname());
            player.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
            player.setAvatarUrl(Player.DEFAULT_AVATAR);
            player.setStatus(true);
            Player savedPlayer = playerRepository.save(player);
            String jwt = jwtTokenUtil.generateToken(savedPlayer);
            JwtResponse response = new JwtResponse(
                    jwt, "Bearer",
                    savedPlayer.getPlayerId(),
                    savedPlayer.getNickname(),
                    savedPlayer.getAvatarUrl() != null ? savedPlayer.getAvatarUrl() : Player.DEFAULT_AVATAR
            );
            System.out.println("Успешная регистрация для: " + signUpRequest.getNickname());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Ошибка регистрации: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ошибка при регистрации: " + e.getMessage()));
        }
    }

    /** Запрос для выхода из системы*/
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Player player) {
        if (player != null) {
            player.setStatus(false);
            playerRepository.save(player);
        }
        return ResponseEntity.ok().build();
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