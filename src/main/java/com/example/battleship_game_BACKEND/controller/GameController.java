package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.model.*;
import com.example.battleship_game_BACKEND.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/start")
    public ResponseEntity<GameStatus> startGame(@RequestBody StartGameRequest request) {
        GameStatus status = gameService.startGame(request);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/player-shot")
    public ResponseEntity<ShotResult> playerShot(@RequestBody ShotRequest request) {
        ShotResult result = gameService.playerShot(request.getRow(), request.getCol());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/computer-shot")
    public ResponseEntity<ShotResult> computerShot() {
        ShotResult result = gameService.computerShot();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<GameStatus> getGameStatus() {
        GameStatus status = gameService.getGameStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/board")
    public ResponseEntity<Map<String, Object>> getBoard() {
        Map<String, Object> board = gameService.getBoard();
        return ResponseEntity.ok(board);
    }
}