package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class PlayerMultiplayerDTO {
    private Long playerId;
    private String nickname;
    private String avatarUrl;
    public PlayerMultiplayerDTO(Long playerId, String nickname, String avatarUrl) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
}
