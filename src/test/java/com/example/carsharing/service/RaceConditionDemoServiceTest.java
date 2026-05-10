package com.example.carsharing.service;

import com.example.carsharing.dto.RaceDemoResponse;
import com.example.carsharing.dto.RaceMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceConditionDemoServiceTest {

    private final RaceConditionDemoService service = new RaceConditionDemoService();

    @Test
    void runDemo_whenAtomicMode_shouldReturnExpectedValue() {
        RaceDemoResponse response = service.runDemo(2, 10, RaceMode.SAFE_ATOMIC);

        assertEquals(RaceMode.SAFE_ATOMIC, response.getMode());
        assertEquals(2, response.getThreads());
        assertEquals(10, response.getIterationsPerThread());
        assertEquals(20, response.getExpectedValue());
        assertEquals(20, response.getActualValue());
        assertEquals(0, response.getLostUpdates());
        assertTrue(response.getDurationMs() >= 0);
    }

    @Test
    void runDemo_whenSynchronizedMode_shouldReturnExpectedValue() {
        RaceDemoResponse response = service.runDemo(2, 10, RaceMode.SAFE_SYNCHRONIZED);

        assertEquals(20, response.getExpectedValue());
        assertEquals(20, response.getActualValue());
        assertEquals(0, response.getLostUpdates());
    }

    @Test
    void runDemo_whenUnsafeMode_shouldReturnValidRange() {
        RaceDemoResponse response = service.runDemo(1, 10, RaceMode.UNSAFE);

        assertEquals(10, response.getExpectedValue());
        assertEquals(10, response.getActualValue());
        assertEquals(0, response.getLostUpdates());
    }
}
