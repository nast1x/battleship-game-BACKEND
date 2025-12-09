package com.example.battleship_game_BACKEND.dto;

import com.example.battleship_game_BACKEND.model.GameStatus;
import com.example.battleship_game_BACKEND.model.GameType;
import lombok.Data;

@Data
public class GameCreatedResponse {
    private Long gameId;

    private Long player1Id;
    private Long player2Id;

    private Long gameBoard1Id;
    private Long gameBoard2Id;

    private GameStatus gameStatus;
    private GameType gameType;

    private Long currentTurnPlayerId;
}
