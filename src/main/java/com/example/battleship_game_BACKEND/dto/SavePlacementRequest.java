package com.example.battleship_game_BACKEND.dto;

import java.util.List;

public record SavePlacementRequest(
        Long playerId,
        String strategyName,
        List<ShipPlacementDto> ships
) {}