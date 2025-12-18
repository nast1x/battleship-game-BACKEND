package com.example.battleship_game_BACKEND.service.placement;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import org.springframework.stereotype.Component;

@Component
public class DiagonalPlacementStrategy {

    public PlacementStrategy createDiagonalStrategy(Player player) {
        Character[][] matrix = new Character[10][10];

        // Инициализация
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        // Главная диагональ и побочная диагональ
        // 4-палубный по главной диагонали
        matrix[0][0] = 'S'; matrix[1][1] = 'S'; matrix[2][2] = 'S'; matrix[3][3] = 'S';

        // 3-палубные
        matrix[5][5] = 'S'; matrix[6][6] = 'S'; matrix[7][7] = 'S'; // продолжение главной диагонали
        matrix[2][7] = 'S'; matrix[3][6] = 'S'; matrix[4][5] = 'S'; // побочная диагональ

        // 2-палубные
        matrix[0][9] = 'S'; matrix[1][8] = 'S'; // побочная диагональ (угол)
        matrix[8][2] = 'S'; matrix[9][3] = 'S'; // смещенная диагональ
        matrix[6][0] = 'S'; matrix[7][1] = 'S'; // около главной диагонали

        // 1-палубные
        matrix[4][0] = 'S'; // около диагонали
        matrix[9][9] = 'S'; // противоположный угол
        matrix[0][5] = 'S'; // симметрично
        matrix[5][0] = 'S'; // симметрично

        PlacementStrategy strategy = new PlacementStrategy();
        strategy.setPlayer(player);
        strategy.setStrategyName("Диагональная стратегия");
        strategy.setPlacementMatrixFromArray(matrix);

        return strategy;
    }
}