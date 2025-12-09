package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.GameCreateRequest;
import com.example.battleship_game_BACKEND.dto.GameCreatedResponse;
import com.example.battleship_game_BACKEND.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * key = gameId, value = playerId, который сейчас ходит
     * Пока нам нужен только первый ход, но карта пригодится и для дальнейшей логики.
     */
    private final Map<Long, Long> currentTurnByGame = new ConcurrentHashMap<>();

    @PostMapping("/start")
    public ResponseEntity<GameCreatedResponse> startGame(@RequestBody GameCreateRequest request) {
        // 1. Создаём игру в БД
        GameCreatedResponse response = gameService.createGame(request);

        // 2. Рандомим, кто ходит первым: player1 или player2
        Long firstPlayerId = ThreadLocalRandom.current().nextBoolean()
                ? response.getPlayer1Id()
                : response.getPlayer2Id();

        // 3. Сохраняем в памяти, кто сейчас ходит
        currentTurnByGame.put(response.getGameId(), firstPlayerId);

        // 4. Отдаём на фронт
        response.setCurrentTurnPlayerId(firstPlayerId);

        return ResponseEntity.ok(response);
    }

    /**
     * Если вдруг фронту нужно узнать/перепроверить, кто сейчас ходит.
     * Можно пока не использовать, но на будущее пригодится.
     */
    @GetMapping("/{gameId}/turn")
    public ResponseEntity<Long> getCurrentTurn(@PathVariable Long gameId) {
        Long playerId = currentTurnByGame.get(gameId);
        if (playerId == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(playerId);
    }

    /**
     * Пример метода для переключения хода (на будущее, когда будем делать выстрелы).
     */
    @PostMapping("/{gameId}/switch-turn")
    public ResponseEntity<Void> switchTurn(
            @PathVariable Long gameId,
            @RequestParam Long player1Id,
            @RequestParam Long player2Id
    ) {
        Long current = currentTurnByGame.get(gameId);
        if (current == null) {
            return ResponseEntity.notFound().build();
        }
        Long next = current.equals(player1Id) ? player2Id : player1Id;
        currentTurnByGame.put(gameId, next);
        return ResponseEntity.ok().build();
    }
}
