package com.example.battleship_game_BACKEND.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "game_board")
@Data
public class GameBoard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_board_id")
    private Long gameBoardId;

    @Column(name = "placement_matrix", nullable = false, columnDefinition = "TEXT")
    private String placementMatrix;

    @Column(name = "game_state_json", columnDefinition = "TEXT")
    private String gameStateJson;

    public Character[][] getPlacementMatrixAsArray() {
        if (placementMatrix == null || placementMatrix.isEmpty()) {
            return new Character[10][10];
        }

        String[] rows = placementMatrix.split(";");
        Character[][] matrix = new Character[rows.length][];

        for (int i = 0; i < rows.length; i++) {
            String[] cols = rows[i].split(",");
            matrix[i] = new Character[cols.length];
            for (int j = 0; j < cols.length; j++) {
                if (!cols[j].trim().isEmpty()) {
                    matrix[i][j] = cols[j].charAt(0);
                } else {
                    matrix[i][j] = ' ';
                }
            }
        }
        return matrix;
    }

    public void setPlacementMatrixFromArray(Character[][] matrix) {
        if (matrix == null) {
            this.placementMatrix = "";
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            if (i > 0) sb.append(";");
            for (int j = 0; j < matrix[i].length; j++) {
                if (j > 0) sb.append(",");
                sb.append(matrix[i][j] != null ? matrix[i][j] : ' ');
            }
        }
        this.placementMatrix = sb.toString();
    }
}