package com.example.battleship_game_BACKEND.shooting;

import com.example.battleship_game_BACKEND.service.ShotCoordinate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Стратегия «Случайная с добиванием», синхронизированная с BaseShootingStrategy.
 * - Использует tried[][] из базового класса для отслеживания обстрелянных клеток
 * - Наследует hunt-логику из базового класса
 * - Сохраняет логику исключения буфера вокруг потопленных кораблей
 */
//@Component
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
                // Проверяем, что клетка не была обстреляна ранее
                if (!isCellTried(col, row)) {
                    availableCells.add(new ShotCoordinate(col, row));
                }
            }
        }
    }

    @Override
    public ShotCoordinate getNextShot() {
        // Сначала используем hunt-очередь из базового класса
        if (!huntQueue.isEmpty()) {
            ShotCoordinate huntShot = huntQueue.poll();
            // Удаляем из availableCells, чтобы избежать дублирования
            availableCells.remove(huntShot);
            return huntShot;
        }

        // Удаляем клетки, которые уже обстреляны (синхронизация с tried[][])
        availableCells.removeIf(coord -> isCellTried(coord.x(), coord.y()));

        if (availableCells.isEmpty()) {
            // Fallback: находим любую необстрелянную клетку
            return findAnyUntriedCell();
        }

        // Выбираем случайную клетку из доступных
        List<ShotCoordinate> availableList = new ArrayList<>(availableCells);
        ShotCoordinate shot = availableList.get(random.nextInt(availableList.size()));

        // Удаляем выбранную клетку из availableCells
        availableCells.remove(shot);

        return shot;
    }

    @Override
    public void recordShot(ShotCoordinate shot, boolean hit, boolean sunk) {
        // Вызываем родительский метод для обновления состояния
        super.recordShot(shot, hit, sunk);

        // Удаляем обстрелянную клетку из availableCells
        availableCells.remove(shot);

        if (hit && sunk) {
            // Потопили корабль - вычисляем и исключаем буфер вокруг корабля
            Set<ShotCoordinate> buffer = computeBuffer(getLastHits());
            availableCells.removeAll(buffer);

            // Сбрасываем состояние добивания
            resetHuntMode();
        } else if (hit) {
            // Попадание (корабль еще не потоплен)
            // Базовая логика уже добавила координату в huntHits
            // Генерируем новые цели вокруг попадания
            generateTargetsAroundHit(shot);
        }
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

                    int newX = cell.x() + dx;
                    int newY = cell.y() + dy;

                    if (newX >= 0 && newX < SIZE && newY >= 0 && newY < SIZE) {
                        ShotCoordinate neighbor = new ShotCoordinate(newX, newY);
                        buffer.add(neighbor);
                    }
                }
            }
        }

        // Убираем сами клетки корабля
        buffer.removeAll(shipCells);
        return buffer;
    }

    /**
     * Генерирует цели вокруг попадания для режима добивания
     */
    private void generateTargetsAroundHit(ShotCoordinate hit) {
        // Проверяем четыре направления (вверх, вниз, влево, вправо)
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        for (int[] dir : directions) {
            int newX = hit.x() + dir[0];
            int newY = hit.y() + dir[1];

            if (newX >= 0 && newX < SIZE && newY >= 0 && newY < SIZE) {
                ShotCoordinate target = new ShotCoordinate(newX, newY);

                // Добавляем в очередь, если клетка не обстреляна и доступна
                if (!isCellTried(newX, newY) && availableCells.contains(target)) {
                    huntQueue.add(target);
                }
            }
        }
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
    public void restoreCell(ShotCoordinate cell) {
        if (isValidCell(cell.x(), cell.y()) && !isCellTried(cell.x(), cell.y())) {
            availableCells.add(cell);
        }
    }

    /**
     * Проверяет валидность клетки
     */
    private boolean isValidCell(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }
}