package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.model.*;
import com.example.battleship_game_BACKEND.shooting.BaseShootingStrategy;
import com.example.battleship_game_BACKEND.shooting.DensityAnalysisStrategy;
import com.example.battleship_game_BACKEND.shooting.RandomFinishingStrategy;
import com.example.battleship_game_BACKEND.shooting.AdaptiveDensityStrategy;
import com.example.battleship_game_BACKEND.shooting.DiagonalProbabilityStrategy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class GameService {

    // Внутренние сетки 10x10
    private int[][] playerGrid = new int[10][10];
    private int[][] computerGrid = new int[10][10];

    // Оставшиеся палубы по ID корабля
    private Map<Integer, Integer> playerRemainingDecks = new HashMap<>();
    private Map<Integer, Integer> computerRemainingDecks = new HashMap<>();

    // Флаги состояния игры
    private boolean isPlayerTurn = true;
    private boolean isBattleOver = false;
    private Difficulty difficulty = Difficulty.EASY;
    private ShootingStrategy shootingStrategy;

    // Корабли игроков
    private List<ShipPlacement> playerShips = new ArrayList<>();
    private List<ShipPlacement> computerShips = new ArrayList<>();

    // Константы
    private static final int CELL_MISS = -99;

    public GameStatus startGame(StartGameRequest request) {
        // Инициализируем сетки
        initializeGrids();

        // Сохраняем корабли
        this.playerShips = request.getPlayerShips();
        this.computerShips = request.getComputerShips();
        this.difficulty = request.getDifficulty();

        // Устанавливаем стратегию в зависимости от сложности
        this.shootingStrategy = createStrategyForDifficulty(this.difficulty);

        // Заполняем сетки кораблями
        placeShipsOnGrids();

        // Сбрасываем флаги
        this.isPlayerTurn = true;
        this.isBattleOver = false;

        return getGameStatus();
    }

    private void initializeGrids() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                playerGrid[i][j] = 0;
                computerGrid[i][j] = 0;
            }
        }
        playerRemainingDecks.clear();
        computerRemainingDecks.clear();
    }

    private BaseShootingStrategy createStrategyForDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new RandomFinishingStrategy();
            case MEDIUM -> new DiagonalProbabilityStrategy();
            case HARD -> new DensityAnalysisStrategy();
            default -> new RandomFinishingStrategy();
        };
    }

    private void placeShipsOnGrids() {
        // Расставляем корабли игрока
        for (ShipPlacement ship : playerShips) {
            playerRemainingDecks.put(ship.getShipId(), ship.getLength());
            for (int i = 0; i < ship.getLength(); i++) {
                int r = ship.getStartRow() + (ship.isVertical() ? i : 0);
                int c = ship.getStartCol() + (ship.isVertical() ? 0 : i);
                if (r >= 0 && r < 10 && c >= 0 && c < 10) {
                    playerGrid[r][c] = ship.getShipId();
                }
            }
        }

        // Расставляем корабли компьютера
        for (ShipPlacement ship : computerShips) {
            computerRemainingDecks.put(ship.getShipId(), ship.getLength());
            for (int i = 0; i < ship.getLength(); i++) {
                int r = ship.getStartRow() + (ship.isVertical() ? i : 0);
                int c = ship.getStartCol() + (ship.isVertical() ? 0 : i);
                if (r >= 0 && r < 10 && c >= 0 && c < 10) {
                    computerGrid[r][c] = ship.getShipId();
                }
            }
        }
    }

    public ShotResult playerShot(int row, int col) {
        if (isBattleOver || !isPlayerTurn) {
            return new ShotResult(row, col, false, false, null, null);
        }

        int cellValue = computerGrid[row][col];
        if (cellValue > 0) {
            // Попадание в корабль
            return processHit(row, col, computerGrid, computerRemainingDecks, computerShips, false);
        } else {
            // Промах
            return processMiss(row, col, computerGrid, false);
        }
    }

    public ShotResult computerShot() {
        if (isBattleOver || isPlayerTurn) {
            return new ShotResult(0, 0, false, false, null, null);
        }

        // Получаем координаты выстрела от стратегии
        int[] shot = shootingStrategy.getNextShot();
        int row = shot[0];
        int col = shot[1];

        int cellValue = playerGrid[row][col];
        if (cellValue > 0) {
            // Попадание в корабль игрока
            return processHit(row, col, playerGrid, playerRemainingDecks, playerShips, true);
        } else {
            // Промах компьютера
            return processMiss(row, col, playerGrid, true);
        }
    }

    private ShotResult processHit(int row, int col, int[][] grid, Map<Integer, Integer> remainingDecks,
                                  List<ShipPlacement> ships, boolean forComputer) {
        int shipId = grid[row][col];
        grid[row][col] = -shipId; // Помечаем как пораженную палубу

        // Уменьшаем оставшиеся палубы
        remainingDecks.put(shipId, remainingDecks.get(shipId) - 1);

        boolean justSunk = (remainingDecks.get(shipId) == 0);

        if (justSunk) {
            // Найти потопленный корабль
            ShipPlacement sunkShip = ships.stream()
                    .filter(ship -> ship.getShipId() == shipId)
                    .findFirst()
                    .orElse(null);

            if (sunkShip != null) {
                // Вычисляем буферные клетки
                List<int[]> buffer = computeBufferCells(sunkShip);

                // Помечаем буфер как промахи
                for (int[] cell : buffer) {
                    int r = cell[0];
                    int c = cell[1];
                    if (r >= 0 && r < 10 && c >= 0 && c < 10 && grid[r][c] >= 0) {
                        grid[r][c] = CELL_MISS;
                    }
                }

                // Проверяем, закончена ли игра
                if (allShipsSunk(remainingDecks)) {
                    isBattleOver = true;
                }

                // Обновляем стратегию
                if (forComputer) {
                    shootingStrategy.setShotResult(true, true);
                }

                return new ShotResult(row, col, true, true, sunkShip, buffer);
            }
        } else {
            // Просто попадание
            if (forComputer) {
                shootingStrategy.setShotResult(true, false);
            }
            return new ShotResult(row, col, true, false, null, null);
        }

        return new ShotResult(row, col, true, justSunk, null, null);
    }

    private ShotResult processMiss(int row, int col, int[][] grid, boolean forComputer) {
        if (grid[row][col] == 0) {
            grid[row][col] = CELL_MISS;
        }

        if (forComputer) {
            shootingStrategy.setShotResult(false, false);
            isPlayerTurn = true; // Передаем ход игроку
        } else {
            isPlayerTurn = false; // Передаем ход компьютеру
        }

        return new ShotResult(row, col, false, false, null, null);
    }

    private List<int[]> computeBufferCells(ShipPlacement ship) {
        List<int[]> buffer = new ArrayList<>();
        for (int i = -1; i <= ship.getLength(); i++) {
            for (int j = -1; j <= 1; j++) {
                int r = ship.getStartRow() + (ship.isVertical() ? i : j);
                int c = ship.getStartCol() + (ship.isVertical() ? j : i);
                if (r >= 0 && r < 10 && c >= 0 && c < 10) {
                    boolean onShip = (i >= 0 && i < ship.getLength() && j == 0);
                    if (!onShip) {
                        buffer.add(new int[]{r, c});
                    }
                }
            }
        }
        return buffer;
    }

    private boolean allShipsSunk(Map<Integer, Integer> remainingDecks) {
        return remainingDecks.values().stream().allMatch(count -> count == 0);
    }

    public GameStatus getGameStatus() {
        return new GameStatus(isPlayerTurn, isBattleOver, difficulty);
    }

    public Map<String, Object> getBoard() {
        Map<String, Object> board = new HashMap<>();
        board.put("playerGrid", playerGrid);
        board.put("computerGrid", computerGrid);
        board.put("isPlayerTurn", isPlayerTurn);
        board.put("isBattleOver", isBattleOver);
        return board;
    }
}