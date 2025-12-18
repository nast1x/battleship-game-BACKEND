package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ComputerGameStartRequest {
    private String placementStrategy; // RANDOM, COASTS, DIAGONAL, HALF
    private List<ShipPlacementDto> playerShips;
}

