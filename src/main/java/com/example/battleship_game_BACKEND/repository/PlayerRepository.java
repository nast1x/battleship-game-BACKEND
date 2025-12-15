package com.example.battleship_game_BACKEND.repository;

import com.example.battleship_game_BACKEND.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByNickname(String nickname);
    boolean existsByNickname(String nickname);
    List<Player> findByStatus(boolean status);
}
