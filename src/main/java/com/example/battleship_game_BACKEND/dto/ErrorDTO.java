// ErrorDTO.java
package com.example.battleship_game_BACKEND.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class ErrorDTO {
    private boolean error = true;
    private String message;
    private Long gameId;
    private Long playerId;

    // Конструктор для удобства
    public ErrorDTO(String message, Long gameId, Long playerId) {
        this.message = message;
        this.gameId = gameId;
        this.playerId = playerId;
    }

    public ErrorDTO(String message) {
        this.message = message;
    }
}