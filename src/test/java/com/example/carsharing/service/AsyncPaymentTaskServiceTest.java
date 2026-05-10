package com.example.carsharing.service;

import com.example.carsharing.dto.AsyncTaskStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncPaymentTaskServiceTest {

    @Mock
    private PaymentTaskRegistryService paymentTaskRegistryService;
    @Mock
    private PaymentAsyncProcessor paymentAsyncProcessor;

    @InjectMocks
    private AsyncPaymentTaskService service;

    @Test
    void startVerificationTask_shouldCreateAndStartTask() {
        when(paymentAsyncProcessor.verifyPaymentInBackground(anyString(), org.mockito.ArgumentMatchers.eq(9L)))
                .thenReturn(CompletableFuture.completedFuture(null));

        var response = service.startVerificationTask(9L);

        assertNotNull(response.getTaskId());
        verify(paymentTaskRegistryService).createTask(response.getTaskId());
        verify(paymentAsyncProcessor).verifyPaymentInBackground(response.getTaskId(), 9L);
    }

    @Test
    void getTaskStatus_shouldDelegateToRegistry() {
        AsyncTaskStatusResponse expected = new AsyncTaskStatusResponse("task", null, null, null, null, null, null);
        when(paymentTaskRegistryService.getTaskStatus("task")).thenReturn(expected);

        assertEquals(expected, service.getTaskStatus("task"));
    }
}
