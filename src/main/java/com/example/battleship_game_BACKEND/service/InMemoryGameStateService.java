// InMemoryGameStateService.java
package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.GameStateDTO;
import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameBoard;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryGameStateService {

    private final Map<Long, GameStateDTO> gameStates = new ConcurrentHashMap<>();

    /**
     * Инициализирует состояние игры при старте
     */
    public void initializeGameState(Game game, Long firstTurnPlayerId) {
        GameStateDTO state = new GameStateDTO();
        state.setGameId(game.getGameId());
        state.setCurrentTurnPlayerId(firstTurnPlayerId);
        state.setGameStatus("ACTIVE");

        // Инициализируем поля и попадания
        GameBoard player1Board = game.getGameBoard1();
        GameBoard player2Board = game.getGameBoard2();

        // Получаем матрицы кораблей
        Character[][] player1Field = player1Board.getPlacementMatrixAsArray();
        Character[][] player2Field = player2Board.getPlacementMatrixAsArray();

        // Создаем пустые матрицы попаданий (10x10)
        Character[][] emptyHits = createEmptyHitsMatrix();

        state.setPlayer1Field(player1Field);
        state.setPlayer2Field(player2Field);


        state.setPlayer1Hits(createEmptyHitsMatrix());
        state.setPlayer2Hits(createEmptyHitsMatrix());

        // Считаем начальное количество кораблей
        state.setPlayer1ShipsLeft(countShips(player1Field));
        state.setPlayer2ShipsLeft(countShips(player2Field));

        // Инициализируем счетчики
        state.setPlayer1ShotsFired(0);
        state.setPlayer2ShotsFired(0);
        state.setPlayer1HitsCount(0);
        state.setPlayer2HitsCount(0);

        gameStates.put(game.getGameId(), state);
    }

    /**
     * Получает состояние игры
     */
    public GameStateDTO getGameState(Long gameId) {
        return gameStates.get(gameId);
    }

    /**
     * Обновляет состояние игры
     */
    public void updateGameState(GameStateDTO state) {
        gameStates.put(state.getGameId(), state);
    }

    /**
     * Удаляет состояние игры (при завершении)
     */
    public void removeGameState(Long gameId) {
        gameStates.remove(gameId);
    }

    /**
     * Проверяет существование состояния игры
     */
    public boolean hasGameState(Long gameId) {
        return gameStates.containsKey(gameId);
    }

    /**
     * Создает пустую матрицу попаданий
     */
    private Character[][] createEmptyHitsMatrix() {
        Character[][] matrix = new Character[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }
        return matrix;
    }

    /**
     * Подсчитывает количество кораблей в матрице
     */
    private int countShips(Character[][] field) {
        // Используем DFS для подсчета отдельных кораблей
        boolean[][] visited = new boolean[10][10];
        int shipCount = 0;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (field[i][j] == 'S' && !visited[i][j]) {
                    // Нашли начало нового корабля
                    shipCount++;
                    markShipVisited(i, j, field, visited);
                }
            }
        }

        return shipCount;
    }

    /**
     * Помечает все клетки корабля как посещенные
     */
    private void markShipVisited(int row, int col, Character[][] field, boolean[][] visited) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10 || visited[row][col] || field[row][col] != 'S') {
            return;
        }

        visited[row][col] = true;

        // Рекурсивно проверяем соседние клетки
        markShipVisited(row - 1, col, field, visited);
        markShipVisited(row + 1, col, field, visited);
        markShipVisited(row, col - 1, field, visited);
        markShipVisited(row, col + 1, field, visited);
    }
}