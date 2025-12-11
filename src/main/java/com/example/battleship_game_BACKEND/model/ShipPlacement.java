package com.example.battleship_game_BACKEND.model;

/**
 * @param shipId Getters
 */
public record ShipPlacement(int shipId, int size, int row, int col, boolean vertical) {

    @org.jetbrains.annotations.NotNull
    @org.jetbrains.annotations.Contract(pure = true)
    @Override
    public String toString() {
        return String.format("Ship{id=%d, size=%d, pos=(%d,%d), vertical=%s}",
                shipId, size, row, col, vertical);
    }

}