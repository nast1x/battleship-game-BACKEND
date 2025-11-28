package com.example.battleship_game_BACKEND.dto;

public record ShipPlacementDto(
        int shipId,
        int size,
        int row, // 0-9
        int col, // 0-9
        boolean vertical
) {}
