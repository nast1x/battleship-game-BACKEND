// GameStateDTO.java
package com.example.battleship_game_BACKEND.dto;

import lombok.Data;
import java.util.Map;

@Data
public class GameStateDTO {
    private Long gameId;
    private Long currentTurnPlayerId;
    private String gameStatus;
    private Integer player1ShipsLeft;
    private Integer player2ShipsLeft;
    private Character[][] player1Field;
    private Character[][] player2Field;
    private Character[][] player1Hits;
    private Character[][] player2Hits;
    private Integer player1ShotsFired;
    private Integer player2ShotsFired;
    private Integer player1HitsCount;
    private Integer player2HitsCount;
}