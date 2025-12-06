package com.example.battleship_game_BACKEND.shooting;

import com.example.battleship_game_BACKEND.model.Difficulty;

public class ShootingStrategyFactory {
    public static BaseShootingStrategy createStrategy(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new DiagonalProbabilityStrategy();
            case MEDIUM -> new AdaptiveDensityStrategy();
            case HARD -> new DensityAnalysisStrategy();
        };
    }
}