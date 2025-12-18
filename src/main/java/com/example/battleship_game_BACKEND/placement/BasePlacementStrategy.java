package com.example.battleship_game_BACKEND.placement;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.repository.PlacementStrategyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

//@Component
public abstract class BasePlacementStrategy {

    protected static final int BOARD_SIZE = 10;
    protected static final int MAX_ATTEMPTS = 1000;

    protected Random rand;
    protected PlacementStrategyRepository placementStrategyRepository;

    /** Флот: (длина → уникальный shipId) - неизменяемый */
    private static final List<Map.Entry<Integer, Integer>> FLEET = List.of(
            new AbstractMap.SimpleEntry<>(4, 1),
            new AbstractMap.SimpleEntry<>(3, 2),
            new AbstractMap.SimpleEntry<>(3, 3),
            new AbstractMap.SimpleEntry<>(2, 4),
            new AbstractMap.SimpleEntry<>(2, 5),
            new AbstractMap.SimpleEntry<>(2, 6),
            new AbstractMap.SimpleEntry<>(1, 7),
            new AbstractMap.SimpleEntry<>(1, 8),
            new AbstractMap.SimpleEntry<>(1, 9),
            new AbstractMap.SimpleEntry<>(1, 10)
    );

    // ===============================================================================
    // Конструкторы (ИСПРАВЛЕННЫЕ)
    // ===============================================================================

    /**
     * Конструктор по умолчанию для Spring
     */
    @Autowired
    protected BasePlacementStrategy(PlacementStrategyRepository placementStrategyRepository) {
        this.placementStrategyRepository = placementStrategyRepository;
        this.rand = new Random();
    }

    /**
     * Конструктор для тестирования с контролируемым Random
     */
    protected BasePlacementStrategy(PlacementStrategyRepository placementStrategyRepository, Random rand) {
        this.placementStrategyRepository = placementStrategyRepository;
        this.rand = rand;
    }

    // ===============================================================================
    // Публичные методы
    // ===============================================================================

    /**
     * Генерирует расстановку и сохраняет её для указанного игрока
     */
    public PlacementStrategy generateAndSavePlacement(Player player, String strategyName) {
        validatePlayerAndStrategyName(player, strategyName);

        List<PlacementStrategy.ShipPlacement> placements = generatePlacement();

        PlacementStrategy strategy = placementStrategyRepository
                .findByPlayerAndStrategyName(player, strategyName)
                .orElse(new PlacementStrategy());

        strategy.setPlayer(player);
        strategy.setStrategyName(strategyName);
        strategy.setPlacementDataFromList(placements);

        return placementStrategyRepository.save(strategy);
    }

