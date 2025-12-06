package com.example.battleship_game_BACKEND.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class StartGameRequest {
    // Геттеры и сеттеры
    private List<ShipPlacement> playerShips;
    private List<ShipPlacement> computerShips;
    private Difficulty difficulty;

    public StartGameRequest() {}

    public StartGameRequest(List<ShipPlacement> playerShips, List<ShipPlacement> computerShips, Difficulty difficulty) {
        this.playerShips = playerShips;
        this.computerShips = computerShips;
        this.difficulty = difficulty;
    }

}