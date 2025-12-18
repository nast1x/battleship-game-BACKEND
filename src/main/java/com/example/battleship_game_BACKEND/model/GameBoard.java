package com.example.battleship_game_BACKEND.model;

import com.example.battleship_game_BACKEND.dto.ShipPlacementDto;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_board")
@Data
@ToString(exclude = "ships") // Исключаем ships из toString, чтобы избежать цикличности
public class GameBoard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_board_id")
    private Long gameBoardId;

    @Column(name = "placement_matrix", nullable = false, columnDefinition = "TEXT")
    private String placementMatrix;

    // Transient-поле для хранения кораблей в памяти (не сохраняется в БД)
    @Transient
    private List<ShipPlacementDto> ships = new ArrayList<>();

    // Transient-поле для метаданных доски
    @Transient
    private GameBoardMetadata metadata = new GameBoardMetadata();

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

    /**
     * Загружает корабли из матрицы (вызывается при загрузке доски)
     */
    public void loadShipsFromMatrix() {
        this.ships = parseShipsFromMatrix();
    }

    /**
     * Сохраняет корабли в матрицу (вызывается перед сохранением доски)
     */
    public void saveShipsToMatrix() {
        Character[][] matrix = createEmptyMatrix(10, 10);

        // Размещаем корабли в матрице
        for (ShipPlacementDto ship : ships) {
            for (int i = 0; i < ship.size(); i++) {
                int row = ship.row() + (ship.vertical() ? i : 0);
                int col = ship.col() + (ship.vertical() ? 0 : i);
                if (row >= 0 && row < 10 && col >= 0 && col < 10) {
                    matrix[row][col] = 'S';
                }
            }
        }

        setPlacementMatrixFromArray(matrix);
    }

    /**
     * Парсинг кораблей из матрицы
     */
    private List<ShipPlacementDto> parseShipsFromMatrix() {
        List<ShipPlacementDto> shipList = new ArrayList<>();
        Character[][] matrix = getPlacementMatrixAsArray();
        boolean[][] visited = new boolean[10][10];

        // Проходим по всей матрице
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S' && !visited[i][j]) {
                    ShipPlacementDto ship = detectShip(matrix, visited, i, j, shipList);
                    shipList.add(ship);
                }
            }
        }

        return shipList;
    }

    /**
     * Обнаружение корабля в матрице
     */
    private ShipPlacementDto detectShip(Character[][] matrix, boolean[][] visited,
                                        int startRow, int startCol, List<ShipPlacementDto> shipList) {
        // Проверяем направление корабля
        boolean horizontal = true;
        boolean vertical = true;

        // Проверяем возможность горизонтального расположения
        if (startCol < 9 && matrix[startRow][startCol + 1] == 'S') {
            vertical = false;
        } else if (startRow < 9 && matrix[startRow + 1][startCol] == 'S') {
            horizontal = false;
        }

        int size = 0;
        int row = startRow;
        int col = startCol;

        if (horizontal && !vertical) {
            // Горизонтальный корабль
            while (col < 10 && matrix[row][col] == 'S' && !visited[row][col]) {
                visited[row][col] = true;
                size++;
                col++;
            }
            return new ShipPlacementDto(shipList.size() + 1, size, startRow, startCol, false);
        } else if (!horizontal && vertical) {
            // Вертикальный корабль
            while (row < 10 && matrix[row][col] == 'S' && !visited[row][col]) {
                visited[row][col] = true;
                size++;
                row++;
            }
            return new ShipPlacementDto(shipList.size() + 1, size, startRow, startCol, true);
        } else {
            // Корабль размером 1
            visited[startRow][startCol] = true;
            return new ShipPlacementDto(shipList.size() + 1, 1, startRow, startCol, false);
        }
    }

    /**
     * Создает пустую матрицу
     */
    private Character[][] createEmptyMatrix(int rows, int cols) {
        Character[][] matrix = new Character[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = ' ';
            }
        }
        return matrix;
    }

    /**
     * Класс для метаданных доски
     */
    @Data
    public static class GameBoardMetadata {
        private String placementStrategy;
        private Integer hits = 0;
        private Integer misses = 0;
        private Boolean allShipsPlaced = false;
        private Long lastUpdated = System.currentTimeMillis();
    }
}