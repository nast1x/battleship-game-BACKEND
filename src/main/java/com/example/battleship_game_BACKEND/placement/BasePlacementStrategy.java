package com.example.battleship_game_BACKEND.placement;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.model.ShipPlacement;
import com.example.battleship_game_BACKEND.repository.PlacementStrategyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public abstract class BasePlacementStrategy {

    protected Random rand;

    protected PlacementStrategyRepository placementStrategyRepository;

    /** Флот: (длина → уникальный shipId) */
    private final List<Map.Entry<Integer, Integer>> fleet = Arrays.asList(
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

    protected BasePlacementStrategy() {
        this.rand = new Random();
    }

    protected BasePlacementStrategy(Random rand) {
        this.rand = rand;
    }

    @Autowired
   protected BasePlacementStrategy(PlacementStrategyRepository placementStrategyRepository) {
        this.placementStrategyRepository = placementStrategyRepository;
    }

    // Удаляем конструктор с @Autowired для Random и PlacementStrategyRepository
    // Spring будет использовать setter injection для placementStrategyRepository

    protected List<Map.Entry<Integer, Integer>> getFleet() {
        return new ArrayList<>(fleet);
    }

    /**
     * Генерирует расстановку и сохраняет её для указанного игрока
     */
    public PlacementStrategy generateAndSavePlacement(Player player, String strategyName) {
        List<ShipPlacement> placements = generatePlacement();

        // Создаем или обновляем запись стратегии
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
    public List<ShipPlacement> generatePlacement() {
        final int maxAttempts = 1000;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<ShipPlacement> result = attemptPlacement();
            if (result != null) {
                return result;
            }
        }

        throw new IllegalStateException("Failed to generate ship placement after " + maxAttempts + " attempts");
    }

    private List<ShipPlacement> attemptPlacement() {
        boolean[][] occ = new boolean[10][10];
        List<Map.Entry<Integer, Integer>> queue = new ArrayList<>(getFleet());
        Collections.shuffle(queue, rand);
        List<ShipPlacement> result = new ArrayList<>();
        List<Map.Entry<Integer, Integer>> cells = scanCells();

        for (Map.Entry<Integer, Integer> cell : cells) {
            if (queue.isEmpty()) {
                break;
            }

            if (!tryPlaceShipsInQueue(occ, queue, result, cell)) {
                return null; // Не удалось разместить корабли в этой клетке
            }
        }

        return queue.isEmpty() ? result : null;
    }

    private boolean tryPlaceShipsInQueue(
            boolean[][] occ,
            List<Map.Entry<Integer, Integer>> queue,
            List<ShipPlacement> result,
            Map.Entry<Integer, Integer> cell) {

        int r = cell.getKey();
        int c = cell.getValue();
        int initialQueueSize = queue.size();

        for (int tries = 0; tries < initialQueueSize; tries++) {
            Map.Entry<Integer, Integer> ship = queue.removeFirst();

            if (tryPlaceShipAtCell(occ, ship, r, c, result)) {
                return true; // Успешно разместили корабль
            }

            queue.add(ship); // Возвращаем корабль в очередь
        }

        return false; // Не удалось разместить ни один корабль в этой клетке
    }

    private boolean tryPlaceShipAtCell(
            boolean[][] occ,
            Map.Entry<Integer, Integer> ship,
            int row, int col,
            List<ShipPlacement> result) {

        int size = ship.getKey();
        int shipId = ship.getValue();

        List<Boolean> orientations = Arrays.asList(true, false);
        Collections.shuffle(orientations, rand);

        for (boolean horizontal : orientations) {
            if (tryPlace(occ, col, row, size, horizontal)) {
                result.add(new ShipPlacement(shipId, size, row, col, !horizontal));
                return true;
            }
        }

        return false;
    }

    /**
     * Загружает сохраненную расстановку для игрока
     */
    public List<ShipPlacement> loadPlacement(Player player, String strategyName) {
        return placementStrategyRepository
                .findByPlayerAndStrategyName(player, strategyName)
                .map(PlacementStrategy::getPlacementDataAsList)
                .orElse(Collections.emptyList());
    }

    protected boolean canPlace(
            boolean[][] occ,
            int x0, int y0,
            int size,
            boolean horizontal
    ) {
        int dx = horizontal ? 1 : 0;
        int dy = horizontal ? 0 : 1;
        int endX = x0 + dx * (size - 1);
        int endY = y0 + dy * (size - 1);

        if (endX < 0 || endX > 9 || endY < 0 || endY > 9) return false;

        for (int k = 0; k < size; k++) {
            int x = x0 + dx * k;
            int y = y0 + dy * k;

            for (int ry = y - 1; ry <= y + 1; ry++) {
                for (int rx = x - 1; rx <= x + 1; rx++) {
                    if (rx >= 0 && rx <= 9 && ry >= 0 && ry <= 9 && occ[ry][rx]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected boolean tryPlace(
            boolean[][] occ,
            int x0, int y0,
            int size,
            boolean horizontal
    ) {
        if (!canPlace(occ, x0, y0, size, horizontal)) return false;

        int dx = horizontal ? 1 : 0;
        int dy = horizontal ? 0 : 1;

        for (int k = 0; k < size; k++) {
            int x = x0 + dx * k;
            int y = y0 + dy * k;
            occ[y][x] = true;
        }

        return true;
    }

    /**
     * Каждая стратегия выдаёт список (row, col) в том порядке,
     * в котором мы будем пытаться там разместить корабли.
     */
    protected abstract List<Map.Entry<Integer, Integer>> scanCells();
}