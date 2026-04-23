package com.example.carsharing.service;

import com.example.carsharing.dto.AsyncTaskStartResponse;
import com.example.carsharing.dto.AsyncTaskStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncPaymentTaskService {

    private final PaymentTaskRegistryService paymentTaskRegistryService;
    private final PaymentAsyncProcessor paymentAsyncProcessor;

    public AsyncTaskStartResponse startVerificationTask(Long paymentId) {
        String taskId = UUID.randomUUID().toString();
        paymentTaskRegistryService.createTask(taskId);
        paymentAsyncProcessor.verifyPaymentInBackground(taskId, paymentId);

        return new AsyncTaskStartResponse(taskId);
    }

    public AsyncTaskStatusResponse getTaskStatus(String taskId) {
        return paymentTaskRegistryService.getTaskStatus(taskId);
    }
}
