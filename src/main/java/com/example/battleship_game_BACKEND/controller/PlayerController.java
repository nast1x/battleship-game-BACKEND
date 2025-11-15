package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.AvatarUpdateRequest;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PutMapping("/avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestBody AvatarUpdateRequest request,
            @AuthenticationPrincipal Player player) {
        try {
            System.out.println("Updating avatar for player: " + player.getNickname() + " to: " + request.getAvatarFileName());
            Player updatedPlayer = playerService.updateAvatar(player.getPlayerId(), request.getAvatarFileName());
            return ResponseEntity.ok(updatedPlayer);
        } catch (RuntimeException e) {
            System.out.println("Error updating avatar: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error updating avatar: " + e.getMessage());
        }
    }

    @GetMapping("/avatars")
    public ResponseEntity<String[]> getAvailableAvatars() {
        System.out.println("Fetching available avatars");
        String[] avatars = playerService.getAvailableAvatars();
        System.out.println("Returning avatars: " + Arrays.toString(avatars));
        return ResponseEntity.ok(avatars);
    }

    // PlayerController.java - добавьте этот метод
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentPlayer(@AuthenticationPrincipal Player player) {
        try {
            System.out.println("Getting current player data for: " + player.getNickname());

            // Загружаем актуальные данные из базы
            Player currentPlayer = playerService.getPlayerByNickname(player.getNickname())
                    .orElseThrow(() -> new RuntimeException("Player not found"));

            System.out.println("Found player: " + currentPlayer.getNickname() + ", avatar: " + currentPlayer.getAvatarUrl());

            return ResponseEntity.ok(currentPlayer);
        } catch (RuntimeException e) {
            System.out.println("Error getting current player: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error getting player data: " + e.getMessage());
        }
    }

    // нужен для получения всех игроков
    @GetMapping("/all")
    public ResponseEntity<List<Player>> getAllPlayers() {
        try {
            System.out.println("Fetching all players");
            List<Player> players = playerService.getAllPlayers();
            System.out.println("Returning " + players.size() + " players");
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            System.out.println("Error fetching players: " + e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
}