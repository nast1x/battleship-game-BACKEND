package com.example.battleship_game_BACKEND.shooting;

import java.util.*;

/**
 * Стратегия «Сложный» с анализом плотности (Heatmap) и адаптивными коррекциями.
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
    private int consecutiveMisses = 0;

    private final Random random = new Random();
    private ShotCoordinate lastShot;

    public DensityAnalysisStrategy() {
        // Инициализация поля
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = CellState.EMPTY;
            }
        }
    }

    @Override
    protected ShotCoordinate computeNextShot() {
        // ——— 1) Hunt-режим (добивание) ———
        ShotCoordinate cell = getShotCoordinate();
        if (cell != null) return cell;

        // ——— 2) Heatmap-режим ———
        return computeHeatmapShot();
    }

    private ShotCoordinate getShotCoordinate() {
        if (!huntQueue.isEmpty()) {
            while (!huntQueue.isEmpty()) {
                ShotCoordinate cell = huntQueue.removeFirst();
                if (isValidCell(cell) &&
                        board[cell.y()][cell.x()] == CellState.EMPTY &&
                        hasTried(cell)) {
                    return cell;
                }
            }
            huntQueue.clear();
        }
        return null;
    }

    @Override
    public ShotCoordinate getNextShot() {
        ShotCoordinate nextShot = computeNextShot();
        lastShot = nextShot;
        return nextShot;
    }

    @Override
    public void setShotResult(boolean hit, boolean sunk) {
        if (lastShot == null) return;

        if (hit && sunk) {
            handleSunkShip();
        } else if (hit) {
            handleHit();
        } else {
            handleMiss();
        }
    }

    private void handleSunkShip() {
        if (!huntHits.contains(lastShot)) {
            huntHits.add(lastShot);
        }
        List<ShotCoordinate> chain = findSunkChain(huntHits, lastShot);
        if (chain == null) {
            chain = Collections.singletonList(lastShot);
        }
        int justSunkLen = chain.size();
        markBufferAround(chain);
        remainingShips.remove(Integer.valueOf(justSunkLen));
        resetHuntMode();
        consecutiveMisses = 0;
    }

    private void handleHit() {
        board[lastShot.y()][lastShot.x()] = CellState.HIT;
        huntHits.add(lastShot);
        enqueueBasedOnHits();
        consecutiveMisses = 0;
    }

    private void handleMiss() {
        board[lastShot.y()][lastShot.x()] = CellState.MISS;
        consecutiveMisses++;
    }

    private ShotCoordinate computeHeatmapShot() {
        int[][] counts = new int[SIZE][SIZE];

        for (int length : remainingShips) {
            boolean includeVertical = (length > 1);
            int shipWeight = length;

            // Горизонтальные варианты
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c <= SIZE - length; c++) {
                    boolean canPlace = true;
                    for (int k = 0; k < length; k++) {
                        if (board[r][c + k] != CellState.EMPTY) {
                            canPlace = false;
                            break;
                        }
                    }
                    if (canPlace) {
                        for (int k = 0; k < length; k++) {
                            counts[r][c + k] += shipWeight;
                        }
                    }
                }
            }

            if (includeVertical) {
                // Вертикальные варианты
                for (int c = 0; c < SIZE; c++) {
                    for (int r = 0; r <= SIZE - length; r++) {
                        boolean canPlace = true;
                        for (int k = 0; k < length; k++) {
                            if (board[r + k][c] != CellState.EMPTY) {
                                canPlace = false;
                                break;
                            }
                        }
                        if (canPlace) {
                            for (int k = 0; k < length; k++) {
                                counts[r + k][c] += shipWeight;
                            }
                        }
                    }
                }
            }
        }

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

    private void applyEdgeBonus(int[][] counts) {
        for (int i = 0; i < SIZE; i++) {
            if (board[0][i] == CellState.EMPTY) counts[0][i] += 10;
            if (board[SIZE - 1][i] == CellState.EMPTY) counts[SIZE - 1][i] += 10;
            if (board[i][0] == CellState.EMPTY) counts[i][0] += 10;
            if (board[i][SIZE - 1] == CellState.EMPTY) counts[i][SIZE - 1] += 10;
        }
    }

    private int findMaxCount(int[][] counts) {
        int maxCount = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == CellState.EMPTY && counts[r][c] > maxCount) {
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
                if (board[r][c] == CellState.EMPTY && counts[r][c] == maxCount) {
                    candidates.add(new ShotCoordinate(c, r)); // Исправлено здесь
                }
            }
        }
        return candidates;
    }

    private ShotCoordinate findFirstEmptyCell() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                ShotCoordinate coordinate = new ShotCoordinate(c, r); // Исправлено здесь
                if (board[r][c] == CellState.EMPTY && hasTried(coordinate)) {
                    return coordinate;
                }
            }
        }
        throw new IllegalStateException("No empty cell left");
    }

    private List<ShotCoordinate> findSunkChain(List<ShotCoordinate> hits, ShotCoordinate start) {
        int r = start.y();
        int c = start.x();

        // Горизонтальная цепочка
        int left = c;
        while (left > 0 && hits.contains(new ShotCoordinate(left - 1, r))) left--; // Исправлено здесь
        int right = c;
        while (right < SIZE - 1 && hits.contains(new ShotCoordinate(right + 1, r))) right++; // Исправлено здесь
        List<ShotCoordinate> horizontalChain = new ArrayList<>();
        for (int col = left; col <= right; col++) {
            horizontalChain.add(new ShotCoordinate(col, r)); // Исправлено здесь
        }

        // Вертикальная цепочка
        int up = r;
        while (up > 0 && hits.contains(new ShotCoordinate(c, up - 1))) up--; // Исправлено здесь
        int down = r;
        while (down < SIZE - 1 && hits.contains(new ShotCoordinate(c, down + 1))) down++; // Исправлено здесь
        List<ShotCoordinate> verticalChain = new ArrayList<>();
        for (int row = up; row <= down; row++) {
            verticalChain.add(new ShotCoordinate(c, row)); // Исправлено здесь
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

    // Методы для доступа к состоянию
    public int getConsecutiveMisses() {
        return consecutiveMisses;
    }

    public List<Integer> getRemainingShips() {
        return new ArrayList<>(remainingShips);
    }

    public CellState getCellState(int row, int col) {
        return board[row][col];
    }

    public boolean isInHuntMode() {
        return !huntQueue.isEmpty() || !huntHits.isEmpty();
    }
}