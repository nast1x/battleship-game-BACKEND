package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class GameRejectRequest {
    private Long inviterId;
    private Long opponentId; // тот, кто отверг приглашение
}
