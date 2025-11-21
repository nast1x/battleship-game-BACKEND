package com.example.battleship_game_BACKEND.shooting;

import java.util.*;

/**
 * Стратегия «Случайная с добиванием».
 * 1) Пока нет попаданий → выбираем любую случайную клетку из availableCells.
 * 2) Если есть попадание, мы записываем его в huntHits (список попаданий текущего корабля)
 *    и пересоздаём очередь добивания (huntQueue) через enqueueBasedOnHits().
 * 3) При потоплении вычисляем буфер вокруг всех hitCells (с помощью computeBuffer()),
 *    удаляем из availableCells сам корабль (hitCells) и весь буфер, а затем сбрасываем режим добивания.
 * В любой момент стратегия НЕ вернёт клетку, которой нет в availableCells.
 */
public class RandomFinishingStrategy extends BaseShootingStrategy {

    private final Random random = new Random();

    /** Множество всех ещё не обстрелянных клеток. */
    private final Set<ShotCoordinate> availableCells = new HashSet<>();

    public RandomFinishingStrategy() {
        initializeAvailableCells();
    }

    private void initializeAvailableCells() {
        // Заполняем все клетки поля (0-9 по x и y)
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                availableCells.add(ShotCoordinate.of(col, row));
            }
        }
    }

    @Override
    public ShotCoordinate getNextShot() {
        // 1) Если очередь добивания непуста, пытаемся взять оттуда первую доступную клетку
        while (!huntQueue.isEmpty()) {
            ShotCoordinate cell = huntQueue.removeFirst();
            if (availableCells.contains(cell)) {
                availableCells.remove(cell);
                return cell;
            }
        }

        // 2) Иначе выбираем случайную клетку из availableCells
        if (!availableCells.isEmpty()) {
            int randomIndex = random.nextInt(availableCells.size());
            Iterator<ShotCoordinate> iterator = availableCells.iterator();
            ShotCoordinate cell = null;
            for (int i = 0; i <= randomIndex; i++) {
                cell = iterator.next();
            }
            availableCells.remove(cell);
            return cell;
        }

        // 3) На всякий случай (теоретически недостижимо) – возвращаем первую не tried клетку
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                ShotCoordinate coordinate = ShotCoordinate.of(col, row);
                if (hasTried(coordinate)) {
                    return coordinate;
                }
            }
        }

        throw new IllegalStateException("No available cells to shoot");
    }

    @Override
    public void setShotResult(boolean hit, boolean sunk) {
        ShotCoordinate lastShot = getLastShot();
        if (lastShot == null) return;

        if (hit && sunk) {
            // ——— Потопили корабль ———
            // 1) Добавляем эту клетку в huntHits, если ещё не было
            if (!huntHits.contains(lastShot)) {
                huntHits.add(lastShot);
            }
            // 2) Считаем буфер вокруг всех палуб потопленного корабля
            Set<ShotCoordinate> buffer = computeBuffer(huntHits);
            // 3) Удаляем из availableCells и сам корабль (huntHits), и весь буфер
            availableCells.removeAll(huntHits);
            availableCells.removeAll(buffer);
            // 4) Полный сброс добивания
            resetHuntMode();
        } else if (hit) {
            // ——— Просто попадание (еще не потопили) ———
            // 1) Сохраняем попадание
            huntHits.add(lastShot);
            // 2) Перестраиваем очередь добивания
            enqueueBasedOnHits();
        }
        // ——— Промах ——— ничего не делаем: точка уже удалена из availableCells на этапе getNextShot
    }

    /**
     * Строит «буфер» вокруг каждой клетки shipCells (8 соседей),
     * исключая сами shipCells.
     */
    private Set<ShotCoordinate> computeBuffer(List<ShotCoordinate> shipCells) {
        Set<ShotCoordinate> buffer = new HashSet<>();
        for (ShotCoordinate cell : shipCells) {
            int x = cell.x();
            int y = cell.y();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    int newX = x + dx;
                    int newY = y + dy;

                    if (newX >= 0 && newX < 10 && newY >= 0 && newY < 10) {
                        buffer.add(ShotCoordinate.of(newX, newY));
                    }
                }
            }
        }
        buffer.removeAll(shipCells);
        return buffer;
    }

    /**
     * Проверяет, была ли уже обстреляна клетка
     */
    protected boolean hasTried(ShotCoordinate coordinate) {
        return availableCells.contains(coordinate);
    }

    @Override
    protected ShotCoordinate computeNextShot() {
        return null;
    }

    /**
     * Получает последний сделанный выстрел
     */
    public ShotCoordinate getLastShot() {
        // Эта логика зависит от реализации BaseShootingStrategy
        // Предположим, что в базовом классе есть метод для этого
        // Если нет, нужно будет отслеживать последний выстрел в этой стратегии
        return null; // Заглушка - в реальной реализации нужно получить последний выстрел
    }
}