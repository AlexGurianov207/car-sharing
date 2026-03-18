package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.PaymentCreateRequest;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.Rental;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toEntity(PaymentCreateRequest request, Rental rental) {
        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setTransactionId(request.getTransactionId());
        return payment;
    }

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentResponse response = new PaymentResponse();

        response.setId(payment.getId());
        response.setAmount(payment.getAmount());
        response.setCarAmount(payment.getCarAmount());
        response.setServicesAmount(payment.getServicesAmount());
        response.setPaymentDate(payment.getPaymentDate());

        if (payment.getStatus() != null) {
            response.setStatus(payment.getStatus().name());
        }

        response.setTransactionId(payment.getTransactionId());

        if (payment.getRental() != null) {
            Rental rental = payment.getRental();

            response.setRentalId(rental.getId());
        }

        return response;
    }
}