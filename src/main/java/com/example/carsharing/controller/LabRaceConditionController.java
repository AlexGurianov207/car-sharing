package com.example.carsharing.controller;

import com.example.carsharing.dto.RaceDemoResponse;
import com.example.carsharing.dto.RaceMode;
import com.example.carsharing.service.RaceConditionDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab/race")
@RequiredArgsConstructor
@Tag(name = "Lab Race Condition", description = "Endpoints for race condition demo")
public class LabRaceConditionController {

    private static final int DEMO_THREADS = 73;
    private static final int DEMO_ITERATIONS = 10000;

    private final RaceConditionDemoService raceConditionDemoService;

    @PostMapping("/unsafe")
    @Operation(summary = "Run race condition demo with unsafe counter")
    public RaceDemoResponse runUnsafeDemo() {
        return raceConditionDemoService.runDemo(DEMO_THREADS, DEMO_ITERATIONS, RaceMode.UNSAFE);
    }

    @PostMapping("/atomic")
    @Operation(summary = "Run race condition demo with AtomicInteger counter")
    public RaceDemoResponse runAtomicDemo() {
        return raceConditionDemoService.runDemo(DEMO_THREADS, DEMO_ITERATIONS, RaceMode.SAFE_ATOMIC);
    }

    @PostMapping("/synchronized")
    @Operation(summary = "Run race condition demo with synchronized counter")
    public RaceDemoResponse runSynchronizedDemo() {
        return raceConditionDemoService.runDemo(DEMO_THREADS, DEMO_ITERATIONS, RaceMode.SAFE_SYNCHRONIZED);
    }
}
