package com.example.battleship_game_BACKEND.shooting;

import lombok.Getter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Базовая абстракция для любой стратегии стрельбы.
 * Отвечает за:
 *  1) Хранение матрицы tried[row][col]: true — если клетка уже обстреляна.
 *  2) Проверку валидности координат (0..SIZE-1).
 *  3) Предоставление общих «hunt-helper-методов» (добивание).
 *  4) Определение контракта getNextShot() → computeNextShot() → setShotResult().
 *
 * Наследники должны реализовать:
 *  - computeNextShot() — возвращает (row, col) для следующего выстрела без учёта tried[].
 *  - onShotResult(...) — получает результат (hit/sunk) последнего выстрела и обновляет внутреннее состояние.
 *
 * ВАЖНО: Все координаты используют систему (x, y), где x - столбец, y - строка.
 */
public abstract class BaseShootingStrategy implements ShootingStrategy {

    protected static final int SIZE = 10;
    protected static final List<Integer> INITIAL_SHIPS = Arrays.asList(4, 3, 3, 2, 2, 2, 1, 1, 1, 1);

    // ===============================================================================
    // 1) «Известные» уже обстрелянные клетки
    // ===============================================================================
    private final boolean[][] tried = new boolean[SIZE][SIZE];

    /** Быстрый поиск для избежания дубликатов в очереди */
    private final Set<ShotCoordinate> huntQueueSet = new HashSet<>();

    @Getter
    private ShotCoordinate lastShot;

    // ===============================================================================
    // 2) «Hunt-Mode» (добивание) — общие поля для всех стратегий
    // ===============================================================================
    /** Очередь клеток для «добивания» (после попадания). */
    protected final Deque<ShotCoordinate> huntQueue = new ArrayDeque<>();

    /**
     * Список уже подбитых точек одного конкретного корабля,
     * нужный для построения очереди добивания.
     */
    protected final List<ShotCoordinate> huntHits = new ArrayList<>();

    // ===============================================================================
    // 3) Финальные методы: getNextShot() и setShotResult()
    // ===============================================================================

    /**
     * ВАЖНЫЙ КОНТРАКТ: гарантирует возврат непробованной клетки.
     * Наследники ДОЛЖНЫ гарантировать, что computeNextShot() возвращает валидные координаты.
     */
    @Override
    public ShotCoordinate getNextShot() {
        validateState();

        // Сначала пытаемся получить клетку из hunt-очереди
        ShotCoordinate huntShot = getShotFromHuntQueue();
        if (huntShot != null) {
            lastShot = huntShot;
            return huntShot;
        }

        // Пытаемся до 50 раз вызвать computeNextShot() для получения непробованной клетки
        for (int i = 0; i < 50; i++) {
            ShotCoordinate shot = computeNextShot();
            if (isValidCell(shot) && isCellUntried(shot)) {
                lastShot = shot;
                return shot;
            }
        }

        // Fallback: детерминированный поиск первой свободной клетки
        ShotCoordinate fallbackShot = findAnyUntriedCell();
        if (fallbackShot != null) {
            lastShot = fallbackShot;
            return fallbackShot;
        }

        throw new IllegalStateException("No available cells to shoot");
    }

    @Override
    public void setShotResult(boolean hit, boolean sunk) {
        if (lastShot != null && isValidCell(lastShot)) {
            tried[lastShot.y()][lastShot.x()] = true;
        }
        onShotResult(lastShot, hit, sunk);
    }

    // ===============================================================================
    // 4) Вспомогательные методы для наследников
    // ===============================================================================

    /** Проверяет, что координаты лежат в диапазоне 0..SIZE-1. */
    protected boolean isValidCell(ShotCoordinate cell) {
        return cell != null &&
                cell.x() >= 0 && cell.x() < SIZE &&
                cell.y() >= 0 && cell.y() < SIZE;
    }

