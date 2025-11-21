package com.example.battleship_game_BACKEND.placement;

import com.example.battleship_game_BACKEND.model.ShipPlacement;
import com.example.battleship_game_BACKEND.repository.PlacementStrategyRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HalfFieldPlacer extends BasePlacementStrategy {

    @Getter
    private final boolean useLeftHalf;
    private final List<Integer> primaryColumns;

    // ===============================================================================
    // Конструкторы (СИНХРОНИЗИРОВАНЫ)
    // ===============================================================================

    /**
     * Конструктор для Spring
     */
    @Autowired
    public HalfFieldPlacer(PlacementStrategyRepository placementStrategyRepository) {
        super(placementStrategyRepository);
        this.useLeftHalf = rand.nextBoolean();
        this.primaryColumns = createPrimaryColumns();
    }

    /**
     * Конструктор для тестирования с контролируемым Random
     */
    public HalfFieldPlacer(PlacementStrategyRepository placementStrategyRepository, Random rand) {
        super(placementStrategyRepository, rand);
        this.useLeftHalf = rand.nextBoolean();
        this.primaryColumns = createPrimaryColumns();
    }

    /**
     * Конструктор для конкретной половины (для тестирования)
     */
    public HalfFieldPlacer(PlacementStrategyRepository placementStrategyRepository, boolean useLeftHalf) {
        super(placementStrategyRepository);
        this.useLeftHalf = useLeftHalf;
        this.primaryColumns = createPrimaryColumns();
    }

    // ===============================================================================
    // Основная логика размещения (УПРОЩЕНА)
    // ===============================================================================

    @Override
    public List<ShipPlacement> generatePlacement() {
        // Сначала пытаемся разместить в выбранной половине
        List<ShipPlacement> primaryResult = attemptHalfFieldPlacement();
        if (!primaryResult.isEmpty()) {
            return primaryResult;
        }

        // Fallback: размещаем по всему полю
        return attemptFullFieldPlacement();
    }

    @Override
    protected List<Map.Entry<Integer, Integer>> scanCells() {
        return generatePrimaryHalfCells();
    }

    // ===============================================================================
    // Логика размещения в половине поля
    // ===============================================================================

    private List<ShipPlacement> attemptHalfFieldPlacement() {
        boolean[][] occupied = new boolean[BOARD_SIZE][BOARD_SIZE];
        List<Map.Entry<Integer, Integer>> shipsQueue = new ArrayList<>(getFleet());
        List<ShipPlacement> result = new ArrayList<>();

        Collections.shuffle(shipsQueue, rand);
        List<Map.Entry<Integer, Integer>> cells = generatePrimaryHalfCells();

        return placeShipsWithConstraints(occupied, shipsQueue, result, cells, true) ?
                result : Collections.emptyList();
    }

    private List<ShipPlacement> attemptFullFieldPlacement() {
        // Используем базовую логику, но с нашим порядком клеток
        boolean[][] occupied = new boolean[BOARD_SIZE][BOARD_SIZE];
        List<Map.Entry<Integer, Integer>> shipsQueue = new ArrayList<>(getFleet());
        List<ShipPlacement> result = new ArrayList<>();

        Collections.shuffle(shipsQueue, rand);
        List<Map.Entry<Integer, Integer>> cells = generateAllCellsPrioritized();

        return placeShipsWithConstraints(occupied, shipsQueue, result, cells, false) ?
                result : Collections.emptyList();
    }

    private boolean placeShipsWithConstraints(
            boolean[][] occupied,
            List<Map.Entry<Integer, Integer>> shipsQueue,
            List<ShipPlacement> result,
            List<Map.Entry<Integer, Integer>> cells,
            boolean enforceHalfField
    ) {
        for (Map.Entry<Integer, Integer> cell : cells) {
            if (shipsQueue.isEmpty()) break;

            int row = cell.getKey();
            int col = cell.getValue();

            // Пытаемся разместить любой корабль из очереди в этой клетке
            if (!tryPlaceAnyShip(occupied, shipsQueue, result, row, col, enforceHalfField)) {
                continue;
            }
        }

        return shipsQueue.isEmpty();
    }

    private boolean tryPlaceAnyShip(
            boolean[][] occupied,
            List<Map.Entry<Integer, Integer>> shipsQueue,
            List<ShipPlacement> result,
            int row, int col,
            boolean enforceHalfField
    ) {
        // Создаем копию для безопасной итерации
        List<Map.Entry<Integer, Integer>> queueCopy = new ArrayList<>(shipsQueue);

        for (Map.Entry<Integer, Integer> ship : queueCopy) {
            if (tryPlaceShip(occupied, ship, row, col, result, enforceHalfField)) {
                shipsQueue.remove(ship);
                return true;
            }
        }

        return false;
    }

    private boolean tryPlaceShip(
            boolean[][] occupied,
            Map.Entry<Integer, Integer> ship,
            int row, int col,
            List<ShipPlacement> result,
            boolean enforceHalfField
    ) {
        int size = ship.getKey();
        int shipId = ship.getValue();

        // Пробуем обе ориентации в случайном порядке
        List<Boolean> orientations = Arrays.asList(true, false);
        Collections.shuffle(orientations, rand);

        for (boolean horizontal : orientations) {
            if (enforceHalfField && !isWithinPrimaryHalf(col, size, horizontal)) {
                continue;
            }

            if (tryPlace(occupied, col, row, size, horizontal)) {
                result.add(new ShipPlacement(shipId, size, row, col, !horizontal));
                return true;
            }
        }

        return false;
    }

    // ===============================================================================
    // Генерация клеток (ОПТИМИЗИРОВАНА)
    // ===============================================================================

    private List<Map.Entry<Integer, Integer>> generatePrimaryHalfCells() {
        List<Map.Entry<Integer, Integer>> cells = new ArrayList<>();

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col : primaryColumns) {
                cells.add(cell(row, col));
            }
        }

        Collections.shuffle(cells, rand);
        return cells;
    }

    private List<Map.Entry<Integer, Integer>> generateAllCellsPrioritized() {

        // Сначала добавляем клетки из основной половины
        List<Map.Entry<Integer, Integer>> cells = new ArrayList<>(generatePrimaryHalfCells());

        // Затем добавляем клетки из вторичной половины
        List<Integer> secondaryColumns = getSecondaryColumns();
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col : secondaryColumns) {
                cells.add(cell(row, col));
            }
        }

        return cells;
    }

    private List<Integer> createPrimaryColumns() {
        List<Integer> columns = new ArrayList<>();
        int startCol = useLeftHalf ? 0 : BOARD_SIZE / 2;
        int endCol = useLeftHalf ? (BOARD_SIZE / 2) - 1 : BOARD_SIZE - 1;

        for (int col = startCol; col <= endCol; col++) {
            columns.add(col);
        }
        return columns;
    }

    private List<Integer> getSecondaryColumns() {
        List<Integer> columns = new ArrayList<>();
        int startCol = useLeftHalf ? BOARD_SIZE / 2 : 0;
        int endCol = useLeftHalf ? BOARD_SIZE - 1 : (BOARD_SIZE / 2) - 1;

        for (int col = startCol; col <= endCol; col++) {
            columns.add(col);
        }
        return columns;
    }

    // ===============================================================================
    // Проверки ограничений
    // ===============================================================================

    private boolean isWithinPrimaryHalf(int startCol, int size, boolean horizontal) {
        int endCol = startCol + (horizontal ? size - 1 : 0);

        if (useLeftHalf) {
            return startCol >= 0 && startCol < BOARD_SIZE / 2 &&
                    endCol >= 0 && endCol < BOARD_SIZE / 2;
        } else {
            return startCol >= BOARD_SIZE / 2 && startCol < BOARD_SIZE &&
                    endCol >= BOARD_SIZE / 2 && endCol < BOARD_SIZE;
        }
    }

    @Override
    protected boolean canPlace(
            boolean[][] occupied,
            int startX, int startY,
            int size,
            boolean horizontal
    ) {
        // Всегда вызываем базовую проверку сначала
        return super.canPlace(occupied, startX, startY, size, horizontal);

        // Дополнительная проверка на половину поля не нужна здесь,
        // так как мы контролируем это в tryPlaceShip
    }

    // ===============================================================================
    // Методы для анализа и тестирования
    // ===============================================================================

    /**
     * Возвращает статистику по размещению
     */
    public PlacementStatistics getPlacementStatistics(List<ShipPlacement> placements) {
        int primaryHalfShips = 0;
        int secondaryHalfShips = 0;

        for (ShipPlacement placement : placements) {
            if (isInPrimaryHalf(placement)) {
                primaryHalfShips++;
            } else {
                secondaryHalfShips++;
            }
        }

        return new PlacementStatistics(
                placements.size(),
                primaryHalfShips,
                secondaryHalfShips,
                useLeftHalf ? "LEFT" : "RIGHT"
        );
    }

    private boolean isInPrimaryHalf(ShipPlacement placement) {
        boolean isHorizontal = !placement.isVertical();
        int startCol = placement.getCol();
        int size = placement.getSize();
        int endCol = startCol + (isHorizontal ? size - 1 : 0);

        if (useLeftHalf) {
            return endCol < BOARD_SIZE / 2;
        } else {
            return startCol >= BOARD_SIZE / 2;
        }
    }

    /**
     * Проверяет, соответствует ли размещение стратегии половины поля
     */
    public boolean isValidHalfFieldPlacement(List<ShipPlacement> placements) {
        if (!isValidPlacement(placements)) {
            return false;
        }

        // Для стратегии половины поля проверяем, что большинство кораблей в основной половине
        PlacementStatistics stats = getPlacementStatistics(placements);
        return stats.getPrimaryHalfShips() >= stats.getSecondaryHalfShips();
    }

    // ===============================================================================
    // Вспомогательные классы
    // ===============================================================================

    @Getter
    public class PlacementStatistics {
        private final int totalShips;
        private final int primaryHalfShips;
        private final int secondaryHalfShips;
        private final String primaryHalf;
        private final double primaryPercentage;

        public PlacementStatistics(int totalShips, int primaryHalfShips, int secondaryHalfShips, String primaryHalf) {
            this.totalShips = totalShips;
            this.primaryHalfShips = primaryHalfShips;
            this.secondaryHalfShips = secondaryHalfShips;
            this.primaryHalf = primaryHalf;
            this.primaryPercentage = totalShips > 0 ? (double) primaryHalfShips / totalShips * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format(
                    "HalfFieldPlacer Statistics: %s half=%s, primary=%d (%.1f%%), secondary=%d",
                    primaryHalf, useLeftHalf ? "LEFT" : "RIGHT",
                    primaryHalfShips, primaryPercentage, secondaryHalfShips
            );
        }
    }

    // ===============================================================================
    // Методы для конфигурации
    // ===============================================================================

    /**
     * Возвращает границы выбранной половины
     */
    public String getHalfBoundaries() {
        if (useLeftHalf) {
            return String.format("Columns: 0-%d", (BOARD_SIZE / 2) - 1);
        } else {
            return String.format("Columns: %d-%d", BOARD_SIZE / 2, BOARD_SIZE - 1);
        }
    }

    /**
     * Возвращает количество клеток в основной половине
     */
    public int getPrimaryHalfCellCount() {
        return BOARD_SIZE * (BOARD_SIZE / 2);
    }

    /**
     * Для отладки: выводит информацию о стратегии
     */
    public void printStrategyInfo() {
        System.out.println("HalfFieldPlacer Strategy Info:");
        System.out.println("Primary half: " + (useLeftHalf ? "LEFT" : "RIGHT"));
        System.out.println("Primary columns: " + primaryColumns);
        System.out.println("Primary cells count: " + getPrimaryHalfCellCount());
        System.out.println("Boundaries: " + getHalfBoundaries());
    }
}