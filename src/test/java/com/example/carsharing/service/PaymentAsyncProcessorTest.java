package com.example.carsharing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentAsyncProcessorTest {

    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentTaskRegistryService paymentTaskRegistryService;

    @InjectMocks
    private PaymentAsyncProcessor processor;

    @Test
    void verifyPaymentInBackground_whenVerificationSucceeds_shouldMarkSuccess() {
        processor.verifyPaymentInBackground("task", 3L).join();

        verify(paymentTaskRegistryService).markProcessing("task");
        verify(paymentService).verifyExistingPaymentForLab(3L);
        verify(paymentTaskRegistryService).markSuccess("task", 3L);
    }

    @Test
    void verifyPaymentInBackground_whenVerificationFails_shouldMarkFailedWithMessage() {
        doThrow(new InvalidDataAccessApiUsageException("not allowed"))
                .when(paymentService).verifyExistingPaymentForLab(3L);

        processor.verifyPaymentInBackground("task", 3L).join();

        verify(paymentTaskRegistryService).markProcessing("task");
        verify(paymentTaskRegistryService).markFailed("task", "not allowed");
    }

    @Test
    void verifyPaymentInBackground_whenMessageBlank_shouldUseExceptionClassName() {
        doThrow(new IllegalStateException(" "))
                .when(paymentService).verifyExistingPaymentForLab(3L);

        processor.verifyPaymentInBackground("task", 3L).join();

        verify(paymentTaskRegistryService).markFailed("task", "IllegalStateException");
    }
}
