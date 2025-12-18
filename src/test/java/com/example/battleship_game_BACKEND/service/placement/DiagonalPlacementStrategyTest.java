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
class DiagonalPlacementStrategyTest {

    private final DiagonalPlacementStrategy strategyService = new DiagonalPlacementStrategy();

    @Mock
    private Player mockPlayer;

    @Test
    void createDiagonalStrategy_shouldCreateValidStrategyObject() {
        // Execute
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);

        // Verify
        assertNotNull(strategy, "Strategy object should not be null");
        assertEquals(mockPlayer, strategy.getPlayer(), "Player should be associated with strategy");
        assertEquals("Диагональная стратегия", strategy.getStrategyName(), "Strategy name should be correct");
        assertNotNull(strategy.getPlacementMatrix(), "Placement matrix should not be null");
        assertFalse(strategy.getPlacementMatrix().isEmpty(), "Placement matrix should not be empty");

        // Ensure no unnecessary interactions with mock
        verifyNoInteractions(mockPlayer);
    }

    @Test
    void createDiagonalStrategy_shouldPlaceFourDeckShipCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify 4-deck ship on main diagonal
        assertShipSegment(matrix, 0, 0);
        assertShipSegment(matrix, 1, 1);
        assertShipSegment(matrix, 2, 2);
        assertShipSegment(matrix, 3, 3);
    }

    @Test
    void createDiagonalStrategy_shouldPlaceAllThreeDeckShipsCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify first 3-deck ship on main diagonal continuation
        assertShipSegment(matrix, 5, 5);
        assertShipSegment(matrix, 6, 6);
        assertShipSegment(matrix, 7, 7);

        // Verify second 3-deck ship on anti-diagonal
        assertShipSegment(matrix, 2, 7);
        assertShipSegment(matrix, 3, 6);
        assertShipSegment(matrix, 4, 5);
    }

    @Test
    void createDiagonalStrategy_shouldPlaceAllTwoDeckShipsCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 2-deck ships
        // Top-right anti-diagonal
        assertShipSegment(matrix, 0, 9);
        assertShipSegment(matrix, 1, 8);

        // Bottom-left diagonal shift
        assertShipSegment(matrix, 8, 2);
        assertShipSegment(matrix, 9, 3);

        // Near main diagonal
        assertShipSegment(matrix, 6, 0);
        assertShipSegment(matrix, 7, 1);
    }

    @Test
    void createDiagonalStrategy_shouldPlaceAllSingleDeckShipsCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 1-deck ships
        assertShipSegment(matrix, 4, 0); // Near diagonal
        assertShipSegment(matrix, 9, 9); // Opposite corner
        assertShipSegment(matrix, 0, 5); // Symmetric position
        assertShipSegment(matrix, 5, 0); // Symmetric position
    }

    @Test
    void createDiagonalStrategy_shouldHaveExactlyTwentyShipCells() {
        // Execute
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Count all ship cells
        int shipCount = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    shipCount++;
                }
            }
        }

        // Verify total count: 4 + 3*2 + 2*3 + 1*4 = 20
        assertEquals(20, shipCount, "Total ship cells should be exactly 20");
    }

    @Test
    void createDiagonalStrategy_shouldHaveValidMatrixFormat() {
        // Execute
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        String placementMatrix = strategy.getPlacementMatrix();

        // Verify matrix structure
        assertNotNull(placementMatrix, "Matrix string should not be null");
        String[] rows = placementMatrix.split(";");
        assertEquals(10, rows.length, "Matrix should contain exactly 10 rows");

        for (int i = 0; i < rows.length; i++) {
            String row = rows[i];
            assertNotNull(row, "Row " + i + " should not be null");
            String[] cells = row.split(",");
            assertEquals(10, cells.length, "Row " + i + " should contain exactly 10 cells");

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

    @Test
    void createDiagonalStrategy_shouldPlaceShipsOnDiagonalsOnly() {
        // Execute
        PlacementStrategy strategy = strategyService.createDiagonalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all ship segments are on valid diagonal positions
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    boolean isOnValidDiagonal =
                            // Main diagonal and its continuation
                            (i == j) ||
                                    // Anti-diagonal segment
                                    (i + j == 9 && i >= 0 && i <= 1) ||
                                    (i + j == 9 && i >= 2 && i <= 4) ||
                                    // Shifted diagonals
                                    (i - j == 6) || // (8,2), (9,3)
                                    (j - i == 6) || // (0,6) not used, but (6,0), (7,1)
                                    // Special single-deck positions that follow diagonal symmetry
                                    (i == 4 && j == 0) ||
                                    (i == 0 && j == 5) ||
                                    (i == 5 && j == 0);

                    assertTrue(isOnValidDiagonal,
                            "Ship at [" + i + "][" + j + "] is not on a valid diagonal position");
                }
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

    // Helper method to assert ship segment presence
    private void assertShipSegment(Character[][] matrix, int row, int col) {
        assertTrue(row >= 0 && row < 10 && col >= 0 && col < 10,
                "Invalid coordinates [" + row + "][" + col + "]");
        assertEquals('S', matrix[row][col],
                "Expected ship segment 'S' at position [" + row + "][" + col + "]");
    }
}