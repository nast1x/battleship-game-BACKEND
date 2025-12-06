package com.example.battleship_game_BACKEND.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ShotResult {
    private int row;
    private int col;
    private boolean hit;
    private boolean sunk;
    private ShipPlacement sunkShip;
    private List<int[]> bufferCells;

    public ShotResult(int row, int col, boolean hit, boolean sunk, ShipPlacement sunkShip, List<int[]> bufferCells) {
        this.row = row;
        this.col = col;
        this.hit = hit;
        this.sunk = sunk;
        this.sunkShip = sunkShip;
        this.bufferCells = bufferCells;
    }

}