package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.*;
import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.placement.*;
import com.example.battleship_game_BACKEND.repository.PlacementStrategyRepository;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PlacementService {

    private final PlacementStrategyRepository placementStrategyRepository;
    private final PlayerRepository playerRepository;

    public PlacementService(
            PlacementStrategyRepository placementStrategyRepository,
            PlayerRepository playerRepository) {
        this.placementStrategyRepository = placementStrategyRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Генерация расстановки кораблей
     */
    public PlacementResponse generatePlacement(PlacementRequest request) {
        String strategyName = request.strategyName();
        List<ShipPlacementDto> ships = generateShipsByStrategy(strategyName);

        return new PlacementResponse(ships); // Для record
    }

    /**
     * Генерация кораблей по стратегии (метод для совместимости со старым кодом)
     */
    public PlacementResponse generatePlacement(String strategyName) {
        List<ShipPlacementDto> ships = generateShipsByStrategy(strategyName);
        return new PlacementResponse(ships); // Для record
    }

    /**
     * Генерация кораблей по имени стратегии
     */
    private List<ShipPlacementDto> generateShipsByStrategy(String strategyName) {
        BasePlacementStrategy strategy = createPlacementStrategy(strategyName);
        List<PlacementStrategy.ShipPlacement> placements = strategy.generatePlacement();

        return placements.stream()
                .map(p -> new ShipPlacementDto(
                        p.shipId(),
                        p.size(),
                        p.row(),
                        p.col(),
                        p.vertical()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Создание стратегии размещения
     */
    private BasePlacementStrategy createPlacementStrategy(String strategyName) {
        Random random = new Random();

        switch (strategyName.toUpperCase()) {
            case "COASTS":
                return new CoastsPlacer(null, random);
            case "DIAGONAL":
                return new DiagonalPlacer(null, random);
            case "HALFFIELD":
                return new HalfFieldPlacer(null, random);
            case "RANDOM":
            default:
                return new BasePlacementStrategy(null, random) {
                    @Override
                    protected List<Map.Entry<Integer, Integer>> scanCells() {
                        return generateRandomCells();
                    }
                };
        }
    }

    /**
     * Сохранение пользовательской расстановки
     */
    @Transactional
    public void saveUserPlacement(SavePlacementRequest request) {
        // Используем методы record без get
        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        // Проверяем, существует ли уже стратегия с таким именем
        if (placementStrategyRepository.existsByPlayerAndStrategyName(player, request.strategyName())) {
            throw new RuntimeException("Strategy with this name already exists");
        }

        PlacementStrategy strategy = new PlacementStrategy();
        strategy.setPlayer(player);
        strategy.setStrategyName(request.strategyName());
        strategy.setPlacementDataFromList(
                request.ships().stream()  // Используем ships() вместо getShips()
                        .map(ShipPlacementDto::toPlacementStrategy)
                        .collect(Collectors.toList())
        );

        placementStrategyRepository.save(strategy);
    }

    /**
     * Получение расстановок пользователя
     */
    public List<UserPlacementResponse> getUserPlacements(Long playerId) {
        // Проверяем, существует ли игрок
        if (!playerRepository.existsById(playerId)) {
            throw new RuntimeException("Player not found");
        }

        List<PlacementStrategy> strategies = placementStrategyRepository.findByPlayerPlayerId(playerId);

        return strategies.stream()
                .map(strategy -> new UserPlacementResponse(
                        strategy.getStrategyId(),
                        strategy.getStrategyName(),
                        strategy.getPlacementDataAsList().stream()
                                .map(placement -> new ShipPlacementDto(
                                        placement.shipId(),
                                        placement.size(),
                                        placement.row(),
                                        placement.col(),
                                        placement.vertical()
                                ))
                                .toList()
                ))
                .toList();
    }
}