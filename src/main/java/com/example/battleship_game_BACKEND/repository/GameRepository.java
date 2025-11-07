package com.example.battleship_game_BACKEND.repository;

import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByGameStatus(GameStatus status);
    List<Game> findByPlayer1PlayerIdOrPlayer2PlayerId(Long player1Id, Long player2Id);
}
