package com.example.battleship_game_BACKEND.config;

import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ComputerPlayerConfig {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ComputerPlayerConfig(
            PlayerRepository playerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Transactional
    public CommandLineRunner initComputerPlayer() {
        return args -> {
            // Проверяем существование по уникальному никнейму
            if (playerRepository.findByNickname("COMPUTER").isEmpty()) {
                Player computer = new Player();
                computer.setNickname("COMPUTER");
                // Корректно хешируем пароль
                computer.setPassword(passwordEncoder.encode("computer_default_password"));
                computer.setAvatarUrl("computer_avatar.png");
                computer.setStatus(true);

                // Сохраняем и получаем сгенерированный ID
                Player savedPlayer = playerRepository.save(computer);
                System.out.println("Computer player initialized with ID: " + savedPlayer.getPlayerId());
            } else {
                System.out.println("Computer player already exists");
            }
        };
    }
}