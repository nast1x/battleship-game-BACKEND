package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class GameInvitationRequest {
    private Long inviterId;
    private Long opponentId;
    private String inviterNickname;
    private String inviterAvatarUrl;
}
