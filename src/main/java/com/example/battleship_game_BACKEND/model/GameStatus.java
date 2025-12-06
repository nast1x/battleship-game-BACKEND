package com.example.battleship_game_BACKEND.model;

public class GameStatus {
    private boolean isPlayerTurn;
    private boolean isBattleOver;
    private Difficulty difficulty;

    public GameStatus(boolean isPlayerTurn, boolean isBattleOver, Difficulty difficulty) {
        this.isPlayerTurn = isPlayerTurn;
        this.isBattleOver = isBattleOver;
        this.difficulty = difficulty;
    }

    // Геттеры и сеттеры
    public boolean isPlayerTurn() {
        return isPlayerTurn;
    }

    public void setPlayerTurn(boolean playerTurn) {
        isPlayerTurn = playerTurn;
    }

    public boolean isBattleOver() {
        return isBattleOver;
    }

    public void setBattleOver(boolean battleOver) {
        isBattleOver = battleOver;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
}