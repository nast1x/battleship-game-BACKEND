package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

import java.util.List;

@Data
public class BoardLayoutDTO {
    // например, список кораблей
    private List<ShipDTO> ships;
    private Character[][] matrix;
}

@Data
class ShipDTO {
    private int size;
    private int x;
    private int y;
    private boolean horizontal;
}