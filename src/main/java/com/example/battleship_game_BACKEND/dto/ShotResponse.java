package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class ShotResponse {
    private boolean hit;
    private boolean sunk;
    private Integer sunkShipId;
    private boolean gameOver;
    private String message;

    // Компьютерный ход
    private Integer computerRow;
    private Integer computerCol;
    private boolean computerHit;
    private boolean computerSunk;
    private Integer computerSunkShipId;

    // Статистика
    private int playerShots;
    private int playerHits;
    private int computerShots;
    private int computerHits;
    private int playerShipsRemaining;
    private int computerShipsRemaining;
}