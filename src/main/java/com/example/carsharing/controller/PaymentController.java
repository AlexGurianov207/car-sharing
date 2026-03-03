package com.example.carsharing.controller;

import com.example.carsharing.dto.PaymentCreateRequest;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @GetMapping("/by-rental/{rentalId}")
    public PaymentResponse getPaymentByRentalId(@PathVariable Long rentalId) {
        return paymentService.getPaymentByRentalId(rentalId);
    }

    @GetMapping("/user/{userId}")
    public List<PaymentResponse> getUserPayments(@PathVariable Long userId) {
        return paymentService.getUserPayments(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        return paymentService.createPayment(request);
    }

    @PatchMapping("/{id}/refund")
    public PaymentResponse refundPayment(@PathVariable Long id) {
        return paymentService.refundPayment(id);
    }
}