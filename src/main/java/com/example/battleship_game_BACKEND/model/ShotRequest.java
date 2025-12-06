package com.example.battleship_game_BACKEND.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ShotRequest {
    private int row;
    private int col;

    public ShotRequest() {}

    public ShotRequest(int row, int col) {
        this.row = row;
        this.col = col;
    }

}