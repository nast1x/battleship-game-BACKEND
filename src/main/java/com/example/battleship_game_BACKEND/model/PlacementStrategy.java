package com.example.battleship_game_BACKEND.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

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

    @Column(name = "placement_matrix", nullable = false, columnDefinition = "CHAR(10)[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] placementMatrix;

    // Транзиентное поле для работы с данными расстановки
    @Transient
    private List<ShipPlacement> placementData;

    // Record для хранения информации о корабле
    public record ShipPlacement(int shipId, int size, int row, int col, boolean vertical) {
        public ShipPlacement {
            if (size <= 0 || row < 0 || row >= 10 || col < 0 || col >= 10) {
                throw new IllegalArgumentException("Invalid ship placement data");
            }
        }
    }

    // Инициализация при загрузке из БД
    @PostLoad
    public void initPlacementData() {
        try {
            this.placementData = parsePlacementDataFromMatrix();
        } catch (Exception e) {
            this.placementData = new ArrayList<>();
        }
    }

    // Сохранение перед записью в БД
    @PrePersist
    @PreUpdate
    public void updatePlacementMatrix() {
        if (placementData != null && !placementData.isEmpty()) {
            updateMatrixFromPlacementData();
        } else if (placementMatrix == null || placementMatrix.length == 0) {
            // Создаем пустую матрицу по умолчанию
            placementMatrix = createEmptyMatrix();
        }
    }


    // Метод для получения данных расстановки в виде списка
    public List<ShipPlacement> getPlacementDataAsList() {
        if (placementData == null) {
            placementData = parsePlacementDataFromMatrix();
        }
        return new ArrayList<>(placementData);
    }

    // Метод для установки данных расстановки из списка
    public void setPlacementDataFromList(List<ShipPlacement> placements) {
        this.placementData = new ArrayList<>(placements);
        updateMatrixFromPlacementData();
    }

    // Конвертация матрицы в список кораблей
    private List<ShipPlacement> parsePlacementDataFromMatrix() {
        if (placementMatrix == null || placementMatrix.length == 0) {
            return new ArrayList<>();
        }

        Character[][] matrix = getPlacementMatrixAsArray();
        List<ShipPlacement> placements = new ArrayList<>();
        boolean[][] visited = new boolean[10][10];

        // Стандартные корабли для морского боя: 1x4, 2x3, 3x2, 4x1
        int[] shipSizes = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
        int shipId = 1;

        for (int size : shipSizes) {
            boolean found = findAndMarkShip(matrix, visited, size, placements, shipId);
            if (found) {
                shipId++;
            }
        }

        return placements;
    }

    // Поиск и маркировка корабля определенного размера
    private boolean findAndMarkShip(Character[][] matrix, boolean[][] visited,
                                    int size, List<ShipPlacement> placements, int shipId) {
        // Поиск горизонтального корабля
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j <= 10 - size; j++) {
                if (isValidHorizontalShip(matrix, visited, i, j, size)) {
                    markVisited(visited, i, j, size, true);
                    placements.add(new ShipPlacement(shipId, size, i, j, false));
                    return true;
                }
            }
        }

        // Поиск вертикального корабля
        for (int i = 0; i <= 10 - size; i++) {
            for (int j = 0; j < 10; j++) {
                if (isValidVerticalShip(matrix, visited, i, j, size)) {
                    markVisited(visited, i, j, size, false);
                    placements.add(new ShipPlacement(shipId, size, i, j, true));
                    return true;
                }
            }
        }

        return false;
    }

    // Проверка возможности размещения горизонтального корабля
    private boolean isValidHorizontalShip(Character[][] matrix, boolean[][] visited,
                                          int row, int col, int size) {
        // Проверяем, что все клетки свободны и не посещены
        for (int k = 0; k < size; k++) {
            if (matrix[row][col + k] != 'S' || visited[row][col + k]) {
                return false;
            }
        }

        // Проверяем границы (не должно быть кораблей рядом)
        for (int k = -1; k <= size; k++) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int r = row + dr;
                    int c = col + k + dc;
                    if (r >= 0 && r < 10 && c >= 0 && c < 10) {
                        if (matrix[r][c] == 'S' && !(dr == 0 && k >= 0 && k < size)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    // Проверка возможности размещения вертикального корабля
    private boolean isValidVerticalShip(Character[][] matrix, boolean[][] visited,
                                        int row, int col, int size) {
        // Проверяем, что все клетки свободны и не посещены
        for (int k = 0; k < size; k++) {
            if (matrix[row + k][col] != 'S' || visited[row + k][col]) {
                return false;
            }
        }

        // Проверяем границы
        for (int k = -1; k <= size; k++) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int r = row + k + dr;
                    int c = col + dc;
                    if (r >= 0 && r < 10 && c >= 0 && c < 10) {
                        if (matrix[r][c] == 'S' && !(k >= 0 && k < size && dc == 0)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    // Отметить посещенные клетки
    private void markVisited(boolean[][] visited, int row, int col, int size, boolean horizontal) {
        for (int k = 0; k < size; k++) {
            if (horizontal) {
                visited[row][col + k] = true;
            } else {
                visited[row + k][col] = true;
            }
        }
    }

    // Обновление матрицы на основе данных о кораблях
    private void updateMatrixFromPlacementData() {
        Character[][] matrix = createEmptyCharacterMatrix();

        if (placementData != null) {
            for (ShipPlacement placement : placementData) {
                int dx = placement.vertical() ? 1 : 0;
                int dy = placement.vertical() ? 0 : 1;

                for (int k = 0; k < placement.size(); k++) {
                    int x = placement.row() + dx * k;
                    int y = placement.col() + dy * k;

                    if (x >= 0 && x < 10 && y >= 0 && y < 10) {
                        matrix[x][y] = 'S'; // 'S' - корабль
                    }
                }
            }
        }

        setPlacementMatrixFromArray(matrix);
    }

    // Получение матрицы как двумерного массива символов
    public Character[][] getPlacementMatrixAsArray() {
        if (placementMatrix == null || placementMatrix.length == 0) {
            return createEmptyCharacterMatrix();
        }

        Character[][] matrix = new Character[10][10];
        for (int i = 0; i < 10 && i < placementMatrix.length; i++) {
            String row = placementMatrix[i];
            for (int j = 0; j < 10 && j < row.length(); j++) {
                matrix[i][j] = row.charAt(j);
            }
            // Заполняем остальные ячейки пробелами
            for (int j = row.length(); j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        // Заполняем недостающие строки
        for (int i = placementMatrix.length; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        return matrix;
    }

    // Установка матрицы из двумерного массива символов
    public void setPlacementMatrixFromArray(Character[][] matrix) {
        if (matrix == null) {
            this.placementMatrix = createEmptyMatrix();
            return;
        }

        String[] rows = new String[10];
        for (int i = 0; i < 10; i++) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < 10; j++) {
                row.append(matrix[i][j] != null ? matrix[i][j] : ' ');
            }
            rows[i] = row.toString();
        }
        this.placementMatrix = rows;

        // Обновляем placementData на основе новой матрицы
        this.placementData = parsePlacementDataFromMatrix();
    }

    // Создание пустой матрицы символов
    private Character[][] createEmptyCharacterMatrix() {
        Character[][] matrix = new Character[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }
        return matrix;
    }

    // Создание пустой матрицы строк
    private String[] createEmptyMatrix() {
        String[] matrix = new String[10];
        for (int i = 0; i < 10; i++) {
            matrix[i] = "          "; // 10 пробелов
        }
        return matrix;
    }

    // Дополнительный метод для быстрого создания стратегии с кораблями
    public static PlacementStrategy createWithShips(Player player, String strategyName,
                                                    List<ShipPlacement> ships) {
        PlacementStrategy strategy = new PlacementStrategy();
        strategy.setPlayer(player);
        strategy.setStrategyName(strategyName);
        strategy.setPlacementDataFromList(ships);
        return strategy;
    }
}