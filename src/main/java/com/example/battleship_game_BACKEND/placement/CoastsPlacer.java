package com.example.battleship_game_BACKEND.placement;

import com.example.battleship_game_BACKEND.model.ShipPlacement;
import com.example.battleship_game_BACKEND.repository.PlacementStrategyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Расставляет корабли на границах поля (строки 0 или 9 для горизонтали,
 * столбцы 0 или 9 для вертикали).
 * Синхронизирован с BasePlacementStrategy.
 */
@Component
public class CoastsPlacer extends BasePlacementStrategy {

    // ===============================================================================
    // Конструкторы (СИНХРОНИЗИРОВАНЫ)
    // ===============================================================================

    /**
     * Конструктор для Spring
     */
    @Autowired
    public CoastsPlacer(PlacementStrategyRepository placementStrategyRepository) {
        super(placementStrategyRepository);
    }

    /**
     * Конструктор для тестирования с контролируемым Random
     */
    public CoastsPlacer(PlacementStrategyRepository placementStrategyRepository, Random rand) {
        super(placementStrategyRepository, rand);
    }

    // ===============================================================================
    // Основная логика размещения (СИНХРОНИЗИРОВАНА)
    // ===============================================================================

    @Override
    protected List<Map.Entry<Integer, Integer>> scanCells() {
        List<Map.Entry<Integer, Integer>> cells = new ArrayList<>();

        // Верхняя и нижняя строка
        for (int col = 0; col < BOARD_SIZE; col++) {
            cells.add(cell(0, col));                    // Верхняя граница
            cells.add(cell(BOARD_SIZE - 1, col));       // Нижняя граница
        }

        // Левая и правая колонка (исключая углы, которые уже добавлены)
        for (int row = 1; row < BOARD_SIZE - 1; row++) {
            cells.add(cell(row, 0));                    // Левая граница
            cells.add(cell(row, BOARD_SIZE - 1));       // Правая граница
        }

        // Перемешиваем для более случайного распределения
        Collections.shuffle(cells, rand);
        return cells;
    }

    // ===============================================================================
    // Логика проверки размещения (УЛУЧШЕННАЯ)
    // ===============================================================================

    @Override
    protected boolean canPlace(
            boolean[][] occupied,
            int startX, int startY,
            int size,
            boolean horizontal
    ) {
        // Сначала проверяем базовые ограничения
        if (!super.canPlace(occupied, startX, startY, size, horizontal)) {
            return false;
        }

        // Для стратегии "берегового размещения" проверяем специальные правила
        return isValidCoastalPlacement(startX, startY, size, horizontal);
    }

    /**
     * Проверяет, соответствует ли размещение правилам береговой стратегии
     */
    private boolean isValidCoastalPlacement(int startX, int startY, int size, boolean horizontal) {
        // Определяем конечные координаты
        int endX = startX + (horizontal ? size - 1 : 0);
        int endY = startY + (horizontal ? 0 : size - 1);

        // Проверяем, что корабль полностью находится на границах
        boolean isOnTopBorder = startY == 0 && endY == 0 && horizontal;
        boolean isOnBottomBorder = startY == BOARD_SIZE - 1 && endY == BOARD_SIZE - 1 && horizontal;
        boolean isOnLeftBorder = startX == 0 && endX == 0 && !horizontal;
        boolean isOnRightBorder = startX == BOARD_SIZE - 1 && endX == BOARD_SIZE - 1 && !horizontal;

        // Для кораблей размером 1 проверяем обе ориентации
        if (size == 1) {
            boolean isCornerCell = (startX == 0 || startX == BOARD_SIZE - 1) &&
                    (startY == 0 || startY == BOARD_SIZE - 1);
            boolean isBorderCell = startX == 0 || startX == BOARD_SIZE - 1 ||
                    startY == 0 || startY == BOARD_SIZE - 1;

            if (isCornerCell) {
                // Угловые клетки - разрешены обе ориентации (корабль размером 1)
                return true;
            } else if (isBorderCell) {
                // Граничные клетки - должны соответствовать ориентации границы
                boolean shouldBeHorizontal = startY == 0 || startY == BOARD_SIZE - 1;
                boolean shouldBeVertical = startX == 0 || startX == BOARD_SIZE - 1;
                return (horizontal && shouldBeHorizontal) || (!horizontal && shouldBeVertical);
            }
        }

        // Для кораблей размером > 1 применяем строгие правила
        return isOnTopBorder || isOnBottomBorder || isOnLeftBorder || isOnRightBorder;
    }

