package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.PlayerProfileDTO;
import com.example.battleship_game_BACKEND.dto.UserDTO;
import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameStatus;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.repository.GameRepository;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;

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

    public List<Player> getOnlinePlayers() {
        return playerRepository.findByStatus(true); // ← JPA-метод
    }

    public PlayerProfileDTO getPlayerProfileWithStats(Player player) {
        Long pid = player.getPlayerId();

        // Получаем все завершенные игры
        List<Game> games = gameRepository.findByPlayer1PlayerIdOrPlayer2PlayerId(pid, pid)
                .stream()
                .filter(g -> g.getGameStatus() == GameStatus.COMPLETED)
                .toList();

        int total = games.size();

        // Считаем победы (где result == string ID игрока)
        int wins = (int) games.stream()
                .filter(g -> g.getResult() != null && g.getResult().equals(pid.toString()))
                .count();

        // Считаем поражения (результат не ничья и не наш ID)
        int losses = (int) games.stream()
                .filter(g -> g.getResult() != null
                        && !g.getResult().equals("DRAW")
                        && !g.getResult().equals(pid.toString()))
                .count();

        // Собираем DTO
        PlayerProfileDTO profile = new PlayerProfileDTO();
        profile.setPlayerId(pid);
        profile.setNickname(player.getNickname());
        profile.setAvatarUrl(player.getAvatarUrl());
        profile.setTotalGames(total);
        profile.setWins(wins);
        profile.setLosses(losses);

        return profile;
    }

}