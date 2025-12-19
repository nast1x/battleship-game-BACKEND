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

        assertNotNull(strategy, "Strategy should not be null");
        assertEquals(mockPlayer, strategy.getPlayer(), "Player should be associated correctly");
        assertEquals("Половинчатая стратегия", strategy.getStrategyName(), "Strategy name should match");
        assertNotNull(strategy.getPlacementMatrix(), "Placement matrix should not be null");
        assertFalse(strategy.getPlacementMatrix().isEmpty(), "Placement matrix should not be empty");

        verifyNoInteractions(mockPlayer);
    }

    @Test
    void createHalfStrategyRight_shouldCreateValidRightHalfStrategy() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);

        assertNotNull(strategy, "Strategy should not be null");
        assertEquals(mockPlayer, strategy.getPlayer(), "Player should be associated correctly");
        assertEquals("Половинчатая стратегия (правая)", strategy.getStrategyName(), "Strategy name should match");
        assertNotNull(strategy.getPlacementMatrix(), "Placement matrix should not be null");
        assertFalse(strategy.getPlacementMatrix().isEmpty(), "Placement matrix should not be empty");

        verifyNoInteractions(mockPlayer);
    }

    @Test
    void createHalfStrategy_shouldPlaceFourDeckShipCorrectlyInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify 4-deck ship at row 2, columns 0-3
        assertShipSegment(matrix, 2, 0, "4-deck ship start");
        assertShipSegment(matrix, 2, 1, "4-deck ship middle");
        assertShipSegment(matrix, 2, 2, "4-deck ship middle");
        assertShipSegment(matrix, 2, 3, "4-deck ship end");
    }

    @Test
    void createHalfStrategyRight_shouldPlaceFourDeckShipCorrectlyInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify 4-deck ship at row 2, columns 5-8
        assertShipSegment(matrix, 2, 5, "4-deck ship start");
        assertShipSegment(matrix, 2, 6, "4-deck ship middle");
        assertShipSegment(matrix, 2, 7, "4-deck ship middle");
        assertShipSegment(matrix, 2, 8, "4-deck ship end");
    }

    @Test
    void createHalfStrategy_shouldPlaceAllThreeDeckShipsCorrectlyInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Horizontal 3-deck at row 5, columns 0-2
        assertShipSegment(matrix, 5, 0, "3-deck horizontal start");
        assertShipSegment(matrix, 5, 1, "3-deck horizontal middle");
        assertShipSegment(matrix, 5, 2, "3-deck horizontal end");

        // Vertical 3-deck at column 4, rows 0-2
        assertShipSegment(matrix, 0, 4, "3-deck vertical top");
        assertShipSegment(matrix, 1, 4, "3-deck vertical middle");
        assertShipSegment(matrix, 2, 4, "3-deck vertical bottom");
    }

    @Test
    void createHalfStrategyRight_shouldPlaceAllThreeDeckShipsCorrectlyInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Horizontal 3-deck at row 5, columns 5-7
        assertShipSegment(matrix, 5, 5, "3-deck horizontal start");
        assertShipSegment(matrix, 5, 6, "3-deck horizontal middle");
        assertShipSegment(matrix, 5, 7, "3-deck horizontal end");

        // Vertical 3-deck at column 9, rows 0-2
        assertShipSegment(matrix, 0, 9, "3-deck vertical top");
        assertShipSegment(matrix, 1, 9, "3-deck vertical middle");
        assertShipSegment(matrix, 2, 9, "3-deck vertical bottom");
    }

    @Test
    void createHalfStrategy_shouldPlaceAllTwoDeckShipsCorrectlyInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Horizontal 2-deck at row 7, columns 0-1
        assertShipSegment(matrix, 7, 0, "2-deck horizontal start");
        assertShipSegment(matrix, 7, 1, "2-deck horizontal end");

        // Vertical 2-deck at column 3, rows 8-9
        assertShipSegment(matrix, 8, 3, "2-deck vertical top");
        assertShipSegment(matrix, 9, 3, "2-deck vertical bottom");

        // Fixed 2-deck at row 4, columns 2-3 (no intersection)
        assertShipSegment(matrix, 4, 2, "2-deck fixed start");
        assertShipSegment(matrix, 4, 3, "2-deck fixed end");
    }

    @Test
    void createHalfStrategyRight_shouldPlaceAllTwoDeckShipsCorrectlyInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Horizontal 2-deck at row 7, columns 5-6
        assertShipSegment(matrix, 7, 5, "2-deck horizontal start");
        assertShipSegment(matrix, 7, 6, "2-deck horizontal end");

        // Vertical 2-deck at column 8, rows 8-9
        assertShipSegment(matrix, 8, 8, "2-deck vertical top");
        assertShipSegment(matrix, 9, 8, "2-deck vertical bottom");

        // Fixed 2-deck at row 4, columns 7-8 (no intersection)
        assertShipSegment(matrix, 4, 7, "2-deck fixed start");
        assertShipSegment(matrix, 4, 8, "2-deck fixed end");
    }

    @Test
    void createHalfStrategy_shouldPlaceAllSingleDeckShipsCorrectlyInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // All 1-deck ships in left half
        assertShipSegment(matrix, 0, 0, "1-deck top-left corner");
        assertShipSegment(matrix, 9, 0, "1-deck bottom-left corner");
        assertShipSegment(matrix, 3, 1, "1-deck center-left");
        assertShipSegment(matrix, 6, 4, "1-deck right boundary");
    }

    @Test
    void createHalfStrategyRight_shouldPlaceAllSingleDeckShipsCorrectlyInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // All 1-deck ships in right half
        assertShipSegment(matrix, 0, 5, "1-deck top boundary");
        assertShipSegment(matrix, 9, 5, "1-deck bottom boundary");
        assertShipSegment(matrix, 3, 6, "1-deck center-right");
        assertShipSegment(matrix, 6, 9, "1-deck right edge");
    }

    @Test
    void createHalfStrategy_shouldHaveExactlyTwentyShipCellsInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        int shipCount = countShipCells(matrix);
        assertEquals(20, shipCount, "Left half should have exactly 20 ship cells after fix");
    }

    @Test
    void createHalfStrategyRight_shouldHaveExactlyTwentyShipCellsInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        int shipCount = countShipCells(matrix);
        assertEquals(20, shipCount, "Right half should have exactly 20 ship cells after fix");
    }

    @Test
    void createHalfStrategy_shouldConfineShipsToLeftHalfOnly() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    assertTrue(j <= 4,
                            "Ship at [" + i + "][" + j + "] is outside left half (column > 4)");
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
                            "Ship at [" + i + "][" + j + "] is outside right half (column < 5)");
                }
            }
        }
    }

    @Test
    void createHalfStrategy_shouldHaveNoShipIntersectionsInLeftHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Create a copy of placement instructions to verify no intersections
        Character[][] expectedPlacement = new Character[10][10];
        initializeEmptyMatrix(expectedPlacement);

        // Apply placements in order and check for intersections
        int[][] fourDeck = {{2,0}, {2,1}, {2,2}, {2,3}};
        int[][] threeDeck1 = {{5,0}, {5,1}, {5,2}};
        int[][] threeDeck2 = {{0,4}, {1,4}, {2,4}};
        int[][] twoDeck1 = {{7,0}, {7,1}};
        int[][] twoDeck2 = {{8,3}, {9,3}};
        int[][] twoDeck3 = {{4,2}, {4,3}}; // Fixed position
        int[][] singleDeck = {{0,0}, {9,0}, {3,1}, {6,4}};

        applyShipPlacement(expectedPlacement, fourDeck, "4-deck");
        applyShipPlacement(expectedPlacement, threeDeck1, "3-deck-1");
        applyShipPlacement(expectedPlacement, threeDeck2, "3-deck-2");
        applyShipPlacement(expectedPlacement, twoDeck1, "2-deck-1");
        applyShipPlacement(expectedPlacement, twoDeck2, "2-deck-2");
        applyShipPlacement(expectedPlacement, twoDeck3, "2-deck-3");
        applyShipPlacement(expectedPlacement, singleDeck, "1-deck");

        // Verify actual matrix matches expected placement
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                assertEquals(expectedPlacement[i][j], matrix[i][j],
                        "Cell [" + i + "][" + j + "] has unexpected value. " +
                                "Expected: " + expectedPlacement[i][j] + ", Actual: " + matrix[i][j]);
            }
        }
    }

    @Test
    void createHalfStrategyRight_shouldHaveNoShipIntersectionsInRightHalf() {
        PlacementStrategy strategy = strategyService.createHalfStrategyRight(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Create expected placement matrix
        Character[][] expectedPlacement = new Character[10][10];
        initializeEmptyMatrix(expectedPlacement);

        // Apply placements in order
        int[][] fourDeck = {{2,5}, {2,6}, {2,7}, {2,8}};
        int[][] threeDeck1 = {{5,5}, {5,6}, {5,7}};
        int[][] threeDeck2 = {{0,9}, {1,9}, {2,9}};
        int[][] twoDeck1 = {{7,5}, {7,6}};
        int[][] twoDeck2 = {{8,8}, {9,8}};
        int[][] twoDeck3 = {{4,7}, {4,8}}; // Fixed position
        int[][] singleDeck = {{0,5}, {9,5}, {3,6}, {6,9}};

        applyShipPlacement(expectedPlacement, fourDeck, "4-deck");
        applyShipPlacement(expectedPlacement, threeDeck1, "3-deck-1");
        applyShipPlacement(expectedPlacement, threeDeck2, "3-deck-2");
        applyShipPlacement(expectedPlacement, twoDeck1, "2-deck-1");
        applyShipPlacement(expectedPlacement, twoDeck2, "2-deck-2");
        applyShipPlacement(expectedPlacement, twoDeck3, "2-deck-3");
        applyShipPlacement(expectedPlacement, singleDeck, "1-deck");

        // Verify actual matrix matches expected placement
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                assertEquals(expectedPlacement[i][j], matrix[i][j],
                        "Cell [" + i + "][" + j + "] has unexpected value. " +
                                "Expected: " + expectedPlacement[i][j] + ", Actual: " + matrix[i][j]);
            }
        }
    }

    @Test
    void createHalfStrategy_shouldHaveValidMatrixFormat() {
        PlacementStrategy strategy = strategyService.createHalfStrategy(mockPlayer);
        String placementMatrix = strategy.getPlacementMatrix();

        // Verify format structure
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
                assertEquals(1, cell.length(), "Cell [" + i + "][" + j + "] should be single character");
                char content = cell.charAt(0);
                assertTrue(content == ' ' || content == 'S',
                        "Cell [" + i + "][" + j + "] contains invalid character: '" + content + "'");
            }
        }
    }

    // Helper methods
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

    private void assertShipSegment(Character[][] matrix, int row, int col, String description) {
        assertTrue(row >= 0 && row < 10 && col >= 0 && col < 10,
                "Invalid coordinates [" + row + "][" + col + "] for " + description);
        assertEquals('S', matrix[row][col],
                "Expected ship segment at [" + row + "][" + col + "] for " + description);
    }

    private void initializeEmptyMatrix(Character[][] matrix) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }
    }

    private void applyShipPlacement(Character[][] matrix, int[][] coordinates, String shipName) {
        for (int[] coord : coordinates) {
            int row = coord[0];
            int col = coord[1];
            if (matrix[row][col] == 'S') {
                fail("Intersection detected at [" + row + "][" + col + "] for " + shipName);
            }
            matrix[row][col] = 'S';
        }
    }
}