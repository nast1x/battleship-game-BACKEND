package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.AvatarUpdateRequest;
import com.example.battleship_game_BACKEND.dto.PlayerMultiplayerDTO;
import com.example.battleship_game_BACKEND.dto.PlayerProfileDTO;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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


    /** Обновление аватара у пользователя */
    @PutMapping("/avatar")
    @SneakyThrows
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

    /** Получение всех аватарок пользователей */
    @GetMapping("/avatars")
    public ResponseEntity<String[]> getAvailableAvatars() {
        System.out.println("Fetching available avatars");
        String[] avatars = playerService.getAvailableAvatars();
        System.out.println("Returning avatars: " + Arrays.toString(avatars));
        return ResponseEntity.ok(avatars);
    }

    /** Просмотр профиля пользователя */
    @GetMapping("/current")
    public ResponseEntity<PlayerProfileDTO> getCurrentPlayer(@AuthenticationPrincipal Player player) {
        try {
            // Просто вызываем сервис, который теперь умеет считать статистику
            PlayerProfileDTO profile = playerService.getPlayerProfileWithStats(player);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** Вывод всех игроков в мультиплеере*/
    @GetMapping("/all")
    public ResponseEntity<List<PlayerMultiplayerDTO>> getAllPlayers() {
        try {
            System.out.println("Fetching all players");
            List<Player> players = playerService.getOnlinePlayers();
            List<PlayerMultiplayerDTO> playerSummaries = players.stream()
                    .map(player -> new PlayerMultiplayerDTO( player.getPlayerId(), player.getNickname(), player.getAvatarUrl()))
                    .toList();

            System.out.println("Returning " + playerSummaries.size() + " player summaries");
            return ResponseEntity.ok(playerSummaries);

        } catch (Exception e) {
            System.out.println("Error fetching players: " + e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
}