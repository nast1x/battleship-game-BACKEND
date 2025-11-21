package com.example.battleship_game_BACKEND.model;


import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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

    @Column(name = "placement_data", nullable = false, columnDefinition = "TEXT")
    private String placementData; // JSON с данными расстановки кораблей

    // Методы для работы с данными расстановки
    public List<ShipPlacement> getPlacementDataAsList() {
        if (placementData == null || placementData.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(placementData,
                    mapper.getTypeFactory().constructCollectionType(List.class, ShipPlacement.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse placement data", e);
        }
    }

    public void setPlacementDataFromList(List<ShipPlacement> placements) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.placementData = mapper.writeValueAsString(placements);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize placement data", e);
        }
    }

    // Дополнительный метод для обратной совместимости с матрицей
    public Character[][] getPlacementMatrixAsArray() {
        List<ShipPlacement> placements = getPlacementDataAsList();
        Character[][] matrix = new Character[10][10];

        // Инициализируем пустую матрицу
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        // Размещаем корабли в матрице
        for (ShipPlacement placement : placements) {
            int dx = placement.isVertical() ? 0 : 1;
            int dy = placement.isVertical() ? 1 : 0;

            for (int k = 0; k < placement.getSize(); k++) {
                int x = placement.getCol() + dx * k;
                int y = placement.getRow() + dy * k;
                if (x < 10 && y < 10) {
                    // Используем символ, представляющий корабль
                    matrix[y][x] = 'S';
                }
            }
        }

        return matrix;
    }

    public void setPlacementMatrixFromArray(Character[][] matrix) {
        if (matrix == null || matrix.length != 10 || matrix[0].length != 10) {
            this.placementData = "[]";
            return;
        }

        List<ShipPlacement> placements = new ArrayList<>();
        boolean[][] processed = new boolean[10][10];

        // Карта для отслеживания использованных shipId по размерам
        Map<Integer, Integer> sizeCount = new HashMap<>();
        sizeCount.put(4, 0);
        sizeCount.put(3, 0);
        sizeCount.put(2, 0);
        sizeCount.put(1, 0);

        // Сначала находим большие корабли (размером > 1)
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (!processed[i][j] && isShipCell(matrix[i][j])) {
                    ShipInfo shipInfo = detectShip(matrix, processed, i, j);
                    if (shipInfo != null) {
                        int shipId = generateShipId(shipInfo.size, sizeCount);
                        placements.add(new ShipPlacement(shipId, shipInfo.size,
                                shipInfo.startRow, shipInfo.startCol,
                                !shipInfo.horizontal));
                        sizeCount.put(shipInfo.size, sizeCount.get(shipInfo.size) + 1);
                    }
                }
            }
        }

        setPlacementDataFromList(placements);
    }

    private boolean isShipCell(Character cell) {
        return cell != null && cell != ' ';
    }

    private ShipInfo detectShip(Character[][] matrix, boolean[][] processed, int startRow, int startCol) {
        // Проверяем горизонтальное направление
        int horizontalLength = getShipLength(matrix, processed, startRow, startCol, 0, 1);
        int verticalLength = getShipLength(matrix, processed, startRow, startCol, 1, 0);

        if (horizontalLength > verticalLength) {
            // Горизонтальный корабль
            markShipProcessed(processed, startRow, startCol, horizontalLength, true);
            return new ShipInfo(startRow, startCol, horizontalLength, true);
        } else if (verticalLength > 1) {
            // Вертикальный корабль
            markShipProcessed(processed, startRow, startCol, verticalLength, false);
            return new ShipInfo(startRow, startCol, verticalLength, false);
        } else {
            // Корабль размером 1
            processed[startRow][startCol] = true;
            return new ShipInfo(startRow, startCol, 1, true);
        }
    }

    private int getShipLength(Character[][] matrix, boolean[][] processed, int row, int col, int rowStep, int colStep) {
        int length = 0;
        int r = row;
        int c = col;

        while (r >= 0 && r < 10 && c >= 0 && c < 10 &&
                !processed[r][c] && isShipCell(matrix[r][c])) {
            length++;
            r += rowStep;
            c += colStep;
        }

        return length;
    }

    private void markShipProcessed(boolean[][] processed, int row, int col, int length, boolean horizontal) {
        int rowStep = horizontal ? 0 : 1;
        int colStep = horizontal ? 1 : 0;

        for (int i = 0; i < length; i++) {
            int r = row + rowStep * i;
            int c = col + colStep * i;
            if (r >= 0 && r < 10 && c >= 0 && c < 10) {
                processed[r][c] = true;
            }
        }
    }

    private int generateShipId(int size, Map<Integer, Integer> sizeCount) {
        // Генерируем shipId на основе размера и количества уже найденных кораблей этого размера
        return switch (size) {
            case 4 -> 1;
            case 3 -> sizeCount.get(3) == 0 ? 2 : 3;
            case 2 -> 4 + sizeCount.get(2); // 4, 5, 6
            case 1 -> 7 + sizeCount.get(1); // 7, 8, 9, 10
            default -> 0;
        };
    }

    // Вспомогательный класс для информации о корабле
    private static class ShipInfo {
        int startRow;
        int startCol;
        int size;
        boolean horizontal;

        ShipInfo(int startRow, int startCol, int size, boolean horizontal) {
            this.startRow = startRow;
            this.startCol = startCol;
            this.size = size;
            this.horizontal = horizontal;
        }
    }
}