package com.example.battleship_game_BACKEND.placement;

import com.example.battleship_game_BACKEND.model.ShipPlacement;
import lombok.Getter;

import java.util.*;

public class HalfFieldPlacer extends BasePlacementStrategy {

    private final boolean useLeft;
    private final List<Integer> primaryCols;

    public HalfFieldPlacer() {
        super();
        this.useLeft = new Random().nextBoolean();
        this.primaryCols = createPrimaryCols();
    }

    public HalfFieldPlacer(Random rand) {
        super(rand);
        this.useLeft = rand.nextBoolean();
        this.primaryCols = createPrimaryCols();
    }

    private List<Integer> createPrimaryCols() {
        List<Integer> cols = new ArrayList<>();
        int start = useLeft ? 0 : 5;
        int end = useLeft ? 4 : 9;

        for (int c = start; c <= end; c++) {
            cols.add(c);
        }
        return cols;
    }

    @Override
    public List<ShipPlacement> generatePlacement() {
        // Пытаемся разместить в основной половине
        Optional<List<ShipPlacement>> primaryResult = attemptPlacementInPrimaryHalf();
        return primaryResult.orElseGet(this::attemptPlacementInFullField);

        // Если не получилось - используем всё поле
    }

    private Optional<List<ShipPlacement>> attemptPlacementInPrimaryHalf() {
        return attemptPlacement(false);
    }

    private List<ShipPlacement> attemptPlacementInFullField() {
        return attemptPlacement(true).orElse(Collections.emptyList());
    }

    private Optional<List<ShipPlacement>> attemptPlacement(boolean useFullField) {
        final int maxAttempts = 1000;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            PlacementAttempt attemptInstance = new PlacementAttempt(useFullField);
            if (attemptInstance.execute()) {
                return Optional.of(attemptInstance.getResult());
            }
        }

        return Optional.empty();
    }

    @Override
    protected List<Map.Entry<Integer, Integer>> scanCells() {
        return generateCellList(false);
    }



    private List<Map.Entry<Integer, Integer>> generateCellList(boolean includeSecondary) {
        List<Map.Entry<Integer, Integer>> cells = new ArrayList<>();

        addPrimaryHalfCells(cells);

        if (includeSecondary) {
            addSecondaryHalfCells(cells);
        }

        return cells;
    }

    private void addPrimaryHalfCells(List<Map.Entry<Integer, Integer>> cells) {
        for (int row = 0; row <= 9; row++) {
            for (int col : primaryCols) {
                cells.add(new AbstractMap.SimpleEntry<>(row, col));
            }
        }
    }

    private void addSecondaryHalfCells(List<Map.Entry<Integer, Integer>> cells) {
        // Добавляем резервную половину в обратном порядке по строкам
        List<Integer> secondaryCols = getSecondaryCols();

        for (int row = 9; row >= 0; row--) {
            for (int col : secondaryCols) {
                cells.add(new AbstractMap.SimpleEntry<>(row, col));
            }
        }
    }

    private List<Integer> getSecondaryCols() {
        List<Integer> cols = new ArrayList<>();
        if (useLeft) {
            // Правая половина: 5-9
            for (int c = 5; c <= 9; c++) {
                cols.add(c);
            }
        } else {
            // Левая половина: 0-4
            for (int c = 4; c >= 0; c--) {
                cols.add(c);
            }
        }
        return cols;
    }

    @Override
    protected boolean canPlace(
            boolean[][] occ,
            int x0, int y0,
            int size,
            boolean horizontal
    ) {
        if (!super.canPlace(occ, x0, y0, size, horizontal)) {
            return false;
        }

        return isWithinPrimaryHalf(x0, size, horizontal);
    }

    private boolean isWithinPrimaryHalf(int startX, int size, boolean horizontal) {
        int dx = horizontal ? 1 : 0;
        int endX = startX + dx * (size - 1);

        if (useLeft) {
            return startX >= 0 && startX <= 4 && endX >= 0 && endX <= 4;
        } else {
            return startX >= 5 && startX <= 9 && endX >= 5 && endX <= 9;
        }
    }

    private class PlacementAttempt {
        private final boolean[][] occupied;
        private final List<Map.Entry<Integer, Integer>> shipQueue;
        @Getter
        private final List<ShipPlacement> result;
        private final List<Map.Entry<Integer, Integer>> cells;

        public PlacementAttempt(boolean useFullField) {
            this.occupied = new boolean[10][10];
            this.shipQueue = new ArrayList<>(getFleet());
            this.result = new ArrayList<>();
            this.cells = useFullField ? scanCellsAll() : scanCells();
            Collections.shuffle(shipQueue, rand);
        }
        private List<Map.Entry<Integer, Integer>> scanCellsAll() {
            return generateCellList(true);
        }

        public boolean execute() {
            for (Map.Entry<Integer, Integer> cell : cells) {
                if (shipQueue.isEmpty()) break;
                processCell(cell);
            }
            return shipQueue.isEmpty();
        }

        private void processCell(Map.Entry<Integer, Integer> cell) {
            int r = cell.getKey();
            int c = cell.getValue();

            int tries = 0;
            while (tries < shipQueue.size()) {
                Map.Entry<Integer, Integer> ship = shipQueue.removeFirst();
                int size = ship.getKey();
                int shipId = ship.getValue();

                if (tryPlaceShip(shipId, size, r, c)) {
                    break;
                }

                shipQueue.add(ship);
                tries++;
            }
        }

        private boolean tryPlaceShip(int shipId, int size, int row, int col) {
            List<Boolean> orientations = Arrays.asList(true, false);
            Collections.shuffle(orientations, rand);

            for (boolean horizontal : orientations) {
                if (tryPlace(occupied, col, row, size, horizontal)) {
                    result.add(new ShipPlacement(shipId, size, row, col, !horizontal));
                    return true;
                }
            }
            return false;
        }

    }
}