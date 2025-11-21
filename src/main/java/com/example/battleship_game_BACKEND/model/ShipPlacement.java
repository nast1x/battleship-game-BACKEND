package com.example.battleship_game_BACKEND.model;

public class ShipPlacement {
    private final int shipId;
    private final int size;
    private final int row;
    private final int col;
    private final boolean vertical;

    public ShipPlacement(int shipId, int size, int row, int col, boolean vertical) {
        this.shipId = shipId;
        this.size = size;
        this.row = row;
        this.col = col;
        this.vertical = vertical;
    }

    // Getters
    public int getShipId() { return shipId; }
    public int getSize() { return size; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public boolean isVertical() { return vertical; }

    @Override
    public String toString() {
        return String.format("Ship{id=%d, size=%d, pos=(%d,%d), vertical=%s}",
                shipId, size, row, col, vertical);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipPlacement that = (ShipPlacement) o;
        return shipId == that.shipId &&
                size == that.size &&
                row == that.row &&
                col == that.col &&
                vertical == that.vertical;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shipId, size, row, col, vertical);
    }
}