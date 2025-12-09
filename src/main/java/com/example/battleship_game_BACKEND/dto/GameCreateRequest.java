package com.example.battleship_game_BACKEND.dto;

import com.example.battleship_game_BACKEND.model.GameType;
import lombok.Data;

@Data
public class GameCreateRequest {
    private Long player1Id;   // инициатор
    private Long player2Id;   // оппонент
    private Long gameBoard1Id;
    private Long gameBoard2Id;
    private GameType gameType; // PVP / MULTIPLAYER / что у тебя есть в enum
}
