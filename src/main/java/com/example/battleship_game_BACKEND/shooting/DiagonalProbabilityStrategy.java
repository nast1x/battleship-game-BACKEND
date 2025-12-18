package com.example.battleship_game_BACKEND.shooting;

import com.example.battleship_game_BACKEND.service.ShotCoordinate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Стратегия «Диагональная с вероятностным расширенным поиском».
 * Приоритет ходов: hunt-режим > диагональная фаза > probability-режим.
 * Синхронизирована с BaseShootingStrategy.
 */
//@Component
public class DiagonalProbabilityStrategy extends BaseShootingStrategy {

    /** Состояние клетки на «виртуальном» поле. */
    private enum CellState { EMPTY, MISS, HIT, SUNK }

    /** Виртуальное поле для хранения статусов: EMPTY/MISS/HIT/SUNK. */
    private final CellState[][] board = new CellState[SIZE][SIZE];

    /** Длины тех кораблей, что ещё не потоплены. */
    private final List<Integer> remainingShips = new ArrayList<>(INITIAL_SHIPS);

    /** Счётчик подряд идущих промахов. */
    private int consecutiveMisses = 0;

    /** Флаг: мы ещё в диагональной фазе? */
    private boolean diagonalPhase = true;

    /** Порядок точек «главной» диагонали. */
    private final List<ShotCoordinate> mainShots;

    /** Порядок точек «побочной» диагонали. */
    private final List<ShotCoordinate> secondaryShots;

    private int mainIndex = 0;
    private int secondaryIndex = 0;
    private boolean useMain = true;

    private final Random random = new Random();

    public DiagonalProbabilityStrategy() {
        // Инициализация поля
        for (int i = 0; i < SIZE; i++) {
            Arrays.fill(board[i], CellState.EMPTY);
        }

        // Инициализация диагональных выстрелов
        List<Integer> diagonalOrder = Arrays.asList(0, 9, 1, 8, 2, 7, 3, 6, 4, 5);
        this.mainShots = new ArrayList<>();
        this.secondaryShots = new ArrayList<>();

        for (Integer i : diagonalOrder) {
            mainShots.add(new ShotCoordinate(i, i));
            secondaryShots.add(new ShotCoordinate(i, SIZE - 1 - i));
        }
    }

    @Override
    public ShotCoordinate getNextShot() {
        // Синхронизация: обновляем board на основе tried[][]
        syncBoardWithTried();

        // 1. Hunt-режим (добивание)
        ShotCoordinate huntShot = getShotFromHuntQueue(this::isCellAvailable);
        if (huntShot != null) {
            return huntShot;
        }

        // 2. Диагональная фаза
        int largest = remainingShips.stream().max(Integer::compareTo).orElse(1);
        int dynamicThreshold = calculateDynamicThreshold(largest);

        if (diagonalPhase && consecutiveMisses < dynamicThreshold) {
            ShotCoordinate diagonalShot = getDiagonalShot();
            if (diagonalShot != null) {
                return diagonalShot;
            }
        } else {
            diagonalPhase = false;
        }

        // 3. Probability Mode
        return computeProbabilityShot();
    }

    @Override
    public void recordShot(ShotCoordinate shot, boolean hit, boolean sunk) {
        // Отмечаем клетку как обстрелянную
        tried[shot.x()][shot.y()] = true;

        // Обновляем состояние доски
        if (sunk) {
            board[shot.y()][shot.x()] = CellState.SUNK;
            handleSunkShip(shot);
        } else if (hit) {
            board[shot.y()][shot.x()] = CellState.HIT;
            handleHit(shot);
        } else {
            board[shot.y()][shot.x()] = CellState.MISS;
            handleMiss(shot);
        }

        // Синхронизируем состояния
        syncBoardWithTried();
    }

    // ===============================================================================
    // Методы синхронизации
    // ===============================================================================

