package com.example.battleship_game_BACKEND.shooting;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Supplier;

/**
 * Стратегия «Экспертный» с анализом плотности (Heatmap) и адаптивными коррекциями.
 * Синхронизирована с BaseShootingStrategy.
 */
public class AdaptiveDensityStrategy extends BaseShootingStrategy {

    private static final int EDGE_BONUS_THRESHOLD = 5;
    private static final int MISS_BONUS_THRESHOLD = 8;

    /** Состояние клетки на «виртуальном» поле. */
    private enum CellState { EMPTY, MISS, HIT, SUNK }

    /** Виртуальное поле (SIZE×SIZE) для учёта MISS/HIT/SUNK/EMPTY. */
    private final CellState[][] board = new CellState[SIZE][SIZE];

    /** Длины тех кораблей игрока, что ещё не потоплены. */
    private final List<Integer> remainingShips = new ArrayList<>(INITIAL_SHIPS);

    /** Счётчик подряд промахов. */
    @Getter
    private int consecutiveMisses = 0;

    /** Провайдер, который вернёт все живые палубы противника. */
    @Setter
    private Supplier<List<ShotCoordinate>> enemyShipProvider;

    // ——————————————————————————————————————————————————————————
    // Для распознавания «стратегии Берега» и «Половины поля»
    // ——————————————————————————————————————————————————————————

    /** Сколько кораблей всего потоплено. */
    @Getter
    private int totalSunkShips = 0;

    /** Сколько из потопленных лежали полностью на границе. */
    private int borderSunkShips = 0;

    /** Сколько потопленных кораблей лежало строго в левой половине (x ≤ 4). */
    private int sunkLeftCount = 0;

    /** Сколько потопленных кораблей лежало строго в правой половине (x ≥ 5). */
    private int sunkRightCount = 0;

    /** Флаг: считаем, что противник играет «по краю». */
    @Getter
    private boolean assumeEdgeStrategy = false;

    /**
     * Флаг: если true → бьем только в левой половине,
     * если false → бьем только в правой половине,
     * если null → ещё не определили никакую «половину».
     */
    @Getter
    private Boolean assumeHalfFieldLeft = null;

    private final Random random = new Random();

