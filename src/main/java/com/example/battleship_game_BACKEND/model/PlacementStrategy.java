package com.example.battleship_game_BACKEND.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "placement_strategy")
@Data
public class PlacementStrategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "strategy_id")
    private Long strategyId;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "strategy_name", nullable = false, length = 100)
    private String strategyName;

    @Column(name = "placement_matrix", nullable = false, columnDefinition = "TEXT")
    private String placementMatrix;

    // Методы для преобразования между String и Character[][]
    public Character[][] getPlacementMatrixAsArray() {
        if (placementMatrix == null || placementMatrix.isEmpty()) {
            return new Character[10][10]; // Возвращаем пустую матрицу 10x10 по умолчанию
        }

        String[] rows = placementMatrix.split(";");
        Character[][] matrix = new Character[rows.length][];

        for (int i = 0; i < rows.length; i++) {
            String[] cols = rows[i].split(",");
            matrix[i] = new Character[cols.length];
            for (int j = 0; j < cols.length; j++) {
                if (cols[j].length() > 0) {
                    matrix[i][j] = cols[j].charAt(0);
                } else {
                    matrix[i][j] = ' '; // Пустая клетка по умолчанию
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