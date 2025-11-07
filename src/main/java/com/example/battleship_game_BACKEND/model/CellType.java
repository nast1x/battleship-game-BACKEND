package com.example.battleship_game_BACKEND.model;

public enum CellType {
    SHIP,      // корабль
    SEA,       // море (пустая клетка)
    MISS,      // промах
    HIT        // попадание
}