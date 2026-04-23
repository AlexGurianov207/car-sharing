package com.example.carsharing.service;

import com.example.carsharing.dto.AsyncTaskState;
import com.example.carsharing.dto.AsyncTaskStatusResponse;
import com.example.carsharing.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PaymentTaskRegistryService {

    private final ConcurrentMap<String, TaskStateRecord> tasks = new ConcurrentHashMap<>();

    public void createTask(String taskId) {
        tasks.put(taskId, TaskStateRecord.queued(taskId));
    }

    public void markProcessing(String taskId) {
        tasks.computeIfPresent(taskId, (id, state) -> state.processing());
    }

    public void markSuccess(String taskId, Long paymentId) {
        tasks.computeIfPresent(taskId, (id, state) -> state.success(paymentId));
    }

    public void markFailed(String taskId, String errorMessage) {
        tasks.computeIfPresent(taskId, (id, state) -> state.failed(errorMessage));
    }

    public AsyncTaskStatusResponse getTaskStatus(String taskId) {
        TaskStateRecord state = tasks.get(taskId);
        if (state == null) {
            throw new NotFoundException("Task not found with id: " + taskId);
        }
        return state.toResponse();
    }

    private record TaskStateRecord(
            String taskId,
            AsyncTaskState status,
            Long paymentId,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        private static TaskStateRecord queued(String taskId) {
            return new TaskStateRecord(taskId, AsyncTaskState.QUEUED, null, null, LocalDateTime.now(), null, null);
        }

        private TaskStateRecord processing() {
            return new TaskStateRecord(
                    taskId,
                    AsyncTaskState.PROCESSING,
                    null,
                    null,
                    createdAt,
                    LocalDateTime.now(),
                    null
            );
        }

        private TaskStateRecord success(Long paymentId) {
            return new TaskStateRecord(
                    taskId,
                    AsyncTaskState.SUCCESS,
                    paymentId,
                    null,
                    createdAt,
                    startedAt,
                    LocalDateTime.now()
            );
        }

        private TaskStateRecord failed(String errorMessage) {
            return new TaskStateRecord(
                    taskId,
                    AsyncTaskState.FAILED,
                    null,
                    errorMessage,
                    createdAt,
                    startedAt,
                    LocalDateTime.now()
            );
        }

        private AsyncTaskStatusResponse toResponse() {
            return new AsyncTaskStatusResponse(taskId, status, paymentId, errorMessage, createdAt, startedAt, finishedAt);
        }
    }
}
