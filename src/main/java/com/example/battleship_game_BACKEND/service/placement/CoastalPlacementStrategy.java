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

        // Размещение кораблей только по краям
        // 4-палубный (0 - левый край)
        matrix[0][0] = 'S'; matrix[0][1] = 'S'; matrix[0][2] = 'S'; matrix[0][3] = 'S';

        // 3-палубные
        matrix[9][0] = 'S'; matrix[9][1] = 'S'; matrix[9][2] = 'S'; // нижний край
        matrix[0][9] = 'S'; matrix[1][9] = 'S'; matrix[2][9] = 'S'; // правый край

        // 2-палубные
        matrix[9][7] = 'S'; matrix[9][8] = 'S'; // нижний край
        matrix[7][0] = 'S'; matrix[8][0] = 'S'; // левый край
        matrix[0][5] = 'S'; matrix[1][5] = 'S'; // верхний край

        // 1-палубные
        matrix[9][9] = 'S'; // правый нижний угол
        matrix[0][7] = 'S'; // верхний край
        matrix[3][0] = 'S'; // левый край
        matrix[6][9] = 'S'; // правый край

        PlacementStrategy strategy = new PlacementStrategy();
        strategy.setPlayer(player);
        strategy.setStrategyName("Береговая стратегия");
        strategy.setPlacementMatrixFromArray(matrix);

        return strategy;
    }
}