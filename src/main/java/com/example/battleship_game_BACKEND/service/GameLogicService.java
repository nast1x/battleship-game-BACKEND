// GameLogicService.java
package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.GameStateDTO;
import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameBoard;
import com.example.battleship_game_BACKEND.model.GameStatus;
import com.example.battleship_game_BACKEND.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameLogicService {

    private final InMemoryGameStateService gameStateService;
    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Обработка хода игрока
     */
    public Map<String, Object> processMove(Long gameId, Long playerId, int row, int col) {
        GameStateDTO state = gameStateService.getGameState(gameId);
        if (state == null) {
            throw new RuntimeException("Игра не найдена или не активна");
        }

        // Проверяем, что игра активна
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Игра не найдена"));

        if (game.getGameStatus() != GameStatus.ACTIVE) {
            throw new RuntimeException("Игра не активна");
        }

        // Проверяем, что сейчас ход этого игрока
        if (!state.getCurrentTurnPlayerId().equals(playerId)) {
            throw new RuntimeException("Не ваш ход");
        }

        // Проверяем валидность координат
        if (row < 0 || row >= 10 || col < 0 || col >= 10) {
            throw new RuntimeException("Неверные координаты");
        }

        // Определяем, кто стреляет (player1 или player2)
        Long player1Id = game.getPlayer1().getPlayerId();
        Long player2Id = game.getPlayer2().getPlayerId();

        boolean isPlayer1 = playerId.equals(player1Id);
        boolean isPlayer2 = playerId.equals(player2Id);

        if (!isPlayer1 && !isPlayer2) {
            throw new RuntimeException("Игрок не участвует в этой игре");
        }

        // Определяем поле противника и матрицу попаданий
        Character[][] opponentField = isPlayer1 ? state.getPlayer2Field() : state.getPlayer1Field();
        Character[][] playerHits = isPlayer1 ? state.getPlayer1Hits() : state.getPlayer2Hits();

        // Проверяем, что в эту клетку еще не стреляли
        if (playerHits[row][col] != ' ') {
            throw new RuntimeException("Уже стреляли в эту клетку");
        }

        Map<String, Object> result = new HashMap<>();

        // Проверяем попадание
        if (opponentField[row][col] == 'S') {
            // Попадание
            playerHits[row][col] = 'H'; // Hit
            result.put("hit", true);
            result.put("message", "Попадание!");

            // Обновляем счетчики
            if (isPlayer1) {
                state.setPlayer1HitsCount(state.getPlayer1HitsCount() + 1);
            } else {
                state.setPlayer2HitsCount(state.getPlayer2HitsCount() + 1);
            }

            // Проверяем, потоплен ли корабль
            boolean isSunk = checkIfShipSunk(row, col, opponentField, playerHits);
            result.put("sunk", isSunk);

            if (isSunk) {
                result.put("message", "Корабль потоплен!");
                markSurroundingAsMiss(opponentField, playerHits, row, col);
                // Обновляем количество оставшихся кораблей
                int shipsLeft = countShipsLeft(opponentField, playerHits);
                if (isPlayer1) {
                    state.setPlayer2ShipsLeft(shipsLeft);
                } else {
                    state.setPlayer1ShipsLeft(shipsLeft);
                }
            }
        } else {
            // Промах
            playerHits[row][col] = 'M'; // Miss
            result.put("hit", false);
            result.put("message", "Промах");

            // Меняем ход
            Long nextPlayerId = isPlayer1 ? player2Id : player1Id;
            state.setCurrentTurnPlayerId(nextPlayerId);
        }

        // Обновляем счетчик выстрелов
        if (isPlayer1) {
            state.setPlayer1ShotsFired(state.getPlayer1ShotsFired() + 1);
        } else {
            state.setPlayer2ShotsFired(state.getPlayer2ShotsFired() + 1);
        }

        // Обновляем состояние в памяти
        gameStateService.updateGameState(state);
        // Отправляем обновления обоим игрокам
        sendGameStateToPlayers(game);
        // Проверяем, не закончилась ли игра
        checkGameOver(game, state);

        // Добавляем информацию в результат
        result.put("gameId", gameId);
        result.put("playerId", playerId);
        result.put("row", row);
        result.put("col", col);
        result.put("nextTurnPlayerId", state.getCurrentTurnPlayerId());
        result.put("player1ShipsLeft", state.getPlayer1ShipsLeft());
        result.put("player2ShipsLeft", state.getPlayer2ShipsLeft());

        return result;
    }

    /**
     * Проверка потопления корабля
     */
    private boolean checkIfShipSunk(int row, int col, Character[][] field, Character[][] hits) {
        // Проверяем, все ли клетки корабля подбиты
        boolean[][] visited = new boolean[10][10];
        return checkShipCell(row, col, field, hits, visited, true);
    }

    private boolean checkShipCell(int row, int col, Character[][] field, Character[][] hits,
                                  boolean[][] visited, boolean allHit) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10 || visited[row][col]) {
            return allHit;
        }

        visited[row][col] = true;

        if (field[row][col] == 'S') {
            // Если клетка корабля не подбита, корабль не потоплен
            if (hits[row][col] != 'H') {
                allHit = false;
            }

            // Проверяем соседние клетки
            allHit = checkShipCell(row - 1, col, field, hits, visited, allHit);
            allHit = checkShipCell(row + 1, col, field, hits, visited, allHit);
            allHit = checkShipCell(row, col - 1, field, hits, visited, allHit);
            allHit = checkShipCell(row, col + 1, field, hits, visited, allHit);
        }

        return allHit;
    }

    /**
     * Подсчет оставшихся кораблей
     */
    private int countShipsLeft(Character[][] field, Character[][] hits) {
        boolean[][] visited = new boolean[10][10];
        int shipCount = 0;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (field[i][j] == 'S' && !visited[i][j]) {
                    // Проверяем, не потоплен ли этот корабль
                    boolean isThisShipSunk = checkIfShipSunk(i, j, field, hits);

                    // Если корабль НЕ потоплен, считаем его
                    if (!isThisShipSunk) {
                        shipCount++;
                    }

                    // Помечаем все клетки корабля как посещенные
                    markShipVisited(i, j, field, visited);
                }
            }
        }

        return shipCount;
    }

    private boolean isShipSunk(int row, int col, Character[][] field, Character[][] hits) {
        boolean[][] visited = new boolean[10][10];
        return !checkShipCell(row, col, field, hits, visited, true);
    }

    private void markShipVisited(int row, int col, Character[][] field, boolean[][] visited) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10 || visited[row][col] || field[row][col] != 'S') {
            return;
        }

        visited[row][col] = true;

        markShipVisited(row - 1, col, field, visited);
        markShipVisited(row + 1, col, field, visited);
        markShipVisited(row, col - 1, field, visited);
        markShipVisited(row, col + 1, field, visited);
    }

    /**
     * Проверка завершения игры
     */
    private void checkGameOver(Game game, GameStateDTO state) {
        if (state.getPlayer1ShipsLeft() == 0 || state.getPlayer2ShipsLeft() == 0) {
            game.setGameStatus(GameStatus.COMPLETED);
            game.setEndDate(LocalDateTime.now());

            if (state.getPlayer1ShipsLeft() == 0 && state.getPlayer2ShipsLeft() == 0) {
                game.setResult("DRAW");
                game.setResult(null); // В случае ничьей победителя нет
            } else if (state.getPlayer1ShipsLeft() == 0) {
                game.setResult(game.getPlayer2().getPlayerId().toString());
            } else {
                game.setResult(game.getPlayer1().getPlayerId().toString());
            }

            gameRepository.save(game);

            // Удаляем состояние из памяти (опционально, можно оставить для истории)
            gameStateService.removeGameState(game.getGameId());
        }
    }

    /**
     * Получение состояния игры для конкретного игрока
     */
    public Map<String, Object> getGameStateForPlayer(Long gameId, Long playerId) {
        GameStateDTO state = gameStateService.getGameState(gameId);
        if (state == null) {
            throw new RuntimeException("Игра не найдена");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Игра не найдена"));

        // Определяем, кто является текущим игроком
        boolean isPlayer1 = playerId.equals(game.getPlayer1().getPlayerId());
        boolean isPlayer2 = playerId.equals(game.getPlayer2().getPlayerId());

        if (!isPlayer1 && !isPlayer2) {
            throw new RuntimeException("Игрок не участвует в этой игре");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("gameId", gameId);
        result.put("currentTurnPlayerId", state.getCurrentTurnPlayerId());
        result.put("gameStatus", state.getGameStatus());

        // Для текущего игрока показываем его поле и попадания по нему
        if (isPlayer1) {
            result.put("myField", state.getPlayer1Field()); // его корабли
            result.put("myHits", state.getPlayer2Hits());   // ВЫСТРЕЛЫ СОПЕРНИКА ПО НЕМУ!
            result.put("opponentField", hideShips(state.getPlayer2Field(), state.getPlayer1Hits())); // его выстрелы по сопернику
            result.put("myShipsLeft", state.getPlayer1ShipsLeft());
            result.put("opponentShipsLeft", state.getPlayer2ShipsLeft());
            result.put("myShotsFired", state.getPlayer1ShotsFired());
            result.put("myHitsCount", state.getPlayer1HitsCount());
        } else {
            result.put("myField", state.getPlayer2Field()); // его корабли
            result.put("myHits", state.getPlayer1Hits());   // ВЫСТРЕЛЫ СОПЕРНИКА ПО НЕМУ!
            result.put("opponentField", hideShips(state.getPlayer1Field(), state.getPlayer2Hits())); // его выстрелы по сопернику
            result.put("myShipsLeft", state.getPlayer2ShipsLeft());
            result.put("opponentShipsLeft", state.getPlayer1ShipsLeft());
            result.put("myShotsFired", state.getPlayer2ShotsFired());
            result.put("myHitsCount", state.getPlayer2HitsCount());
        }

        result.put("isMyTurn", playerId.equals(state.getCurrentTurnPlayerId()));

        return result;
    }

    /**
     * Скрывает неподбитые корабли противника
     */
    private Character[][] hideShips(Character[][] field, Character[][] hits) {
        Character[][] hidden = new Character[10][10];

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (hits[i][j] == 'H' || hits[i][j] == 'M') {
                    // Показываем только попадания и промахи
                    hidden[i][j] = hits[i][j];
                } else if (field[i][j] == 'S') {
                    // Скрываем неподбитые корабли
                    hidden[i][j] = ' ';
                } else {
                    hidden[i][j] = ' ';
                }
            }
        }

        return hidden;
    }

    private void sendGameStateToPlayers(Game game) {
        Long playerId1 = game.getPlayer1().getPlayerId();
        Long playerId2 = game.getPlayer2().getPlayerId();

        // Отправляем обновление состояния каждому игроку
        sendGameStateToPlayer(game.getGameId(), playerId1);
        sendGameStateToPlayer(game.getGameId(), playerId2);
    }

    private void sendGameStateToPlayer(Long gameId, Long playerId) {
        try {
            Map<String, Object> gameState = getGameStateForPlayer(gameId, playerId);
            messagingTemplate.convertAndSend(
                    "/queue/game.state/" + playerId,
                    gameState
            );
        } catch (Exception e) {
            System.err.println("Ошибка при отправке состояния игроку " + playerId + ": " + e.getMessage());
        }
    }
    /**
     * Помечает клетки вокруг уничтоженного корабля как "Промах" (M)
     */
    private void markSurroundingAsMiss(Character[][] ships, Character[][] hits, int startRow, int startCol) {
        // 1. Находим все части этого корабля
        List<int[]> shipParts = findShipParts(ships, startRow, startCol);

        // 2. Проходим по каждой части корабля
        for (int[] part : shipParts) {
            int r = part[0];
            int c = part[1];

            // 3. Для каждой части проверяем соседей (квадрат 3x3)
            for (int i = r - 1; i <= r + 1; i++) {
                for (int j = c - 1; j <= c + 1; j++) {
                    if (i >= 0 && i < 10 && j >= 0 && j < 10) {
                        if (hits[i][j] != 'H') {
                            hits[i][j] = 'M';
                        }
                    }
                }
            }
        }
    }

    /**
     * Находит все координаты корабля, зная одну его точку
     */
    private List<int[]> findShipParts(Character[][] ships, int row, int col) {
        List<int[]> parts = new ArrayList<>();
        // Добавляем точку, в которую попали
        parts.add(new int[]{row, col});
        // Ищем вверх
        int r = row - 1;
        while (r >= 0 && ships[r][col] == 'S') {
            parts.add(new int[]{r, col});
            r--;
        }
        // Ищем вниз
        r = row + 1;
        while (r < 10 && ships[r][col] == 'S') {
            parts.add(new int[]{r, col});
            r++;
        }
        // Ищем влево
        int c = col - 1;
        while (c >= 0 && ships[row][c] == 'S') {
            parts.add(new int[]{row, c});
            c--;
        }
        // Ищем вправо
        c = col + 1;
        while (c < 10 && ships[row][c] == 'S') {
            parts.add(new int[]{row, c});
            c++;
        }

        return parts;
    }
}