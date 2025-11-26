package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class PlayerProfileDTO {
    private Long playerId;
    private String nickname;
    private String avatarUrl;
    private int totalGames;
    private int wins;
    private int losses;
    private int savedLayouts;
}
