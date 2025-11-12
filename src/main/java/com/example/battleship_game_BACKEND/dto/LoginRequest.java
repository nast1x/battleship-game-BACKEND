package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String nickname;
    private String password;
}