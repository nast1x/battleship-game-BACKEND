package com.example.battleship_game_BACKEND.exeption;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(String message) {
        super(message);
    }
}