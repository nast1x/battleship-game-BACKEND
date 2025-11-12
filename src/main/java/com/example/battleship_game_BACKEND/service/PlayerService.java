package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    public Player createPlayer(Player player) {
        return playerRepository.save(player);
    }

    public Optional<Player> getPlayerByNickname(String nickname) {
        return playerRepository.findByNickname(nickname);
    }

    public boolean nicknameExists(String nickname) {
        return playerRepository.existsByNickname(nickname);
    }

    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }
}