package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ComputerGameStartRequest {
    private String placementStrategy; // RANDOM, COASTS, DIAGONAL, HALF
    private List<ShipPlacementDto> playerShips;
}

@Data
class ShotDto {
    private Integer row;
    private Integer col;
    private boolean hit;
    private LocalDateTime time;
}