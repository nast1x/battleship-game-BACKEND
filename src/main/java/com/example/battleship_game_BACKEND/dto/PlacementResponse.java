package com.example.battleship_game_BACKEND.dto;

import java.util.List;

public record PlacementResponse(List<ShipPlacementDto> ships) {}