    package com.example.battleship_game_BACKEND.controller;

    import com.example.battleship_game_BACKEND.dto.*;
    import com.example.battleship_game_BACKEND.model.Game;
    import com.example.battleship_game_BACKEND.model.GameBoard;
    import com.example.battleship_game_BACKEND.model.GameStatus;
    import com.example.battleship_game_BACKEND.model.Player;
    import com.example.battleship_game_BACKEND.repository.GameBoardRepository;
    import com.example.battleship_game_BACKEND.repository.GameRepository;
    import com.example.battleship_game_BACKEND.repository.PlayerRepository;
    import com.example.battleship_game_BACKEND.service.GameLogicService;
    import com.example.battleship_game_BACKEND.service.InMemoryGameStateService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.messaging.handler.annotation.MessageMapping;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.stereotype.Controller;

    import java.time.LocalDateTime;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;
    import java.util.concurrent.ThreadLocalRandom;

    @Controller
    @RequiredArgsConstructor
    public class GameWebSocketController {

        private final PlayerRepository playerRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final GameRepository gameRepository;
        private final GameBoardRepository gameBoardRepository;
        private final Map<String, PendingGame> pendingGames = new ConcurrentHashMap<>();
        private final GameLogicService gameLogicService;
        private final InMemoryGameStateService inMemoryGameStateService;

        private String makeKey(Long a, Long b) {
            long min = Math.min(a, b);
            long max = Math.max(a, b);
            return min + "-" + max;
        }
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
            System.out.println("payload=" + forInviter + " to " + "/queue/game.start/" + inviter.getPlayerId());
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

        @MessageMapping("/game.ready")
        public void playerReady(GameReadyMessage msg) {
            Long pId = msg.getPlayerId();
            Long oppId = msg.getOpponentId();

            if (pId == null || oppId == null) {
                System.out.println("GameReady: playerId или opponentId == null");
                return;
            }

            String key = makeKey(pId, oppId);
            System.out.println("GameReady from player " + pId + " vs " + oppId + " key=" + key);

            pendingGames.compute(key, (k, existing) -> {
                if (existing == null) {
                    // Первый, кто нажал «Готов»
                    PendingGame pg = new PendingGame();
                    pg.setPlayer1Id(pId);
                    pg.setBoard1(msg.getBoardLayout());
                    pg.setGameType(msg.getGameType());
                    return pg;
                } else {
                    // Второй игрок
                    if (existing.getPlayer1Id().equals(pId)) {
                        // тот же игрок прислал второй раз — просто перезапишем доску
                        existing.setBoard1(msg.getBoardLayout());
                        return existing;
                    } else {
                        existing.setPlayer2Id(pId);
                        existing.setBoard2(msg.getBoardLayout());
                        if (existing.getGameType() == null) {
                            existing.setGameType(msg.getGameType());
                        }

                        // если оба есть и у обоих есть доски — запуск игры
                        if (existing.getPlayer1Id() != null && existing.getPlayer2Id() != null
                                && existing.getBoard1() != null && existing.getBoard2() != null) {
                            startGameFromPending(k, existing);
                            // возвращаем null → удаляем из карты
                            return null;
                        }
                        return existing;
                    }
                }
            });
        }

        private void startGameFromPending(String key, PendingGame pg) {
            Long p1Id = pg.getPlayer1Id();
            Long p2Id = pg.getPlayer2Id();

            System.out.println("Запускаем игру для пары: " + key);

            Player p1 = playerRepository.findById(p1Id)
                    .orElseThrow(() -> new RuntimeException("Player1 not found " + p1Id));
            Player p2 = playerRepository.findById(p2Id)
                    .orElseThrow(() -> new RuntimeException("Player2 not found " + p2Id));

            // 🔹 здесь ты парсишь layout в GameBoard
            GameBoard board1 = createBoardFromLayout(pg.getBoard1());
            GameBoard board2 = createBoardFromLayout(pg.getBoard2());

            gameBoardRepository.save(board1);
            gameBoardRepository.save(board2);

            Game game = new Game();
            game.setPlayer1(p1);
            game.setPlayer2(p2);
            game.setGameBoard1(board1);
            game.setGameBoard2(board2);
            game.setGameStatus(GameStatus.ACTIVE);
            game.setGameType(pg.getGameType());
            game.setStartDate(LocalDateTime.now());

            Game saved = gameRepository.save(game);

            // рандомим, кто ходит первым
            Long currentTurn = ThreadLocalRandom.current().nextBoolean() ?
                    p1.getPlayerId() : p2.getPlayerId();
            inMemoryGameStateService.initializeGameState(saved, currentTurn);

            // 1. Отправляем обоим GameStartNotification
            sendGameStartToPlayers(saved, currentTurn);


            sendGameStateToPlayer(saved.getGameId(), p1Id);
            sendGameStateToPlayer(saved.getGameId(), p2Id);
        }

        private GameBoard createBoardFromLayout(BoardLayoutDTO layout /*, Player owner если пригодится позже */) {
            GameBoard board = new GameBoard();

            // На всякий случай создаём нормальный массив 10x10
            Character[][] matrix = new Character[10][10];

            // Если layout пришёл — аккуратно копируем, иначе заполняем пробелами
            Character[][] src = (layout != null) ? layout.getMatrix() : null;

            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 10; col++) {
                    Character c = ' '; // по умолчанию пустая клетка

                    if (src != null
                            && row < src.length
                            && src[row] != null
                            && col < src[row].length
                            && src[row][col] != null) {

                        c = src[row][col];
                    }

                    matrix[row][col] = c;
                }
            }

            // Конвертируем 10x10 в строку и сохраняем в сущности
            board.setPlacementMatrixFromArray(matrix);

            // Если позже добавишь в GameBoard поле owner — тогда тут можно будет сделать:
            // board.setOwner(owner);

            return board;
        }



        private void sendGameStartToPlayers(Game game, Long currentTurnPlayerId) {
            Player p1 = game.getPlayer1();
            Player p2 = game.getPlayer2();

            GameStartNotification forP1 = new GameStartNotification();
            forP1.setGameId(game.getGameId());
            forP1.setOpponentId(p2.getPlayerId());
            forP1.setOpponentNickname(p2.getNickname());
            forP1.setOpponentAvatarUrl(p2.getAvatarUrl());
            forP1.setCurrentTurnPlayerId(currentTurnPlayerId);

            GameStartNotification forP2 = new GameStartNotification();
            forP2.setGameId(game.getGameId());
            forP2.setOpponentId(p1.getPlayerId());
            forP2.setOpponentNickname(p1.getNickname());
            forP2.setOpponentAvatarUrl(p1.getAvatarUrl());
            forP2.setCurrentTurnPlayerId(currentTurnPlayerId);

            System.out.println("Отправка уведомления о старте игры игрокам " + p1.getPlayerId() + " и " + p2.getPlayerId());
            messagingTemplate.convertAndSend(
                    "/queue/game.start/" + p1.getPlayerId(),
                    forP1
            );

            messagingTemplate.convertAndSend(
                    "/queue/game.start/" + p2.getPlayerId(),
                    forP2
            );
        }


        /**
         * Обработка действий игры (сдача, предложение ничьи)
         */

        @MessageMapping("/game.move")
        public void handleMove(GameMoveDTO move) {
            System.out.println("Получен ход от игрока " + move.getPlayerId() +
                    " в игре " + move.getGameId() +
                    " по координатам [" + move.getRow() + "," + move.getColumn() + "]");

            try {
                // ИСПРАВЛЕНИЕ: передаем все параметры отдельно
                Map<String, Object> result = gameLogicService.processMove(
                        move.getGameId(),
                        move.getPlayerId(),
                        move.getRow(),
                        move.getColumn()
                );

                // Получаем игру
                Game game = gameRepository.findById(move.getGameId())
                        .orElseThrow(() -> new RuntimeException("Game not found"));

                // Отправляем обновление обоим игрокам
                sendGameUpdateToPlayers(game, result);

            } catch (Exception e) {
                System.err.println("Ошибка при обработке хода: " + e.getMessage());

                // ИСПРАВЛЕНИЕ: создаем простой Map вместо ErrorDTO
                Map<String, Object> error = new HashMap<>();
                error.put("error", true);
                error.put("message", e.getMessage());
                error.put("gameId", move.getGameId());

                messagingTemplate.convertAndSend(
                        "/queue/game.error/" + move.getPlayerId(),
                        error
                );
            }
        }


        @MessageMapping("/game.state")
        public void handleGetGameState(GetGameStateRequest request) {
            try {
                Map<String, Object> gameState = gameLogicService.getGameStateForPlayer(
                        request.getGameId(),
                        request.getPlayerId()
                );

                messagingTemplate.convertAndSend(
                        "/queue/game.state/" + request.getPlayerId(),
                        gameState
                );

            } catch (Exception e) {
                sendErrorToPlayer(request.getPlayerId(), e.getMessage());
            }
        }

        /**
         * Отправка обновления состояния игры обоим игрокам
         */
        private void sendGameUpdateToPlayers(Game game, Map<String, Object> moveResult) {
            Long player1Id = game.getPlayer1().getPlayerId();
            Long player2Id = game.getPlayer2().getPlayerId();

            // Отправляем обновление состояния каждому игроку
            sendGameStateToPlayer(game.getGameId(), player1Id);
            sendGameStateToPlayer(game.getGameId(), player2Id);

            // Если игра завершена, отправляем уведомление о завершении
            if (game.getGameStatus() == GameStatus.COMPLETED) {
                sendGameEndNotification(game);
            }
        }

        /**
         * Отправка состояния игры конкретному игроку
         */
        private void sendGameStateToPlayer(Long gameId, Long playerId) {
            try {
                Map<String, Object> gameState = gameLogicService.getGameStateForPlayer(gameId, playerId);
                messagingTemplate.convertAndSend(
                        "/queue/game.state/" + playerId,
                        gameState
                );
            } catch (Exception e) {
                System.err.println("Ошибка при отправке состояния игроку " + playerId + ": " + e.getMessage());
            }
        }

        /**
         * Отправка уведомления об окончании игры
         */
        private void sendGameEndNotification(Game game) {
            Map<String, Object> endNotification = new HashMap<>();
            endNotification.put("gameId", game.getGameId());
            endNotification.put("result", game.getResult());

            // Определяем победителя
            if ("PLAYER1_WINS".equals(game.getResult())) {
                endNotification.put("winnerId", game.getPlayer1().getPlayerId());
            } else if ("PLAYER2_WINS".equals(game.getResult())) {
                endNotification.put("winnerId", game.getPlayer2().getPlayerId());
            } else {
                endNotification.put("draw", true);
            }

            messagingTemplate.convertAndSend(
                    "/queue/game.end/" + game.getPlayer1().getPlayerId(),
                    endNotification
            );

            messagingTemplate.convertAndSend(
                    "/queue/game.end/" + game.getPlayer2().getPlayerId(),
                    endNotification
            );
        }

        /**
         * Отправка ошибки игроку
         */
        private void sendErrorToPlayer(Long playerId, String errorMessage) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", errorMessage);

            messagingTemplate.convertAndSend(
                    "/queue/game.error/" + playerId,
                    error
            );
        }
        @MessageMapping("/game.action")
        public void handleGameAction(GameActionDTO action) {
            try {
                Game game = gameRepository.findById(action.getGameId())
                        .orElseThrow(() -> new RuntimeException("Game not found"));

                switch (action.getActionType()) {
                    case "SURRENDER":
                        handleSurrender(game, action.getPlayerId());
                        break;
                    case "OFFER_DRAW":
                        handleDrawOffer(game, action.getPlayerId());
                        break;
                    case "ACCEPT_DRAW":
                        handleDrawAccept(game, action.getPlayerId());
                        break;
                    case "DECLINE_DRAW":
                        handleDrawDecline(game, action.getPlayerId());
                        break;
                }
            } catch (Exception e) {
                sendErrorToPlayer(action.getPlayerId(), e.getMessage());
            }
        }

        private void handleSurrender(Game game, Long playerId) {
            game.setGameStatus(GameStatus.COMPLETED);
            game.setEndDate(LocalDateTime.now());

            if (game.getPlayer1().getPlayerId().equals(playerId)) {
                game.setResult("PLAYER2_WINS_SURRENDER");
            } else {
                game.setResult("PLAYER1_WINS_SURRENDER");
            }

            gameRepository.save(game);

            // Удаляем состояние из памяти
            inMemoryGameStateService.removeGameState(game.getGameId());

            sendGameEndNotification(game);
        }

        private void handleDrawOffer(Game game, Long playerId) {
            Map<String, Object> drawOffer = new HashMap<>();
            drawOffer.put("gameId", game.getGameId());
            drawOffer.put("fromPlayerId", playerId);
            drawOffer.put("action", "DRAW_OFFER");

            Long opponentId = getOpponentId(game, playerId);
            messagingTemplate.convertAndSend(
                    "/queue/game.draw/" + opponentId,
                    drawOffer
            );
        }

        private void handleDrawAccept(Game game, Long playerId) {
            game.setGameStatus(GameStatus.COMPLETED);
            game.setEndDate(LocalDateTime.now());
            game.setResult("DRAW");

            gameRepository.save(game);

            // Удаляем состояние из памяти
            inMemoryGameStateService.removeGameState(game.getGameId());

            sendGameEndNotification(game);
        }

        private void handleDrawDecline(Game game, Long playerId) {
            Map<String, Object> drawDecline = new HashMap<>();
            drawDecline.put("gameId", game.getGameId());
            drawDecline.put("fromPlayerId", playerId);
            drawDecline.put("action", "DRAW_DECLINED");

            Long opponentId = getOpponentId(game, playerId);
            messagingTemplate.convertAndSend(
                    "/queue/game.draw/" + opponentId,
                    drawDecline
            );
        }

        private Long getOpponentId(Game game, Long playerId) {
            if (game.getPlayer1().getPlayerId().equals(playerId)) {
                return game.getPlayer2().getPlayerId();
            } else {
                return game.getPlayer1().getPlayerId();
            }
        }

    }
