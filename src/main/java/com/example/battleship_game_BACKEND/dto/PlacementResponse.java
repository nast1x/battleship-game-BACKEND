package com.example.battleship_game_BACKEND.dto;

import java.util.List;

public record PlacementResponse(
        boolean success,
        String message,
        List<ShipPlacementDto> placements,
        String visualization // опционально: предварительно сгенерированная визуализация
) {}
