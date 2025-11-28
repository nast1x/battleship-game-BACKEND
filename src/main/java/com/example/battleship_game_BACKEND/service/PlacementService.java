package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.*;
import com.example.battleship_game_BACKEND.model.ShipPlacement;
import com.example.battleship_game_BACKEND.placement.BasePlacementStrategy;
import com.example.battleship_game_BACKEND.repository.PlacementStrategyRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlacementService {

    private final Map<String, BasePlacementStrategy> strategies;
    @Getter
    private final PlacementStrategyRepository repository;

    public PlacementService(List<BasePlacementStrategy> strategyList,
                            PlacementStrategyRepository repository) {
        this.repository = repository;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        strategy -> {
                            String className = strategy.getClass().getSimpleName();
                            return className.replace("Strategy", "").toLowerCase();
                        },
                        Function.identity()
                ));
    }

    public PlacementResponse generatePlacement(PlacementRequest request) {
        try {
            // Получаем стратегию
            BasePlacementStrategy strategy = strategies.get(request.strategy());
            if (strategy == null) {
                strategy = strategies.get("random"); // fallback
            }

            if (strategy == null) {
                return new PlacementResponse(false, "Стратегия не найдена", null, null);
            }

            // Генерируем расстановку
            List<ShipPlacement> serverPlacements = strategy.generatePlacement();
            List<ShipPlacementDto> placements = convertToDto(serverPlacements);

            // Сохраняем если нужно
            if (request.saveToProfile()) {
                saveUserPlacement(new SavePlacementRequest(
                        request.userId(),
                        "Авто-" + request.strategy(),
                        placements
                ));
            }

            return new PlacementResponse(true, "Расстановка успешно сгенерирована", placements, null);

        } catch (Exception e) {
            return new PlacementResponse(false, "Ошибка генерации: " + e.getMessage(), null, null);
        }
    }

    public void saveUserPlacement(SavePlacementRequest request) {
        // TODO: Реализовать сохранение пользовательской расстановки
        // Пока заглушка - можно сохранять в базу или кеш
        System.out.println("Сохранение расстановки для пользователя: " + request.userId());
        System.out.println("Название: " + request.placementName());
        System.out.println("Корабли: " + request.ships().size());
    }

    public List<UserPlacementResponse> getUserPlacements(String userId) {
        // TODO: Реализовать загрузку пользовательских расстановок
        // Пока возвращаем пустой список
        return List.of();
    }

    private List<ShipPlacementDto> convertToDto(List<ShipPlacement> serverPlacements) {
        return serverPlacements.stream()
                .map(sp -> new ShipPlacementDto(
                        sp.shipId(),
                        sp.size(),
                        sp.row(),
                        sp.col(),
                        sp.vertical()
                ))
                .collect(Collectors.toList());
    }

}