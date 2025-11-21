package com.example.battleship_game_BACKEND.shooting;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Стратегия «Диагональная с вероятностным расширенным поиском».
 *
 * 1) **Hunt-режим (добивание)**
 *    — Если в базовом huntQueue есть координаты (после попадания) → стреляем по ним
 *      (используется enqueueOrthogonal() и enqueueBasedOnHits() из BaseShootingStrategy).
 *
 * 2) **Диагональная фаза (пока подряд промахов < dynamicThreshold)**
 *    — Заранее задаём порядок точек:
 *       • mainShots  = [(0,0),(9,9),(2,2),(7,7),(4,4),(5,5)]
 *       • secondaryShots = [(0,9),(9,0),(2,7),(7,2),(4,5),(5,4)]
 *    — Пока подряд промахов меньше порога (dynamicThreshold зависит от длины самого большого живого корабля):
 *       • сначала стреляем по mainShots в указанном порядке,
 *       • затем — по secondaryShots.
 *    — Как только либо списки исчерпались, либо промахов подряд стало ≥ dynamicThreshold,
 *      выключаем diagonalPhase и переходим в probability-режим.
 *
 * 3) **Probability Mode («тепловая матрица»)**
 *    — Берём список оставшихся кораблей remainingShips (их длины).
 *    — Строим двумерный массив counts[SIZE][SIZE] = 0.
 *    — Для каждого корабля длины L перебираем все возможные горизонтальные и вертикальные «вставки»:
 *       • Если все L ячеек свободны (CellState.EMPTY), то для каждой из них делаем counts[++].
 *    — В конце выбираем любую EMPTY-клетку, у которой counts максимален, и стреляем в неё.
 *
 * После каждого выстрела (onShotResult()):
 *  — Если (hit && sunk):
 *       • Добавляем (r,c) в huntHits (если ещё нет).
 *       • Вычисляем цепочку попаданий нужной длины (extractSunkChain).
 *       • Помечаем эти клетки как SUNK + строим вокруг буфер MISS (markBufferAround).
 *       • Удаляем длину корабля из remainingShips, сбрасываем hunt-режим, обнуляем consecutiveMisses.
 *  — Если (hit но не sunk):
 *       • Помечаем board[r][c] = HIT, добавляем (r,c) в huntHits, вызываем enqueueBasedOnHits(), сбрасываем consecutiveMisses=0.
 *  — Если (miss):
 *       • Помечаем board[r][c] = MISS, increment consecutiveMisses++.
 *
 * Приоритет ходов: **hunt-режим > диагональная фаза > probability-режим**.
 */
public class DiagonalProbabilityStrategy extends BaseShootingStrategy {

    private static final int SIZE = 10;
    private static final List<Integer> INITIAL_SHIPS = Arrays.asList(4, 3, 3, 2, 2, 2, 1, 1, 1, 1);

    /** Состояние клетки на «виртуальном» поле. */
    private enum CellState { EMPTY, MISS, HIT, SUNK }

    /** Виртуальное поле (10×10) для хранения статусов: EMPTY/MISS/HIT/SUNK. */
    private final CellState[][] board = new CellState[SIZE][SIZE];

    /** Длины тех кораблей, что ещё не потоплены. Копия INITIAL_SHIPS. */
    private final List<Integer> remainingShips = new ArrayList<>(INITIAL_SHIPS);

    /** Счётчик подряд идущих промахов (для выхода из диагональной фазы). */
    private int consecutiveMisses = 0;

    /** Флаг: мы ещё в диагональной фазе? */
    private boolean diagonalPhase = true;

    private final List<Integer> diagonalOrder = Arrays.asList(0, 9, 1, 8, 2, 7, 3, 6, 4, 5);

    /** Порядок точек «главной» диагонали. */
    private final List<ShotCoordinate> mainShots;

    /** Порядок точек «побочной» диагонали. */
    private final List<ShotCoordinate> secondaryShots;

    private int mainIndex = 0;         // текущий индекс в mainShots
    private int secondaryIndex = 0;    // текущий индекс в secondaryShots
    private boolean useMain = true;    // true→ бьём по mainShots; false→ по secondaryShots

    private final Random random = new Random();
    private ShotCoordinate lastShot;

