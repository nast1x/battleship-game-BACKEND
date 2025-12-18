package com.example.battleship_game_BACKEND.controller;


import com.example.battleship_game_BACKEND.service.computer.ComputerGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/computer")
public class ComputerGameController {

    @Autowired
    private ComputerGameService computerGameService;

    /**
     * Ход компьютера
     */
    @PostMapping("/game/{gameId}/move")
    public ResponseEntity<?> computerMove(@PathVariable Long gameId) {
        try {
            ComputerGameService.ShotResult result = computerGameService.computerMakeMove(gameId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        }
    }

    /**
     * Инициализация состояния компьютера для новой игры
     */
    @PostMapping("/game/{gameId}/init")
    public ResponseEntity<?> initComputerState(@PathVariable Long gameId) {
        try {
            // Этот метод может быть вызван при начале новой игры с компьютером
            // В реальности это будет сделано в GameService при создании игры
            return ResponseEntity.ok("Состояние компьютера инициализировано");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        }
    }
}