package com.example.battleship_game_BACKEND.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "computer_game")
@Data
public class ComputerGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "computer_game_id")
    private Long computerGameId;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_status", columnDefinition = "game_status_enum")
    private GameStatus gameStatus = GameStatus.WAITING;

    @Column(name = "player_turn", nullable = false)
    private boolean playerTurn = true;

    @Column(name = "computer_strategy", length = 50)
    private String computerStrategy;

    @Column(name = "placement_strategy", length = 50)
    private String placementStrategy;

    // Доска игрока (храним в формате JSON)
    @Column(name = "player_board", columnDefinition = "TEXT")
    private String playerBoard;

    // Доска компьютера (храним в формате JSON)
    @Column(name = "computer_board", columnDefinition = "TEXT")
    private String computerBoard;

    // Выстрелы игрока (храним как JSON)
    @Column(name = "player_shots", columnDefinition = "TEXT")
    private String playerShots;

    // Выстрелы компьютера (храним как JSON)
    @Column(name = "computer_shots", columnDefinition = "TEXT")
    private String computerShots;

    // Корабли игрока (храним как JSON)
    @Column(name = "player_ships", columnDefinition = "TEXT")
    private String playerShipsJson;

    // Корабли компьютера (храним как JSON)
    @Column(name = "computer_ships", columnDefinition = "TEXT")
    private String computerShipsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_move_at")
    private LocalDateTime lastMoveAt = LocalDateTime.now();

    @Column(name = "player_hits")
    private int playerHits = 0;

    @Column(name = "player_misses")
    private int playerMisses = 0;

    @Column(name = "computer_hits")
    private int computerHits = 0;

    @Column(name = "computer_misses")
    private int computerMisses = 0;

    // Вспомогательные методы для работы с досками
    public Character[][] getPlayerBoardAsMatrix() {
        return convertJsonToMatrix(playerBoard);
    }

    public void setPlayerBoardFromMatrix(Character[][] matrix) {
        this.playerBoard = convertMatrixToJson(matrix);
    }

    public Character[][] getComputerBoardAsMatrix() {
        return convertJsonToMatrix(computerBoard);
    }

    public void setComputerBoardFromMatrix(Character[][] matrix) {
        this.computerBoard = convertMatrixToJson(matrix);
    }

    private Character[][] convertJsonToMatrix(String json) {
        if (json == null || json.isEmpty()) {
            return createEmptyBoard();
        }

        try {
            // Простое преобразование: каждая строка - это строка доски
            String[] rows = json.split(";");
            Character[][] matrix = new Character[rows.length][];

            for (int i = 0; i < rows.length; i++) {
                String row = rows[i];
                matrix[i] = new Character[row.length()];
                for (int j = 0; j < row.length(); j++) {
                    matrix[i][j] = row.charAt(j);
                }
            }
            return matrix;
        } catch (Exception e) {
            return createEmptyBoard();
        }
    }

    private String convertMatrixToJson(Character[][] matrix) {
        if (matrix == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            if (i > 0) sb.append(";");
            for (int j = 0; j < matrix[i].length; j++) {
                sb.append(matrix[i][j] != null ? matrix[i][j] : ' ');
            }
        }
        return sb.toString();
    }

    private Character[][] createEmptyBoard() {
        Character[][] board = new Character[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                board[i][j] = ' ';
            }
        }
        return board;
    }
}