package com.example.carsharing.service;

import com.example.carsharing.dto.PaymentCreateRequest;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.PaymentStatus;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.RentalStatus;
import com.example.carsharing.repository.PaymentRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.service.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final PaymentMapper paymentMapper;

    private static final String PAYMENT_NOT_FOUND_MESSAGE = "Payment not found with id:";

    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Rental rental = rentalRepository.findById(request.getRentalId())
                .orElseThrow(() -> new NoSuchElementException("Rental not found with id: " + request.getRentalId()));

        if (rental.getStatus() != RentalStatus.COMPLETED) {
            throw new InvalidDataAccessApiUsageException("Cannot create payment for incomplete rental. Status: "
                    + rental.getStatus());
        }

        if (paymentRepository.existsByRentalId(request.getRentalId())) {
            throw new DataIntegrityViolationException("Payment already exists for rental: " + request.getRentalId());
        }

        String transactionId = Optional.ofNullable(request.getTransactionId())
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .orElseGet(() -> "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        request.setTransactionId(transactionId);

        Payment payment = paymentMapper.toEntity(request, rental);

        long hours = Duration.between(rental.getStartTime(), rental.getEndTime()).toHours();
        if (hours < 1) {
            hours = 1;
        }
        long days = hours / 24 + (hours % 24 == 0 ? 0 : 1);

        double carPrice = rental.getCar().getPricePerHour() * hours;
        double servicesPrice = Optional.ofNullable(rental.getSelectedServices())
                .orElseGet(List::of)
                .stream()
                .mapToDouble(s -> s.getPricePerDay() * days)
                .sum();

        payment.setCarAmount(carPrice);
        payment.setServicesAmount(servicesPrice);
        payment.setAmount(carPrice + servicesPrice);

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(savedPayment);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(PAYMENT_NOT_FOUND_MESSAGE + id));
        return paymentMapper.toResponse(payment);
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    public PaymentResponse refundPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(PAYMENT_NOT_FOUND_MESSAGE + id));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidDataAccessApiUsageException("Cannot refund payment with status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment updatedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponse(updatedPayment);
    }

    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(PAYMENT_NOT_FOUND_MESSAGE + id));

        Optional.ofNullable(payment.getRental()).ifPresent(linkedRental -> {
            linkedRental.setPayment(null);
            rentalRepository.save(linkedRental);
        });

        paymentRepository.delete(payment);
    }
}
