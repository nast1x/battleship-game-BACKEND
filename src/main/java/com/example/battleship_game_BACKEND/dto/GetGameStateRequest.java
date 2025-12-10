// GetGameStateRequest.java
package com.example.battleship_game_BACKEND.dto;

import lombok.Data;

@Data
public class GetGameStateRequest {
    private Long gameId;
    private Long playerId;
}