package com.example.carsharing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PaymentAsyncProcessor {

    private final PaymentService paymentService;
    private final PaymentTaskRegistryService paymentTaskRegistryService;

    @Async("labTaskExecutor")
    public CompletableFuture<Void> verifyPaymentInBackground(String taskId, Long paymentId) {
        paymentTaskRegistryService.markProcessing(taskId);
        try {
            paymentService.verifyExistingPaymentForLab(paymentId);
            paymentTaskRegistryService.markSuccess(taskId, paymentId);
        } catch (Exception ex) {
            String errorMessage = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ex.getClass().getSimpleName()
                    : ex.getMessage();
            paymentTaskRegistryService.markFailed(taskId, errorMessage);
        }

        return CompletableFuture.completedFuture(null);
    }
}
