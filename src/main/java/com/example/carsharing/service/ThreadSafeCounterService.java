package com.example.carsharing.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class ThreadSafeCounterService {

    private final AtomicLong counter = new AtomicLong(0);

    public long incrementAndGet() {
        return counter.incrementAndGet();
    }

    public long incrementManyAndGet(int times) {
        for (int i = 0; i < times; i++) {
            counter.incrementAndGet();
        }
        return counter.get();
    }

    public long getValue() {
        return counter.get();
    }

    public long resetAndGet() {
        counter.set(0);
        return counter.get();
    }
}
