package com.example.battleship_game_BACKEND.service.placement;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import org.springframework.stereotype.Component;

@Component
public class HalfPlacementStrategy {

    public PlacementStrategy createHalfStrategy(Player player) {
        Character[][] matrix = new Character[10][10];

        // Инициализация
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        // Размещаем корабли только в левой половине (колонки 0-4)
        // 4-палубный горизонтально
        matrix[2][0] = 'S'; matrix[2][1] = 'S'; matrix[2][2] = 'S'; matrix[2][3] = 'S';

        // 3-палубные (исправлено: избегаем пересечения)
        matrix[5][0] = 'S'; matrix[5][1] = 'S'; matrix[5][2] = 'S'; // горизонтально
        matrix[0][4] = 'S'; matrix[1][4] = 'S'; matrix[2][4] = 'S'; // вертикально у границы

        // 2-палубные (исправлено: избегаем пересечения с 3-палубным)
        matrix[7][0] = 'S'; matrix[7][1] = 'S'; // горизонтально
        matrix[8][3] = 'S'; matrix[9][3] = 'S'; // вертикально
        matrix[4][2] = 'S'; matrix[4][3] = 'S'; // изменили на [4][3] вместо [5][2]

        // 1-палубные
        matrix[0][0] = 'S'; // левый верхний угол
        matrix[9][0] = 'S'; // левый нижний угол
        matrix[3][1] = 'S'; // центр левой половины
        matrix[6][4] = 'S'; // у правой границы левой половины

        PlacementStrategy strategy = new PlacementStrategy();
        strategy.setPlayer(player);
        strategy.setStrategyName("Половинчатая стратегия");
        strategy.setPlacementMatrixFromArray(matrix);

        return strategy;
    }

    // Вариант для правой половины (исправлено)
    public PlacementStrategy createHalfStrategyRight(Player player) {
        Character[][] matrix = new Character[10][10];

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        // Размещаем корабли только в правой половине (колонки 5-9)
        // 4-палубный
        matrix[2][5] = 'S'; matrix[2][6] = 'S'; matrix[2][7] = 'S'; matrix[2][8] = 'S';

        // 3-палубные
        matrix[5][5] = 'S'; matrix[5][6] = 'S'; matrix[5][7] = 'S';
        matrix[0][9] = 'S'; matrix[1][9] = 'S'; matrix[2][9] = 'S';

        // 2-палубные (исправлено: избегаем пересечения)
        matrix[7][5] = 'S'; matrix[7][6] = 'S';
        matrix[8][8] = 'S'; matrix[9][8] = 'S';
        matrix[4][7] = 'S'; matrix[4][8] = 'S'; // изменили на [4][8] вместо [5][7]

        // 1-палубные
        matrix[0][5] = 'S';
        matrix[9][5] = 'S';
        matrix[3][6] = 'S';
        matrix[6][9] = 'S';

        PlacementStrategy strategy = new PlacementStrategy();
        strategy.setPlayer(player);
        strategy.setStrategyName("Половинчатая стратегия (правая)");
        strategy.setPlacementMatrixFromArray(matrix);

        return strategy;
    }
}