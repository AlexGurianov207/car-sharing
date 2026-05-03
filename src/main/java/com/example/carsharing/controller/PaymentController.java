package com.example.carsharing.controller;

import com.example.carsharing.dto.AsyncTaskStartResponse;
import com.example.carsharing.dto.AsyncTaskStatusResponse;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.security.CurrentAccessService;
import com.example.carsharing.service.AsyncPaymentTaskService;
import com.example.carsharing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payments", description = "Operations for payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AsyncPaymentTaskService asyncPaymentTaskService;
    private final CurrentAccessService currentAccessService;

    @GetMapping
    @Operation(summary = "Get all payments")
    public List<PaymentResponse> getAllPayments(Authentication authentication) {
        if (!currentAccessService.isAdmin(authentication)) {
            return paymentService.getPaymentsByUserId(currentAccessService.requireCurrentUserId(authentication));
        }
        return paymentService.getAllPayments();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user payments")
    public List<PaymentResponse> getCurrentUserPayments(Authentication authentication) {
        return paymentService.getPaymentsByUserId(currentAccessService.requireCurrentUserId(authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public PaymentResponse getPaymentById(
            @PathVariable @Positive(message = "Payment ID must be positive") Long id,
            Authentication authentication) {
        if (!currentAccessService.isAdmin(authentication)) {
            return paymentService.getPaymentByIdAndUserId(id, currentAccessService.requireCurrentUserId(authentication));
        }
        return paymentService.getPaymentById(id);
    }

    @PatchMapping("/{id}/refund")
    @Operation(summary = "Refund payment")
    public PaymentResponse refundPayment(@PathVariable @Positive(message = "Payment ID must be positive") Long id) {
        return paymentService.refundPayment(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete payment")
    public void deletePayment(@PathVariable @Positive(message = "Payment ID must be positive") Long id) {
        paymentService.deletePayment(id);
    }

    @PostMapping("/{id}/verify/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start async verification for existing payment and return task ID")
    public AsyncTaskStartResponse verifyPaymentAsync(
            @PathVariable("id") @Positive(message = "Payment ID must be positive") Long paymentId) {
        return asyncPaymentTaskService.startVerificationTask(paymentId);
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Get async payment task status")
    public AsyncTaskStatusResponse getTaskStatus(@PathVariable String taskId) {
        return asyncPaymentTaskService.getTaskStatus(taskId);
    }
}
