package com.example.battleship_game_BACKEND.shooting;

import lombok.Getter;

import java.util.*;

/**
 * Стратегия «Сложный» с анализом плотности (Heatmap) и адаптивными коррекциями.
 * Синхронизирована с BaseShootingStrategy.
 */
public class DensityAnalysisStrategy extends BaseShootingStrategy {

    private static final int EDGE_BONUS_THRESHOLD = 8;

    /** Состояние клетки на «виртуальном» поле. */
    private enum CellState { EMPTY, MISS, HIT, SUNK }

    /** Виртуальное поле (SIZE×SIZE) для учёта MISS/HIT/SUNK/EMPTY. */
    private final CellState[][] board = new CellState[SIZE][SIZE];

    /** Длины тех кораблей игрока, что ещё не потоплены. */
    private final List<Integer> remainingShips = new ArrayList<>(INITIAL_SHIPS);

    /** Счётчик подряд промахов. */
    @Getter
    private int consecutiveMisses = 0;

    private final Random random = new Random();

    public DensityAnalysisStrategy() {
        // Инициализация поля
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = CellState.EMPTY;
            }
        }
    }

    // ===============================================================================
    // Основная логика выстрела (СИНХРОНИЗИРОВАНА)
    // ===============================================================================

    @Override
    protected ShotCoordinate computeNextShot() {
        // Синхронизация: обновляем board на основе tried[][]
        syncBoardWithTried();

        // ——— 1) Hunt-режим (добивание) ———
        ShotCoordinate huntShot = getShotFromHuntQueue(this::isCellAvailable
        );
        if (huntShot != null) {
            return huntShot;
        }

        // ——— 2) Heatmap-режим ———
        return computeHeatmapShot();
    }

    @Override
    protected void onShotResult(ShotCoordinate lastShot, boolean hit, boolean sunk) {
        if (lastShot == null) return;

        // Синхронизация: сначала обновляем board
        updateBoardState(lastShot, hit, sunk);

        // Затем обрабатываем логику
        if (hit && sunk) {
            handleSunkShip(lastShot);
        } else if (hit) {
            handleHit(lastShot);
        } else {
            handleMiss(lastShot);
        }
    }

    // ===============================================================================
    // Методы синхронизации с базовым классом
    // ===============================================================================

    /**
     * Синхронизирует состояние board с tried[][] из базового класса
     */
    private void syncBoardWithTried() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (isCellTried(row, col) && board[row][col] == CellState.EMPTY) {
                    // Если клетка обстреляна в базовом классе, но у нас помечена как EMPTY,
                    // значит это промах (т.к. попадания обрабатываются в onShotResult)
                    board[row][col] = CellState.MISS;
                }
            }
        }
    }

    /**
     * Обновляет состояние board на основе результата выстрела
     */
    private void updateBoardState(ShotCoordinate shot, boolean hit, boolean sunk) {
        if (sunk) {
            board[shot.y()][shot.x()] = CellState.SUNK;
        } else if (hit) {
            board[shot.y()][shot.x()] = CellState.HIT;
        } else {
            board[shot.y()][shot.x()] = CellState.MISS;
        }
    }

    /**
     * Проверяет, доступна ли клетка для выстрела (синхронизированная проверка)
     */
    private boolean isCellAvailable(ShotCoordinate cell) {
        return board[cell.y()][cell.x()] == CellState.EMPTY && isCellUntried(cell);
    }

    // ===============================================================================
    // Обработка результатов выстрела (СИНХРОНИЗИРОВАНА)
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

        // Используем базовый метод для сброса hunt-режима
        resetHuntMode();
        consecutiveMisses = 0;
    }

    private void handleHit(ShotCoordinate shot) {
        huntHits.add(shot);
        // Используем базовый метод для построения очереди добивания
        enqueueBasedOnHits();
        consecutiveMisses = 0;
    }

    private void handleMiss(ShotCoordinate shot) {
        consecutiveMisses++;
    }

    // ===============================================================================
    // Heatmap логика (СИНХРОНИЗИРОВАНА)
    // ===============================================================================

    private ShotCoordinate computeHeatmapShot() {
        int[][] counts = buildProbabilityHeatmap();

        if (consecutiveMisses >= EDGE_BONUS_THRESHOLD) {
            applyEdgeBonus(counts);
        }

        int maxCount = findMaxCount(counts);
        List<ShotCoordinate> candidates = collectCandidates(counts, maxCount);

        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        return findFirstEmptyCell();
    }

    private int[][] buildProbabilityHeatmap() {
        int[][] counts = new int[SIZE][SIZE];

        for (int shipWeight : remainingShips) {
            boolean includeVertical = (shipWeight > 1);

            // Горизонтальные варианты
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c <= SIZE - shipWeight; c++) {
                    if (canPlaceShipHorizontally(r, c, shipWeight)) {
                        for (int k = 0; k < shipWeight; k++) {
                            counts[r][c + k] += shipWeight;
                        }
                    }
                }
            }

            if (includeVertical) {
                // Вертикальные варианты
                for (int c = 0; c < SIZE; c++) {
                    for (int r = 0; r <= SIZE - shipWeight; r++) {
                        if (canPlaceShipVertically(r, c, shipWeight)) {
                            for (int k = 0; k < shipWeight; k++) {
                                counts[r + k][c] += shipWeight;
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

    private void applyEdgeBonus(int[][] counts) {
        for (int i = 0; i < SIZE; i++) {
            if (isCellAvailable(ShotCoordinate.of(i, 0))) counts[0][i] += 10;
            if (isCellAvailable(ShotCoordinate.of(i, SIZE - 1))) counts[SIZE - 1][i] += 10;
            if (isCellAvailable(ShotCoordinate.of(0, i))) counts[i][0] += 10;
            if (isCellAvailable(ShotCoordinate.of(SIZE - 1, i))) counts[i][SIZE - 1] += 10;
        }
    }

    private int findMaxCount(int[][] counts) {
        int maxCount = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                ShotCoordinate cell = ShotCoordinate.of(c, r);
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
                ShotCoordinate cell = ShotCoordinate.of(c, r);
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
                ShotCoordinate coordinate = ShotCoordinate.of(c, r);
                if (isCellAvailable(coordinate)) {
                    return coordinate;
                }
            }
        }
        // Fallback к базовому классу
        return findAnyUntriedCell();
    }

    // ===============================================================================
    // Вспомогательные методы для работы с цепочками кораблей
    // ===============================================================================

    private List<ShotCoordinate> findSunkChain(List<ShotCoordinate> hits, ShotCoordinate start) {
        int r = start.y();
        int c = start.x();

        // Горизонтальная цепочка
        int left = c;
        while (left > 0 && hits.contains(ShotCoordinate.of(left - 1, r))) left--;
        int right = c;
        while (right < SIZE - 1 && hits.contains(ShotCoordinate.of(right + 1, r))) right++;
        List<ShotCoordinate> horizontalChain = new ArrayList<>();
        for (int col = left; col <= right; col++) {
            horizontalChain.add(ShotCoordinate.of(col, r));
        }

        // Вертикальная цепочка
        int up = r;
        while (up > 0 && hits.contains(ShotCoordinate.of(c, up - 1))) up--;
        int down = r;
        while (down < SIZE - 1 && hits.contains(ShotCoordinate.of(c, down + 1))) down++;
        List<ShotCoordinate> verticalChain = new ArrayList<>();
        for (int row = up; row <= down; row++) {
            verticalChain.add(ShotCoordinate.of(c, row));
        }

        return horizontalChain.size() >= verticalChain.size() ? horizontalChain : verticalChain;
    }

    private void markBufferAround(List<ShotCoordinate> hits) {
        if (hits.isEmpty()) return;

        for (ShotCoordinate hit : hits) {
            board[hit.y()][hit.x()] = CellState.SUNK;
        }

        for (ShotCoordinate hit : hits) {
            int r = hit.y();
            int c = hit.x();

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = r + dr;
                    int nc = c + dc;
                    if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE &&
                            board[nr][nc] == CellState.EMPTY) {
                        board[nr][nc] = CellState.MISS;
                    }
                }
            }
        }
    }

    // ===============================================================================
    // Методы для доступа к состоянию (для тестирования)
    // ===============================================================================

    public List<Integer> getRemainingShips() {
        return new ArrayList<>(remainingShips);
    }

    public CellState getCellState(int row, int col) {
        return board[row][col];
    }

    public boolean isInHuntMode() {
        return !huntQueue.isEmpty() || !huntHits.isEmpty();
    }

    /**
     * Проверяет синхронизацию между board и tried[][]
     */
    public void validateSynchronization() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                ShotCoordinate cell = ShotCoordinate.of(col, row);
                boolean isTried = isCellTried(cell);
                CellState state = board[row][col];

                // Проверка согласованности
                if (isTried && state == CellState.EMPTY) {
                    throw new IllegalStateException("Synchronization error: cell " + cell +
                            " is tried but marked as EMPTY in board");
                }
            }
        }
    }
}