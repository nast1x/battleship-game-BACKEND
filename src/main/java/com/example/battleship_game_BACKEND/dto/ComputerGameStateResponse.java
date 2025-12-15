package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ComputerGameStateResponse {
    private Long gameId;
    private String status; // waiting, active, completed, cancelled
    private boolean playerTurn;
    private LocalDateTime lastMoveTime;

    // Игровые поля
    private Character[][] playerBoard; // Вид игрока на свое поле
    private Character[][] computerBoard; // Вид игрока на поле компьютера
}
