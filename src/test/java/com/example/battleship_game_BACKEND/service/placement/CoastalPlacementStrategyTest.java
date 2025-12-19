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
class CoastalPlacementStrategyTest {

    private final CoastalPlacementStrategy strategyService = new CoastalPlacementStrategy();

    @Mock
    private Player mockPlayer;

    @Test
    void createCoastalStrategy_shouldCreateValidStrategyObject() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);

        // Verify
        assertNotNull(strategy, "Strategy object should not be null");
        assertEquals(mockPlayer, strategy.getPlayer(), "Player should be associated with strategy");
        assertEquals("Береговая стратегия v2", strategy.getStrategyName(), "Strategy name should be correct");
        assertNotNull(strategy.getPlacementMatrix(), "Placement matrix should not be null");
        assertFalse(strategy.getPlacementMatrix().isEmpty(), "Placement matrix should not be empty");

        verifyNoInteractions(mockPlayer);
    }

    @Test
    void createCoastalStrategy_shouldPlaceFourDeckShipCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify 4-deck ship: horizontal at top edge (row 0, columns 0-3)
        assertShipSegment(matrix, 0, 0);
        assertShipSegment(matrix, 0, 1);
        assertShipSegment(matrix, 0, 2);
        assertShipSegment(matrix, 0, 3);
    }

    @Test
    void createCoastalStrategy_shouldPlaceFirstThreeDeckShipCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify first 3-deck ship: vertical at left edge (rows 0-2, column 0)
        assertShipSegment(matrix, 0, 0);
        assertShipSegment(matrix, 1, 0);
        assertShipSegment(matrix, 2, 0);
    }

    @Test
    void createCoastalStrategy_shouldPlaceSecondThreeDeckShipCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify second 3-deck ship: vertical at right edge (rows 7-9, column 9)
        assertShipSegment(matrix, 7, 9);
        assertShipSegment(matrix, 8, 9);
        assertShipSegment(matrix, 9, 9);
    }

    @Test
    void createCoastalStrategy_shouldPlaceAllTwoDeckShipsCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 2-deck ships
        // First 2-deck: horizontal at bottom edge (row 9, columns 2-3)
        assertShipSegment(matrix, 9, 2);
        assertShipSegment(matrix, 9, 3);

        // Second 2-deck: horizontal at top edge right (row 0, columns 7-8)
        assertShipSegment(matrix, 0, 7);
        assertShipSegment(matrix, 0, 8);

        // Third 2-deck: vertical at right edge middle (rows 4-5, column 9)
        assertShipSegment(matrix, 4, 9);
        assertShipSegment(matrix, 5, 9);
    }

    @Test
    void createCoastalStrategy_shouldPlaceAllSingleDeckShipsCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 1-deck ships
        assertShipSegment(matrix, 9, 6); // Bottom edge
        assertShipSegment(matrix, 2, 9); // Right edge
        assertShipSegment(matrix, 0, 5); // Top edge
        assertShipSegment(matrix, 6, 0); // Left edge
    }

    @Test
    void createCoastalStrategy_shouldHaveNineteenShipCellsDueToOverlap() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Count all ship cells
        int shipCount = countShipCells(matrix);

        // Verify total count: 4 + 3 + 3 + 2*3 + 1*4 = 20, but there's overlap at (0,0)
        assertEquals(19, shipCount, "Total ship cells should be 19 due to overlap at (0,0)");
    }

    @Test
    void createCoastalStrategy_shouldPlaceShipsOnlyOnBorders() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all ship segments are on borders (row 0, row 9, col 0, col 9)
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    boolean isOnBorder = (i == 0 || i == 9 || j == 0 || j == 9);
                    assertTrue(isOnBorder,
                            "Ship segment at [" + i + "][" + j + "] is not on border");
                }
            }
        }
    }

    @Test
    void createCoastalStrategy_shouldHaveValidMatrixFormat() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        String placementMatrix = strategy.getPlacementMatrix();

        // Verify matrix structure
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

    // Helper method to convert string matrix to 2D array
    private Character[][] convertStringToMatrix(String matrixString) {
        Character[][] matrix = new Character[10][10];
        String[] rows = matrixString.split(";");

        for (int i = 0; i < 10 && i < rows.length; i++) {
            String[] cells = rows[i].split(",");
            for (int j = 0; j < 10 && j < cells.length; j++) {
                matrix[i][j] = cells[j].charAt(0);
            }
        }

        // Initialize remaining cells if matrixString is incomplete
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == null) {
                    matrix[i][j] = ' ';
                }
            }
        }

        return matrix;
    }

    // Helper method to count ship cells in matrix
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

    // Helper method to assert ship segment presence
    private void assertShipSegment(Character[][] matrix, int row, int col) {
        assertTrue(row >= 0 && row < 10 && col >= 0 && col < 10,
                "Invalid coordinates [" + row + "][" + col + "]");
        assertEquals('S', matrix[row][col],
                "Expected ship segment 'S' at position [" + row + "][" + col + "]");
    }
}