package com.example.battleship_game_BACKEND.shooting;

/**
 * Интерфейс для алгоритмов выбора следующей клетки для выстрела.
 *
 * Реализации должны предоставлять:
 * - Выбор следующей клетки для выстрела (getNextShot)
 * - Обработку результата выстрела (setShotResult)
 */
public record ShotCoordinate(int x, int y) {
    public ShotCoordinate {
        if (x < 0 || x > 9 || y < 0 || y > 9) {
            throw new IllegalArgumentException("Coordinates must be in range 0-9");
        }
    }

    public static ShotCoordinate of(int x, int y) {
        return new ShotCoordinate(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}