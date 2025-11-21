package com.example.battleship_game_BACKEND.placement;

import java.util.*;

/**
 * Расставляет корабли на границах поля (строки 0 или 9 для горизонтали,
 * столбцы 0 или 9 для вертикали).
 */
public class CoastsPlacer extends BasePlacementStrategy {

    public CoastsPlacer() {
        super();
    }

    public CoastsPlacer(Random rand) {
        super(rand);
    }

    @Override
    protected List<Map.Entry<Integer, Integer>> scanCells() {
        List<Map.Entry<Integer, Integer>> cells = new ArrayList<>();

        // Верхняя и нижняя строка: (r=0, c=0..9), (r=9, c=0..9)
        for (int c = 0; c <= 9; c++) {
            cells.add(new AbstractMap.SimpleEntry<>(0, c));
            cells.add(new AbstractMap.SimpleEntry<>(9, c));
        }

        // Левая и правая колонка: (r=1..8, c=0), (r=1..8, c=9)
        for (int r = 1; r <= 8; r++) {
            cells.add(new AbstractMap.SimpleEntry<>(r, 0));
            cells.add(new AbstractMap.SimpleEntry<>(r, 9));
        }

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
        boolean requiredOrientation;

        // Верхняя/нижняя граница - только горизонтальная ориентация
        if (y0 == 0 || y0 == 9) {
            requiredOrientation = true;
        }
        // Левая/правая граница - только вертикальная ориентация
        else if (x0 == 0 || x0 == 9) {
            requiredOrientation = false;
        }
        // Для клеток рядом с границей разрешаем обе ориентации
        else {
            requiredOrientation = horizontal;
        }

        // Если запрошенная ориентация не соответствует требуемой - отказ
        if (horizontal != requiredOrientation) {
            return false;
        }

        return super.canPlace(occ, x0, y0, size, horizontal);
    }
}