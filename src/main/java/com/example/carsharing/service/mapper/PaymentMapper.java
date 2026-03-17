package com.example.carsharing.service.mapper;

import com.example.carsharing.dto.PaymentCreateRequest;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.model.*;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toEntity(PaymentCreateRequest request, Rental rental, User user) {
        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setUser(user);
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

        if (payment.getUser() != null) {
            response.setUserId(payment.getUser().getId());
            response.setUserFullName(
                    (payment.getUser().getFirstName() != null ? payment.getUser().getFirstName() : "") + " " +
                            (payment.getUser().getLastName() != null ? payment.getUser().getLastName() : "")
            );
        }

        if (payment.getRental() != null) {
            response.setRentalId(payment.getRental().getId());

            response.setRentalStartTime(payment.getRental().getStartTime());
            response.setRentalEndTime(payment.getRental().getEndTime());

            if (payment.getRental().getStartTime() != null && payment.getRental().getEndTime() != null) {
                long hours = java.time.Duration.between(
                        payment.getRental().getStartTime(),
                        payment.getRental().getEndTime()
                ).toHours();
                response.setRentalHours(hours < 1 ? 1 : hours);
            }

            if (payment.getRental().getCar() != null) {
                Car car = payment.getRental().getCar();
                response.setCarInfo(
                        (car.getBrand() != null ? car.getBrand() : "") + " " +
                                (car.getModel() != null ? car.getModel() : "")
                );
            } else {
                response.setCarInfo("Car deleted");
            }
        } else {
            response.setRentalId(null);
            response.setCarInfo("Car deleted");
            response.setRentalHours(null);
        }

        return response;
    }
}