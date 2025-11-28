package com.example.battleship_game_BACKEND.dto;

import java.util.Date;
import java.util.List;

public record UserPlacementResponse(
        Long id,
        String name,
        Date createdDate,
        List<ShipPlacementDto> ships
) {}