    public DiagonalProbabilityStrategy() {
        // Инициализация поля
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = CellState.EMPTY;
            }
        }

        // Инициализация диагональных выстрелов
        this.mainShots = diagonalOrder.stream()
                .map(i -> ShotCoordinate.of(i, i))
                .collect(Collectors.toList());

        this.secondaryShots = diagonalOrder.stream()
                .map(i -> ShotCoordinate.of(i, SIZE - 1 - i))
                .collect(Collectors.toList());
    }

    // ================================================================
    // 1) getNextShot() — финальный метод выбора хода
    // ================================================================
    @Override
    public ShotCoordinate getNextShot() {
        ShotCoordinate nextShot = computeNextShot();
        lastShot = nextShot;
        return nextShot;
    }

    protected ShotCoordinate computeNextShot() {
        // ——— 1. Hunt-режим (добивание) ———
        if (!huntQueue.isEmpty()) {
            while (!huntQueue.isEmpty()) {
                ShotCoordinate cell = huntQueue.removeFirst();
                if (isValidCell(cell) &&
                        board[cell.y()][cell.x()] == CellState.EMPTY &&
                        hasTried(cell)) {
                    return cell;
                }
            }
            // Если все кандидаты устарели → сбрасываем очередь
            huntQueue.clear();
        }

        // ——— 2. Диагональная фаза ———
        int largest = remainingShips.stream().max(Integer::compareTo).orElse(1);
        int dynamicThreshold = calculateDynamicThreshold(largest);

        if (diagonalPhase && consecutiveMisses < dynamicThreshold) {
            ShotCoordinate diagonalShot = getDiagonalShot();
            if (diagonalShot != null) {
                return diagonalShot;
            }
        } else {
            // Либо промахов подряд стало слишком много, либо диагонали кончились
            diagonalPhase = false;
        }

        // ——— 3. Probability Mode ———
        return computeProbabilityShot();
    }

    private int calculateDynamicThreshold(int largestShip) {
        switch (largestShip) {
            case 4: return 14;
            case 3: return 12;
            case 2: return 8;
            default: return 6;
        }
    }

    private ShotCoordinate getDiagonalShot() {
        if (useMain) {
            // 2.1) Бьём по mainShots
            while (mainIndex < mainShots.size()) {
                ShotCoordinate cell = mainShots.get(mainIndex++);
                if (board[cell.y()][cell.x()] == CellState.EMPTY && hasTried(cell)) {
                    return cell;
                }
            }
            // Если mainShots исчерпаны → переключаемся на secondaryShots
            useMain = false;
            return getDiagonalShot();
        } else {
            // 2.2) Бьём по secondaryShots
            while (secondaryIndex < secondaryShots.size()) {
                ShotCoordinate cell = secondaryShots.get(secondaryIndex++);
                if (board[cell.y()][cell.x()] == CellState.EMPTY && hasTried(cell)) {
                    return cell;
                }
            }
            // Обе диагонали исчерпаны → конец диагональной фазы
            diagonalPhase = false;
            return null;
        }
    }

    // ================================================================
    // 2) setShotResult() — обновление после хода
    // ================================================================
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
        // [a] Добавляем lastShot в huntHits, если его там ещё нет
        if (!huntHits.contains(lastShot)) {
            huntHits.add(lastShot);
        }
        // [b] Ищем цепочку hit-точек нужной длины
        List<ShotCoordinate> chain = findSunkChain(huntHits, lastShot);
        if (chain == null) {
            chain = Collections.singletonList(lastShot);
        }
        // [c] Определяем длину потопленного корабля
        int justSunkLen = chain.size();
        // [d] Помечаем chain как SUNK + строим буфер MISS
        markBufferAround(chain);
        // [e] Удаляем потопленную длину из remainingShips
        remainingShips.remove(Integer.valueOf(justSunkLen));
        // [f] Сбрасываем hunt-режим
        resetHuntMode();
        // [g] Сбрасываем consecutiveMisses
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

    // ================================================================
    // 3) Probability Mode: строим «тепловую карту» по оставшимся кораблям
    // ================================================================
    private ShotCoordinate computeProbabilityShot() {
        // [1] Инициализируем массив весов
        int[][] counts = new int[SIZE][SIZE];

        // [2] Перебираем каждый корабль длины length
        for (int length : remainingShips) {
            boolean includeVertical = (length > 1);
            int shipWeight = length;

            // — Горизонтальные варианты
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
                // — Вертикальные варианты
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

        // [3] Ищем максимум среди EMPTY-клеток
        int maxCount = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == CellState.EMPTY && counts[r][c] > maxCount) {
                    maxCount = counts[r][c];
                }
            }
        }

        // [4] Составляем список кандидатов с counts == maxCount
        List<ShotCoordinate> candidates = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == CellState.EMPTY && counts[r][c] == maxCount) {
                    candidates.add(ShotCoordinate.of(c, r));
                }
            }
        }

        // [5] Если есть кандидаты — выбираем случайного
        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        // [6] Иначе (крайний случай) — возвращаем первую свободную EMPTY
        return findFirstEmptyCell();
    }

    // ================================================================
    // 4) Находит первую EMPTY ячейку, по которой ещё не стреляли
    // ================================================================
    private ShotCoordinate findFirstEmptyCell() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                ShotCoordinate coordinate = ShotCoordinate.of(c, r);
                if (board[r][c] == CellState.EMPTY && hasTried(coordinate)) {
                    return coordinate;
                }
            }
        }
        throw new IllegalStateException("No empty cell left");
    }

    // ================================================================
    // 5) Вспомогательные методы
    // ================================================================
    protected boolean isValidCell(ShotCoordinate cell) {
        return cell.x() >= 0 && cell.x() < SIZE &&
                cell.y() >= 0 && cell.y() < SIZE;
    }
    protected boolean hasTried(ShotCoordinate cell) {
        // Эта реализация зависит от базового класса
        // Предполагаем, что есть метод для проверки, был ли уже выстрел в эту клетку
        // Временная заглушка - в реальной реализации нужно интегрировать с историей выстрелов
        return board[cell.y()][cell.x()] == CellState.EMPTY;
    }

    // ================================================================
    // 6) Ищем «цепочку» hit-точек (горизонтально или вертикально)
    // ================================================================
    private List<ShotCoordinate> findSunkChain(
            List<ShotCoordinate> hits,
            ShotCoordinate start
    ) {
        int r = start.y();
        int c = start.x();

        // Находим горизонтальную цепочку
        int left = c;
        while (left > 0 && hits.contains(ShotCoordinate.of(left - 1, r))) left--;
        int right = c;
        while (right < SIZE - 1 && hits.contains(ShotCoordinate.of(right + 1, r))) right++;
        List<ShotCoordinate> horizontalChain = new ArrayList<>();
        for (int col = left; col <= right; col++) {
            horizontalChain.add(ShotCoordinate.of(col, r));
        }

        // Находим вертикальную цепочку
        int up = r;
        while (up > 0 && hits.contains(ShotCoordinate.of(c, up - 1))) up--;
        int down = r;
        while (down < SIZE - 1 && hits.contains(ShotCoordinate.of(c, down + 1))) down++;
        List<ShotCoordinate> verticalChain = new ArrayList<>();
        for (int row = up; row <= down; row++) {
            verticalChain.add(ShotCoordinate.of(c, row));
        }

        // Выбираем цепочку с большей длиной, если длины равны, выбираем горизонтальную
        return horizontalChain.size() >= verticalChain.size() ? horizontalChain : verticalChain;
    }

    // ================================================================
    // 7) Помечаем hits как SUNK и строим вокруг них буфер MISS
    // ================================================================
    private void markBufferAround(List<ShotCoordinate> hits) {
        if (hits.isEmpty()) return;

        // 7.1) Помечаем сами hits как SUNK
        for (ShotCoordinate hit : hits) {
            board[hit.y()][hit.x()] = CellState.SUNK;
        }

        // 7.2) Вокруг каждой точки помечаем буфер (8 соседних клеток) как MISS (если они были EMPTY)
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

    // ================================================================
    // 8) Методы для доступа к состоянию (для тестирования)
    // ================================================================
    public boolean isDiagonalPhase() {
        return diagonalPhase;
    }

    public int getConsecutiveMisses() {
        return consecutiveMisses;
    }

    public List<Integer> getRemainingShips() {
        return new ArrayList<>(remainingShips);
    }

    public CellState getCellState(int row, int col) {
        return board[row][col];
    }
}