package com.example.battleship_game_BACKEND.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    // Основная информация профиля
    private Long player_id;
    private String nickname;
    private String email;
    private String avatarUrl;

    // Поля статистики для отображения в профиле
    private int totalGames;
    private int wins;
    private int losses;

    // Другие поля, если они используются на фронтенде
    // Например, для сохраненных расстановок
    private int savedLayouts;
}