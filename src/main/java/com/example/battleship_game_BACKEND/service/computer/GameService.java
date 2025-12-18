package com.example.battleship_game_BACKEND.service.computer;

import com.example.battleship_game_BACKEND.model.*;
import com.example.battleship_game_BACKEND.repository.GameRepository;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ComputerGameService computerGameService;

    /**
     * Создать игру с компьютером
     */
    public Game createSinglePlayerGame(Player humanPlayer) {
        // Создаем компьютерного игрока
        Player computerPlayer = getOrCreateComputerPlayer();

        // Создаем доски
        GameBoard humanBoard = createGameBoardWithShips(); // Доска игрока с кораблями
        GameBoard computerBoard = createComputerBoard(); // Доска компьютера с кораблями

        // Создаем игру
        Game game = new Game();
        game.setPlayer1(humanPlayer);
        game.setPlayer2(computerPlayer);
        game.setGameBoard1(humanBoard);
        game.setGameBoard2(computerBoard);
        game.setGameType(GameType.SINGLEPLAYER);
        game.setGameStatus(GameStatus.ACTIVE);
        game.setStartDate(LocalDateTime.now());

        // Инициализируем состояние компьютера
        computerGameService.initializeComputerState(game);

        return gameRepository.save(game);
    }

    /**
     * Получить или создать компьютерного игрока
     */
    private Player getOrCreateComputerPlayer() {
        return playerRepository.findByNickname("Компьютер")
                .orElseGet(() -> {
                    Player computer = new Player();
                    computer.setNickname("Компьютер");
                    computer.setPassword("computer");
                    computer.setAvatarUrl(Player.DEFAULT_AVATAR);
                    computer.setStatus(true);
                    return playerRepository.save(computer);
                });
    }

    /**
     * Создать доску с кораблями для игрока
     */
    private GameBoard createGameBoardWithShips() {
        // Здесь должна быть логика расстановки кораблей игроком
        // Для примера создадим пустую доску
        GameBoard board = new GameBoard();
        board.setPlacementMatrix(createEmptyBoardString());
        return board;
    }

    /**
     * Создать доску с кораблями для компьютера
     */
    private GameBoard createComputerBoard() {
        // Случайная расстановка кораблей компьютера
        GameBoard board = new GameBoard();
        board.setPlacementMatrix(generateRandomShips());
        return board;
    }

    /**
     * Создать строку пустой доски
     */
    private String createEmptyBoardString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i > 0) sb.append(";");
            for (int j = 0; j < 10; j++) {
                if (j > 0) sb.append(",");
                sb.append(" "); // Пустая клетка
            }
        }
        return sb.toString();
    }

    /**
     * Сгенерировать случайную расстановку кораблей
     */
    private String generateRandomShips() {
        Character[][] matrix = new Character[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = ' ';
            }
        }

        // Простая расстановка (можно улучшить)
        // 4-палубный
        matrix[0][0] = 'S'; matrix[0][1] = 'S'; matrix[0][2] = 'S'; matrix[0][3] = 'S';

        // 3-палубные
        matrix[2][2] = 'S'; matrix[2][3] = 'S'; matrix[2][4] = 'S';
        matrix[4][5] = 'S'; matrix[5][5] = 'S'; matrix[6][5] = 'S';

        // 2-палубные
        matrix[8][0] = 'S'; matrix[8][1] = 'S';
        matrix[9][8] = 'S'; matrix[9][9] = 'S';

        // 1-палубные
        matrix[3][7] = 'S';
        matrix[7][7] = 'S';

        return convertMatrixToString(matrix);
    }

    private String convertMatrixToString(Character[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            if (i > 0) sb.append(";");
            for (int j = 0; j < matrix[i].length; j++) {
                if (j > 0) sb.append(",");
                sb.append(matrix[i][j]);
            }
        }
        return sb.toString();
    }
}