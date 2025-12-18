package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.shooting.BaseShootingStrategy;

public record ShotCoordinate(int x, int y) {
    public static ShotCoordinate of(int x, int y) {
        if (x < 0 || x >= BaseShootingStrategy.SIZE ||
                y < 0 || y >= BaseShootingStrategy.SIZE) {
            throw new IllegalArgumentException("Invalid coordinates: (" + x + ", " + y + ")");
        }
        return new ShotCoordinate(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}