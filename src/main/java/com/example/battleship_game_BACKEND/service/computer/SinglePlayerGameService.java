package com.example.battleship_game_BACKEND.service.computer;

import com.example.battleship_game_BACKEND.model.*;
import com.example.battleship_game_BACKEND.repository.GameRepository;
import com.example.battleship_game_BACKEND.service.computer.RandomHuntStrategyService;
import com.example.battleship_game_BACKEND.service.placement.CoastalPlacementStrategy;
import com.example.battleship_game_BACKEND.service.placement.DiagonalPlacementStrategy;
import com.example.battleship_game_BACKEND.service.placement.HalfPlacementStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SinglePlayerGameService {

    private final RandomHuntStrategyService computerStrategy;
    private final CoastalPlacementStrategy coastalPlacement;
    private final DiagonalPlacementStrategy diagonalPlacement;
    private final HalfPlacementStrategy halfPlacement;
    private final GameRepository gameRepository;

    // Виртуальный игрок для компьютера
    private final Player computerPlayer = createComputerPlayer();

    // Кэш для стратегий размещения
    private final Map<String, PlacementStrategy> strategyCache = new HashMap<>();

    private Player createComputerPlayer() {
        Player player = new Player();
        player.setPlayerId(-1L); // Виртуальный ID
        player.setNickname("Компьютер");
        player.setAvatarUrl(Player.DEFAULT_AVATAR);
        player.setStatus(true);
        return player;
    }

    /**
     * Создать новую игру с компьютером
     */
    @Transactional
    public Game createNewGame(Player humanPlayer, String computerStrategyName) {
        // Создаем доски для обоих игроков
        GameBoard humanBoard = createEmptyBoard();
        GameBoard computerBoard = createComputerBoard(computerStrategyName);

        // Создаем игру
        Game game = new Game();
        game.setPlayer1(humanPlayer);
        game.setPlayer2(computerPlayer);
        game.setGameBoard1(humanBoard);
        game.setGameBoard2(computerBoard);
        game.setGameType(GameType.SINGLEPLAYER);
        game.setGameStatus(GameStatus.ACTIVE);
        game.setStartDate(LocalDateTime.now());
        game.setResult("PLACING_SHIPS"); // Сначала игрок размещает корабли

        return gameRepository.save(game); // <-- Сохраняем игру в БД
    }

    /**
     * Сделать ход игрока
     */
    @Transactional
    public GameMoveResult makePlayerMove(Game game, int row, int col) {
        if (game.getGameStatus() != GameStatus.ACTIVE) {
            throw new IllegalStateException("Игра уже завершена");
        }

        if (!"PLAYER_TURN".equals(game.getResult())) {
            throw new IllegalStateException("Сейчас ход компьютера");
        }

        GameBoard computerBoard = game.getGameBoard2();
        Character[][] boardMatrix = convertStringToMatrix(computerBoard.getPlacementMatrix());

        // Проверяем, не стреляли ли мы уже в эту клетку
        char cellValue = boardMatrix[row][col];
        if (cellValue == 'X' || cellValue == 'O') {
            throw new IllegalArgumentException("В эту клетку уже стреляли");
        }

        boolean hit = false;
        boolean shipSunk = false;
        String hitShipType = "";

        // Проверяем попадание
        if (cellValue == 'S') {
            hit = true;
            boardMatrix[row][col] = 'X'; // Отмечаем попадание

            // Проверяем, потоплен ли корабль
            shipSunk = checkIfShipSunk(boardMatrix, row, col);
            if (shipSunk) {
                hitShipType = "корабль потоплен";
            } else {
                hitShipType = "корабль ранен";
            }
        } else {
            boardMatrix[row][col] = 'O'; // Отмечаем промах
            hitShipType = "промах";
        }

        // Обновляем доску компьютера
        computerBoard.setPlacementMatrix(convertMatrixToString(boardMatrix));

        // Проверяем победу
        boolean playerWins = checkVictory(boardMatrix);
        if (playerWins) {
            game.setGameStatus(GameStatus.COMPLETED);
            game.setEndDate(LocalDateTime.now());
            game.setResult("PLAYER_WON");
        } else if (!hit) {
            // Если промах, передаем ход компьютеру
            game.setResult("COMPUTER_TURN");
        }
        // Если попадание, игрок продолжает ходить

        GameMoveResult result = new GameMoveResult();
        result.setHit(hit);
        result.setShipSunk(shipSunk);
        result.setHitShipType(hitShipType);
        result.setPlayerWins(playerWins);
        result.setNextTurn(hit ? "PLAYER_TURN" : "COMPUTER_TURN");

        if (!hit || playerWins) {
            // Если промах или победа, обрабатываем ответный ход компьютера
            if (!playerWins) {
                ComputerMoveResult computerMove = makeComputerMove(game);
                result.setComputerMove(computerMove);
            }
        }

        return result;
    }

    /**
     * Сделать ход компьютера
     */
    private ComputerMoveResult makeComputerMove(Game game) {
        GameBoard humanBoard = game.getGameBoard1();
        Character[][] boardMatrix = convertStringToMatrix(humanBoard.getPlacementMatrix());

        // Получаем следующий выстрел компьютера
        int[] shot = computerStrategy.getNextShot(game, humanBoard);
        int row = shot[0];
        int col = shot[1];

        boolean hit = false;
        boolean shipSunk = false;
        String hitShipType = "";

        // Проверяем попадание
        if (boardMatrix[row][col] == 'S') {
            hit = true;
            boardMatrix[row][col] = 'X'; // Попадание

            // Проверяем, потоплен ли корабль
            shipSunk = checkIfShipSunk(boardMatrix, row, col);
            if (shipSunk) {
                hitShipType = "корабль потоплен";
            } else {
                hitShipType = "корабль ранен";
            }
        } else {
            boardMatrix[row][col] = 'O'; // Промах
            hitShipType = "промах";
        }

        // Обновляем доску человека
        humanBoard.setPlacementMatrix(convertMatrixToString(boardMatrix));

        // Обновляем состояние компьютера
        computerStrategy.updateAfterShot(game, row, col, hit, shipSunk);

        // Проверяем победу компьютера
        boolean computerWins = checkVictory(boardMatrix);
        if (computerWins) {
            game.setGameStatus(GameStatus.COMPLETED);
            game.setEndDate(LocalDateTime.now());
            game.setResult("COMPUTER_WON");
        } else if (!hit) {
            // Если промах, передаем ход игроку
            game.setResult("PLAYER_TURN");
        }
        // Если попадание, компьютер продолжает ходить

        ComputerMoveResult result = new ComputerMoveResult();
        result.setRow(row);
        result.setCol(col);
        result.setHit(hit);
        result.setShipSunk(shipSunk);
        result.setHitShipType(hitShipType);
        result.setComputerWins(computerWins);
        result.setNextTurn(hit ? "COMPUTER_TURN" : "PLAYER_TURN");

        return result;
    }

    /**
     * Разместить корабли игрока на доске
     */
    @Transactional
    public void placePlayerShips(Game game, Character[][] shipPlacement) {

        GameBoard humanBoard = game.getGameBoard1();
        humanBoard.setPlacementMatrix(convertMatrixToString(shipPlacement));

        // После размещения кораблей начинается игра
        game.setResult("PLAYER_TURN");
    }

    /**
     * Получить текущее состояние игры для фронтенда
     */
    public GameStateResponse getGameState(Game game) {
        GameStateResponse response = new GameStateResponse();
        response.setGameId(game.getGameId());
        response.setGameStatus(game.getGameStatus());

        // Определение текущего игрока
        if ("PLAYER_TURN".equals(game.getResult()) || "PLAYER_WON".equals(game.getResult())) {
            response.setCurrentPlayer("HUMAN");
        } else if ("COMPUTER_TURN".equals(game.getResult()) || "COMPUTER_WON".equals(game.getResult())) {
            response.setCurrentPlayer("COMPUTER");
        } else if ("PLACING_SHIPS".equals(game.getResult())) {
            response.setCurrentPlayer("HUMAN");
        } else {
            response.setCurrentPlayer("HUMAN"); // По умолчанию
        }

        // Доска человека
        Character[][] humanBoard = convertStringToMatrix(game.getGameBoard1().getPlacementMatrix());
        response.setHumanBoard(convertTo2DStringArray(humanBoard));

        // Доска компьютера (только выстрелы)
        Character[][] computerBoard = convertStringToMatrix(game.getGameBoard2().getPlacementMatrix());
        Character[][] visibleComputerBoard = new Character[10][10];

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                char cell = computerBoard[i][j];
                visibleComputerBoard[i][j] = (cell == 'X' || cell == 'O') ? cell : ' ';
            }
        }

        response.setComputerBoard(convertTo2DStringArray(visibleComputerBoard));

        // Дополнительные поля для фронтенда
        response.setShipsPlaced(!"PLACING_SHIPS".equals(game.getResult()));
        response.setGameOver(game.getGameStatus() == GameStatus.COMPLETED);

        if (game.getGameStatus() == GameStatus.COMPLETED) {
            if ("PLAYER_WON".equals(game.getResult())) {
                response.setWinner("HUMAN");
            } else if ("COMPUTER_WON".equals(game.getResult())) {
                response.setWinner("COMPUTER");
            }
        }

        return response;
    }
    /**
            * Получить игру по ID
     */
    @Transactional(readOnly = true)
    public Game getGameById(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found with ID: " + gameId));
    }

    // Вспомогательные методы
    private void validateShipPlacement(Character[][] placement) {
        if (placement == null || placement.length != 10 || placement[0].length != 10) {
            throw new IllegalArgumentException("Invalid ship placement matrix size");
        }

        int shipCount = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (placement[i][j] == 'S') {
                    shipCount++;
                }
            }
        }

        if (shipCount != 20) {
            throw new IllegalArgumentException("Invalid number of ship cells: " + shipCount + ". Expected 20.");
        }
    }

    private String[][] convertTo2DStringArray(Character[][] charArray) {
        String[][] result = new String[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                result[i][j] = String.valueOf(charArray[i][j] != null ? charArray[i][j] : ' ');
            }
        }
        return result;
    }

    private GameBoard createEmptyBoard() {
        GameBoard board = new GameBoard();
        Character[][] emptyBoard = new Character[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                emptyBoard[i][j] = ' '; // Пустое поле
            }
        }
        board.setPlacementMatrix(convertMatrixToString(emptyBoard));
        return board;
    }

    private GameBoard createComputerBoard(String strategyName) {
        GameBoard board = new GameBoard();

        // Используем кэш для стратегий
        PlacementStrategy strategy = strategyCache.computeIfAbsent(strategyName.toLowerCase(), key -> {
            switch (key) {
                case "coastal":
                    return coastalPlacement.createCoastalStrategy(computerPlayer);
                case "diagonal":
                    return diagonalPlacement.createDiagonalStrategy(computerPlayer);
                case "half_left":
                    return halfPlacement.createHalfStrategy(computerPlayer);
                case "half_right":
                    return halfPlacement.createHalfStrategyRight(computerPlayer);
                default:
                    throw new IllegalArgumentException("Неизвестная стратегия: " + strategyName);
            }
        });

        board.setPlacementMatrix(strategy.getPlacementMatrix());
        return board;
    }

    private Character[][] convertStringToMatrix(String placementMatrix) {
        if (placementMatrix == null || placementMatrix.isEmpty()) {
            return new Character[10][10];
        }

        String[] rows = placementMatrix.split(";");
        Character[][] matrix = new Character[10][10];

        // Заполняем матрицу начальными значениями
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        // Заполняем данными из строки
        for (int i = 0; i < Math.min(rows.length, 10); i++) {
            String[] cells = rows[i].split(",");
            for (int j = 0; j < Math.min(cells.length, 10); j++) {
                if (j < cells.length && !cells[j].isEmpty()) {
                    matrix[i][j] = cells[j].charAt(0);
                }
            }
        }

        return matrix;
    }

    private String convertMatrixToString(Character[][] matrix) {
        if (matrix == null || matrix.length < 10 || matrix[0].length < 10) {
            matrix = new Character[10][10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    matrix[i][j] = ' ';
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i > 0) sb.append(";");
            for (int j = 0; j < 10; j++) {
                if (j > 0) sb.append(",");
                char value = matrix[i][j] != null ? matrix[i][j] : ' ';
                sb.append(value);
            }
        }
        return sb.toString();
    }

    private boolean checkIfShipSunk(Character[][] board, int row, int col) {
        // Простая проверка: ищем соседние клетки корабля
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int newRow = row + dx[i];
            int newCol = col + dy[i];

            if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 10) {
                if (board[newRow][newCol] == 'S') {
                    return false; // Есть живая часть корабля
                }
            }
        }

        return true; // Корабль потоплен
    }

    private boolean checkVictory(Character[][] board) {
        // Проверяем, остались ли еще непотопленные корабли
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (board[i][j] == 'S') {
                    return false; // Найден живой корабль
                }
            }
        }
        return true; // Все корабли потоплены
    }

    @Transactional
    public void surrenderGame(Game game, Player currentPlayer) {
        game.setGameStatus(GameStatus.COMPLETED);
        game.setEndDate(LocalDateTime.now());
        game.setResult("COMPUTER_WON");
    }


    // Классы для ответов API
    @lombok.Data
    public static class GameMoveResult {
        private boolean hit;
        private boolean shipSunk;
        private String hitShipType;
        private boolean playerWins;
        private String nextTurn;
        private ComputerMoveResult computerMove;
    }

    @lombok.Data
    public static class ComputerMoveResult {
        private int row;
        private int col;
        private boolean hit;
        private boolean shipSunk;
        private String hitShipType;
        private boolean computerWins;
        private String nextTurn;
    }

    @lombok.Data
    public static class GameStateResponse {
        private Long gameId;
        private GameStatus gameStatus;
        private String currentPlayer; // "HUMAN" или "COMPUTER"
        private String[][] humanBoard;
        private String[][] computerBoard;
        private boolean shipsPlaced;
        private boolean gameOver;
        private String winner; // "HUMAN", "COMPUTER"
    }
}