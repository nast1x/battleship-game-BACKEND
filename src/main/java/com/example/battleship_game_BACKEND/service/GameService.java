package com.example.battleship_game_BACKEND.service;

import com.example.battleship_game_BACKEND.dto.GameCreateRequest;
import com.example.battleship_game_BACKEND.dto.GameCreatedResponse;
import com.example.battleship_game_BACKEND.model.*;
import com.example.battleship_game_BACKEND.repository.GameBoardRepository;
import com.example.battleship_game_BACKEND.repository.GameRepository;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {

    private final PlayerRepository playerRepository;
    private final GameBoardRepository gameBoardRepository;
    private final GameRepository gameRepository;

    @Transactional
    public GameCreatedResponse createGame(GameCreateRequest request) {
        Player p1 = playerRepository.findById(request.getPlayer1Id())
                .orElseThrow(() -> new IllegalArgumentException("Player1 not found: " + request.getPlayer1Id()));
        Player p2 = playerRepository.findById(request.getPlayer2Id())
                .orElseThrow(() -> new IllegalArgumentException("Player2 not found: " + request.getPlayer2Id()));

        GameBoard board1 = gameBoardRepository.findById(request.getGameBoard1Id())
                .orElseThrow(() -> new IllegalArgumentException("GameBoard1 not found: " + request.getGameBoard1Id()));
        GameBoard board2 = gameBoardRepository.findById(request.getGameBoard2Id())
                .orElseThrow(() -> new IllegalArgumentException("GameBoard2 not found: " + request.getGameBoard2Id()));

        Game game = new Game();
        game.setPlayer1(p1);
        game.setPlayer2(p2);
        game.setGameBoard1(board1);
        game.setGameBoard2(board2);
        game.setGameStatus(GameStatus.ACTIVE);
        game.setGameType(request.getGameType()); // TODO: передавай нужный тип
        game.setStartDate(LocalDateTime.now());

        Game saved = gameRepository.save(game);

        GameCreatedResponse resp = new GameCreatedResponse();
        resp.setGameId(saved.getGameId());
        resp.setPlayer1Id(saved.getPlayer1().getPlayerId());
        resp.setPlayer2Id(saved.getPlayer2().getPlayerId());
        resp.setGameBoard1Id(saved.getGameBoard1().getGameBoardId());
        resp.setGameBoard2Id(saved.getGameBoard2().getGameBoardId());
        resp.setGameStatus(saved.getGameStatus());
        resp.setGameType(saved.getGameType());

        return resp;
    }
}
