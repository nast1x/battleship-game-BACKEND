package com.example.battleship_game_BACKEND.dto;

import java.util.List;

public record SavePlacementRequest(
        String userId,
        String placementName,
        List<ShipPlacementDto> ships
) {}