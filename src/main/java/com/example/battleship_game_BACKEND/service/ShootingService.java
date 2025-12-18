package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.shooting.DensityAnalysisStrategy;
import com.example.battleship_game_BACKEND.shooting.RandomFinishingStrategy;
import org.springframework.stereotype.Service;

@Service
public class ShootingService {

    private DensityAnalysisStrategy densityStrategy;
    private RandomFinishingStrategy randomStrategy;

    // Можно инициализировать при создании сервиса
    public ShootingService() {
        this.densityStrategy = new DensityAnalysisStrategy();
        this.randomStrategy = new RandomFinishingStrategy();
    }

    /**
     * Получить следующий выстрел для компьютера
     */
    public ShotCoordinate getComputerShot(String difficulty) {
        return switch (difficulty.toLowerCase()) {
            case "hard" -> densityStrategy.getNextShot();
            case "medium", "easy" -> randomStrategy.getNextShot();
            default -> randomStrategy.getNextShot();
        };
    }

    /**
     * Записать результат выстрела компьютера
     */
    public void recordComputerShot(String difficulty, ShotCoordinate shot, boolean hit, boolean sunk) {
        switch (difficulty.toLowerCase()) {
            case "hard" -> densityStrategy.recordShot(shot, hit, sunk);
            case "medium", "easy" -> randomStrategy.recordShot(shot, hit, sunk);
            default -> randomStrategy.recordShot(shot, hit, sunk);
        }
    }

    /**
     * Сбросить состояние стратегии
     */
    public void resetStrategy(String difficulty) {
        // Создаем новые экземпляры стратегий
        if ("hard".equalsIgnoreCase(difficulty)) {
            this.densityStrategy = new DensityAnalysisStrategy();
        } else {
            this.randomStrategy = new RandomFinishingStrategy();
        }
    }
}