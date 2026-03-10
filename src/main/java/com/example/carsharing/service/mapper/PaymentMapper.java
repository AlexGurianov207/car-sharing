package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.PaymentCreateRequest;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.User;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toEntity(PaymentCreateRequest request, Rental rental, User user) {
        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setUser(user);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(request.getTransactionId());
        return payment;
    }

    public PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setRentalId(payment.getRental().getId());
        response.setUserId(payment.getUser().getId());
        response.setUserFullName(payment.getUser().getFirstName() + " " + payment.getUser().getLastName());
        response.setAmount(payment.getAmount());
        response.setCarAmount(payment.getCarAmount());           // ✅ добавить
        response.setServicesAmount(payment.getServicesAmount());
        response.setPaymentDate(payment.getPaymentDate());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setStatus(payment.getStatus());
        response.setTransactionId(payment.getTransactionId());

        // ✅ добавить информацию об аренде
        if (payment.getRental() != null) {
            response.setCarInfo(payment.getRental().getCar().getBrand() + " " +
                    payment.getRental().getCar().getModel());
            response.setRentalStartTime(payment.getRental().getStartTime());
            response.setRentalEndTime(payment.getRental().getEndTime());
            if (payment.getRental().getEndTime() != null) {
                long hours = java.time.Duration.between(
                        payment.getRental().getStartTime(),
                        payment.getRental().getEndTime()
                ).toHours();
                response.setRentalHours(hours < 1 ? 1 : hours);
            }
        }

        return response;
    }
}