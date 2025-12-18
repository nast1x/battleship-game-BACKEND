package com.example.battleship_game_BACKEND.repository;

import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameStatus;
import com.example.battleship_game_BACKEND.model.GameType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    // Найти все игры игрока (вне зависимости от его роли - player1 или player2)
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId OR g.player2.playerId = :playerId")
    List<Game> findByPlayerId(@Param("playerId") Long playerId);

    // Найти singleplayer игры игрока (где он player1)
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId AND g.gameType = 'SINGLEPLAYER'")
    List<Game> findSingleplayerGamesByPlayerId(@Param("playerId") Long playerId);

    // Найти singleplayer игры по статусу
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId AND g.gameType = 'SINGLEPLAYER' AND g.gameStatus = :status")
    List<Game> findSingleplayerGamesByPlayerIdAndStatus(
            @Param("playerId") Long playerId,
            @Param("status") GameStatus status
    );

    // Найти активную singleplayer игру
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId AND g.gameType = 'SINGLEPLAYER' AND g.gameStatus = 'ACTIVE'")
    Optional<Game> findActiveSingleplayerGame(@Param("playerId") Long playerId);

    // Найти игры по типу
    List<Game> findByGameType(GameType gameType);

    // Найти игры где игрок - player1
    List<Game> findByPlayer1PlayerId(Long playerId);

    // Найти игры где игрок - player2
    List<Game> findByPlayer2PlayerId(Long playerId);

    // Найти игры по обоим игрокам
    @Query("SELECT g FROM Game g WHERE (g.player1.playerId = :player1Id AND g.player2.playerId = :player2Id) OR (g.player1.playerId = :player2Id AND g.player2.playerId = :player1Id)")
    List<Game> findGamesBetweenPlayers(@Param("player1Id") Long player1Id, @Param("player2Id") Long player2Id);

    // Найти игры против компьютера для игрока
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId AND g.player2.nickname = 'COMPUTER'")
    List<Game> findGamesAgainstComputer(@Param("playerId") Long playerId);

    // Найти активную игру против компьютера
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId AND g.player2.nickname = 'COMPUTER' AND g.gameStatus = 'ACTIVE'")
    Optional<Game> findActiveGameAgainstComputer(@Param("playerId") Long playerId);

    // Найти ожидающие игры определенного типа
    List<Game> findByGameTypeAndGameStatus(GameType gameType, GameStatus waiting);

    // Проверить существует ли игра для игрока с определенным статусом
    boolean existsByPlayer1PlayerIdAndGameStatus(Long playerId, GameStatus status);
}