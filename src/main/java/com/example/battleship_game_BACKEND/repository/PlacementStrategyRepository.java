package com.example.battleship_game_BACKEND.repository;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import com.example.battleship_game_BACKEND.model.Player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


/*@Repository
public interface PlacementStrategyRepository extends JpaRepository<PlacementStrategy, Long> {
    List<PlacementStrategy> findByPlayerPlayerId(Long playerId);
}*/
@Repository
public interface PlacementStrategyRepository extends JpaRepository<PlacementStrategy, Long> {
    List<PlacementStrategy> findByPlayerPlayerId(Long playerId);
    Optional<PlacementStrategy> findByPlayerAndStrategyName(Player player, String strategyName);
    boolean existsByPlayerAndStrategyName(Player player, String strategyName);
}