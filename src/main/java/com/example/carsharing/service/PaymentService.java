package com.example.carsharing.service;

import com.example.carsharing.dto.PaymentCreateRequest;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.User;
import com.example.carsharing.repository.PaymentRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.service.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;

    public PaymentResponse createPayment(PaymentCreateRequest request) {
        // Проверяем существование аренды
        Rental rental = rentalRepository.findById(request.getRentalId())
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + request.getRentalId()));

        // Проверяем, не оплачена ли уже эта аренда
        if (paymentRepository.existsByRentalId(request.getRentalId())) {
            throw new RuntimeException("Payment already exists for rental: " + request.getRentalId());
        }

        // Проверяем существование пользователя
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        // Генерируем transactionId, если не указан
        if (request.getTransactionId() == null || request.getTransactionId().isEmpty()) {
            request.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        Payment payment = paymentMapper.toEntity(request, rental, user);
        Payment savedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponse(savedPayment);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return paymentMapper.toResponse(payment);
    }

    public PaymentResponse getPaymentByRentalId(Long rentalId) {
        Payment payment = paymentRepository.findByRentalId(rentalId)
                .orElseThrow(() -> new RuntimeException("Payment not found for rental: " + rentalId));
        return paymentMapper.toResponse(payment);
    }

    public List<PaymentResponse> getUserPayments(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        return paymentRepository.findByUserId(userId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PaymentResponse refundPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));

        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new RuntimeException("Cannot refund payment with status: " + payment.getStatus());
        }

        payment.setStatus("REFUNDED");
        Payment updatedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponse(updatedPayment);
    }
}