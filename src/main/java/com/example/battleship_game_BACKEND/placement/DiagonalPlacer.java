package com.example.battleship_game_BACKEND.placement;

import java.util.*;

public class DiagonalPlacer extends BasePlacementStrategy {

    public DiagonalPlacer() {
        super();
    }

    public DiagonalPlacer(Random rand) {
        super(rand);
    }

    @Override
    protected List<Map.Entry<Integer, Integer>> scanCells() {
        List<Map.Entry<Integer, Integer>> cells = new ArrayList<>();

        // Добавляем все клетки, кроме тех, что на главной и побочной диагонали
        for (int r = 0; r <= 9; r++) {
            for (int c = 0; c <= 9; c++) {
                // Пропускаем клетки на главной и побочной диагонали
                if (r == c || r + c == 9) continue;
                cells.add(new AbstractMap.SimpleEntry<>(r, c));
            }
        }

        // Перемешиваем результат
        Collections.shuffle(cells, rand);
        return cells;
    }

    // Переопределяем canPlace, чтобы запретить палубы на диагоналях
    @Override
    protected boolean canPlace(
            boolean[][] occ,
            int x0, int y0,
            int size,
            boolean horizontal
    ) {
        // Сначала проверяем базовые границы
        if (!super.canPlace(occ, x0, y0, size, horizontal)) {
            return false;
        }

        // Проверяем, что ни одна палуба не попадёт на r==c или r+c==9
        int dx = horizontal ? 1 : 0;
        int dy = horizontal ? 0 : 1;

        for (int k = 0; k < size; k++) {
            int x = x0 + dx * k;
            int y = y0 + dy * k;

            if (x == y || x + y == 9) {
                return false;
            }
        }

        return true;
    }
}