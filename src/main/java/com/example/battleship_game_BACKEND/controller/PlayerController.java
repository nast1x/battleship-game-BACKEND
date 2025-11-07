package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.service.PlayerService; // маленькая 's'
import com.example.battleship_game_BACKEND.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;

    @GetMapping
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Player API is working!");
    }

    @PostMapping("/register")
    public ResponseEntity<Player> registerPlayer(@RequestBody Player player) {
        if (playerService.nicknameExists(player.getNickname())) {
            return ResponseEntity.badRequest().build();
        }
        Player savedPlayer = playerService.createPlayer(player);
        return ResponseEntity.ok(savedPlayer);
    }

    @GetMapping("/{nickname}")
    public ResponseEntity<Player> getPlayer(@PathVariable String nickname) {
        return playerService.getPlayerByNickname(nickname)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}