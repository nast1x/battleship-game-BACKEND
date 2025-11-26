package com.example.battleship_game_BACKEND.shooting;

/**
 * Record для представления координат выстрела (Java 16+)
 */
interface ShootingStrategy {

    /**
     * Возвращает координаты следующего выстрела в диапазоне 0-9.
     * Гарантирует, что клетка еще не обстреливалась.
     */
    ShotCoordinate getNextShot();

    /**
     * Обновляет состояние стратегии на основе результата выстрела.
     * @param hit - попали ли в корабль
     * @param sunk - потопили ли корабль целиком
     */
    void setShotResult(boolean hit, boolean sunk);
}
