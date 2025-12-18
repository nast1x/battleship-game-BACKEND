package com.example.battleship_game_BACKEND.shooting;

import com.example.battleship_game_BACKEND.service.ShotCoordinate;

import java.util.*;

/**
 * Базовый класс для стратегий стрельбы
 */
public abstract class BaseShootingStrategy {
    public static final int SIZE = 10;
    protected static final List<Integer> INITIAL_SHIPS = List.of(4, 3, 3, 2, 2, 2, 1, 1, 1, 1);

    protected boolean[][] tried = new boolean[SIZE][SIZE];
    protected Queue<ShotCoordinate> huntQueue = new LinkedList<>();
    protected List<ShotCoordinate> huntHits = new ArrayList<>();

    /**
     * Получить следующий выстрел
     */
    public abstract ShotCoordinate getNextShot();

    /**
     * Записать результат выстрела
     */
    public void recordShot(ShotCoordinate shot, boolean hit, boolean sunk) {
        // Отмечаем клетку как обстрелянную
        tried[shot.x()][shot.y()] = true;

        // Обрабатываем попадание
        if (hit) {
            if (!huntHits.contains(shot)) {
                huntHits.add(shot);
            }

            if (sunk) {
                // При потоплении корабля сбрасываем состояние
                huntHits.clear();
                huntQueue.clear();
            }
        }
    }

    /**
     * Получить последние попадания (последний корабль)
     */
    public List<ShotCoordinate> getLastHits() {
        return new ArrayList<>(huntHits);
    }

    /**
     * Проверить, была ли клетка обстреляна
     */
    protected boolean isCellTried(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) {
            return false;
        }
        return tried[x][y];
    }

    protected boolean isCellTried(ShotCoordinate cell) {
        return isCellTried(cell.x(), cell.y());
    }

    protected boolean isCellUntried(ShotCoordinate cell) {
        return !isCellTried(cell);
    }

    protected ShotCoordinate findAnyUntriedCell() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (!tried[x][y]) {
                    return new ShotCoordinate(x, y);
                }
            }
        }
        return null;
    }

    protected ShotCoordinate getShotFromHuntQueue(java.util.function.Predicate<ShotCoordinate> filter) {
        Iterator<ShotCoordinate> iterator = huntQueue.iterator();
        while (iterator.hasNext()) {
            ShotCoordinate shot = iterator.next();
            if (filter.test(shot)) {
                iterator.remove();
                return shot;
            }
        }
        return null;
    }

    protected void enqueueBasedOnHits() {
        for (ShotCoordinate hit : huntHits) {
            int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

            for (int[] dir : directions) {
                int newX = hit.x() + dir[0];
                int newY = hit.y() + dir[1];

                if (newX >= 0 && newX < SIZE && newY >= 0 && newY < SIZE) {
                    ShotCoordinate target = new ShotCoordinate(newX, newY);
                    if (!tried[newX][newY] && !huntQueue.contains(target)) {
                        huntQueue.add(target);
                    }
                }
            }
        }
    }

    protected void resetHuntMode() {
        huntQueue.clear();
        huntHits.clear();
    }

    protected boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }
}