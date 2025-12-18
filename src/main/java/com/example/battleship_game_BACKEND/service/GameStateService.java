package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.ShipPlacementDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameStateService {

    // Храним состояние игры в памяти
    private final Map<Long, GameState> gameStates = new ConcurrentHashMap<>();

    /**
     * Получить состояние игры по ID
     */
    public GameState getGameState(Long gameId) {
        return gameStates.computeIfAbsent(gameId, k -> new GameState());
    }

    /**
     * Сохранить состояние игры
     */
    public void saveGameState(Long gameId, GameState state) {
        gameStates.put(gameId, state);
    }

    /**
     * Удалить состояние игры
     */
    public void removeGameState(Long gameId) {
        gameStates.remove(gameId);
    }

    /**
     * Получить корабли для доски
     */
    public List<ShipPlacementDto> getShipsForBoard(Long gameId, Long boardId) {
        GameState state = getGameState(gameId);
        return state.getBoardShips().getOrDefault(boardId, new ArrayList<>());
    }

    /**
     * Сохранить корабли для доски
     */
    public void saveShipsForBoard(Long gameId, Long boardId, List<ShipPlacementDto> ships) {
        GameState state = getGameState(gameId);
        state.getBoardShips().put(boardId, new ArrayList<>(ships));
    }

    /**
     * Класс для хранения состояния игры
     */
    @Data
    public static class GameState {
        // Корабли для каждой доски: boardId -> список кораблей
        private Map<Long, List<ShipPlacementDto>> boardShips = new HashMap<>();

        // Выстрелы игрока
        private List<int[]> playerShots = new ArrayList<>();

        // Выстрелы компьютера
        private List<int[]> computerShots = new ArrayList<>();

        // Дополнительные метаданные игры
        private Map<String, Object> metadata = new HashMap<>();
    }
}