    public AdaptiveDensityStrategy() {
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

        // ——— 2) Эвристическое угадывание (после ≥8 подряд промахов) ———
        if (consecutiveMisses >= MISS_BONUS_THRESHOLD && enemyShipProvider != null) {
            ShotCoordinate guessShot = getHeuristicGuessShot();
            if (guessShot != null) {
                return guessShot;
            }
        }

        // ——— 3) Анализ стратегии противника: игра по краю ———
        if (assumeEdgeStrategy) {
            // Если подряд промахов стали ≥ EDGE_BONUS_THRESHOLD — выходим из edge-режима
            if (consecutiveMisses >= EDGE_BONUS_THRESHOLD) {
                assumeEdgeStrategy = false;
            } else {
                ShotCoordinate edgeShot = getEdgeStrategyShot();
                if (edgeShot != null) {
                    return edgeShot;
                }
                // Если граничные клетки закончились, снимаем флаг
                assumeEdgeStrategy = false;
            }
        }

        // ——— 4) Анализ стратегии противника: игра в половине поля ———
        if (assumeHalfFieldLeft != null) {
            // Если подряд промахов ≥ EDGE_BONUS_THRESHOLD — сбрасываем режим половины
            if (consecutiveMisses >= EDGE_BONUS_THRESHOLD) {
                assumeHalfFieldLeft = null;
            } else {
                ShotCoordinate halfFieldShot = getHalfFieldStrategyShot();
                if (halfFieldShot != null) {
                    return halfFieldShot;
                }
                // Если в выбранной половине пустых клеток нет, сбрасываем флаг
                assumeHalfFieldLeft = null;
            }
        }

        // ——— 5) Heatmap-режим ———
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
    // Эвристическое угадывание (СИНХРОНИЗИРОВАНО)
    // ===============================================================================

    private ShotCoordinate getHeuristicGuessShot() {
        List<ShotCoordinate> allDecks = enemyShipProvider.get();
        // фильтруем те, по которым уже не стреляли:
        List<ShotCoordinate> candidates = allDecks.stream()
                .filter(this::isCellAvailable)
                .toList();

        if (!candidates.isEmpty()) {
            // берём случайную палубу из оставшихся живых палуб
            ShotCoordinate chosen = candidates.get(random.nextInt(candidates.size()));
            // после "точного хода" сбросим счётчик промахов
            consecutiveMisses = 0;
            return chosen;
        }
        return null;
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

        // Анализ стратегии противника
        analyzeOpponentStrategy(chain);

        // Помечаем цепочку как SUNK и строим буфер MISS вокруг неё
        markBufferAround(chain);
        // Удаляем длину потопленного корабля из remainingShips
        remainingShips.remove(Integer.valueOf(justSunkLen));
        // Сбрасываем Hunt-режим через базовый класс
        resetHuntMode();
        // Сбрасываем consecutiveMisses
        consecutiveMisses = 0;
    }

    private void analyzeOpponentStrategy(List<ShotCoordinate> chain) {
        totalSunkShips++;

        // Проверяем, весь ли корабль лежал на границе
        boolean allOnBorder = chain.stream().allMatch(cell ->
                cell.y() == 0 || cell.y() == SIZE - 1 ||
                        cell.x() == 0 || cell.x() == SIZE - 1
        );
        if (allOnBorder) {
            borderSunkShips++;
        }

        // Проверяем, лежит ли корабль строго в левой половине
        if (chain.stream().allMatch(cell -> cell.x() <= 4)) {
            sunkLeftCount++;
        }

        // Проверяем, лежит ли корабль строго в правой половине
        if (chain.stream().allMatch(cell -> cell.x() >= 5)) {
            sunkRightCount++;
        }

        // Решаем: считать ли, что противник играет «по краю»
        if (!assumeEdgeStrategy && totalSunkShips >= 2) {
            if ((double) borderSunkShips / totalSunkShips >= 0.8) {
                assumeEdgeStrategy = true;
            }
        }

        // Решаем: считать ли, что противник играет «одна половина»
        if (assumeHalfFieldLeft == null && totalSunkShips >= 2) {
            if (sunkLeftCount == totalSunkShips) {
                assumeHalfFieldLeft = true;
            } else if (sunkRightCount == totalSunkShips) {
                assumeHalfFieldLeft = false;
            }
        }
    }

    private void handleHit(ShotCoordinate shot) {
        huntHits.add(shot);
        // Используем базовый метод для построения очереди добивания
        enqueueBasedOnHits();
        // Сбрасываем счётчик промахов
        consecutiveMisses = 0;
    }

    private void handleMiss(ShotCoordinate shot) {
        consecutiveMisses++;
    }

    // ===============================================================================
    // Методы для анализа стратегии противника (СИНХРОНИЗИРОВАНЫ)
    // ===============================================================================

    private ShotCoordinate getEdgeStrategyShot() {
        List<ShotCoordinate> edgeCells = new ArrayList<>();

        // Собираем все граничные клетки
        for (int i = 0; i < SIZE; i++) {
            // Верхняя и нижняя границы
            addIfValid(edgeCells, 0, i);
            addIfValid(edgeCells, SIZE - 1, i);
            // Левая и правая границы
            addIfValid(edgeCells, i, 0);
            addIfValid(edgeCells, i, SIZE - 1);
        }

        if (!edgeCells.isEmpty()) {
            return edgeCells.get(random.nextInt(edgeCells.size()));
        }
        return null;
    }

    private void addIfValid(List<ShotCoordinate> cells, int row, int col) {
        ShotCoordinate cell = ShotCoordinate.of(col, row);
        if (isCellAvailable(cell)) {
            cells.add(cell);
        }
    }

    private ShotCoordinate getHalfFieldStrategyShot() {
        List<ShotCoordinate> candidates = new ArrayList<>();
        int startCol = Boolean.TRUE.equals(assumeHalfFieldLeft) ? 0 : 5;
        int endCol = Boolean.TRUE.equals(assumeHalfFieldLeft) ? 4 : 9;

        for (int row = 0; row < SIZE; row++) {
            for (int col = startCol; col <= endCol; col++) {
                ShotCoordinate cell = ShotCoordinate.of(col, row);
                if (isCellAvailable(cell)) {
                    candidates.add(cell);
                }
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        return null;
    }

    // ===============================================================================
    // Heatmap-режим (СИНХРОНИЗИРОВАН)
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

        for (int length : remainingShips) {
            boolean includeVertical = (length > 1);
            int shipWeight = length;

            // Горизонтальные варианты
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c <= SIZE - length; c++) {
                    if (canPlaceShipHorizontally(r, c, length)) {
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
                        if (canPlaceShipVertically(r, c, length)) {
                            for (int k = 0; k < length; k++) {
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

        // Выбираем цепочку с большей длиной
        return horizontalChain.size() >= verticalChain.size() ? horizontalChain : verticalChain;
    }

    private void markBufferAround(List<ShotCoordinate> hits) {
        if (hits.isEmpty()) return;

        // Маркируем сами hits как SUNK
        for (ShotCoordinate hit : hits) {
            board[hit.y()][hit.x()] = CellState.SUNK;
        }

        // Вокруг каждой точки рисуем буфер MISS (8 соседей)
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
    // Переопределение hunt-логики для стратегии "по краю" (СИНХРОНИЗИРОВАНО)
    // ===============================================================================

    @Override
    protected void enqueueBasedOnHits() {
        // Если «игра по краю» и у нас есть ровно одна точка попадания → ориентация вдоль границы
        if (assumeEdgeStrategy && huntHits.size() == 1) {
            resetHuntQueue();
            ShotCoordinate firstHit = huntHits.get(0);
            int r = firstHit.y();
            int c = firstHit.x();

            // Если попали на верхней или нижней границе — корабль может лежать только горизонтально
            if (r == 0 || r == SIZE - 1) {
                addIfValidToQueue(r, c - 1); // left
                addIfValidToQueue(r, c + 1); // right
                return;
            }

            // Если попали на левой или правой границе — корабль может лежать только вертикально
            if (c == 0 || c == SIZE - 1) {
                addIfValidToQueue(r - 1, c); // up
                addIfValidToQueue(r + 1, c); // down
                return;
            }
        }

        // Иначе — дефолтная логика из BaseShootingStrategy
        super.enqueueBasedOnHits();
    }

    private void addIfValidToQueue(int row, int col) {
        ShotCoordinate cell = ShotCoordinate.of(col, row);
        if (isCellAvailable(cell)) {
            addToHuntQueue(cell);
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
                if (!isTried && (state == CellState.MISS || state == CellState.HIT || state == CellState.SUNK)) {
                    throw new IllegalStateException("Synchronization error: cell " + cell +
                            " is not tried but marked as " + state + " in board");
                }
            }
        }
    }
}