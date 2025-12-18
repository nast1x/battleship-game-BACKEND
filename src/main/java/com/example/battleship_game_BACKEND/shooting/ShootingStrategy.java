package com.example.battleship_game_BACKEND.shooting;

import com.example.battleship_game_BACKEND.service.ShotCoordinate;

public interface ShootingStrategy {

    /**
     * Получить следующий выстрел
     */
    ShotCoordinate getNextShot();

    /**
     * Записать результат выстрела
     */
    void recordShot(ShotCoordinate shot, boolean hit, boolean sunk);

    /**
     * Сбросить состояние стратегии
     */
    void reset();

}