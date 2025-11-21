package com.example.battleship_game_BACKEND.shooting;

public enum CellState {
    EMPTY,    // Пустая (ещё не стреляли, нет корабля или скрыт)
    SHIP,     // Клетка с кораблем (видно только у PLAYER-поля)
    HIT,      // Попадание (рисуется крестик)
    MISS,     // Промах (рисуется штриховка)
    SUNK      // Потопленная часть корабля (рисуется корабль «под» крестиком)
}