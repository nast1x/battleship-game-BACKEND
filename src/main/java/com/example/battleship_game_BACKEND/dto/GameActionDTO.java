package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class GameActionDTO {
    private Long gameId;
    private Long playerId;
    private String actionType; // "SURRENDER", "OFFER_DRAW", "ACCEPT_DRAW", "DECLINE_DRAW"
}