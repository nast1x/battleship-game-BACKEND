package com.example.battleship_game_BACKEND.repository;

import com.example.battleship_game_BACKEND.model.ComputerGame;
import com.example.battleship_game_BACKEND.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComputerGameRepository extends JpaRepository<ComputerGame, Long> {

    @Query("SELECT cg FROM ComputerGame cg WHERE cg.player.playerId = :playerId AND cg.gameStatus = :status")
    List<ComputerGame> findByPlayerPlayerIdAndGameStatus(
            @Param("playerId") Long playerId,
            @Param("status") GameStatus status
    );

    List<ComputerGame> findByPlayerPlayerId(Long playerId);
}