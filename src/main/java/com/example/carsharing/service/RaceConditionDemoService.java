package com.example.carsharing.service;

import com.example.carsharing.dto.RaceDemoResponse;
import com.example.carsharing.dto.RaceMode;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RaceConditionDemoService {

    public RaceDemoResponse runDemo(int threads, int iterationsPerThread, RaceMode mode) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        UnsafeCounter unsafeCounter = new UnsafeCounter();
        AtomicInteger atomicCounter = new AtomicInteger(0);
        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                await(start);
                for (int j = 0; j < iterationsPerThread; j++) {
                    incrementByMode(mode, unsafeCounter, atomicCounter, synchronizedCounter);
                }
                done.countDown();
            });
        }

        await(ready);
        start.countDown();
        await(done);
        executor.shutdown();
        awaitShutdown(executor);

        int actual = switch (mode) {
            case UNSAFE -> unsafeCounter.get();
            case SAFE_ATOMIC -> atomicCounter.get();
            case SAFE_SYNCHRONIZED -> synchronizedCounter.get();
        };
        int expected = threads * iterationsPerThread;
        int lostUpdates = Math.max(expected - actual, 0);
        long durationMs = System.currentTimeMillis() - startTime;

        return new RaceDemoResponse(mode, threads, iterationsPerThread, expected, actual, lostUpdates, durationMs);
    }

    private void incrementByMode(
            RaceMode mode,
            UnsafeCounter unsafeCounter,
            AtomicInteger atomicCounter,
            SynchronizedCounter synchronizedCounter
    ) {
        switch (mode) {
            case UNSAFE -> unsafeCounter.increment();
            case SAFE_ATOMIC -> atomicCounter.incrementAndGet();
            case SAFE_SYNCHRONIZED -> synchronizedCounter.increment();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted during race demo", ex);
        }
    }

    private void awaitShutdown(ExecutorService executor) {
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Executor shutdown interrupted", ex);
        }
    }

    private static final class UnsafeCounter {
        private int value;

        private void increment() {
            value++;
        }

        private int get() {
            return value;
        }
    }

    private static final class SynchronizedCounter {
        private int value;

        private synchronized void increment() {
            value++;
        }

        private synchronized int get() {
            return value;
        }
    }
}
