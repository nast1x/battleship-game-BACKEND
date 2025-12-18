package com.example.battleship_game_BACKEND.dto;

public record PlacementRequest(
        Long playerId,
        String strategyName
) {}