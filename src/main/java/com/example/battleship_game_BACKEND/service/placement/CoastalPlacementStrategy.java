package com.example.battleship_game_BACKEND.service.placement;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import org.springframework.stereotype.Component;

@Component
public class CoastalPlacementStrategy {

    public PlacementStrategy createCoastalStrategy(Player player) {
        Character[][] matrix = new Character[10][10];

        // Инициализация пустым полем
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' '; // Море
            }
        }

        // === 1 четырехпалубный корабль (4 клетки) ===
        // Размещаем у верхнего края
        for (int j = 0; j < 4; j++) {
            matrix[0][j] = 'S'; // (0,0)-(0,3)
        }

        // === 2 трехпалубных корабля (3 клетки каждый) ===
        // Первый трехпалубный - у левого края, вертикальный
        for (int i = 0; i < 3; i++) {
            matrix[i][0] = 'S'; // (0,0)-(2,0)
        }

        // Второй трехпалубный - у правого края, вертикальный
        for (int i = 7; i < 10; i++) {
            matrix[i][9] = 'S'; // (7,9)-(9,9)
        }

        // === 3 двухпалубных корабля (2 клетки каждый) ===
        // Первый - у нижнего края
        matrix[9][2] = 'S';
        matrix[9][3] = 'S'; // (9,2)-(9,3)

        // Второй - у верхнего края справа
        matrix[0][7] = 'S';
        matrix[0][8] = 'S'; // (0,7)-(0,8)

        // Третий - у правого края в середине
        matrix[4][9] = 'S';
        matrix[5][9] = 'S'; // (4,9)-(5,9)

        // === 4 однопалубных корабля ===
        matrix[9][6] = 'S';  // Нижний край
        matrix[2][9] = 'S';  // Правый край (учитывая, что (1,9) и (2,9) свободны от касания)
        matrix[0][5] = 'S';  // Верхний край
        matrix[6][0] = 'S';  // Левый край

        PlacementStrategy strategy = new PlacementStrategy();
        strategy.setPlayer(player);
        strategy.setStrategyName("Береговая стратегия v2");
        strategy.setPlacementMatrixFromArray(matrix);

        return strategy;
    }
}