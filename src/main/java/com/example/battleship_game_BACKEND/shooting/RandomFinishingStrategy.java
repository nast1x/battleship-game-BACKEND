package com.example.battleship_game_BACKEND.shooting;

import java.util.*;

/**
 * Стратегия «Случайная с добиванием», синхронизированная с BaseShootingStrategy.
 * - Использует tried[][] из базового класса для отслеживания обстрелянных клеток
 * - Наследует hunt-логику из базового класса
 * - Сохраняет логику исключения буфера вокруг потопленных кораблей
 */
public class RandomFinishingStrategy extends BaseShootingStrategy {

    private final Random random = new Random();

    /** Множество клеток, доступных для случайного выбора (исключает буфер потопленных кораблей) */
    private final Set<ShotCoordinate> availableCells = new HashSet<>();

    public RandomFinishingStrategy() {
        initializeAvailableCells();
    }

    private void initializeAvailableCells() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                availableCells.add(ShotCoordinate.of(col, row));
            }
        }
    }

    @Override
    protected ShotCoordinate computeNextShot() {
        // Удаляем клетки, которые уже обстреляны (синхронизация с tried[][])
        availableCells.removeIf(this::isCellTried);

        if (availableCells.isEmpty()) {
            return findAnyUntriedCell(); // Fallback из базового класса
        }

        // Выбираем случайную клетку из доступных
        List<ShotCoordinate> availableList = new ArrayList<>(availableCells);
        return availableList.get(random.nextInt(availableList.size()));
    }

    @Override
    protected void onShotResult(ShotCoordinate lastShot, boolean hit, boolean sunk) {
        if (hit && sunk) {
            // Потопили корабль
            if (!huntHits.contains(lastShot)) {
                huntHits.add(lastShot);
            }

            // Вычисляем и исключаем буфер вокруг корабля
            Set<ShotCoordinate> buffer = computeBuffer(huntHits);
            huntHits.forEach(availableCells::remove);
            availableCells.removeAll(buffer);

            // Сброс режима добивания через базовый класс
            resetHuntMode();
        } else if (hit) {
            // Попадание (корабль еще не потоплен)
            huntHits.add(lastShot);
            enqueueBasedOnHits(); // Используем логику базового класса
        }
        // Промах - не требует специальной обработки
    }

    @Override
    public ShotCoordinate getNextShot() {
        // Сначала используем hunt-очередь из базового класса
        ShotCoordinate huntShot = getShotFromHuntQueue();
        if (huntShot != null) {
            // Удаляем из availableCells, чтобы избежать дублирования
            availableCells.remove(huntShot);
            return huntShot;
        }

        // Затем используем нашу случайную логику через computeNextShot()
        // Базовая реализация гарантирует, что клетка не была обстреляна
        ShotCoordinate shot = super.getNextShot();

        // Удаляем выбранную клетку из availableCells
        availableCells.remove(shot);

        return shot;
    }

    /**
     * Вычисляет буфер вокруг корабля (все соседние клетки)
     */
    private Set<ShotCoordinate> computeBuffer(List<ShotCoordinate> shipCells) {
        Set<ShotCoordinate> buffer = new HashSet<>();

        for (ShotCoordinate cell : shipCells) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    try {
                        ShotCoordinate neighbor = ShotCoordinate.of(cell.x() + dx, cell.y() + dy);
                        buffer.add(neighbor);
                    } catch (IllegalArgumentException e) {
                        // Игнорируем невалидные координаты
                    }
                }
            }
        }

        // Убираем сами клетки корабля
        shipCells.forEach(buffer::remove);
        return buffer;
    }

    // ===============================================================================
    // Методы для отладки и тестирования
    // ===============================================================================

    /**
     * Проверяет, доступна ли клетка для выстрела (не обстреляна и не в буфере)
     */
    public boolean isCellAvailable(ShotCoordinate coordinate) {
        return availableCells.contains(coordinate);
    }

    /**
     * Возвращает количество доступных клеток
     */
    public int getAvailableCellsCount() {
        return availableCells.size();
    }

    /**
     * Возвращает копию множества доступных клеток
     */
    public Set<ShotCoordinate> getAvailableCells() {
        return new HashSet<>(availableCells);
    }

    /**
     * Восстанавливает доступность клетки (для тестирования)
     */
    protected void restoreCell(ShotCoordinate cell) {
        if (isValidCell(cell)) {
            availableCells.add(cell);
        }
    }
}