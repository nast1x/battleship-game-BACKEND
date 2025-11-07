package com.example.battleship_game_BACKEND.repository;

import com.example.battleship_game_BACKEND.model.GameBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameBoardRepository extends JpaRepository<GameBoard, Long> {
}
