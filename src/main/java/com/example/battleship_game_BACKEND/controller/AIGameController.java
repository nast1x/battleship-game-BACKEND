package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.model.*;
import com.example.battleship_game_BACKEND.service.computer.RandomHuntStrategyService;
import com.example.battleship_game_BACKEND.service.placement.CoastalPlacementStrategy;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AIGameController {

    @Autowired
    private RandomHuntStrategyService aiStrategyService;

    @Autowired
    private CoastalPlacementStrategy coastalPlacementStrategy;

    // Для демо - храним игры в памяти
    private final Map<Long, Game> aiGames = new HashMap<>();
    private final Map<Long, Character[][]> playerBoards = new HashMap<>();
    private final Map<Long, Character[][]> aiBoards = new HashMap<>();
    private long gameCounter = 1;

    /**
     * Запрос на создание новой игры с ИИ
     */
    @Data
    public static class CreateAIGameRequest {
        private Long playerId;
        private BoardLayoutDTO boardLayout;
        private String gameType = "SINGLEPLAYER";
    }

    /**
     * DTO для расстановки кораблей
     */
    @Data
    public static class BoardLayoutDTO {
        private List<ShipPlacementDTO> ships;
        private String[][] matrix;
    }

    /**
     * DTO для размещения одного корабля
     */
    @Data
    public static class ShipPlacementDTO {
        private int shipId;
        private int size;
        private int row;
        private int col;
        private boolean vertical;
    }

    /**
     * Запрос на выполнение хода
     */
    @Data
    public static class MoveRequest {
        private Long playerId;
        private int row;
        private int col;
    }

    /**
     * Ответ с состоянием игры
     */
    @Data
    public static class GameStateResponse {
        private Long gameId;
        private Long playerId;
        private String[][] playerField;        // Поле игрока с кораблями и выстрелами ИИ
        private String[][] opponentField;      // Поле ИИ с выстрелами игрока
        private String[][] opponentHits;       // Выстрелы ИИ по полю игрока
        private String[][] playerHits;         // Выстрелы игрока по полю ИИ
        private int playerShipsLeft;
        private int opponentShipsLeft;
        private boolean isPlayerTurn;
        private String gameStatus;
        private String message;
        private boolean gameOver;
        private String winner;                 // "PLAYER" или "COMPUTER"
        private int[] lastAIShot;             // Последний выстрел ИИ [row, col]
        private boolean lastAIShotHit;        // Был ли последний выстрел ИИ попаданием
    }

    /**
     * Создание новой игры с ИИ
     */
    @PostMapping("/game/create")
    public ResponseEntity<?> createAIGame(@RequestBody CreateAIGameRequest request) {
        try {
            // 1. Создаем ID игры
            Long gameId = gameCounter++;

            // 2. Создаем поле игрока из полученной матрицы
            Character[][] playerBoard = convertMatrixToCharArray(request.getBoardLayout().getMatrix());
            playerBoards.put(gameId, playerBoard);

            // 3. Создаем поле ИИ (случайная расстановка)
            Character[][] aiBoard = generateAIBoard();
            aiBoards.put(gameId, aiBoard);

            // 4. Создаем игру
            Game game = new Game();
            game.setGameId(gameId);

            // Создаем виртуальных игроков
            Player player = new Player();
            player.setPlayerId(request.getPlayerId());
            player.setNickname("Player_" + request.getPlayerId());

            Player aiPlayer = new Player();
            aiPlayer.setPlayerId(999L); // ID для ИИ
            aiPlayer.setNickname("COMPUTER");

            game.setPlayer1(player);
            game.setPlayer2(aiPlayer);
            game.setGameType(GameType.SINGLEPLAYER);
            game.setGameStatus(GameStatus.ACTIVE);
            game.setStartDate(LocalDateTime.now());

            // Инициализируем состояние ИИ
            String aiState = aiStrategyService.parseComputerState(null).serialize();
            game.setResult("AI_STATE:" + aiState);

            // Создаем игровые доски
            GameBoard playerGameBoard = new GameBoard();
            playerGameBoard.setGameBoardId(gameId * 10 + 1);
            playerGameBoard.setPlacementMatrix(convertCharArrayToString(playerBoard));

            GameBoard aiGameBoard = new GameBoard();
            aiGameBoard.setGameBoardId(gameId * 10 + 2);
            aiGameBoard.setPlacementMatrix(convertCharArrayToString(aiBoard));

            game.setGameBoard1(playerGameBoard);
            game.setGameBoard2(aiGameBoard);

            aiGames.put(gameId, game);

            // 5. Формируем ответ
            GameStateResponse response = createGameStateResponse(
                    gameId,
                    request.getPlayerId(),
                    playerBoard,
                    aiBoard,
                    true, // Игрок ходит первым
                    aiBoard
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка создания игры: " + e.getMessage()));
        }
    }

    /**
     * Выполнение хода игрока
     */
    @PostMapping("/game/{gameId}/move")
    public ResponseEntity<?> makeMove(@PathVariable Long gameId, @RequestBody MoveRequest request) {
        try {
            // 1. Проверяем существование игры
            Game game = aiGames.get(gameId);
            if (game == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Игра не найдена"));
            }

            if (game.getGameStatus() != GameStatus.ACTIVE) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Игра завершена"));
            }

            // 2. Получаем доски
            Character[][] playerBoard = playerBoards.get(gameId);
            Character[][] aiBoard = aiBoards.get(gameId);

            // 3. Проверяем валидность хода
            int row = request.getRow();
            int col = request.getCol();

            if (row < 0 || row >= 10 || col < 0 || col >= 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Неверные координаты"));
            }

            // Проверяем, не стреляли ли уже в эту клетку
            if (aiBoard[row][col] == 'H' || aiBoard[row][col] == 'M') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "В эту клетку уже стреляли"));
            }

            // 4. Обрабатываем выстрел игрока
            boolean hit = false;
            boolean sunk = false;

            if (aiBoard[row][col] == 'S') {
                // Попадание
                hit = true;
                aiBoard[row][col] = 'H';

                // Проверяем, потоплен ли корабль
                sunk = isShipSunk(row, col, aiBoard);

                if (sunk) {
                    // Отмечаем все клетки вокруг потопленного корабля как промахи
                    markAroundSunkShip(row, col, aiBoard);
                }
            } else {
                // Промах
                aiBoard[row][col] = 'M';
            }

            // 5. Проверяем, выиграл ли игрок
            boolean playerWon = checkAllShipsSunk(aiBoard);

            if (playerWon) {
                game.setGameStatus(GameStatus.COMPLETED);
                game.setEndDate(LocalDateTime.now());
                game.setResult("PLAYER_WINS");

                GameStateResponse response = createGameStateResponse(
                        gameId,
                        request.getPlayerId(),
                        playerBoard,
                        aiBoard,
                        false,
                        aiBoard
                );
                response.setGameOver(true);
                response.setWinner("PLAYER");
                return ResponseEntity.ok(response);
            }

            // 6. Ход ИИ
            int[] aiShot = aiStrategyService.getNextShot(game, game.getGameBoard2());
            boolean aiHit = false;
            boolean aiSunk = false;
            int aiRow = aiShot[0];
            int aiCol = aiShot[1];

            if (playerBoard[aiRow][aiCol] == 'S') {
                // Попадание ИИ
                aiHit = true;
                playerBoard[aiRow][aiCol] = 'H';

                // Проверяем, потоплен ли корабль
                aiSunk = isShipSunk(aiRow, aiCol, playerBoard);

                if (aiSunk) {
                    // Отмечаем все клетки вокруг потопленного корабля как промахи
                    markAroundSunkShip(aiRow, aiCol, playerBoard);
                }
            } else {
                // Промах ИИ
                playerBoard[aiRow][aiCol] = 'M';
            }

            // Обновляем состояние ИИ
            aiStrategyService.updateAfterShot(game, aiRow, aiCol, aiHit, aiSunk);

            // 7. Проверяем, выиграл ли ИИ
            boolean aiWon = checkAllShipsSunk(playerBoard);

            if (aiWon) {
                game.setGameStatus(GameStatus.COMPLETED);
                game.setEndDate(LocalDateTime.now());
                game.setResult("COMPUTER_WINS");

                GameStateResponse response = createGameStateResponse(
                        gameId,
                        request.getPlayerId(),
                        playerBoard,
                        aiBoard,
                        false,
                        aiBoard
                );
                response.setGameOver(true);
                response.setWinner("COMPUTER");
                response.setLastAIShot(aiShot);
                response.setLastAIShotHit(aiHit);
                return ResponseEntity.ok(response);
            }

            // 8. Обновляем состояние игры
            aiGames.put(gameId, game);
            playerBoards.put(gameId, playerBoard);
            aiBoards.put(gameId, aiBoard);

            // 9. Формируем ответ
            GameStateResponse response = createGameStateResponse(
                    gameId,
                    request.getPlayerId(),
                    playerBoard,
                    aiBoard,
                    true, // Следующий ход игрока
                    aiBoard
            );
            response.setLastAIShot(aiShot);
            response.setLastAIShotHit(aiHit);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка выполнения хода: " + e.getMessage()));
        }
    }

    /**
     * Получение состояния игры
     */
    @GetMapping("/game/{gameId}/state")
    public ResponseEntity<?> getGameState(@PathVariable Long gameId, @RequestParam Long playerId) {
        try {
            Game game = aiGames.get(gameId);
            if (game == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Игра не найдена"));
            }

            Character[][] playerBoard = playerBoards.get(gameId);
            Character[][] aiBoard = aiBoards.get(gameId);

            boolean isPlayerTurn = game.getGameStatus() == GameStatus.ACTIVE;

            GameStateResponse response = createGameStateResponse(
                    gameId,
                    playerId,
                    playerBoard,
                    aiBoard,
                    isPlayerTurn,
                    aiBoard
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка получения состояния: " + e.getMessage()));
        }
    }

    /**
     * Создание ответа с состоянием игры
     */
    private GameStateResponse createGameStateResponse(
            Long gameId,
            Long playerId,
            Character[][] playerBoard,
            Character[][] aiBoard,
            boolean isPlayerTurn,
            Character[][] originalAIBoard // оригинальная доска ИИ для подсчета кораблей
    ) {
        GameStateResponse response = new GameStateResponse();
        response.setGameId(gameId);
        response.setPlayerId(playerId);
        response.setPlayerTurn(isPlayerTurn);

        // Преобразуем доски в String[][]
        String[][] playerField = new String[10][10];
        String[][] opponentField = new String[10][10];
        String[][] playerHits = new String[10][10];
        String[][] opponentHits = new String[10][10];

        // Подсчет кораблей
        int playerShipsLeft = countShipsLeft(playerBoard);
        int opponentShipsLeft = countShipsLeft(originalAIBoard);

        // Заполняем поля
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                // Поле игрока: показываем корабли и попадания ИИ
                if (playerBoard[i][j] == 'S') {
                    playerField[i][j] = "S"; // Корабль
                } else if (playerBoard[i][j] == 'H') {
                    playerField[i][j] = "H"; // Попадание ИИ
                    opponentHits[i][j] = "H"; // Для статистики ИИ
                } else if (playerBoard[i][j] == 'M') {
                    playerField[i][j] = "M"; // Промах ИИ
                    opponentHits[i][j] = "M";
                } else {
                    playerField[i][j] = " ";
                    opponentHits[i][j] = " ";
                }

                // Поле противника (ИИ): показываем только выстрелы игрока
                if (aiBoard[i][j] == 'H') {
                    opponentField[i][j] = "H"; // Попадание игрока
                    playerHits[i][j] = "H";
                } else if (aiBoard[i][j] == 'M') {
                    opponentField[i][j] = "M"; // Промах игрока
                    playerHits[i][j] = "M";
                } else {
                    opponentField[i][j] = " "; // Скрываем корабли ИИ
                    playerHits[i][j] = " ";
                }
            }
        }

        response.setPlayerField(playerField);
        response.setOpponentField(opponentField);
        response.setPlayerHits(playerHits);
        response.setOpponentHits(opponentHits);
        response.setPlayerShipsLeft(playerShipsLeft);
        response.setOpponentShipsLeft(opponentShipsLeft);
        response.setGameOver(false);

        return response;
    }

    /**
     * Генерация случайного поля для ИИ
     */
    private Character[][] generateAIBoard() {
        // Используем береговую стратегию для ИИ
        Player aiPlayer = new Player();
        aiPlayer.setPlayerId(999L);
        aiPlayer.setNickname("COMPUTER");

        // Создаем стратегию размещения
        PlacementStrategy strategy = coastalPlacementStrategy.createCoastalStrategy(aiPlayer);

        // Получаем матрицу из стратегии
        Character[][] matrix = strategy.getPlacementMatrixAsArray();

        return matrix;
    }

    /**
     * Преобразование матрицы String в Character
     */
    private Character[][] convertMatrixToCharArray(String[][] matrix) {
        Character[][] charMatrix = new Character[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] != null && matrix[i][j].length() > 0) {
                    charMatrix[i][j] = matrix[i][j].charAt(0);
                } else {
                    charMatrix[i][j] = ' ';
                }
            }
        }
        return charMatrix;
    }

    /**
     * Преобразование Character массива в строку
     */
    private String convertCharArrayToString(Character[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i > 0) sb.append(";");
            for (int j = 0; j < 10; j++) {
                if (j > 0) sb.append(",");
                sb.append(matrix[i][j] != null ? matrix[i][j] : ' ');
            }
        }
        return sb.toString();
    }

    /**
     * Проверка, потоплен ли корабль
     */
    private boolean isShipSunk(int row, int col, Character[][] board) {
        // Если в этой клетке нет корабля или она не поражена
        if (board[row][col] != 'H') {
            return false;
        }

        // Ищем все клетки корабля
        Set<String> shipCells = new HashSet<>();
        findShipCells(row, col, board, new boolean[10][10], shipCells);

        // Проверяем, все ли клетки корабля поражены
        for (String cell : shipCells) {
            String[] coords = cell.split(",");
            int r = Integer.parseInt(coords[0]);
            int c = Integer.parseInt(coords[1]);
            if (board[r][c] != 'H') {
                return false;
            }
        }

        return true;
    }

    /**
     * Рекурсивный поиск всех клеток корабля
     */
    private void findShipCells(int row, int col, Character[][] board, boolean[][] visited, Set<String> shipCells) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10 || visited[row][col]) {
            return;
        }

        visited[row][col] = true;

        // Если это корабль (S) или попадание (H), добавляем
        if (board[row][col] == 'S' || board[row][col] == 'H') {
            shipCells.add(row + "," + col);

            // Проверяем соседние клетки
            findShipCells(row - 1, col, board, visited, shipCells); // вверх
            findShipCells(row + 1, col, board, visited, shipCells); // вниз
            findShipCells(row, col - 1, board, visited, shipCells); // влево
            findShipCells(row, col + 1, board, visited, shipCells); // вправо
        }
    }

    /**
     * Отметка клеток вокруг потопленного корабля как промахов
     */
    private void markAroundSunkShip(int row, int col, Character[][] board) {
        Set<String> shipCells = new HashSet<>();
        findShipCells(row, col, board, new boolean[10][10], shipCells);

        // Для каждой клетки корабля отмечаем соседние клетки как промахи
        for (String cell : shipCells) {
            String[] coords = cell.split(",");
            int r = Integer.parseInt(coords[0]);
            int c = Integer.parseInt(coords[1]);

            // Проверяем все 8 направлений
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int newRow = r + dr;
                    int newCol = c + dc;

                    if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 10) {
                        if (board[newRow][newCol] == ' ') {
                            board[newRow][newCol] = 'M';
                        }
                    }
                }
            }
        }
    }

    /**
     * Проверка, все ли корабли потоплены
     */
    private boolean checkAllShipsSunk(Character[][] board) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (board[i][j] == 'S') {
                    return false; // Найден непотопленный корабль
                }
            }
        }
        return true;
    }

    /**
     * Подсчет оставшихся кораблей
     */
    private int countShipsLeft(Character[][] board) {
        boolean[][] visited = new boolean[10][10];
        int shipCount = 0;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if ((board[i][j] == 'S' || board[i][j] == 'H') && !visited[i][j]) {
                    // Нашли клетку корабля
                    Set<String> shipCells = new HashSet<>();
                    findShipCells(i, j, board, visited, shipCells);

                    // Проверяем, потоплен ли корабль
                    boolean sunk = true;
                    for (String cell : shipCells) {
                        String[] coords = cell.split(",");
                        int r = Integer.parseInt(coords[0]);
                        int c = Integer.parseInt(coords[1]);
                        if (board[r][c] != 'H') {
                            sunk = false;
                            break;
                        }
                    }

                    if (!sunk) {
                        shipCount++;
                    }
                }
            }
        }

        return shipCount;
    }

    /**
     * Завершение игры
     */
    @PostMapping("/game/{gameId}/surrender")
    public ResponseEntity<?> surrender(@PathVariable Long gameId, @RequestParam Long playerId) {
        try {
            Game game = aiGames.get(gameId);
            if (game == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Игра не найдена"));
            }

            game.setGameStatus(GameStatus.COMPLETED);
            game.setEndDate(LocalDateTime.now());
            game.setResult("COMPUTER_WINS"); // Игрок сдался

            return ResponseEntity.ok(Map.of(
                    "message", "Вы сдались. Игра завершена.",
                    "winner", "COMPUTER"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка: " + e.getMessage()));
        }
    }
}