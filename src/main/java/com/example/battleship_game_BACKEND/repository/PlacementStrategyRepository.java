package com.example.battleship_game_BACKEND.repository;

import com.example.battleship_game_BACKEND.model.PlacementStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlacementStrategyRepository extends JpaRepository<PlacementStrategy, Long> {
    List<PlacementStrategy> findByPlayerPlayerId(Long playerId);
}
