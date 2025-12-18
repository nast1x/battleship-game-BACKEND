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
        assertNotNull(strategy);
        assertEquals(mockPlayer, strategy.getPlayer());
        assertEquals("Береговая стратегия", strategy.getStrategyName());
        assertNotNull(strategy.getPlacementMatrix());
        assertFalse(strategy.getPlacementMatrix().isEmpty());

        // Проверяем, что методы мока не вызывались ненужно
        verifyNoInteractions(mockPlayer);
    }

    @Test
    void createCoastalStrategy_shouldPlaceShipsInCoastalZone() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify ships are placed in coastal zone (within 1 cell from borders)
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    boolean isCoastal = (i <= 1 || i >= 8 || j <= 1 || j >= 8);
                    assertTrue(isCoastal,
                            "Ship segment at [" + i + "][" + j + "] is not in coastal zone");
                }
            }
        }
    }

    @Test
    void createCoastalStrategy_shouldPlaceCorrectNumberOfShipCells() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Count ship cells
        int shipCellCount = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == 'S') {
                    shipCellCount++;
                }
            }
        }

        // Verify: 1 four-deck (4) + 2 three-deck (6) + 3 two-deck (6) + 4 one-deck (4) = 20 cells
        assertEquals(20, shipCellCount, "Total ship cells should be 20");
    }

    @Test
    void createCoastalStrategy_shouldPlaceFourDeckShipCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify 4-deck ship at top-left corner (row 0, columns 0-3)
        assertShipSegment(matrix, 0, 0);
        assertShipSegment(matrix, 0, 1);
        assertShipSegment(matrix, 0, 2);
        assertShipSegment(matrix, 0, 3);
    }

    @Test
    void createCoastalStrategy_shouldPlaceAllThreeDeckShipsCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify first 3-deck ship at bottom-left (row 9, columns 0-2)
        assertShipSegment(matrix, 9, 0);
        assertShipSegment(matrix, 9, 1);
        assertShipSegment(matrix, 9, 2);

        // Verify second 3-deck ship at top-right (rows 0-2, column 9)
        assertShipSegment(matrix, 0, 9);
        assertShipSegment(matrix, 1, 9);
        assertShipSegment(matrix, 2, 9);
    }

    @Test
    void createCoastalStrategy_shouldPlaceAllTwoDeckShipsCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 2-deck ships
        assertShipSegment(matrix, 9, 7);
        assertShipSegment(matrix, 9, 8);
        assertShipSegment(matrix, 7, 0);
        assertShipSegment(matrix, 8, 0);
        assertShipSegment(matrix, 0, 5);
        assertShipSegment(matrix, 1, 5);
    }

    @Test
    void createCoastalStrategy_shouldPlaceAllSingleDeckShipsCorrectly() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        Character[][] matrix = convertStringToMatrix(strategy.getPlacementMatrix());

        // Verify all 1-deck ships
        assertShipSegment(matrix, 9, 9);
        assertShipSegment(matrix, 0, 7);
        assertShipSegment(matrix, 3, 0);
        assertShipSegment(matrix, 6, 9);
    }

    @Test
    void createCoastalStrategy_shouldHaveValidMatrixFormat() {
        // Execute
        PlacementStrategy strategy = strategyService.createCoastalStrategy(mockPlayer);
        String placementMatrix = strategy.getPlacementMatrix();

        // Verify format: 10 rows separated by semicolons, each with 10 cells separated by commas
        String[] rows = placementMatrix.split(";");
        assertEquals(10, rows.length, "Matrix should have 10 rows");

        for (String row : rows) {
            String[] cells = row.split(",");
            assertEquals(10, cells.length, "Each row should have 10 cells");
            for (String cell : cells) {
                assertTrue(cell.equals(" ") || cell.equals("S"),
                        "Cell should contain only ' ' or 'S', found: '" + cell + "'");
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