    /** Проверяет, что координаты (row, col) лежат в диапазоне 0..SIZE-1. */
    protected boolean isValidCell(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    /**
     * Проверка, НЕ была ли уже клетка обстреляна.
     * ЯСНАЯ СЕМАНТИКА: возвращает true если клетка еще НЕ пробована.
     */
    protected boolean isCellUntried(ShotCoordinate cell) {
        return cell != null && !tried[cell.y()][cell.x()];
    }

    /**
     * Проверка, НЕ была ли уже клетка (row, col) обстреляна.
     */
    protected boolean isCellUntried(int row, int col) {
        return !tried[row][col];
    }

    /**
     * Проверка, была ли клетка уже обстреляна.
     * Альтернативный метод с прямой семантикой.
     */
    protected boolean isCellTried(ShotCoordinate cell) {
        return cell != null && tried[cell.y()][cell.x()];
    }

    /**
     * «Сырая» логика выбора следующей клетки без учёта tried[].
     * КОНТРАКТ: должен возвращать ВАЛИДНЫЕ координаты в диапазоне [0, SIZE-1].
     * Может возвращать уже обстрелянные клетки - базовая логика это отфильтрует.
     */
    protected abstract ShotCoordinate computeNextShot();

    /**
     * Обработка результата последнего выстрела. Наследник тут обновляет своё internal-state.
     */
    protected void onShotResult(ShotCoordinate lastShot, boolean hit, boolean sunk) {
        // По умолчанию ничего не делаем.
    }

    // ===============================================================================
    // 5) Hunt-helpers: методы добивания (универсальные для всех стратегий)
    // ===============================================================================

    /**
     * Добавляет в очередь добивания все 4 ортогональных соседа клетки.
     * ИСПРАВЛЕННАЯ ЛОГИКА: добавляет только НЕобстрелянные клетки.
     */
    protected void enqueueOrthogonal(int row, int col) {
        List<ShotCoordinate> neighbors = Arrays.asList(
                ShotCoordinate.of(col + 1, row),
                ShotCoordinate.of(col - 1, row),
                ShotCoordinate.of(col, row + 1),
                ShotCoordinate.of(col, row - 1)
        );

        for (ShotCoordinate cell : neighbors) {
            if (isValidCell(cell) && isCellUntried(cell) && !huntQueueSet.contains(cell)) {
                huntQueue.addLast(cell);
                huntQueueSet.add(cell); // Поддерживаем синхронно
            }
        }
    }

    /**
     * Добавляет в очередь добивания все 4 ортогональных соседа клетки.
     */
    protected void enqueueOrthogonal(ShotCoordinate cell) {
        if (cell != null) {
            enqueueOrthogonal(cell.y(), cell.x());
        }
    }

    /**
     * Основная логика построения очереди «добивания».
     * ИСПРАВЛЕННАЯ ЛОГИКА: использует правильные условия для непробованных клеток.
     */
    protected void enqueueBasedOnHits() {
        // Сброс предыдущей очереди
        resetHuntQueue();

        if (huntHits.isEmpty()) {
            return;
        }

        if (huntHits.size() == 1) {
            ShotCoordinate firstHit = huntHits.get(0);
            enqueueOrthogonal(firstHit);
            return;
        }

        // Проверяем, все ли попадания в одной строке?
        boolean sameRow = huntHits.stream()
                .map(ShotCoordinate::y)
                .distinct()
                .count() == 1;

        // Проверяем, все ли попадания в одном столбце?
        boolean sameCol = huntHits.stream()
                .map(ShotCoordinate::x)
                .distinct()
                .count() == 1;

        if (sameRow) {
            // Горизонтальный корабль
            int row = huntHits.get(0).y();
            List<Integer> sortedCols = huntHits.stream()
                    .map(ShotCoordinate::x)
                    .sorted()
                    .collect(Collectors.toList());

            int leftC = sortedCols.get(0);
            int rightC = sortedCols.get(sortedCols.size() - 1);

            // Клетка «слева» от минимальной
            ShotCoordinate leftCell = ShotCoordinate.of(leftC - 1, row);
            if (isValidCell(leftCell) && isCellUntried(leftCell)) {
                addToHuntQueue(leftCell);
            }

            // Клетка «справа» от максимальной
            ShotCoordinate rightCell = ShotCoordinate.of(rightC + 1, row);
            if (isValidCell(rightCell) && isCellUntried(rightCell)) {
                addToHuntQueue(rightCell);
            }
        }
        else if (sameCol) {
            // Вертикальный корабль
            int col = huntHits.get(0).x();
            List<Integer> sortedRows = huntHits.stream()
                    .map(ShotCoordinate::y)
                    .sorted()
                    .collect(Collectors.toList());

            int topR = sortedRows.get(0);
            int bottomR = sortedRows.get(sortedRows.size() - 1);

            // Клетка «сверху»
            ShotCoordinate upCell = ShotCoordinate.of(col, topR - 1);
            if (isValidCell(upCell) && isCellUntried(upCell)) {
                addToHuntQueue(upCell);
            }

            // Клетка «снизу»
            ShotCoordinate downCell = ShotCoordinate.of(col, bottomR + 1);
            if (isValidCell(downCell) && isCellUntried(downCell)) {
                addToHuntQueue(downCell);
            }
        }
        else {
            // ≥2 попаданий, но не по одной линии → enqueueOrthogonal() от последнего попадания
            ShotCoordinate lastHit = huntHits.get(huntHits.size() - 1);
            enqueueOrthogonal(lastHit);
        }
    }

    /**
     * Пытается получить следующую клетку для выстрела из huntQueue.
     * Проверяет валидность клетки и то, что по ней еще не стреляли.
     */
    protected ShotCoordinate getShotFromHuntQueue(Predicate<ShotCoordinate> additionalCondition) {
        while (!huntQueue.isEmpty()) {
            ShotCoordinate cell = huntQueue.removeFirst();
            huntQueueSet.remove(cell); // Поддерживаем синхронно

            if (isValidCell(cell) && isCellUntried(cell) && additionalCondition.test(cell)) {
                return cell;
            }
        }
        return null;
    }

    /**
     * Пытается получить следующую клетку для выстрела из huntQueue.
     * Базовый вариант без дополнительных условий.
     */
    protected ShotCoordinate getShotFromHuntQueue() {
        return getShotFromHuntQueue(cell -> true);
    }

    /**
     * Полный сброс «hunt-режима».
     */
    protected void resetHuntMode() {
        resetHuntQueue();
        huntHits.clear();
    }

    /**
     * Сброс только очереди hunt.
     */
    protected void resetHuntQueue() {
        huntQueue.clear();
        huntQueueSet.clear();
    }

    // ===============================================================================
    // 6) Валидация и утилитные методы
    // ===============================================================================

    /**
     * Валидация состояния стратегии.
     */
    protected void validateState() {
        if (remainingUntriedCells() == 0) {
            throw new IllegalStateException("No untried cells remaining");
        }

        // Проверить, что huntHits содержат только действительные попадания
        for (ShotCoordinate hit : huntHits) {
            if (!isValidCell(hit)) {
                throw new IllegalStateException("Invalid hunt hit coordinate: " + hit);
            }
        }

        // Проверить согласованность huntQueue и huntQueueSet
        if (huntQueue.size() != huntQueueSet.size()) {
            throw new IllegalStateException("Hunt queue and set are out of sync");
        }
    }

    /**
     * Находит первую непробованную клетку в детерминированном порядке.
     */
    protected ShotCoordinate findAnyUntriedCell() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (!tried[row][col]) {
                    return ShotCoordinate.of(col, row);
                }
            }
        }
        return null;
    }

    /**
     * Подсчитывает количество оставшихся непробованных клеток.
     */
    protected int remainingUntriedCells() {
        int count = 0;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (!tried[row][col]) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Безопасное добавление в hunt-очередь с поддержанием синхронизации.
     */
    protected void addToHuntQueue(ShotCoordinate cell) {
        if (isValidCell(cell) && isCellUntried(cell) && !huntQueueSet.contains(cell)) {
            huntQueue.addLast(cell);
            huntQueueSet.add(cell);
        }
    }

    // ===============================================================================
    // 7) Методы для доступа к состоянию (для тестирования и отладки)
    // ===============================================================================

    public boolean isCellTried(int row, int col) {
        return tried[row][col];
    }

    public Deque<ShotCoordinate> getHuntQueue() {
        return new ArrayDeque<>(huntQueue);
    }

    public List<ShotCoordinate> getHuntHits() {
        return new ArrayList<>(huntHits);
    }

    public int getRemainingUntriedCellsCount() {
        return remainingUntriedCells();
    }
}