    /**
     * Синхронизирует состояние board с tried[][] из базового класса
     */
    private void syncBoardWithTried() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                // Если клетка обстреляна и не помечена в board, отмечаем как промах
                if (tried[col][row] && board[row][col] == CellState.EMPTY) {
                    board[row][col] = CellState.MISS;
                }
            }
        }
    }

    /**
     * Проверяет, доступна ли клетка для выстрела
     */
    private boolean isCellAvailable(ShotCoordinate cell) {
        return board[cell.y()][cell.x()] == CellState.EMPTY && !tried[cell.x()][cell.y()];
    }

    // ===============================================================================
    // Диагональная фаза
    // ===============================================================================

    private int calculateDynamicThreshold(int largestShip) {
        return switch (largestShip) {
            case 4 -> 14;
            case 3 -> 12;
            case 2 -> 8;
            default -> 6;
        };
    }

    private ShotCoordinate getDiagonalShot() {
        if (useMain) {
            while (mainIndex < mainShots.size()) {
                ShotCoordinate cell = mainShots.get(mainIndex++);
                if (isCellAvailable(cell)) {
                    return cell;
                }
            }
            useMain = false;
            return getDiagonalShot();
        } else {
            while (secondaryIndex < secondaryShots.size()) {
                ShotCoordinate cell = secondaryShots.get(secondaryIndex++);
                if (isCellAvailable(cell)) {
                    return cell;
                }
            }
            diagonalPhase = false;
            return null;
        }
    }

    // ===============================================================================
    // Вероятностный режим
    // ===============================================================================

    private ShotCoordinate computeProbabilityShot() {
        int[][] counts = buildProbabilityHeatmap();
        return findBestShotFromHeatmap(counts);
    }

    private int[][] buildProbabilityHeatmap() {
        int[][] counts = new int[SIZE][SIZE];

        for (int length : remainingShips) {
            boolean includeVertical = (length > 1);

            // Горизонтальные размещения
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c <= SIZE - length; c++) {
                    if (canPlaceShipHorizontally(r, c, length)) {
                        for (int k = 0; k < length; k++) {
                            counts[r][c + k] += length;
                        }
                    }
                }
            }

            // Вертикальные размещения
            if (includeVertical) {
                for (int c = 0; c < SIZE; c++) {
                    for (int r = 0; r <= SIZE - length; r++) {
                        if (canPlaceShipVertically(r, c, length)) {
                            for (int k = 0; k < length; k++) {
                                counts[r + k][c] += length;
                            }
                        }
                    }
                }
            }
        }

        return counts;
    }

    private boolean canPlaceShipHorizontally(int row, int startCol, int length) {
        for (int k = 0; k < length; k++) {
            if (board[row][startCol + k] != CellState.EMPTY) {
                return false;
            }
        }
        return true;
    }

    private boolean canPlaceShipVertically(int startRow, int col, int length) {
        for (int k = 0; k < length; k++) {
            if (board[startRow + k][col] != CellState.EMPTY) {
                return false;
            }
        }
        return true;
    }

    private ShotCoordinate findBestShotFromHeatmap(int[][] counts) {
        int maxCount = findMaxCount(counts);
        List<ShotCoordinate> candidates = collectCandidates(counts, maxCount);

        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        return findFirstEmptyCell();
    }

    private int findMaxCount(int[][] counts) {
        int maxCount = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                ShotCoordinate cell = new ShotCoordinate(c, r);
                if (isCellAvailable(cell) && counts[r][c] > maxCount) {
                    maxCount = counts[r][c];
                }
            }
        }
        return maxCount;
    }

    private List<ShotCoordinate> collectCandidates(int[][] counts, int maxCount) {
        List<ShotCoordinate> candidates = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                ShotCoordinate cell = new ShotCoordinate(c, r);
                if (isCellAvailable(cell) && counts[r][c] == maxCount) {
                    candidates.add(cell);
                }
            }
        }
        return candidates;
    }

    private ShotCoordinate findFirstEmptyCell() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                ShotCoordinate coordinate = new ShotCoordinate(c, r);
                if (isCellAvailable(coordinate)) {
                    return coordinate;
                }
            }
        }
        // Fallback
        return findAnyUntriedCell();
    }

    // ===============================================================================
    // Обработка результатов выстрела
    // ===============================================================================

    private void handleSunkShip(ShotCoordinate shot) {
        if (!huntHits.contains(shot)) {
            huntHits.add(shot);
        }

        List<ShotCoordinate> chain = findSunkChain(huntHits, shot);
        if (chain == null) {
            chain = Collections.singletonList(shot);
        }

        int justSunkLen = chain.size();
        markBufferAround(chain);
        remainingShips.remove(Integer.valueOf(justSunkLen));

        resetHuntMode();
        consecutiveMisses = 0;
    }

    private void handleHit(ShotCoordinate shot) {
        huntHits.add(shot);
        enqueueBasedOnHits();
        consecutiveMisses = 0;
    }

    private void handleMiss(ShotCoordinate shot) {
        consecutiveMisses++;
    }

    // ===============================================================================
    // Вспомогательные методы для работы с цепочками кораблей
    // ===============================================================================

    private List<ShotCoordinate> findSunkChain(List<ShotCoordinate> hits, ShotCoordinate start) {
        int r = start.y();
        int c = start.x();

        // Горизонтальная цепочка
        List<ShotCoordinate> horizontalChain = buildHorizontalChain(hits, r, c);

        // Вертикальная цепочка
        List<ShotCoordinate> verticalChain = buildVerticalChain(hits, r, c);

        return horizontalChain.size() >= verticalChain.size() ? horizontalChain : verticalChain;
    }

    private List<ShotCoordinate> buildHorizontalChain(List<ShotCoordinate> hits, int row, int col) {
        int left = col;
        while (left > 0 && hits.contains(new ShotCoordinate(left - 1, row))) left--;

        int right = col;
        while (right < SIZE - 1 && hits.contains(new ShotCoordinate(right + 1, row))) right++;

        List<ShotCoordinate> chain = new ArrayList<>();
        for (int c = left; c <= right; c++) {
            chain.add(new ShotCoordinate(c, row));
        }
        return chain;
    }

    private List<ShotCoordinate> buildVerticalChain(List<ShotCoordinate> hits, int row, int col) {
        int up = row;
        while (up > 0 && hits.contains(new ShotCoordinate(col, up - 1))) up--;

        int down = row;
        while (down < SIZE - 1 && hits.contains(new ShotCoordinate(col, down + 1))) down++;

        List<ShotCoordinate> chain = new ArrayList<>();
        for (int r = up; r <= down; r++) {
            chain.add(new ShotCoordinate(col, r));
        }
        return chain;
    }

    private void markBufferAround(List<ShotCoordinate> hits) {
        if (hits.isEmpty()) return;

        // Помечаем hits как SUNK
        for (ShotCoordinate hit : hits) {
            board[hit.y()][hit.x()] = CellState.SUNK;
        }

        // Строим буфер MISS вокруг каждой точки
        for (ShotCoordinate hit : hits) {
            markMissAroundCell(hit.y(), hit.x());
        }
    }

    private void markMissAroundCell(int row, int col) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE &&
                        board[nr][nc] == CellState.EMPTY) {
                    board[nr][nc] = CellState.MISS;
                }
            }
        }
    }

    // ===============================================================================
    // Геттеры и методы для тестирования
    // ===============================================================================

    public List<Integer> getRemainingShips() {
        return new ArrayList<>(remainingShips);
    }

    public CellState getCellState(int row, int col) {
        return board[row][col];
    }

    public int getConsecutiveMisses() {
        return consecutiveMisses;
    }

    public boolean isDiagonalPhase() {
        return diagonalPhase;
    }

    /**
     * Проверяет синхронизацию между board и tried[][]
     */
    public void validateSynchronization() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                boolean isTried = tried[col][row];
                CellState state = board[row][col];

                // Проверка согласованности
                if (isTried && state == CellState.EMPTY) {
                    throw new IllegalStateException("Synchronization error: cell (" + col + ", " + row +
                            ") is tried but marked as EMPTY in board");
                }
            }
        }
    }
}