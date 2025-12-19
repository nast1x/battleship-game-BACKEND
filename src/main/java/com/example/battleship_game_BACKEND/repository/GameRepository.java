package com.example.battleship_game_BACKEND.repository;

import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameStatus;
import com.example.battleship_game_BACKEND.model.GameType;
import com.example.battleship_game_BACKEND.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    // Явное объявление метода findById
    Optional<Game> findById(Long id);

    // Найти все игры игрока
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId OR g.player2.playerId = :playerId")
    List<Game> findByPlayerId(@Param("playerId") Long playerId);

    // Найти активную singleplayer игру
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId AND g.gameType = 'SINGLEPLAYER' AND g.gameStatus = 'ACTIVE'")
    Optional<Game> findActiveSingleplayerGame(@Param("playerId") Long playerId);

    // Найти активную игру против компьютера
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId AND g.player2.nickname = 'Компьютер' AND g.gameStatus = 'ACTIVE'")
    Optional<Game> findActiveGameAgainstComputer(@Param("playerId") Long playerId);

    // Проверить существует ли активная игра для игрока
    boolean existsByPlayer1PlayerIdAndGameStatus(Long playerId, GameStatus status);

    // Найти игры по типу и статусу
    List<Game> findByGameTypeAndGameStatus(GameType gameType, GameStatus status);

    // Найти все игры против компьютера для игрока
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId AND g.player2.nickname = 'Компьютер'")
    List<Game> findGamesAgainstComputer(@Param("playerId") Long playerId);
}