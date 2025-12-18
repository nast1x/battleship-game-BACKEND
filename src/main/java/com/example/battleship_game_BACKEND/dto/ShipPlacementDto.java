package com.example.battleship_game_BACKEND.dto;

import java.util.List;


public record ShipPlacementDto(
        int shipId,
        int size,
        int row,
        int col,
        boolean vertical
) {
    // Конвертация из PlacementStrategy.ShipPlacement
    public static ShipPlacementDto from(com.example.battleship_game_BACKEND.model.PlacementStrategy.ShipPlacement placement) {
        return new ShipPlacementDto(
                placement.shipId(),
                placement.size(),
                placement.row(),
                placement.col(),
                placement.vertical()
        );
    }

    // Конвертация в PlacementStrategy.ShipPlacement
    public com.example.battleship_game_BACKEND.model.PlacementStrategy.ShipPlacement toPlacementStrategy() {
        return new com.example.battleship_game_BACKEND.model.PlacementStrategy.ShipPlacement(
                shipId(),
                size(),
                row(),
                col(),
                vertical()
        );
    }
}