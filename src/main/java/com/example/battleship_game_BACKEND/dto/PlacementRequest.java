package com.example.battleship_game_BACKEND.dto;

public record PlacementRequest(
        String strategy, // "coastal", "diagonal", "halfField", "spread", "random"
        String userId,
        boolean saveToProfile
) {}