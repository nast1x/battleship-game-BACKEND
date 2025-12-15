package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.*;
import com.example.battleship_game_BACKEND.model.*;
import com.example.battleship_game_BACKEND.placement.*;
import com.example.battleship_game_BACKEND.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComputerGameService {

    private static final int BOARD_SIZE = 10;
    private static final char SHIP = 'S';
    private static final char HIT = 'H';
    private static final char MISS = 'M';
    private static final char EMPTY = ' ';
    private static final Long COMPUTER_PLAYER_ID = 0L; // ID фиктивного игрока

    private final GameRepository gameRepository;
    private final GameBoardRepository gameBoardRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Класс для хранения состояния стрельбы компьютера
    private static class ComputerShotState {
        boolean[][] shotGrid = new boolean[BOARD_SIZE][BOARD_SIZE];
        Random random = new Random();

        int[] getNextShot() {
            // Сначала проверяем, есть ли доступные клетки
            List<int[]> available = new ArrayList<>();
            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    if (!shotGrid[row][col]) {
                        available.add(new int[]{row, col});
                    }
                }
            }

            if (available.isEmpty()) {
                return new int[]{0, 0}; // fallback
            }

            // Выбираем случайную доступную клетку
            int[] cell = available.get(random.nextInt(available.size()));
            shotGrid[cell[0]][cell[1]] = true;
            return cell;
        }
    }


    private final Map<Long, ComputerShotState> computerShotStates = new HashMap<>();

    /**
     * Создать новую игру с компьютером
     */
    @Transactional
    public Game createComputerGame(Long playerId, ComputerGameStartRequest request) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));

        Player computer = playerRepository.findById(COMPUTER_PLAYER_ID)
                .orElseThrow(() -> new RuntimeException("Computer player not found"));

        // Создаем доску игрока
        GameBoard playerBoard = createEmptyGameBoard();
        playerBoard = gameBoardRepository.save(playerBoard);

        // Создаем доску компьютера
        GameBoard computerBoard = createEmptyGameBoard();
        computerBoard = gameBoardRepository.save(computerBoard);

        // Создаем игру
        Game game = new Game();
        game.setPlayer1(player);
        game.setPlayer2(computer);
        game.setGameBoard1(playerBoard);
        game.setGameBoard2(computerBoard);
        game.setGameType(GameType.SINGLEPLAYER);
        game.setGameStatus(GameStatus.WAITING);
        game.setStartDate(LocalDateTime.now());

        return gameRepository.save(game);
    }

    /**
     * Настроить игру (расставить корабли)
     */
    @Transactional
    public Game setupGame(Long gameId, ComputerGameStartRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        if (game.getGameType() != GameType.SINGLEPLAYER) {
            throw new RuntimeException("Game is not singleplayer");
        }

        // Сохраняем корабли игрока
        List<ShipPlacementDto> playerShips = request.getPlayerShips();
        validatePlayerShips(playerShips);

        GameBoard playerBoard = game.getGameBoard1();
        initializeBoardWithShips(playerBoard, playerShips);
        gameBoardRepository.save(playerBoard);

        // Генерируем и сохраняем корабли компьютера
        List<ShipPlacementDto> computerShips = generateComputerShips(request.getPlacementStrategy());
        GameBoard computerBoard = game.getGameBoard2();
        initializeBoardWithShips(computerBoard, computerShips);
        gameBoardRepository.save(computerBoard);

        // Начинаем игру
        game.setGameStatus(GameStatus.ACTIVE);
        gameRepository.save(game);

        log.info("Computer game {} setup. Player ships: {}, Computer ships: {}",
                gameId, playerShips.size(), computerShips.size());

        return game;
    }

    /**
     * Сгенерировать корабли компьютера (прямое создание стратегии)
     */
    private List<ShipPlacementDto> generateComputerShips(String strategyName) {
        Random random = new Random();
        BasePlacementStrategy strategy;

        switch (strategyName.toUpperCase()) {
            case "COASTS":
                strategy = new CoastsPlacer(null, random);
                break;
            case "DIAGONAL":
                strategy = new DiagonalPlacer(null, random);
                break;
            case "HALF":
                // Создаем базовую стратегию для HALF (упрощенно)
                strategy = new BasePlacementStrategy(null, random) {
                    @Override
                    protected List<Map.Entry<Integer, Integer>> scanCells() {
                        // Разделяем поле пополам и выбираем клетки из одной половины
                        List<Map.Entry<Integer, Integer>> allCells = generateAllCells();
                        List<Map.Entry<Integer, Integer>> halfCells = new ArrayList<>();
                        List<Map.Entry<Integer, Integer>> otherCells = new ArrayList<>();

                        for (Map.Entry<Integer, Integer> cell : allCells) {
                            if (cell.getKey() < BOARD_SIZE / 2) { // Верхняя половина
                                halfCells.add(cell);
                            } else {
                                otherCells.add(cell);
                            }
                        }

                        // Сначала клетки из половины, потом остальные
                        halfCells.addAll(otherCells);
                        return halfCells;
                    }
                };
                break;
            case "RANDOM":
            default:
                // Создаем базовую стратегию со случайным порядком клеток
                strategy = new BasePlacementStrategy(null, random) {
                    @Override
                    protected List<Map.Entry<Integer, Integer>> scanCells() {
                        return generateRandomCells();
                    }
                };
        }

        List<com.example.battleship_game_BACKEND.model.ShipPlacement> placements = strategy.generatePlacement();
        return placements.stream()
                .map(p -> new ShipPlacementDto(p.shipId(), p.size(), p.row(), p.col(), p.vertical()))
                .collect(Collectors.toList());
    }

    /**
     * Получить состояние игры
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGameState(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        GameBoard playerBoard = game.getGameBoard1();
        GameBoard computerBoard = game.getGameBoard2();

        Character[][] playerBoardMatrix = playerBoard.getPlacementMatrixAsArray();
        Character[][] computerBoardMatrix = computerBoard.getPlacementMatrixAsArray();

        // Получаем корабли игрока
        List<ShipPlacementDto> playerShips = getShipsFromBoard(playerBoard);
        List<ShipPlacementDto> computerShips = getShipsFromBoard(computerBoard);

        // Считаем статистику
        int playerHits = 0;
        int playerMisses = 0;
        int computerHits = 0;
        int computerMisses = 0;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (playerBoardMatrix[i][j] == HIT) {
                    computerHits++;
                } else if (playerBoardMatrix[i][j] == MISS) {
                    computerMisses++;
                }

                if (computerBoardMatrix[i][j] == HIT) {
                    playerHits++;
                } else if (computerBoardMatrix[i][j] == MISS) {
                    playerMisses++;
                }
            }
        }

        // Считаем оставшиеся корабли
        int playerShipsRemaining = playerShips != null ?
                countRemainingShips(playerShips, playerBoardMatrix) : 10;
        int computerShipsRemaining = computerShips != null ?
                countRemainingShips(computerShips, computerBoardMatrix) : 10;

        // Создаем DTO состояния
        Map<String, Object> state = new HashMap<>();
        state.put("gameId", gameId);
        state.put("status", game.getGameStatus().toString());
        state.put("playerTurn", isPlayerTurn(game)); // Нужно определить логику

        // Доски в упрощенном формате
        state.put("playerBoard", convertBoardToSimpleFormat(playerBoardMatrix, true));
        state.put("computerBoard", convertBoardToSimpleFormat(computerBoardMatrix, false));

        // Статистика
        state.put("playerHits", playerHits);
        state.put("playerMisses", playerMisses);
        state.put("computerHits", computerHits);
        state.put("computerMisses", computerMisses);
        state.put("playerShipsRemaining", playerShipsRemaining);
        state.put("computerShipsRemaining", computerShipsRemaining);

        return state;
    }

    /**
     * Конвертировать доску в простой формат для отправки
     */
    private String[][] convertBoardToSimpleFormat(Character[][] board, boolean showShips) {
        String[][] simpleBoard = new String[BOARD_SIZE][BOARD_SIZE];

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                char cell = board[i][j];
                switch (cell) {
                    case SHIP:
                        simpleBoard[i][j] = showShips ? "SHIP" : "EMPTY";
                        break;
                    case HIT:
                        simpleBoard[i][j] = "HIT";
                        break;
                    case MISS:
                        simpleBoard[i][j] = "MISS";
                        break;
                    case EMPTY:
                    default:
                        simpleBoard[i][j] = "EMPTY";
                }
            }
        }

        return simpleBoard;
    }

    /**
     * Подсчитать оставшиеся корабли
     */
    private int countRemainingShips(List<ShipPlacementDto> ships, Character[][] board) {
        int remaining = 0;

        for (ShipPlacementDto ship : ships) {
            boolean sunk = true;

            for (int i = 0; i < ship.size(); i++) {
                int row = ship.row() + (ship.vertical() ? i : 0);
                int col = ship.col() + (ship.vertical() ? 0 : i);

                if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
                    if (board[row][col] != HIT) {
                        sunk = false;
                        break;
                    }
                }
            }

            if (!sunk) {
                remaining++;
            }
        }

        return remaining;
    }

    /**
     * Определить, чей сейчас ход
     */
    private boolean isPlayerTurn(Game game) {
        // Простая логика: если игра активна, то ход игрока
        // В реальной игре нужно хранить состояние последнего хода
        return game.getGameStatus() == GameStatus.ACTIVE;
    }

    /**
     * Обработать выстрел игрока
     */

    @Transactional
    public ShotResponse processPlayerShot(Long gameId, ShotRequest request) {
        if (request.getGameId() != null && !request.getGameId().equals(gameId)) {
            throw new RuntimeException("Несоответствие gameId в запросе и пути");
        }

        // Устанавливаем gameId
        request.setGameId(gameId);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        validateGameState(game);
        validateShotCoordinates(request.getRow(), request.getCol());

        ShotResponse response = new ShotResponse();
        response.setGameOver(false);

        // Получаем доску компьютера
        GameBoard computerBoard = game.getGameBoard2();
        Character[][] computerBoardMatrix = computerBoard.getPlacementMatrixAsArray();

        // Получаем корабли компьютера
        List<ShipPlacementDto> computerShips = getShipsFromBoard(computerBoard);
        if (computerShips == null) {
            computerShips = new ArrayList<>();
        }

        // Проверяем, можно ли стрелять в эту клетку
        if (isCellAlreadyShot(computerBoardMatrix, request.getRow(), request.getCol())) {
            throw new RuntimeException("Уже стреляли в эту клетку");
        }

        // Обработка выстрела
        boolean hit = false;
        ShipPlacementDto sunkShip = null;

        // Проверяем попадание по кораблям
        for (ShipPlacementDto ship : computerShips) {
            if (isShipHit(ship, request.getRow(), request.getCol())) {
                hit = true;
                computerBoardMatrix[request.getRow()][request.getCol()] = HIT;

                // Проверяем, потоплен ли корабль
                if (isShipSunk(ship, computerBoardMatrix)) {
                    sunkShip = ship;
                }
                break;
            }
        }

        if (!hit) {
            computerBoardMatrix[request.getRow()][request.getCol()] = MISS;
        }

        // Обновляем доску
        computerBoard.setPlacementMatrixFromArray(computerBoardMatrix);
        gameBoardRepository.save(computerBoard);

        // Заполняем ответ
        if (hit) {
            response.setHit(true);
            response.setMessage("Попадание!");

            if (sunkShip != null) {
                response.setSunk(true);
                response.setSunkShipId(sunkShip.shipId());
                response.setMessage("Корабль потоплен!");

                // Помечаем клетки вокруг потопленного корабля
                markAroundSunkShip(computerBoardMatrix, sunkShip);
            }
        } else {
            response.setHit(false);
            response.setMessage("Промах!");
        }

        // Проверяем, выиграл ли игрок
        if (areAllShipsSunk(computerShips, computerBoardMatrix)) {
            game.setGameStatus(GameStatus.COMPLETED);
            game.setResult("PLAYER_WON");
            game.setEndDate(LocalDateTime.now());
            response.setGameOver(true);
            response.setMessage("Вы выиграли! Все корабли компьютера потоплены!");
            updateResponseStats(response, game);
            gameRepository.save(game);
            return response;
        }

        // Если игрок промахнулся - ход компьютера
        if (!hit) {
            ShotResponse computerTurn = processComputerTurn(game);

            response.setComputerRow(computerTurn.getComputerRow());
            response.setComputerCol(computerTurn.getComputerCol());
            response.setComputerHit(computerTurn.isHit());
            response.setComputerSunk(computerTurn.isSunk());
            response.setComputerSunkShipId(computerTurn.getSunkShipId());

            if (computerTurn.isGameOver()) {
                game.setGameStatus(GameStatus.COMPLETED);
                game.setResult("COMPUTER_WON");
                game.setEndDate(LocalDateTime.now());
                response.setGameOver(true);
                response.setMessage("Вы проиграли! Все ваши корабли потоплены!");
            }
        }

        updateResponseStats(response, game);
        gameRepository.save(game);

        return response;
    }

    /**
     * Ход компьютера
     */
    private ShotResponse processComputerTurn(Game game) {
        ShotResponse response = new ShotResponse();

        // Получаем доску игрока
        GameBoard playerBoard = game.getGameBoard1();
        Character[][] playerBoardMatrix = playerBoard.getPlacementMatrixAsArray();

        // Получаем корабли игрока
        List<ShipPlacementDto> playerShips = getShipsFromBoard(playerBoard);
        if (playerShips == null) {
            playerShips = new ArrayList<>();
        }

        // Получаем следующий выстрел компьютера
        ComputerShotState shotState = computerShotStates.computeIfAbsent(
                game.getGameId(), k -> new ComputerShotState()
        );

        int[] shot = shotState.getNextShot();
        int row = shot[0];
        int col = shot[1];

        // Проверяем, можно ли стрелять в эту клетку
        int attempts = 0;
        while (isCellAlreadyShot(playerBoardMatrix, row, col) && attempts < 100) {
            shot = shotState.getNextShot();
            row = shot[0];
            col = shot[1];
            attempts++;
        }

        if (attempts >= 100) {
            // Находим первую доступную клетку
            for (row = 0; row < BOARD_SIZE; row++) {
                for (col = 0; col < BOARD_SIZE; col++) {
                    if (!isCellAlreadyShot(playerBoardMatrix, row, col)) {
                        shot = new int[]{row, col};
                        break;
                    }
                }
            }
        }

        // Обработка выстрела компьютера
        boolean hit = false;
        ShipPlacementDto sunkShip = null;

        // Проверяем попадание по кораблям игрока
        for (ShipPlacementDto ship : playerShips) {
            if (isShipHit(ship, row, col)) {
                hit = true;
                playerBoardMatrix[row][col] = HIT;

                // Проверяем, потоплен ли корабль
                if (isShipSunk(ship, playerBoardMatrix)) {
                    sunkShip = ship;
                }
                break;
            }
        }

        if (!hit) {
            playerBoardMatrix[row][col] = MISS;
        }

        // Обновляем доску
        playerBoard.setPlacementMatrixFromArray(playerBoardMatrix);
        gameBoardRepository.save(playerBoard);

        // Заполняем ответ
        response.setComputerRow(row);
        response.setComputerCol(col);
        response.setComputerHit(hit);

        if (hit && sunkShip != null) {
                response.setComputerSunk(true);
                response.setComputerSunkShipId(sunkShip.shipId());

                // Помечаем клетки вокруг потопленного корабля
                markAroundSunkShip(playerBoardMatrix, sunkShip);
            }


        // Проверяем, выиграл ли компьютер
        if (areAllShipsSunk(playerShips, playerBoardMatrix)) {
            response.setGameOver(true);
        }

        return response;
    }

    /**
     * Создать пустую доску
     */
    private GameBoard createEmptyGameBoard() {
        GameBoard board = new GameBoard();
        board.setPlacementMatrixFromArray(createEmptyMatrix());
        return board;
    }

    /**
     * Инициализировать доску кораблями
     */
    private void initializeBoardWithShips(GameBoard board, List<ShipPlacementDto> ships) {
        Character[][] matrix = createEmptyMatrix();

        // Размещаем корабли на доске
        for (ShipPlacementDto ship : ships) {
            for (int i = 0; i < ship.size(); i++) {
                int row = ship.row() + (ship.vertical() ? i : 0);
                int col = ship.col() + (ship.vertical() ? 0 : i);
                if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE) {
                    matrix[row][col] = SHIP;
                }
            }
        }

        board.setPlacementMatrixFromArray(matrix);

        // Сохраняем корабли в JSON
        try {
            board.setGameStateJson(objectMapper.writeValueAsString(
                    Map.of("ships", ships)
            ));
        } catch (Exception e) {
            log.error("Failed to serialize ships", e);
        }
    }

    /**
     * Валидация расстановки игрока
     */
    private void validatePlayerShips(List<ShipPlacementDto> ships) {
        if (ships.size() != 10) {
            throw new RuntimeException("Должно быть ровно 10 кораблей");
        }

        Map<Integer, Integer> shipCountBySize = new HashMap<>();
        for (ShipPlacementDto ship : ships) {
            shipCountBySize.put(ship.size(),
                    shipCountBySize.getOrDefault(ship.size(), 0) + 1);
        }

        if (!shipCountBySize.equals(Map.of(
                4, 1,
                3, 2,
                2, 3,
                1, 4
        ))) {
            throw new RuntimeException("Неверное распределение кораблей по размерам");
        }
    }

    // Вспомогательные методы
    private Character[][] createEmptyMatrix() {
        Character[][] matrix = new Character[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                matrix[i][j] = EMPTY;
            }
        }
        return matrix;
    }

    private boolean isCellAlreadyShot(Character[][] board, int row, int col) {
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return true;
        }
        char cell = board[row][col];
        return cell == HIT || cell == MISS;
    }

    private boolean areAllShipsSunk(List<ShipPlacementDto> ships, Character[][] board) {
        for (ShipPlacementDto ship : ships) {
            boolean shipSunk = true;
            for (int i = 0; i < ship.size(); i++) {
                int row = ship.row() + (ship.vertical() ? i : 0);
                int col = ship.col() + (ship.vertical() ? 0 : i);
                if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE && board[row][col] != HIT) {
                        shipSunk = false;
                        break;
                    }

            }
            if (!shipSunk) {
                return false;
            }
        }
        return true;
    }

    private boolean isShipHit(ShipPlacementDto ship, int row, int col) {
        for (int i = 0; i < ship.size(); i++) {
            int shipRow = ship.row() + (ship.vertical() ? i : 0);
            int shipCol = ship.col() + (ship.vertical() ? 0 : i);
            if (shipRow == row && shipCol == col) {
                return true;
            }
        }
        return false;
    }

    private boolean isShipSunk(ShipPlacementDto ship, Character[][] board) {
        for (int i = 0; i < ship.size(); i++) {
            int row = ship.row() + (ship.vertical() ? i : 0);
            int col = ship.col() + (ship.vertical() ? 0 : i);
            if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE && board[row][col] != HIT) {
                    return false;
                }

        }
        return true;
    }

    private void markAroundSunkShip(Character[][] board, ShipPlacementDto ship) {
        for (int i = 0; i < ship.size(); i++) {
            int row = ship.row() + (ship.vertical() ? i : 0);
            int col = ship.col() + (ship.vertical() ? 0 : i);

            for (int r = Math.max(0, row - 1); r <= Math.min(BOARD_SIZE - 1, row + 1); r++) {
                for (int c = Math.max(0, col - 1); c <= Math.min(BOARD_SIZE - 1, col + 1); c++) {
                    if (board[r][c] == EMPTY) {
                        board[r][c] = MISS;
                    }
                }
            }
        }
    }

    private void validateGameState(Game game) {
        if (game.getGameStatus() != GameStatus.ACTIVE) {
            throw new RuntimeException("Игра не активна");
        }
    }

    private void validateShotCoordinates(Integer row, Integer col) {
        if (row == null || col == null) {
            throw new RuntimeException("Координаты не могут быть null");
        }

        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            throw new RuntimeException("Координаты вне поля");
        }
    }

    private void updateResponseStats(ShotResponse response, Game game) {
        // TODO: Добавить подсчет статистики при необходимости
    }

    private List<ShipPlacementDto> getShipsFromBoard(GameBoard board) {
        try {
            if (board.getGameStateJson() != null && !board.getGameStateJson().isEmpty()) {
                Map<String, Object> state = objectMapper.readValue(
                        board.getGameStateJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        }
                );

                if (state.containsKey("ships")) {
                    return objectMapper.convertValue(
                            state.get("ships"),
                            new com.fasterxml.jackson.core.type.TypeReference<>() {
                            }
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to get ships from board", e);
        }
        return Collections.emptyList();
    }

    /**
     * Сдаться
     */
    @Transactional
    public void surrender(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        game.setGameStatus(GameStatus.COMPLETED);
        game.setResult("SURRENDER");
        game.setEndDate(LocalDateTime.now());
        gameRepository.save(game);

        log.info("Player surrendered in game {}", gameId);
    }
}