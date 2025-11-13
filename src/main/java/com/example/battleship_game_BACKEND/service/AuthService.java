package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.JwtResponse;
import com.example.battleship_game_BACKEND.dto.LoginRequest;
import com.example.battleship_game_BACKEND.dto.SignupRequest;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import com.example.battleship_game_BACKEND.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getNickname(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        Player player = (Player) authentication.getPrincipal();
        String jwt = jwtTokenUtil.generateToken(player);

        return new JwtResponse(jwt, "Bearer", player.getPlayerId(), player.getUsername());
    }

    public JwtResponse registerUser(SignupRequest signUpRequest) {
        if (playerRepository.existsByNickname(signUpRequest.getNickname())) {
            throw new RuntimeException("Error: Nickname is already taken!");
        }

        Player player = new Player();
        player.setNickname(signUpRequest.getNickname());
        player.setPassword(passwordEncoder.encode(signUpRequest.getPassword())); // Хешируем пароль

        Player savedPlayer = playerRepository.save(player);
        String jwt = jwtTokenUtil.generateToken(savedPlayer);

        return new JwtResponse(jwt, "Bearer", savedPlayer.getPlayerId(), savedPlayer.getUsername());
    }

    public void changePassword(Player player, String oldPassword, String newPassword) {
        // Проверяем старый пароль
        if (!passwordEncoder.matches(oldPassword, player.getPassword())) {
            throw new RuntimeException("Неверный старый пароль");
        }

        // Проверяем, что новый пароль отличается от старого
        if (passwordEncoder.matches(newPassword, player.getPassword())) {
            throw new RuntimeException("Новый пароль должен отличаться от старого");
        }

        // Устанавливаем новый пароль
        player.setPassword(passwordEncoder.encode(newPassword));
        playerRepository.save(player);
    }
}