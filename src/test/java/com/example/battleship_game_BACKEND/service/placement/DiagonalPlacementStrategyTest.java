package com.example.battleship_game_BACKEND.service.placement;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagonalPlacementStrategyTest {

    private final DiagonalPlacementStrategy strategyService = new DiagonalPlacementStrategy();

    @Mock
    private Player mockPlayer;

    @Test
    void createDiagonalStrategy_shouldCreateValidStrategyObject() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);

        assertNotNull(strategy, "Strategy object should not be null");
        assertEquals(mockPlayer, strategy.getPlayer(), "Player should be correctly associated");
        assertEquals("Диагональная стратегия", strategy.getStrategyName(), "Strategy name should match");
        assertNotNull(strategy.getPlacementMatrix(), "Placement matrix should not be null");
        assertFalse(strategy.getPlacementMatrix().isEmpty(), "Placement matrix should not be empty");

        verifyNoInteractions(mockPlayer);
    }

    @Test
    void createDiagonalStrategy_shouldPlaceFourDeckShipCorrectly() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Проверяем 4-палубный корабль в верхнем левом углу (0,0)-(0,3)
        assertShipSegment(matrix, 0, 0, "4-deck start");
        assertShipSegment(matrix, 0, 1, "4-deck middle");
        assertShipSegment(matrix, 0, 2, "4-deck middle");
        assertShipSegment(matrix, 0, 3, "4-deck end");

        // Проверяем, что корабль горизонтальный
        assertTrue(isHorizontalShip(matrix, 0, 0, 4), "4-deck ship should be horizontal");
    }

    @Test
    void createDiagonalStrategy_shouldPlaceThreeDeckShipsCorrectly() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Первый 3-палубный: вертикальный (2,2)-(4,2)
        assertShipSegment(matrix, 2, 2, "3-deck-1 top");
        assertShipSegment(matrix, 3, 2, "3-deck-1 middle");
        assertShipSegment(matrix, 4, 2, "3-deck-1 bottom");
        assertTrue(isVerticalShip(matrix, 2, 2, 3), "First 3-deck ship should be vertical");

        // Второй 3-палубный: вертикальный (4,4)-(6,4)
        assertShipSegment(matrix, 4, 4, "3-deck-2 top");
        assertShipSegment(matrix, 5, 4, "3-deck-2 middle");
        assertShipSegment(matrix, 6, 4, "3-deck-2 bottom");
        assertTrue(isVerticalShip(matrix, 4, 4, 3), "Second 3-deck ship should be vertical");
    }

    @Test
    void createDiagonalStrategy_shouldPlaceTwoDeckShipsCorrectly() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Первый 2-палубный: вертикальный (0,9)-(1,9)
        assertShipSegment(matrix, 0, 9, "2-deck-1 top");
        assertShipSegment(matrix, 1, 9, "2-deck-1 bottom");
        assertTrue(isVerticalShip(matrix, 0, 9, 2), "First 2-deck ship should be vertical");

        // Второй 2-палубный: горизонтальный (7,2)-(7,3)
        assertShipSegment(matrix, 7, 2, "2-deck-2 start");
        assertShipSegment(matrix, 7, 3, "2-deck-2 end");
        assertTrue(isHorizontalShip(matrix, 7, 2, 2), "Second 2-deck ship should be horizontal");

        // Третий 2-палубный: вертикальный (8,7)-(9,7)
        assertShipSegment(matrix, 8, 7, "2-deck-3 top");
        assertShipSegment(matrix, 9, 7, "2-deck-3 bottom");
        assertTrue(isVerticalShip(matrix, 8, 7, 2), "Third 2-deck ship should be vertical");
    }

    @Test
    void createDiagonalStrategy_shouldPlaceSingleDeckShipsCorrectly() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Все однопалубные корабли
        assertShipSegment(matrix, 9, 0, "1-deck bottom-left");
        assertShipSegment(matrix, 9, 9, "1-deck bottom-right");
        assertShipSegment(matrix, 5, 8, "1-deck center-right");
        assertShipSegment(matrix, 3, 6, "1-deck center");

        // Проверяем, что это действительно однопалубные корабли
        assertTrue(isSingleDeckShip(matrix, 9, 0), "Cell (9,0) should be single-deck ship");
        assertTrue(isSingleDeckShip(matrix, 9, 9), "Cell (9,9) should be single-deck ship");
        assertTrue(isSingleDeckShip(matrix, 5, 8), "Cell (5,8) should be single-deck ship");
        assertTrue(isSingleDeckShip(matrix, 3, 6), "Cell (3,6) should be single-deck ship");
    }

    @Test
    void createDiagonalStrategy_shouldHaveExactlyTwentyShipCells() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        int shipCount = countShipCells(matrix);
        assertEquals(20, shipCount, "Total ship cells should be exactly 20");
    }

    @Test
    void createDiagonalStrategy_shouldHaveNoShipOverlaps() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Создаем ожидаемую матрицу с корректным размещением
        Character[][] expected = new Character[10][10];
        initializeEmptyMatrix(expected);

        // Размещаем корабли в ожидаемой матрице
        placeExpectedShip(expected, 0, 0, 4, true);   // 4-палубный
        placeExpectedShip(expected, 2, 2, 3, false);  // 3-палубный 1
        placeExpectedShip(expected, 4, 4, 3, false);  // 3-палубный 2
        placeExpectedShip(expected, 0, 9, 2, false);  // 2-палубный 1
        placeExpectedShip(expected, 7, 2, 2, true);   // 2-палубный 2
        placeExpectedShip(expected, 8, 7, 2, false);  // 2-палубный 3
        expected[9][0] = 'S';  // 1-палубные
        expected[9][9] = 'S';
        expected[5][8] = 'S';
        expected[3][6] = 'S';

        // Сравниваем матрицы
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                assertEquals(expected[i][j], matrix[i][j],
                        "Cell [" + i + "][" + j + "] has unexpected value. " +
                                "Expected: " + expected[i][j] + ", Actual: " + matrix[i][j]);
            }
        }
    }

    @Test
    void createDiagonalStrategy_shouldFollowDiagonalStrategyPattern() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Проверяем наличие кораблей в ключевых точках диагоналей
        boolean hasShipsNearMainDiagonal = false;
        boolean hasShipsNearAntiDiagonal = false;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    // Проверяем близость к главной диагонали (i ≈ j)
                    if (Math.abs(i - j) <= 1) {
                        hasShipsNearMainDiagonal = true;
                    }
                    // Проверяем близость к побочной диагонали (i + j ≈ 9)
                    if (Math.abs(i + j - 9) <= 1) {
                        hasShipsNearAntiDiagonal = true;
                    }
                }
            }
        }

        assertTrue(hasShipsNearMainDiagonal, "Should have ships near main diagonal");
        assertTrue(hasShipsNearAntiDiagonal, "Should have ships near anti-diagonal");
    }

    @Test
    void createDiagonalStrategy_shouldHaveValidMatrixFormat() {
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        String placementMatrix = strategy.getPlacementMatrix();

        assertNotNull(placementMatrix, "Matrix string should not be null");
        String[] rows = placementMatrix.split(";");
        assertEquals(10, rows.length, "Matrix should have exactly 10 rows");

        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            assertNotNull(row, "Row " + i + " should not be null");
            String[] cells = row.split(",");
            assertEquals(10, cells.length, "Row " + i + " should have exactly 10 cells");

            for (int j = 0; j < cells.length; j++) {
                String cell = cells[j];
                assertNotNull(cell, "Cell [" + i + "][" + j + "] should not be null");
                assertEquals(1, cell.length(), "Cell [" + i + "][" + j + "] should contain exactly one character");
                char content = cell.charAt(0);
                assertTrue(content == ' ' || content == 'S',
                        "Cell [" + i + "][" + j + "] should contain only ' ' or 'S', found: '" + content + "'");
            }
        }
    }

    @Test
    void placeShip_shouldThrowExceptionForOutOfBoundsHorizontalShip() {
        Character[][] matrix = new Character[10][10];
        initializeEmptyMatrix(matrix);

        // Попытка разместить корабль, выходящий за правую границу
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            strategyService.placeShip(matrix, 0, 8, 3, true);
        });

        assertTrue(exception.getMessage().contains("выходит за границы"),
                "Exception message should indicate boundary violation");
    }

    @Test
    void placeShip_shouldThrowExceptionForOutOfBoundsVerticalShip() {
        Character[][] matrix = new Character[10][10];
        initializeEmptyMatrix(matrix);

        // Попытка разместить корабль, выходящий за нижнюю границу
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            strategyService.placeShip(matrix, 8, 0, 3, false);
        });

        assertTrue(exception.getMessage().contains("выходит за границы"),
                "Exception message should indicate boundary violation");
    }

    @Test
    void placeShip_shouldThrowExceptionForShipOverlap() {
        Character[][] matrix = new Character[10][10];
        initializeEmptyMatrix(matrix);

        // Размещаем первый корабль
        strategyService.placeShip(matrix, 0, 0, 4, true);

        // Пытаемся разместить второй корабль с пересечением
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            strategyService.placeShip(matrix, 0, 2, 3, true); // Пересечение в (0,2) и (0,3)
        });

        assertTrue(exception.getMessage().contains("Конфликт кораблей"),
                "Exception message should indicate ship conflict");
    }

    // Вспомогательные методы
    private Character[][] convertStringToMatrix(String matrixString) {
        Character[][] matrix = new Character[10][10];
        String[] rows = matrixString.split(";");

        for (int i = 0; i < 10 && i < rows.length; i++) {
            String[] cells = rows[i].split(",");
            for (int j = 0; j < 10 && j < cells.length; j++) {
                matrix[i][j] = cells[j].charAt(0);
            }
        }

        // Инициализируем пустые ячейки
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == null) {
                    matrix[i][j] = ' ';
                }
            }
        }

        return matrix;
    }

    private void initializeEmptyMatrix(Character[][] matrix) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }
    }

    private void placeExpectedShip(Character[][] matrix, int startRow, int startCol, int length, boolean horizontal) {
        if (horizontal) {
            for (int j = startCol; j < startCol + length; j++) {
                matrix[startRow][j] = 'S';
            }
        } else {
            for (int i = startRow; i < startRow + length; i++) {
                matrix[i][startCol] = 'S';
            }
        }
    }

    private int countShipCells(Character[][] matrix) {
        int count = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    count++;
                }
            }
        }
        return count;
    }

    private void assertShipSegment(Character[][] matrix, int row, int col, String description) {
        assertTrue(row >= 0 && row < 10 && col >= 0 && col < 10,
                "Invalid coordinates [" + row + "][" + col + "] for " + description);
        assertEquals('S', matrix[row][col],
                "Expected ship segment 'S' at position [" + row + "][" + col + "] for " + description);
    }

    private boolean isHorizontalShip(Character[][] matrix, int row, int col, int length) {
        for (int j = col; j < col + length; j++) {
            if (matrix[row][j] != 'S') return false;
        }
        return true;
    }

    private boolean isVerticalShip(Character[][] matrix, int row, int col, int length) {
        for (int i = row; i < row + length; i++) {
            if (matrix[i][col] != 'S') return false;
        }
        return true;
    }

    private boolean isSingleDeckShip(Character[][] matrix, int row, int col) {
        // Проверяем, что нет соседних корабельных клеток
        return (!hasShipNeighbor(matrix, row-1, col) && // сверху
                !hasShipNeighbor(matrix, row+1, col) && // снизу
                !hasShipNeighbor(matrix, row, col-1) && // слева
                !hasShipNeighbor(matrix, row, col+1));  // справа
    }

    private boolean hasShipNeighbor(Character[][] matrix, int row, int col) {
        if (row < 0 || row >= 10 || col < 0 || col >= 10) return false;
        return matrix[row][col] == 'S';
    }
}