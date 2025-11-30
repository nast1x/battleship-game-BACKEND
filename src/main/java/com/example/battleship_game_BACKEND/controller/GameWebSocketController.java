package com.example.battleship_game_BACKEND.controller;

import com.example.battleship_game_BACKEND.dto.*;
import com.example.battleship_game_BACKEND.model.Player;
import com.example.battleship_game_BACKEND.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 1. ИНИЦИАТОР шлёт приглашение
     * payload: GameInvitationRequest(inviterId, opponentId, inviterNickname, inviterAvatarUrl)
     * Мы НИЧЕГО не создаём в БД, просто пересылаем оппоненту.
     */
    @MessageMapping("/game.invite")
    public void sendInvitation(GameInvitationRequest request) {
        System.out.println("WS invite request = " + request);

        Player inviter = playerRepository.findById(request.getInviterId())
                .orElseThrow(() -> new RuntimeException("Inviter not found"));
        Player opponent = playerRepository.findById(request.getOpponentId())
                .orElseThrow(() -> new RuntimeException("Opponent not found"));

        GameInvitationResponse response = new GameInvitationResponse();
        // пока реальной игры нет — можно поставить 0L
        response.setGameId(0L);
        response.setInviterId(inviter.getPlayerId());
        response.setInviterNickname(inviter.getNickname());
        response.setInviterAvatarUrl(inviter.getAvatarUrl());
        response.setTimestamp(LocalDateTime.now());

        // Шлём ТОЛЬКО оппоненту
        messagingTemplate.convertAndSend(
                "/queue/invitations/" + opponent.getPlayerId(),
                response
        );
    }

    /**
     * 2. ОППОНЕНТ принял приглашение
     * payload: GameAcceptRequest(inviterId, opponentId)
     * Никакой Game в БД — просто шлём нотификации обоим.
     */
    @MessageMapping("/game.accept")
    public void acceptInvitation(GameAcceptRequest request) {
        Player inviter = playerRepository.findById(request.getInviterId())
                .orElseThrow(() -> new RuntimeException("Inviter not found"));
        Player opponent = playerRepository.findById(request.getOpponentId())
                .orElseThrow(() -> new RuntimeException("Opponent not found"));

        GameStartNotification forInviter = new GameStartNotification();
        forInviter.setGameId(null);
        forInviter.setOpponentId(opponent.getPlayerId());
        forInviter.setOpponentNickname(opponent.getNickname());
        forInviter.setOpponentAvatarUrl(opponent.getAvatarUrl());

        GameStartNotification forOpponent = new GameStartNotification();
        forOpponent.setGameId(null);
        forOpponent.setOpponentId(inviter.getPlayerId());
        forOpponent.setOpponentNickname(inviter.getNickname());
        forOpponent.setOpponentAvatarUrl(inviter.getAvatarUrl());

        // инициатор
        messagingTemplate.convertAndSend(
                "/queue/game.start/" + inviter.getPlayerId(),
                forInviter
        );
        // принявший
        messagingTemplate.convertAndSend(
                "/queue/game.start/" + opponent.getPlayerId(),
                forOpponent
        );
    }


    /**
     * 3. ОППОНЕНТ отклонил приглашение
     * payload: GameRejectRequest(inviterId, opponentId)
     * Просто уведомляем инициатора, что его послали :)
     */
    @MessageMapping("/game.reject")
    public void rejectInvitation(GameRejectRequest request) {
        Player inviter = playerRepository.findById(request.getInviterId())
                .orElseThrow(() -> new RuntimeException("Inviter not found"));
        Player opponent = playerRepository.findById(request.getOpponentId())
                .orElseThrow(() -> new RuntimeException("Opponent not found"));

        GameStartNotification notification = new GameStartNotification();
        notification.setGameId(null);
        notification.setOpponentId(opponent.getPlayerId());
        notification.setOpponentNickname("rejected");
        notification.setOpponentAvatarUrl(null);

        messagingTemplate.convertAndSend(
                "/queue/game.rejected/" + inviter.getPlayerId(),
                notification
        );
    }

}
