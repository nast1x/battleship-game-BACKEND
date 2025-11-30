package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class GameStartNotification {
    private Long gameId;
    private Long opponentId;
    private String opponentNickname;
    private String opponentAvatarUrl;
}
