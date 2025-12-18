package com.example.battleship_game_BACKEND.service.computer;

import com.example.battleship_game_BACKEND.model.*;
import com.example.battleship_game_BACKEND.repository.GameRepository;
import com.example.battleship_game_BACKEND.service.computer.RandomHuntStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ComputerGameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RandomHuntStrategyService randomHuntStrategy;

    /**
     * Выполнить ход компьютера
     */
    public ShotResult computerMakeMove(Long gameId) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new RuntimeException("Игра не найдена");
        }

        Game game = gameOpt.get();

        // Проверяем, что это игра с компьютером
        if (game.getGameType() != GameType.SINGLEPLAYER) {
            throw new RuntimeException("Это не игра с компьютером");
        }

        // Получаем доску игрока (противника для компьютера)
        // Предполагаем: player1 - игрок, player2 - компьютер
        GameBoard humanBoard = game.getGameBoard1();

        // Получаем следующий выстрел от компьютера
        int[] shot = randomHuntStrategy.getNextShot(game, humanBoard);
        int row = shot[0];
        int col = shot[1];

        // Проверяем попадание
        boolean isHit = checkHit(humanBoard, row, col);
        boolean isSunk = false;

        if (isHit) {
            isSunk = checkIfShipSunk(humanBoard, row, col);
        }

        // Обновляем состояние компьютера и сохраняем в поле result
        randomHuntStrategy.updateAfterShot(game, row, col, isHit, isSunk);

        // Обновляем доску игрока
        updateBoardAfterShot(humanBoard, row, col, isHit);

        // Проверяем завершение игры
        if (checkGameOver(humanBoard)) {
            game.setGameStatus(GameStatus.COMPLETED);
            game.setEndDate(LocalDateTime.now());

            // Сохраняем результат игры (кто выиграл)
            // Если компьютеру некуда стрелять (все корабли игрока потоплены)
            // Проверяем: если все клетки игрока отмечены как попадания или промахи
            if (isGameBoardAllHitOrMissed(humanBoard)) {
                game.setResult("ПОБЕДА КОМПЬЮТЕРА");
            }
        }

        // Сохраняем игру
        gameRepository.save(game);

        return new ShotResult(row, col, isHit, isSunk);
    }

    /**
     * Проверить попадание
     */
    private boolean checkHit(GameBoard board, int row, int col) {
        Character[][] matrix = getBoardMatrix(board);
        return matrix[row][col] == 'S'; // 'S' - корабль
    }

    /**
     * Проверить, потоплен ли корабль
     */
    private boolean checkIfShipSunk(GameBoard board, int row, int col) {
        Character[][] matrix = getBoardMatrix(board);

        // Проверяем все направления
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 10) {
                if (matrix[newRow][newCol] == 'S') {
                    return false; // Еще есть часть корабля
                }
            }
        }

        return true; // Одиночная клетка корабля
    }

    /**
     * Обновить доску после выстрела
     */
    private void updateBoardAfterShot(GameBoard board, int row, int col, boolean hit) {
        Character[][] matrix = getBoardMatrix(board);

        if (hit) {
            matrix[row][col] = 'X'; // Попадание
        } else {
            matrix[row][col] = 'O'; // Промах
        }

        board.setPlacementMatrix(convertMatrixToString(matrix));
    }

    /**
     * Проверить завершение игры
     */
    private boolean checkGameOver(GameBoard board) {
        Character[][] matrix = getBoardMatrix(board);

        // Если остались непотопленные корабли ('S'), игра не закончена
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Проверить, все ли клетки доски отмечены
     */
    private boolean isGameBoardAllHitOrMissed(GameBoard board) {
        Character[][] matrix = getBoardMatrix(board);

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                char cell = matrix[i][j];
                if (cell != 'X' && cell != 'O') { // Не попадание и не промах
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Конвертировать доску в матрицу
     */
    private Character[][] getBoardMatrix(GameBoard board) {
        Character[][] matrix = new Character[10][10];
        String placement = board.getPlacementMatrix();

        if (placement != null && !placement.isEmpty()) {
            String[] rows = placement.split(";");
            for (int i = 0; i < rows.length && i < 10; i++) {
                String[] cols = rows[i].split(",");
                for (int j = 0; j < cols.length && j < 10; j++) {
                    matrix[i][j] = cols[j].charAt(0);
                }
            }
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == null) {
                    matrix[i][j] = ' ';
                }
            }
        }

        return matrix;
    }

    /**
     * Конвертировать матрицу в строку
     */
    private String convertMatrixToString(Character[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            if (i > 0) sb.append(";");
            for (int j = 0; j < matrix[i].length; j++) {
                if (j > 0) sb.append(",");
                sb.append(matrix[i][j] != null ? matrix[i][j] : ' ');
            }
        }
        return sb.toString();
    }

    /**
     * Инициализировать состояние компьютера для новой игры
     */
    public void initializeComputerState(Game game) {
        // Очищаем поле result или устанавливаем начальное состояние
        // Можно оставить пустым, так как RandomHuntStrategyService создаст новое состояние при первом выстреле
        game.setResult(""); // Очищаем результат предыдущей игры
    }

    /**
     * DTO для результата выстрела
     */
    public static class ShotResult {
        private int row;
        private int col;
        private boolean hit;
        private boolean sunk;

        public ShotResult(int row, int col, boolean hit, boolean sunk) {
            this.row = row;
            this.col = col;
            this.hit = hit;
            this.sunk = sunk;
        }

        // Getters
        public int getRow() { return row; }
        public int getCol() { return col; }
        public boolean isHit() { return hit; }
        public boolean isSunk() { return sunk; }
    }
}