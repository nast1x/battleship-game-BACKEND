package com.example.battleship_game_BACKEND.placement;

import com.example.battleship_game_BACKEND.model.ShipPlacement;
import com.example.battleship_game_BACKEND.repository.PlacementStrategyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Стратегия размещения кораблей, избегающая главной и побочной диагоналей.
 * Корабли размещаются только в клетках, не лежащих на диагоналях.
 */
@Component
public class DiagonalPlacer extends BasePlacementStrategy {

    // ===============================================================================
    // Конструкторы (СИНХРОНИЗИРОВАНЫ)
    // ===============================================================================

    /**
     * Конструктор для Spring
     */
    @Autowired
    public DiagonalPlacer(PlacementStrategyRepository placementStrategyRepository) {
        super(placementStrategyRepository);
    }

    /**
     * Конструктор для тестирования с контролируемым Random
     */
    public DiagonalPlacer(PlacementStrategyRepository placementStrategyRepository, Random rand) {
        super(placementStrategyRepository, rand);
    }

    // ===============================================================================
    // Основная логика размещения (ОПТИМИЗИРОВАНА)
    // ===============================================================================

    @Override
    protected List<Map.Entry<Integer, Integer>> scanCells() {
        List<Map.Entry<Integer, Integer>> cells = new ArrayList<>();

        // Генерируем только допустимые клетки (не на диагоналях)
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!isOnDiagonal(row, col)) {
                    cells.add(cell(row, col));
                }
            }
        }

        // Перемешиваем для случайного порядка
        Collections.shuffle(cells, rand);
        return cells;
    }

    // ===============================================================================
    // Логика проверки размещения (СИНХРОНИЗИРОВАНА)
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

        // Затем проверяем диагональные ограничения
        return !wouldPlaceOnDiagonal(startX, startY, size, horizontal);
    }

    /**
     * Проверяет, находится ли клетка на главной или побочной диагонали
     */
    private boolean isOnDiagonal(int row, int col) {
        return row == col || row + col == BOARD_SIZE - 1;
    }

    /**
     * Проверяет, будет ли корабль пересекать диагонали
     */
    private boolean wouldPlaceOnDiagonal(int startX, int startY, int size, boolean horizontal) {
        int dx = horizontal ? 1 : 0;
        int dy = horizontal ? 0 : 1;

        for (int k = 0; k < size; k++) {
            int x = startX + dx * k;
            int y = startY + dy * k;

            if (isOnDiagonal(y, x)) { // Обратите внимание: y - строка, x - столбец
                return true;
            }
        }

        return false;
    }

    // ===============================================================================
    // Оптимизированная версия tryPlace для диагональной стратегии
    // ===============================================================================

    @Override
    protected boolean tryPlace(
            boolean[][] occupied,
            int startX, int startY,
            int size,
            boolean horizontal
    ) {
        // Сначала проверяем диагональные ограничения
        if (wouldPlaceOnDiagonal(startX, startY, size, horizontal)) {
            return false;
        }

        // Затем проверяем базовые ограничения
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
     * Возвращает количество допустимых клеток для размещения
     */
    public int getAvailableCellCount() {
        int count = 0;
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!isOnDiagonal(row, col)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Возвращает список всех запрещенных (диагональных) клеток
     */
    public List<Map.Entry<Integer, Integer>> getDiagonalCells() {
        List<Map.Entry<Integer, Integer>> diagonalCells = new ArrayList<>();

        for (int i = 0; i < BOARD_SIZE; i++) {
            // Главная диагональ
            diagonalCells.add(cell(i, i));
            // Побочная диагональ (исключая пересечение с главной)
            if (i != BOARD_SIZE - 1 - i) {
                diagonalCells.add(cell(i, BOARD_SIZE - 1 - i));
            }
        }

        return diagonalCells;
    }

    /**
     * Проверяет, является ли размещение допустимым для диагональной стратегии
     */
    public boolean isValidDiagonalPlacement(List<ShipPlacement> placements) {
        if (!isValidPlacement(placements)) {
            return false;
        }

        // Проверяем, что ни один корабль не пересекает диагонали
        for (ShipPlacement placement : placements) {
            boolean isHorizontal = !placement.isVertical();
            if (wouldPlaceOnDiagonal(
                    placement.getCol(),
                    placement.getRow(),
                    placement.getSize(),
                    isHorizontal
            )) {
                return false;
            }
        }

        return true;
    }

    // ===============================================================================
    // Методы для анализа и отладки
    // ===============================================================================

    /**
     * Возвращает матрицу с отметками диагональных клеток
     */
    public boolean[][] getDiagonalMatrix() {
        boolean[][] diagonalMatrix = new boolean[BOARD_SIZE][BOARD_SIZE];

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                diagonalMatrix[row][col] = isOnDiagonal(row, col);
            }
        }

        return diagonalMatrix;
    }

    /**
     * Выводит отладочную информацию о размещении
     */
    public void printPlacementInfo(List<ShipPlacement> placements) {
        System.out.println("Diagonal Placement Strategy Info:");
        System.out.println("Total diagonal cells: " + (BOARD_SIZE * 2 - 1)); // -1 для учета пересечения
        System.out.println("Total available cells: " + getAvailableCellCount());
        System.out.println("Total ships placed: " + placements.size());
        System.out.println("Valid diagonal placement: " + isValidDiagonalPlacement(placements));

        // Статистика по ориентациям
        long horizontalShips = placements.stream()
                .filter(p -> !p.isVertical())
                .count();
        System.out.println("Horizontal ships: " + horizontalShips);
        System.out.println("Vertical ships: " + (placements.size() - horizontalShips));
    }

    // ===============================================================================
    // Альтернативная оптимизированная реализация scanCells
    // ===============================================================================

    /**
     * Альтернативная реализация, которая генерирует клетки в более оптимальном порядке
     */
    protected List<Map.Entry<Integer, Integer>> scanCellsOptimized() {
        List<Map.Entry<Integer, Integer>> cells = new ArrayList<>();

        // Разделяем поле на зоны для более равномерного распределения
        for (int zone = 0; zone < 4; zone++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    // Пропускаем диагональные клетки
                    if (isOnDiagonal(row, col)) {
                        continue;
                    }

                    // Распределяем по зонам для лучшего покрытия
                    boolean inZone = switch (zone) {
                        case 0 -> (row < BOARD_SIZE / 2 && col < BOARD_SIZE / 2); // Левый верхний квадрант
                        case 1 -> (row < BOARD_SIZE / 2 && col >= BOARD_SIZE / 2); // Правый верхний квадрант
                        case 2 -> (row >= BOARD_SIZE / 2 && col < BOARD_SIZE / 2); // Левый нижний квадрант
                        case 3 -> (row >= BOARD_SIZE / 2 && col >= BOARD_SIZE / 2);
                        default -> false; // Правый нижний квадрант
                    };

                    if (inZone) {
                        cells.add(cell(row, col));
                    }
                }
            }
        }

        // Перемешиваем внутри каждой зоны для случайности
        Collections.shuffle(cells, rand);
        return cells;
    }
}