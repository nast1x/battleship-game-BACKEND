package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.*;
import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.service.ComputerGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/computer-game")
@RequiredArgsConstructor
public class ComputerGameController {

    private final ComputerGameService computerGameService;

    @PostMapping("/start")
    public ResponseEntity<Game> startGame(
            @RequestParam Long playerId,
            @RequestBody ComputerGameStartRequest request) {
        Game game = computerGameService.createComputerGame(playerId, request);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameId}/setup")
    public ResponseEntity<Game> setupGame(
            @PathVariable Long gameId,
            @RequestBody ComputerGameStartRequest request) {
        Game game = computerGameService.setupGame(gameId, request);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<Map<String, Object>> getGameState(@PathVariable Long gameId) {
        Map<String, Object> state = computerGameService.getGameState(gameId);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{gameId}/shot")
    public ResponseEntity<ShotResponse> makeShot(
            @PathVariable Long gameId,
            @RequestBody ShotRequest request) {
        ShotResponse response = computerGameService.processPlayerShot(gameId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/surrender")
    public ResponseEntity<Void> surrender(@PathVariable Long gameId) {
        computerGameService.surrender(gameId);
        return ResponseEntity.ok().build();
    }
}