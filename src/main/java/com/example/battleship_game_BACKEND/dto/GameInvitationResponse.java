package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameInvitationResponse {
    private Long gameId;
    private Long inviterId;
    private String inviterNickname;
    private String inviterAvatarUrl;
    private LocalDateTime timestamp;
}
