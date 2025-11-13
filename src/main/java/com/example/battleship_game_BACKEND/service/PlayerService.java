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
        // Устанавливаем аватар по умолчанию при создании
        if (player.getAvatarUrl() == null) {
            player.setAvatarUrl(Player.DEFAULT_AVATAR);
        }
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

    public Player updateAvatar(Long playerId, String avatarFileName) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + playerId));

        // Проверяем, что аватар из списка допустимых
        if (isValidAvatar(avatarFileName)) {
            player.setAvatarUrl(avatarFileName);
            return playerRepository.save(player);
        } else {
            throw new RuntimeException("Invalid avatar filename: " + avatarFileName);
        }
    }

    public String[] getAvailableAvatars() {
        return Player.DEFAULT_AVATARS;
    }

    private boolean isValidAvatar(String avatarFileName) {
        for (String validAvatar : Player.DEFAULT_AVATARS) {
            if (validAvatar.equals(avatarFileName)) {
                return true;
            }
        }
        return false;
    }

    public Player save(Player player) {
        return playerRepository.save(player);
    }
}