    /**
     * Генерирует расстановку кораблей
     */
    public List<PlacementStrategy.ShipPlacement> generatePlacement() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            List<PlacementStrategy.ShipPlacement> result = attemptPlacement();
            if (result != null) {
                return Collections.unmodifiableList(result);
            }
        }

        throw new IllegalStateException(
                String.format("Failed to generate ship placement after %d attempts", MAX_ATTEMPTS)
        );
    }

    /**
     * Загружает сохраненную расстановку для игрока
     */
    public List<PlacementStrategy.ShipPlacement> loadPlacement(Player player, String strategyName) {
        validatePlayerAndStrategyName(player, strategyName);

        return placementStrategyRepository
                .findByPlayerAndStrategyName(player, strategyName)
                .map(PlacementStrategy::getPlacementDataAsList)
                .orElse(Collections.emptyList());
    }

    // ===============================================================================
    // Защищенные методы для наследников
    // ===============================================================================

    /**
     * Возвращает неизменяемую копию флота
     */
    protected List<Map.Entry<Integer, Integer>> getFleet() {
        return new ArrayList<>(FLEET);
    }

    /**
     * Проверяет возможность размещения корабля с учетом соседних клеток
     */
    protected boolean canPlace(
            boolean[][] occupied,
            int startX, int startY,
            int size,
            boolean horizontal
    ) {
        int dx = horizontal ? 1 : 0;
        int dy = horizontal ? 0 : 1;
        int endX = startX + dx * (size - 1);
        int endY = startY + dy * (size - 1);

        // Проверка границ
        if (endX < 0 || endX >= BOARD_SIZE || endY < 0 || endY >= BOARD_SIZE) {
            return false;
        }

        // Проверка соседних клеток
        for (int k = 0; k < size; k++) {
            int x = startX + dx * k;
            int y = startY + dy * k;

            if (!isAreaClear(occupied, x, y)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Размещает корабль на поле, если это возможно
     */
    protected boolean tryPlace(
            boolean[][] occupied,
            int startX, int startY,
            int size,
            boolean horizontal
    ) {
        if (!canPlace(occupied, startX, startY, size, horizontal)) {
            return false;
        }

        int dx = horizontal ? 1 : 0;
        int dy = horizontal ? 0 : 1;

        for (int k = 0; k < size; k++) {
            int x = startX + dx * k;
            int y = startY + dy * k;
            occupied[y][x] = true;
        }

        return true;
    }

    /**
     * Каждая стратегия выдаёт список (row, col) в том порядке,
     * в котором мы будем пытаться там разместить корабли.
     */
    protected abstract List<Map.Entry<Integer, Integer>> scanCells();

    // ===============================================================================
    // Приватные методы (ИСПРАВЛЕННАЯ ЛОГИКА)
    // ===============================================================================

    private List<PlacementStrategy.ShipPlacement> attemptPlacement() {
        boolean[][] occupied = new boolean[BOARD_SIZE][BOARD_SIZE];
        List<Map.Entry<Integer, Integer>> shipsQueue = new ArrayList<>(getFleet());
        Collections.shuffle(shipsQueue, rand);

        List<PlacementStrategy.ShipPlacement> result = new ArrayList<>();
        List<Map.Entry<Integer, Integer>> cells = scanCells();

        // Пытаемся разместить корабли в заданном порядке клеток
        for (Map.Entry<Integer, Integer> cell : cells) {
            if (shipsQueue.isEmpty()) {
                break;
            }

            // Пытаемся разместить любой корабль из очереди в текущей клетке
            if (!placeAnyShipInCell(occupied, shipsQueue, result, cell)) {
                continue; // Продолжаем со следующей клеткой
            }
        }

        return shipsQueue.isEmpty() ? result : null;
    }

    private boolean placeAnyShipInCell(
            boolean[][] occupied,
            List<Map.Entry<Integer, Integer>> shipsQueue,
            List<PlacementStrategy.ShipPlacement> result,
            Map.Entry<Integer, Integer> cell
    ) {
        int row = cell.getKey();
        int col = cell.getValue();

        // Создаем копию очереди для безопасной итерации
        List<Map.Entry<Integer, Integer>> queueCopy = new ArrayList<>(shipsQueue);

        for (Map.Entry<Integer, Integer> ship : queueCopy) {
            if (tryPlaceShipAtCell(occupied, ship, row, col, result)) {
                shipsQueue.remove(ship); // Удаляем только если успешно разместили
                return true;
            }
        }

        return false;
    }

    private boolean tryPlaceShipAtCell(
            boolean[][] occupied,
            Map.Entry<Integer, Integer> ship,
            int row, int col,
            List<PlacementStrategy.ShipPlacement> result
    ) {
        int size = ship.getKey();
        int shipId = ship.getValue();

        // Пробуем обе ориентации в случайном порядке
        List<Boolean> orientations = Arrays.asList(true, false);
        Collections.shuffle(orientations, rand);

        for (boolean horizontal : orientations) {
            if (tryPlace(occupied, col, row, size, horizontal)) {
                // Важно: ShipPlacement использует (row, col, !horizontal) для вертикальной ориентации
                result.add(new PlacementStrategy.ShipPlacement(shipId, size, row, col, !horizontal));
                return true;
            }
        }

        return false;
    }

    /**
     * Проверяет, что область 3x3 вокруг клетки свободна
     */
    private boolean isAreaClear(boolean[][] occupied, int x, int y) {
        for (int checkY = y - 1; checkY <= y + 1; checkY++) {
            for (int checkX = x - 1; checkX <= x + 1; checkX++) {
                if (checkX >= 0 && checkX < BOARD_SIZE &&
                        checkY >= 0 && checkY < BOARD_SIZE &&
                        occupied[checkY][checkX]) {
                    return false;
                }
            }
        }
        return true;
    }

    // ===============================================================================
    // Валидация и утилитные методы
    // ===============================================================================

    private void validatePlayerAndStrategyName(Player player, String strategyName) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (strategyName == null || strategyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy name cannot be null or empty");
        }
    }

    /**
     * Вспомогательный метод для создания клетки
     */
    protected Map.Entry<Integer, Integer> cell(int row, int col) {
        return new AbstractMap.SimpleEntry<>(row, col);
    }

    /**
     * Генерирует все клетки поля в порядке строк
     */
    protected List<Map.Entry<Integer, Integer>> generateAllCells() {
        List<Map.Entry<Integer, Integer>> allCells = new ArrayList<>();
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                allCells.add(cell(row, col));
            }
        }
        return allCells;
    }

    /**
     * Генерирует клетки в случайном порядке
     */
    protected List<Map.Entry<Integer, Integer>> generateRandomCells() {
        List<Map.Entry<Integer, Integer>> cells = generateAllCells();
        Collections.shuffle(cells, rand);
        return cells;
    }

    // ===============================================================================
    // Методы для тестирования
    // ===============================================================================

    /**
     * Для тестирования: проверяет валидность расстановки
     */
    public boolean isValidPlacement(List<PlacementStrategy.ShipPlacement> placements) {
        if (placements == null || placements.size() != FLEET.size()) {
            return false;
        }

        boolean[][] occupied = new boolean[BOARD_SIZE][BOARD_SIZE];

        for (PlacementStrategy.ShipPlacement placement : placements) {
            int size = placement.size();
            boolean horizontal = !placement.vertical(); // Конвертируем обратно

            if (!canPlace(occupied, placement.col(), placement.row(), size, horizontal)) {
                return false;
            }

            // Размещаем корабль
            int dx = horizontal ? 1 : 0;
            int dy = horizontal ? 0 : 1;

            for (int k = 0; k < size; k++) {
                int x = placement.col() + dx * k;
                int y = placement.row() + dy * k;
                occupied[y][x] = true;
            }
        }

        return true;
    }
}