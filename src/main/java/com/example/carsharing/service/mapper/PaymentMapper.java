package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.PaymentCreateRequest;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.PaymentMethod;
import com.example.carsharing.model.Rental;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PaymentMapper {

    public Payment toEntity(PaymentCreateRequest request, Rental rental) {
        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setPaymentMethod(PaymentMethod.valueOf(String.valueOf(request.getPaymentMethod())));
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

        if (payment.getPaymentMethod() != null) {
            response.setPaymentMethod(payment.getPaymentMethod().name());
        }

        if (payment.getStatus() != null) {
            response.setStatus(payment.getStatus().name());
        }

        response.setTransactionId(payment.getTransactionId());

        if (payment.getRental() != null) {
            Rental rental = payment.getRental();

            response.setRentalId(rental.getId());

            if (rental.getUserFullName() != null) {
                response.setUserFullName(rental.getUserFullName());
            } else if (rental.getUser() != null) {
                response.setUserFullName(rental.getUser().getFirstName() + " " +
                        rental.getUser().getLastName());
            }

            if (rental.getCarInfo() != null) {
                response.setCarInfo(rental.getCarInfo());
            } else if (rental.getCar() != null) {
                response.setCarInfo(rental.getCar().getBrand() + " " +
                        rental.getCar().getModel() + " (" +
                        rental.getCar().getLicensePlate() + ")");
            }

            response.setRentalStartTime(rental.getStartTime());
            response.setRentalEndTime(rental.getEndTime());

            if (payment.getRentalHours() != null) {
                response.setRentalHours(payment.getRentalHours());
            } else if (rental.getStartTime() != null && rental.getEndTime() != null) {
                long hours = Duration.between(rental.getStartTime(), rental.getEndTime()).toHours();
                response.setRentalHours(Math.max(1, hours));
            }

            if (rental.getServiceNames() != null) {
                response.setSelectedServices(rental.getServiceNames());
            }
        }

        return response;
    }
}