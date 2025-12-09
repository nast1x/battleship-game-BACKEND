package com.example.battleship_game_BACKEND.dto;

import com.example.battleship_game_BACKEND.model.GameType;
import lombok.Data;

@Data
public class PendingGame {
    private Long player1Id;
    private BoardLayoutDTO board1;

    private Long player2Id;
    private BoardLayoutDTO board2;

    private GameType gameType;
}