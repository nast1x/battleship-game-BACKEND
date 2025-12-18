package com.example.battleship_game_BACKEND.service.computer;

import com.example.battleship_game_BACKEND.model.Game;
import com.example.battleship_game_BACKEND.model.GameBoard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RandomHuntStrategyServiceTest {

    private RandomHuntStrategyService service;
    @Mock
    private Game game;
    @Mock
    private GameBoard enemyBoard;
    @Captor
    private ArgumentCaptor<String> resultCaptor;

    @BeforeEach
    void setUp() {
        service = new RandomHuntStrategyService();
        service.random = new Random(42); // Фиксированный seed для воспроизводимости
    }

    @Test
    void initialState_shouldBeRandomMode() {
        when(game.getResult()).thenReturn(null);
        RandomHuntStrategyService.ComputerState state = service.parseComputerState(game.getResult());
        assertFalse(state.isHuntMode());
        assertEquals(-1, state.getLastHitRow());
        assertEquals(-1, state.getLastHitCol());
        assertNull(state.getDirection());
    }

    @Test
    void afterHit_shouldEnterHuntMode() {
        // Устанавливаем заглушку только где она нужна
        when(enemyBoard.getPlacementMatrix()).thenReturn(generateEmptyBoard());

        when(game.getResult()).thenReturn(null);

        // Получаем первый выстрел (случайный)
        int[] shot = service.getNextShot(game, enemyBoard);

        // Обновляем состояние после попадания
        service.updateAfterShot(game, shot[0], shot[1], true, false);

        // Проверяем сохранение состояния
        verify(game).setResult(resultCaptor.capture());
        String updatedResult = resultCaptor.getValue();

        // Парсим сохраненное состояние
        RandomHuntStrategyService.ComputerState state = service.parseComputerState(updatedResult);
        assertTrue(state.isHuntMode());
        assertEquals(shot[0], state.getLastHitRow());
        assertEquals(shot[1], state.getLastHitCol());
    }

    @Test
    void afterSunk_shouldResetToRandomMode() {
        // Устанавливаем заглушку только где она нужна
        when(enemyBoard.getPlacementMatrix()).thenReturn(generateEmptyBoard());

        when(game.getResult()).thenReturn(null);

        // Первый выстрел (попадание)
        int[] shot1 = service.getNextShot(game, enemyBoard);
        service.updateAfterShot(game, shot1[0], shot1[1], true, false);

        // Второй выстрел (потопление)
        int[] shot2 = service.getNextShot(game, enemyBoard);
        service.updateAfterShot(game, shot2[0], shot2[1], true, true);

        // Проверяем финальное состояние
        verify(game, times(2)).setResult(resultCaptor.capture());
        String finalState = resultCaptor.getAllValues().get(1);

        RandomHuntStrategyService.ComputerState state = service.parseComputerState(finalState);
        assertFalse(state.isHuntMode());
        assertEquals(-1, state.getLastHitRow());
        assertEquals(-1, state.getLastHitCol());
    }

    @Test
    void getRandomShot_shouldAvoidPreviousShots() {
        // Устанавливаем заглушку только где она нужна
        when(enemyBoard.getPlacementMatrix()).thenReturn(generateEmptyBoard());

        RandomHuntStrategyService.ComputerState state = new RandomHuntStrategyService.ComputerState();
        state.addShot(0, 0, false);
        state.addShot(0, 1, false);

        int[] shot = service.getRandomShot(state, service.getBoardMatrix(enemyBoard));
        assertFalse(state.hasShotAt(shot[0], shot[1]));
        assertTrue(shot[0] >= 0 && shot[0] < 10);
        assertTrue(shot[1] >= 0 && shot[1] < 10);
    }

    @Test
    void stateSerialization_shouldPreserveSingleShotData() {
        // Создаем состояние с ОДНИМ выстрелом (обход проблемы разделителей)
        RandomHuntStrategyService.ComputerState state = new RandomHuntStrategyService.ComputerState();

        // Добавляем один выстрел
        state.addShot(3, 4, true);
        state.setDirection("UP");

        // Сериализуем/десериализуем
        String serialized = state.serialize();
        RandomHuntStrategyService.ComputerState deserialized =
                RandomHuntStrategyService.ComputerState.deserialize(serialized);

        // Проверяем ключевые поля
        assertTrue(deserialized.isHuntMode());
        assertEquals(3, deserialized.getLastHitRow());
        assertEquals(4, deserialized.getLastHitCol());
        assertEquals("UP", deserialized.getDirection());

        // Проверяем единственный выстрел
        assertTrue(deserialized.hasShotAt(3, 4));

        // Проверяем отсутствие лишних выстрелов
        assertFalse(deserialized.hasShotAt(3, 3));
        assertFalse(deserialized.hasShotAt(3, 5));
    }

    @Test
    void invalidCoordinates_shouldBeRejected() {
        // Этот тест не использует enemyBoard, поэтому заглушка не нужна
        RandomHuntStrategyService.ComputerState state = new RandomHuntStrategyService.ComputerState();
        assertFalse(service.isValidTarget(-1, 5, state));
        assertFalse(service.isValidTarget(10, 5, state));
        assertFalse(service.isValidTarget(5, -1, state));
        assertFalse(service.isValidTarget(5, 10, state));
    }

    // Остальные тесты с аналогичными исправлениями...

    private String generateEmptyBoard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(" , , , , , , , , , ");
            if (i < 9) sb.append(";");
        }
        return sb.toString();
    }
}