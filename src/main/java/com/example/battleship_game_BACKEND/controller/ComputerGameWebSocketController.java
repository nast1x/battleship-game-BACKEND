package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.ShotRequest;
import com.example.battleship_game_BACKEND.dto.ShotResponse;
import com.example.battleship_game_BACKEND.service.ComputerGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ComputerGameWebSocketController {

    private final ComputerGameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Обработка выстрела через WebSocket
     */
    @MessageMapping("/computer-game/{gameId}/shot")
    public void handleShot(
            @DestinationVariable Long gameId,
            ShotRequest request) {

        log.info("WebSocket shot request for game {}: ({}, {})",
                gameId, request.getRow(), request.getCol());

        try {
            // Устанавливаем gameId из пути в запрос
            request.setGameId(gameId);

            ShotResponse response = gameService.processPlayerShot(gameId, request);

            // Отправляем результат игроку
            messagingTemplate.convertAndSend(
                    "/topic/computer-game/" + gameId + "/shot-result",
                    response
            );

        } catch (Exception e) {
            log.error("Error processing shot: {}", e.getMessage());

            // Отправляем ошибку
            ShotResponse errorResponse = new ShotResponse();
            errorResponse.setMessage("Ошибка: " + e.getMessage());
            errorResponse.setHit(false);

            messagingTemplate.convertAndSend(
                    "/topic/computer-game/" + gameId + "/error",
                    errorResponse
            );
        }
    }

    /**
     * Подписка на игру
     */
    @MessageMapping("/computer-game/{gameId}/subscribe")
    public void subscribeToGame(@DestinationVariable Long gameId) {
        log.info("Player subscribed to game {}", gameId);

        // Отправляем текущее состояние игры
        // Временное решение - просто отправляем gameId
        messagingTemplate.convertAndSend(
                "/topic/computer-game/" + gameId + "/state",
                Map.of("gameId", gameId, "status", "SUBSCRIBED")
        );
    }
}