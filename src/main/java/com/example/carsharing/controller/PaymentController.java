package com.example.carsharing.controller;

import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.service.PaymentService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable @Positive(message = "Payment ID must be positive") Long id) {
        return paymentService.getPaymentById(id);
    }

    @PatchMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable @Positive(message = "Payment ID must be positive") Long id) {
        return paymentService.refundPayment(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable @Positive(message = "Payment ID must be positive") Long id) {
        paymentService.deletePayment(id);
    }
}
