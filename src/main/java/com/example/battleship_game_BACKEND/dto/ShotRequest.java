package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class ShotRequest {
    private Long gameId;
    private Integer row;
    private Integer col;
}
