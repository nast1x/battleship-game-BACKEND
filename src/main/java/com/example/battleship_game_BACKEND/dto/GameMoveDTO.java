package com.example.battleship_game_BACKEND.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class GameMoveDTO {
    private Long gameId;
    private Long playerId;
    private int row;      // 0-9
    private int column;   // 0-9
}