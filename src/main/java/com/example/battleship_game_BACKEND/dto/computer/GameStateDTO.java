package com.example.battleship_game_BACKEND.dto.computer;

import com.example.battleship_game_BACKEND.model.GameStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GameStateDTO {
    private Long gameId;
    private GameStatus gameStatus;
    private String currentPlayerNickname;
    private String opponentNickname;
    private boolean isPlayerTurn;
    private PlayerBoardDTO playerBoard;
    private PlayerBoardDTO opponentBoard;
    private String message;
    private List<ShipDTO> playerShips;
    private List<ShipDTO> opponentShips;
    private GameResultDTO result;
}

@Data
class PlayerBoardDTO {
    private Long boardId;
    private String nickname;
    private String boardType; // OWN или OPPONENT_VIEW
    private Character[][] cells;
    private boolean[][] hitCells;
    private boolean[][] missCells;
}

@Data
class ShipDTO {
    private int size;
    private boolean isSunk;
    private List<CoordinateDTO> coordinates;
    private boolean isHorizontal;
}

@Data
class CoordinateDTO {
    private int row;
    private int col;
}

@Data
class GameResultDTO {
    private String winnerNickname;
    private String resultText;
    private LocalDateTime endDate;
    private int playerScore;
    private int opponentScore;
}

@Data
class ShotRequestDTO {
    private Long gameId;
    private int row;
    private int col;
}

@Data
class ShotResponseDTO {
    private boolean success;
    private boolean hit;
    private boolean sunk;
    private ShipDTO sunkShip;
    private CoordinateDTO coordinate;
    private String message;
    private GameStateDTO gameState;
}

@Data
class GameSetupRequestDTO {
    private Long playerId;
    private String placementStrategyName; // "RANDOM", "COASTAL", "DIAGONAL", "HALF_LEFT", "HALF_RIGHT"
    private String difficulty; // "EASY", "MEDIUM", "HARD"
}

@Data
class NewGameResponseDTO {
    private Long gameId;
    private GameStateDTO gameState;
    private String message;
}