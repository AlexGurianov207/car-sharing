package com.example.carsharing.service;

import com.example.carsharing.dto.AsyncTaskState;
import com.example.carsharing.dto.AsyncTaskStatusResponse;
import com.example.carsharing.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTaskRegistryServiceTest {

    private final PaymentTaskRegistryService service = new PaymentTaskRegistryService();

    @Test
    void createTask_shouldStoreQueuedStatus() {
        service.createTask("task-1");

        AsyncTaskStatusResponse status = service.getTaskStatus("task-1");

        assertEquals("task-1", status.getTaskId());
        assertEquals(AsyncTaskState.QUEUED, status.getStatus());
        assertNotNull(status.getCreatedAt());
        assertNull(status.getStartedAt());
        assertNull(status.getFinishedAt());
    }

    @Test
    void markProcessingAndSuccess_shouldUpdateStatus() {
        service.createTask("task-2");

        service.markProcessing("task-2");
        service.markSuccess("task-2", 15L);
        AsyncTaskStatusResponse status = service.getTaskStatus("task-2");

        assertEquals(AsyncTaskState.SUCCESS, status.getStatus());
        assertEquals(15L, status.getPaymentId());
        assertNotNull(status.getStartedAt());
        assertNotNull(status.getFinishedAt());
    }

    @Test
    void markFailed_shouldStoreErrorMessage() {
        service.createTask("task-3");

        service.markProcessing("task-3");
        service.markFailed("task-3", "broken");
        AsyncTaskStatusResponse status = service.getTaskStatus("task-3");

        assertEquals(AsyncTaskState.FAILED, status.getStatus());
        assertEquals("broken", status.getErrorMessage());
        assertNotNull(status.getFinishedAt());
    }

    @Test
    void getTaskStatus_whenTaskMissing_shouldThrow() {
        assertThrows(NotFoundException.class, () -> service.getTaskStatus("missing"));
    }
}
