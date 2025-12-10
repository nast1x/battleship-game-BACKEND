package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class GameResultDTO {
    private Long gameId;
    private Long winnerId;
    private Long loserId;
    private String resultType; // "WIN", "SURRENDER", "DRAW"
    private int totalMoves;
    private String endReason;
}