    // ===============================================================================
    // Оптимизированная версия tryPlace для береговой стратегии
    // ===============================================================================

    @Override
    protected boolean tryPlace(
            boolean[][] occupied,
            int startX, int startY,
            int size,
            boolean horizontal
    ) {
        // Сначала проверяем береговые ограничения
        if (!isValidCoastalPlacement(startX, startY, size, horizontal)) {
            return false;
        }

        // Затем проверяем базовые ограничения (соседние клетки и т.д.)
        if (!super.canPlace(occupied, startX, startY, size, horizontal)) {
            return false;
        }

        // Размещаем корабль
        int dx = horizontal ? 1 : 0;
        int dy = horizontal ? 0 : 1;

        for (int k = 0; k < size; k++) {
            int x = startX + dx * k;
            int y = startY + dy * k;
            occupied[y][x] = true;
        }

        return true;
    }

    // ===============================================================================
    // Вспомогательные методы
    // ===============================================================================

    /**
     * Проверяет, находится ли клетка на границе поля
     */
    private boolean isBorderCell(int x, int y) {
        return x == 0 || x == BOARD_SIZE - 1 || y == 0 || y == BOARD_SIZE - 1;
    }

    /**
     * Проверяет, находится ли клетка в углу поля
     */
    private boolean isCornerCell(int x, int y) {
        return (x == 0 && y == 0) || (x == 0 && y == BOARD_SIZE - 1) ||
                (x == BOARD_SIZE - 1 && y == 0) || (x == BOARD_SIZE - 1 && y == BOARD_SIZE - 1);
    }

    /**
     * Возвращает рекомендуемую ориентацию для клетки на границе
     */
    private boolean getPreferredOrientation(int x, int y) {
        if (y == 0 || y == BOARD_SIZE - 1) {
            return true; // Горизонтальная для верхней/нижней границы
        } else return x != 0 && x != BOARD_SIZE - 1; // Вертикальная для левой/правой границы
// По умолчанию горизонтальная
    }

    // ===============================================================================
    // Методы для тестирования и отладки
    // ===============================================================================

    /**
     * Для тестирования: возвращает статистику по размещенным кораблям
     */
    public Map<String, Integer> getPlacementStatistics(List<ShipPlacement> placements) {
        Map<String, Integer> stats = new HashMap<>();
        int coastalShips = 0;
        int horizontalShips = 0;
        int verticalShips = 0;

        for (ShipPlacement placement : placements) {
            boolean isHorizontal = !placement.vertical();
            boolean isCoastal = isShipOnCoast(placement);

            if (isCoastal) coastalShips++;
            if (isHorizontal) horizontalShips++; else verticalShips++;
        }

        stats.put("totalShips", placements.size());
        stats.put("coastalShips", coastalShips);
        stats.put("horizontalShips", horizontalShips);
        stats.put("verticalShips", verticalShips);

        return stats;
    }

    /**
     * Проверяет, находится ли корабль полностью на границе
     */
    private boolean isShipOnCoast(ShipPlacement placement) {
        boolean isHorizontal = !placement.vertical();
        int startX = placement.col();
        int startY = placement.row();
        int size = placement.size();

        if (isHorizontal) {
            // Горизонтальный корабль должен быть на верхней или нижней границе
            return startY == 0 || startY == BOARD_SIZE - 1;
        } else {
            // Вертикальный корабль должен быть на левой или правой границе
            return startX == 0 || startX == BOARD_SIZE - 1;
        }
    }

    /**
     * Валидация специфичная для береговой стратегии
     */
    public boolean validateCoastalPlacement(List<ShipPlacement> placements) {
        if (!isValidPlacement(placements)) {
            return false;
        }

        // Проверяем, что все корабли размещены по правилам береговой стратегии
        for (ShipPlacement placement : placements) {
            if (!isShipOnCoast(placement)) {
                return false;
            }
        }

        return true;
    }
}