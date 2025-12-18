package com.example.battleship_game_BACKEND.service.placement;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HalfPlacementStrategyTest {

    private final HalfPlacementStrategy strategyService = new HalfPlacementStrategy();

    @Mock
    private Player mockPlayer;

    @Test
    void createHalfStrategy_shouldCreateValidLeftHalfStrategy() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);

        assertNotNull(strategy, "Strategy object should not be null");
        assertEquals(mockPlayer, strategy.getPlayer(), "Player should be associated with strategy");
        assertEquals("Половинчатая стратегия", strategy.getStrategyName(), "Strategy name should be correct");
        assertNotNull(strategy.getPlacementMatrix(), "Placement matrix should not be null");
        assertFalse(strategy.getPlacementMatrix().isEmpty(), "Placement matrix should not be empty");

        verifyNoInteractions(mockPlayer);
    }

    @Test
    void createHalfStrategyRight_shouldCreateValidRightHalfStrategy() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);

        assertNotNull(strategy, "Strategy object should not be null");
        assertEquals(mockPlayer, strategy.getPlayer(), "Player should be associated with strategy");
        assertEquals("Половинчатая стратегия (правая)", strategy.getStrategyName(), "Strategy name should be correct");
        assertNotNull(strategy.getPlacementMatrix(), "Placement matrix should not be null");
        assertFalse(strategy.getPlacementMatrix().isEmpty(), "Placement matrix should not be empty");

        verifyNoInteractions(mockPlayer);
    }

    @Test
    void createHalfStrategy_shouldPlaceFourDeckShipCorrectlyInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        assertShipSegment(matrix, 2, 0);
        assertShipSegment(matrix, 2, 1);
        assertShipSegment(matrix, 2, 2);
        assertShipSegment(matrix, 2, 3);
    }

    @Test
    void createHalfStrategyRight_shouldPlaceFourDeckShipCorrectlyInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        assertShipSegment(matrix, 2, 5);
        assertShipSegment(matrix, 2, 6);
        assertShipSegment(matrix, 2, 7);
        assertShipSegment(matrix, 2, 8);
    }

    @Test
    void createHalfStrategy_shouldPlaceAllThreeDeckShipsCorrectlyInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // First 3-deck ship (horizontal)
        assertShipSegment(matrix, 5, 0);
        assertShipSegment(matrix, 5, 1);
        assertShipSegment(matrix, 5, 2);

        // Second 3-deck ship (vertical)
        assertShipSegment(matrix, 0, 4);
        assertShipSegment(matrix, 1, 4);
        assertShipSegment(matrix, 2, 4);
    }

    @Test
    void createHalfStrategyRight_shouldPlaceAllThreeDeckShipsCorrectlyInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // First 3-deck ship (horizontal)
        assertShipSegment(matrix, 5, 5);
        assertShipSegment(matrix, 5, 6);
        assertShipSegment(matrix, 5, 7);

        // Second 3-deck ship (vertical)
        assertShipSegment(matrix, 0, 9);
        assertShipSegment(matrix, 1, 9);
        assertShipSegment(matrix, 2, 9);
    }

    @Test
    void createHalfStrategy_shouldPlaceAllTwoDeckShipsCorrectlyInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 2-deck ships
        assertShipSegment(matrix, 7, 0);
        assertShipSegment(matrix, 7, 1);

        assertShipSegment(matrix, 8, 3);
        assertShipSegment(matrix, 9, 3);

        // Fixed position (no longer overlaps with 3-deck ship)
        assertShipSegment(matrix, 4, 2);
        assertShipSegment(matrix, 4, 3);
    }

    @Test
    void createHalfStrategyRight_shouldPlaceAllTwoDeckShipsCorrectlyInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 2-deck ships
        assertShipSegment(matrix, 7, 5);
        assertShipSegment(matrix, 7, 6);

        assertShipSegment(matrix, 8, 8);
        assertShipSegment(matrix, 9, 8);

        // Fixed position (no longer overlaps with 3-deck ship)
        assertShipSegment(matrix, 4, 7);
        assertShipSegment(matrix, 4, 8);
    }

    @Test
    void createHalfStrategy_shouldPlaceAllSingleDeckShipsCorrectlyInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 1-deck ships
        assertShipSegment(matrix, 0, 0);
        assertShipSegment(matrix, 9, 0);
        assertShipSegment(matrix, 3, 1);
        assertShipSegment(matrix, 6, 4);
    }

    @Test
    void createHalfStrategyRight_shouldPlaceAllSingleDeckShipsCorrectlyInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 1-deck ships
        assertShipSegment(matrix, 0, 5);
        assertShipSegment(matrix, 9, 5);
        assertShipSegment(matrix, 3, 6);
        assertShipSegment(matrix, 6, 9);
    }

    @Test
    void createHalfStrategy_shouldHaveExactlyTwentyShipCellsInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        int shipCount = countShipCells(matrix);
        assertEquals(20, shipCount, "Left half should have exactly 20 ship cells");
    }

    @Test
    void createHalfStrategyRight_shouldHaveExactlyTwentyShipCellsInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        int shipCount = countShipCells(matrix);
        assertEquals(20, shipCount, "Right half should have exactly 20 ship cells");
    }

    @Test
    void createHalfStrategy_shouldConfineShipsToLeftHalfOnly() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    assertTrue(j <= 4,
                            "Ship found at column " + j + " in left half strategy at position [" + i + "][" + j + "]");
                }
            }
        }
    }

    @Test
    void createHalfStrategyRight_shouldConfineShipsToRightHalfOnly() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    assertTrue(j >= 5,
                            "Ship found at column " + j + " in right half strategy at position [" + i + "][" + j + "]");
                }
            }
        }
    }

    @Test
    void createHalfStrategy_shouldHaveValidMatrixFormat() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        String placementMatrix = strategy.getPlacementMatrix();

        String[] rows = placementMatrix.split(";");
        assertEquals(10, rows.length, "Matrix should contain exactly 10 rows");

        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            String[] cells = row.split(",");
            assertEquals(10, cells.length, "Row " + i + " should contain exactly 10 cells");

            for (int j = 0; j < cells.length; j++) {
                String cell = cells[j];
                assertEquals(1, cell.length(), "Cell [" + i + "][" + j + "] should be single character");
                char content = cell.charAt(0);
                assertTrue(content == ' ' || content == 'S',
                        "Cell [" + i + "][" + j + "] contains invalid character: '" + content + "'");
            }
        }
    }

    private Character[][] convertStringToMatrix(String matrixString) {
        Character[][] matrix = new Character[10][10];
        String[] rows = matrixString.split(";");

        for (int i = 0; i < 10 && i < rows.length; i++) {
            String[] cells = rows[i].split(",");
            for (int j = 0; j < 10 && j < cells.length; j++) {
                matrix[i][j] = cells[j].charAt(0);
            }
        }

        // Initialize remaining cells
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == null) {
                    matrix[i][j] = ' ';
                }
            }
        }

        return matrix;
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

    private void assertShipSegment(Character[][] matrix, int row, int col) {
        assertTrue(row >= 0 && row < 10 && col >= 0 && col < 10,
                "Invalid coordinates [" + row + "][" + col + "]");
        assertEquals('S', matrix[row][col],
                "Expected ship segment 'S' at position [" + row + "][" + col + "]");
    }
}