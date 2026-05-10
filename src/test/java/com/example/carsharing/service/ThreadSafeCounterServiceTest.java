package com.example.carsharing.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadSafeCounterServiceTest {

    private final ThreadSafeCounterService service = new ThreadSafeCounterService();

    @Test
    void counterOperations_shouldUpdateValueSafely() {
        assertEquals(0L, service.getValue());
        assertEquals(1L, service.incrementAndGet());
        assertEquals(6L, service.incrementManyAndGet(5));
        assertEquals(6L, service.getValue());
        assertEquals(0L, service.resetAndGet());
    }
}
