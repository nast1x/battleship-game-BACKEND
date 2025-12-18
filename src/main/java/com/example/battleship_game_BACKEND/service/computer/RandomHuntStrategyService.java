package com.example.battleship_game_BACKEND.service.computer;

import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameBoard;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RandomHuntStrategyService {

    Random random = new Random();

    /**
     * Получить следующий выстрел компьютера
     */
    public int[] getNextShot(Game game, GameBoard enemyBoard) {
        // Получаем состояние компьютера из поля result
        ComputerState state = parseComputerState(game.getResult());

        // Получаем матрицу доски противника
        Character[][] enemyMatrix = getBoardMatrix(enemyBoard);

        if (state.isHuntMode()) {
            return getHuntShot(state, enemyMatrix);
        } else {
            return getRandomShot(state, enemyMatrix);
        }
    }

    /**
     * Обновить состояние после выстрела
     */
    public void updateAfterShot(Game game, int row, int col, boolean hit, boolean sunk) {
        ComputerState state = parseComputerState(game.getResult());
        state.addShot(row, col, hit);

        // Если корабль потоплен, сбрасываем режим охоты
        if (sunk) {
            state.resetHuntMode();
        }

        // Сохраняем обновленное состояние в поле result
        game.setResult(state.serialize());
    }

    /**
     * Класс для хранения состояния компьютера
     * Хранится в виде строки в поле result игры
     */
    @Data
    public static class ComputerState {
        private boolean huntMode = false;
        private int lastHitRow = -1;
        private int lastHitCol = -1;
        private String direction = null;
        private List<String> shotHistory = new ArrayList<>();
        private List<String> missedAroundLastHit = new ArrayList<>();

        public void addShot(int row, int col, boolean hit) {
            String coord = row + "," + col;
            shotHistory.add(coord + ":" + (hit ? "HIT" : "MISS"));

            if (hit) {
                huntMode = true;
                lastHitRow = row;
                lastHitCol = col;
                direction = null;
                missedAroundLastHit.clear();
            } else if (huntMode) {
                missedAroundLastHit.add(coord);
            }
        }

        public boolean hasShotAt(int row, int col) {
            String coord = row + "," + col;
            for (String shot : shotHistory) {
                if (shot.startsWith(coord + ":")) {
                    return true;
                }
            }
            return false;
        }

        public void resetHuntMode() {
            huntMode = false;
            lastHitRow = -1;
            lastHitCol = -1;
            direction = null;
        }

        public String serialize() {
            StringBuilder sb = new StringBuilder();
            sb.append("COMPUTER_STATE:");
            sb.append("hunt=").append(huntMode).append(";");
            sb.append("lastHit=").append(lastHitRow).append(",").append(lastHitCol).append(";");
            sb.append("dir=").append(direction != null ? direction : "null").append(";");
            // Используем точку с запятой как разделитель записей
            sb.append("shots=").append(String.join(";", shotHistory)).append(";");
            sb.append("missed=").append(String.join(";", missedAroundLastHit));
            return sb.toString();
        }

        public static ComputerState deserialize(String stateStr) {
            ComputerState state = new ComputerState();
            if (stateStr == null || !stateStr.startsWith("COMPUTER_STATE:")) {
                return state;
            }

            String content = stateStr.substring("COMPUTER_STATE:".length());
            String[] parts = content.split(";");

            for (String part : parts) {
                if (part.startsWith("hunt=")) {
                    state.huntMode = Boolean.parseBoolean(part.substring(5));
                } else if (part.startsWith("lastHit=")) {
                    String[] coords = part.substring(8).split(",");
                    if (coords.length == 2 && !coords[0].equals("-1")) {
                        state.lastHitRow = Integer.parseInt(coords[0]);
                        state.lastHitCol = Integer.parseInt(coords[1]);
                    }
                } else if (part.startsWith("dir=")) {
                    String dir = part.substring(4);
                    state.direction = dir.equals("null") ? null : dir;
                } else if (part.startsWith("shots=")) {
                    String shots = part.substring(6);
                    if (!shots.isEmpty()) {
                        // Используем вертикальную черту как разделитель записей
                        String[] shotArray = shots.split("\\|");
                        for (String shot : shotArray) {
                            if (!shot.isEmpty()) {
                                state.shotHistory.add(shot);
                            }
                        }
                    }
                } else if (part.startsWith("missed=")) {
                    String missed = part.substring(7);
                    if (!missed.isEmpty()) {
                        // Используем вертикальную черту как разделитель записей
                        String[] missedArray = missed.split("\\|");
                        for (String m : missedArray) {
                            if (!m.isEmpty()) {
                                state.missedAroundLastHit.add(m);
                            }
                        }
                    }
                }
            }
            return state;
        }
    }

    /**
     * Распарсить состояние компьютера из строки
     */
    public ComputerState parseComputerState(String result) {
        if (result == null || result.isEmpty()) {
            return new ComputerState();
        }

        // Проверяем, содержит ли result состояние компьютера
        if (result.startsWith("COMPUTER_STATE:")) {
            return ComputerState.deserialize(result);
        }

        // Если в result что-то другое (например, результат игры),
        // создаем новое состояние
        return new ComputerState();
    }

    /**
     * Получить случайный выстрел
     */
    int[] getRandomShot(ComputerState state, Character[][] enemyMatrix) {
        List<int[]> availableCells = new ArrayList<>();

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                if (!state.hasShotAt(row, col)) {
                    availableCells.add(new int[]{row, col});
                }
            }
        }

        if (availableCells.isEmpty()) {
            return new int[]{random.nextInt(10), random.nextInt(10)};
        }

        return availableCells.get(random.nextInt(availableCells.size()));
    }

    /**
     * Получить выстрел в режиме добивания
     */
    int[] getHuntShot(ComputerState state, Character[][] enemyMatrix) {
        int lastHitRow = state.getLastHitRow();
        int lastHitCol = state.getLastHitCol();

        if (state.getDirection() == null) {
            List<String> directions = new ArrayList<>(Arrays.asList("UP", "DOWN", "LEFT", "RIGHT"));

            for (String dir : directions) {
                int[] target = getTargetInDirection(lastHitRow, lastHitCol, dir, 1);
                if (isValidTarget(target[0], target[1], state)) {
                    state.setDirection(dir);
                    return target;
                }
            }

            state.resetHuntMode();
            return getRandomShot(state, enemyMatrix);
        } else {
            String currentDir = state.getDirection();
            int distance = 1;

            while (true) {
                int[] target = getTargetInDirection(lastHitRow, lastHitCol, currentDir, distance);

                if (!isValidTarget(target[0], target[1], state)) {
                    String oppositeDir = getOppositeDirection(currentDir);
                    int[] oppositeTarget = getTargetInDirection(lastHitRow, lastHitCol, oppositeDir, 1);

                    if (isValidTarget(oppositeTarget[0], oppositeTarget[1], state)) {
                        state.setDirection(oppositeDir);
                        return oppositeTarget;
                    } else {
                        state.resetHuntMode();
                        return getRandomShot(state, enemyMatrix);
                    }
                }

                if (!state.hasShotAt(target[0], target[1])) {
                    return target;
                }

                distance++;
            }
        }
    }

    /**
     * Получить цель в заданном направлении
     */
    private int[] getTargetInDirection(int row, int col, String direction, int distance) {
        switch (direction) {
            case "UP": return new int[]{row - distance, col};
            case "DOWN": return new int[]{row + distance, col};
            case "LEFT": return new int[]{row, col - distance};
            case "RIGHT": return new int[]{row, col + distance};
            default: return new int[]{row, col};
        }
    }

    /**
     * Получить противоположное направление
     */
    private String getOppositeDirection(String direction) {
        switch (direction) {
            case "UP": return "DOWN";
            case "DOWN": return "UP";
            case "LEFT": return "RIGHT";
            case "RIGHT": return "LEFT";
            default: return direction;
        }
    }

    /**
     * Проверить валидность цели
     */
    boolean isValidTarget(int row, int col, ComputerState state) {
        return row >= 0 && row < 10 && col >= 0 && col < 10 && !state.hasShotAt(row, col);
    }

    /**
     * Конвертировать GameBoard в матрицу
     */
    Character[][] getBoardMatrix(GameBoard gameBoard) {
        Character[][] matrix = new Character[10][10];
        String placement = gameBoard.getPlacementMatrix();

        if (placement != null && !placement.isEmpty()) {
            String[] rows = placement.split(";");
            for (int i = 0; i < rows.length && i < 10; i++) {
                String[] cols = rows[i].split(",");
                for (int j = 0; j < cols.length && j < 10; j++) {
                    matrix[i][j] = cols[j].charAt(0);
                }
            }
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (matrix[i][j] == null) {
                    matrix[i][j] = ' ';
                }
            }
        }

        return matrix;
    }
}