package com.example.battleship_game_BACKEND.dto;

import com.example.battleship_game_BACKEND.model.GameType;
import lombok.Data;

import java.util.List;

@Data
public class GameReadyMessage {
    private Long playerId;
    private Long opponentId;
    private GameType gameType;          // MULTIPLAYER, и т.п.
    private BoardLayoutDTO boardLayout; // твоя схема кораблей
}