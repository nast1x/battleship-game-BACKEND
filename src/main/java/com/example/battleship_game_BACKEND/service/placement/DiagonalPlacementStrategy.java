package com.example.battleship_game_BACKEND.service.placement;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import org.springframework.stereotype.Component;

@Component
public class DiagonalPlacementStrategy {

    public PlacementStrategy createDiagonalStrategy(Player player) {
        Character[][] matrix = new Character[10][10];

        // Инициализация пустым полем
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        // ====== Главная диагональ (стратегическое расположение) ======

        // 4-палубный: горизонтальный в верхнем левом углу
        placeShip(matrix, 0, 0, 4, true); // (0,0)-(0,3)

        // 3-палубные: вдоль главной диагонали
        placeShip(matrix, 2, 2, 3, false); // (2,2)-(4,2) - вертикальный
        placeShip(matrix, 4, 4, 3, false); // (4,4)-(6,4) - вертикальный

        // 2-палубные: симметрично главной диагонали
        placeShip(matrix, 0, 9, 2, false); // (0,9)-(1,9) - правый верхний угол
        placeShip(matrix, 7, 2, 2, true);  // (7,2)-(7,3) - нижняя часть
        placeShip(matrix, 8, 7, 2, false); // (8,7)-(9,7) - правая часть

        // 1-палубные: по диагонали
        matrix[9][0] = 'S'; // Левый нижний угол
        matrix[9][9] = 'S'; // Правый нижний угол
        matrix[5][8] = 'S'; // Симметрия
        matrix[3][6] = 'S'; // Симметрия

        PlacementStrategy strategy = new PlacementStrategy();
        strategy.setPlayer(player);
        strategy.setStrategyName("Диагональная стратегия");
        strategy.setPlacementMatrixFromArray(matrix);

        return strategy;
    }

    /**
     * Безопасное размещение корабля с проверкой границ
     */
    void placeShip(Character[][] matrix, int startRow, int startCol, int length, boolean horizontal) {
        // Проверка выхода за границы
        if (horizontal) {
            if (startCol + length > 10) {
                throw new IllegalArgumentException("Горизонтальный корабль выходит за границы доски");
            }
            for (int j = startCol; j < startCol + length; j++) {
                if (matrix[startRow][j] == 'S') {
                    throw new IllegalStateException("Конфликт кораблей в позиции [" + startRow + "][" + j + "]");
                }
                matrix[startRow][j] = 'S';
            }
        } else {
            if (startRow + length > 10) {
                throw new IllegalArgumentException("Вертикальный корабль выходит за границы доски");
            }
            for (int i = startRow; i < startRow + length; i++) {
                if (matrix[i][startCol] == 'S') {
                    throw new IllegalStateException("Конфликт кораблей в позиции [" + i + "][" + startCol + "]");
                }
                matrix[i][startCol] = 'S';
            }
        }
    }
}