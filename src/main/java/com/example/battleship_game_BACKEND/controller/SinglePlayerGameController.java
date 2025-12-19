package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameStatus;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.service.computer.SinglePlayerGameService;
import com.example.battleship_game_BACKEND.exeption.GameNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/singleplayer")
@RequiredArgsConstructor
public class SinglePlayerGameController {

    private final SinglePlayerGameService gameService;

    @PostMapping("/new")
    public ResponseEntity<SinglePlayerGameService.GameStateResponse> createNewGame(
            @RequestBody NewGameRequest request,
            @RequestAttribute("currentUser") Player currentPlayer) {

        // Валидация стратегии
        validateStrategy(request.getComputerStrategy());

        Game game = gameService.createNewGame(currentPlayer, request.getComputerStrategy());
        SinglePlayerGameService.GameStateResponse response = gameService.getGameState(game);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/place-ships")
    public ResponseEntity<SinglePlayerGameService.GameStateResponse> placeShips(
            @PathVariable Long gameId,
            @RequestBody ShipPlacementRequest request,
            @RequestAttribute("currentUser") Player currentPlayer) {

        // Получаем существующую игру по ID
        Game game = getGameById(gameId, currentPlayer);

        // Проверяем, что это ход размещения кораблей
        if (!"PLACING_SHIPS".equals(game.getResult())) {
            throw new IllegalStateException("Ships placement phase is over");
        }

        // Валидация размещения кораблей
        validateShipPlacement(request.getPlacementMatrix());

        gameService.placePlayerShips(game, request.getPlacementMatrix());

        // После размещения кораблей начинаем игру
        game.setResult("PLAYER_TURN");
        game.setGameStatus(GameStatus.ACTIVE);

        return ResponseEntity.ok(gameService.getGameState(game));
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<SinglePlayerGameService.GameMoveResult> makeMove(
            @PathVariable Long gameId,
            @RequestBody MoveRequest request,
            @RequestAttribute("currentUser") Player currentPlayer) {

        // Получаем существующую игру по ID
        Game game = getGameById(gameId, currentPlayer);

        // Проверяем, что игра активна
        if (game.getGameStatus() != GameStatus.ACTIVE) {
            throw new IllegalStateException("Game is not active");
        }

        // Проверяем, что сейчас ход игрока
        if (!"PLAYER_TURN".equals(game.getResult())) {
            throw new IllegalStateException("It's not your turn");
        }

        // Валидация координат
        validateCoordinates(request.getRow(), request.getCol());

        SinglePlayerGameService.GameMoveResult result = gameService.makePlayerMove(game, request.getRow(), request.getCol());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{gameId}/surrender")
    public ResponseEntity<SinglePlayerGameService.GameStateResponse> surrenderGame(
            @PathVariable Long gameId,
            @RequestAttribute("currentUser") Player currentPlayer) {

        // Получаем существующую игру по ID
        Game game = getGameById(gameId, currentPlayer);

        // Проверяем, что игра активна
        if (game.getGameStatus() != GameStatus.ACTIVE) {
            throw new IllegalStateException("Game is already finished");
        }

        gameService.surrenderGame(game, currentPlayer);
        return ResponseEntity.ok(gameService.getGameState(game));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<SinglePlayerGameService.GameStateResponse> getGameState(
            @PathVariable Long gameId,
            @RequestAttribute("currentUser") Player currentPlayer) {

        // Получаем существующую игру по ID
        Game game = getGameById(gameId, currentPlayer);
        return ResponseEntity.ok(gameService.getGameState(game));
    }

    // Вспомогательные методы для безопасности и валидации

    private Game getGameById(Long gameId, Player currentPlayer) {
        Game game = gameService.getGameById(gameId);

        if (game == null) {
            throw new GameNotFoundException("Game not found with ID: " + gameId);
        }

        // Проверка, что игрок является участником игры
        if (!game.getPlayer1().equals(currentPlayer)) {
            throw new AccessDeniedException("You don't have access to this game");
        }

        // Проверка, что это одиночная игра
        if (game.getGameType() != com.example.battleship_game_BACKEND.model.GameType.SINGLEPLAYER) {
            throw new IllegalStateException("This is not a single player game");
        }

        return game;
    }

    private void validateStrategy(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Computer strategy cannot be empty");
        }

        String[] validStrategies = {"coastal", "diagonal", "half_left", "half_right"};
        boolean isValid = false;
        for (String valid : validStrategies) {
            if (valid.equals(strategy.toLowerCase())) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new IllegalArgumentException("Invalid computer strategy: " + strategy);
        }
    }

    private void validateShipPlacement(Character[][] placement) {
        if (placement == null || placement.length != 10 || placement[0].length != 10) {
            throw new IllegalArgumentException("Invalid ship placement matrix size");
        }

        // Подсчет количества корабельных клеток
        int shipCount = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (placement[i][j] == 'S') {
                    shipCount++;
                }
            }
        }

        // Проверка на правильное количество кораблей (20 клеток для стандартного набора)
        if (shipCount != 20) {
            throw new IllegalArgumentException("Invalid number of ship cells: " + shipCount + ". Expected 20.");
        }

        // Дополнительная валидация: проверка, что корабли не касаются друг друга
        // (это упрощенная проверка, в реальном приложении должна быть сложнее)
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (placement[i][j] == 'S') {
                    // Проверяем соседние клетки по диагоналям
                    for (int di = -1; di <= 1; di += 2) {
                        for (int dj = -1; dj <= 1; dj += 2) {
                            int ni = i + di;
                            int nj = j + dj;
                            if (ni >= 0 && ni < 10 && nj >= 0 && nj < 10) {
                                if (placement[ni][nj] == 'S') {
                                    throw new IllegalArgumentException("Ships cannot touch each other diagonally at [" + i + "," + j + "] and [" + ni + "," + nj + "]");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void validateCoordinates(int row, int col) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10) {
            throw new IllegalArgumentException("Coordinates out of bounds. Row: " + row + ", Col: " + col);
        }
    }

    // DTO классы
    @lombok.Data
    public static class NewGameRequest {
        private String computerStrategy;
    }

    @lombok.Data
    public static class ShipPlacementRequest {
        private Character[][] placementMatrix;
    }

    @lombok.Data
    public static class MoveRequest {
        private int row;
        private int col;
    }
}