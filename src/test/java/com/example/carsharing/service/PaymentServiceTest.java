package com.example.carsharing.service;

import com.example.carsharing.dto.PaymentCreateRequest;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.exception.NotFoundException;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.PaymentStatus;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.RentalStatus;
import com.example.carsharing.repository.PaymentRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.service.mapper.PaymentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_whenRentalMissing_shouldThrow() {
        PaymentCreateRequest request = createRequest();
        when(rentalRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenRentalNotCompleted_shouldThrow() {
        PaymentCreateRequest request = createRequest();
        Rental rental = baseCompletedRental();
        rental.setStatus(RentalStatus.ACTIVE);
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenPaymentAlreadyExists_shouldThrow() {
        PaymentCreateRequest request = createRequest();
        Rental rental = baseCompletedRental();
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalId(1L)).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> paymentService.createPayment(request));
    }

    @Test
    void createPayment_whenTransactionIdEmpty_shouldGenerateAndCalculateAmounts() {
        PaymentCreateRequest request = createRequest();
        request.setTransactionId("  ");

        Rental rental = baseCompletedRental();
        ExtraService gps = new ExtraService();
        gps.setPricePerDay(5.0);
        rental.setSelectedServices(List.of(gps));

        Payment entity = new Payment();
        entity.setStatus(PaymentStatus.COMPLETED);
        PaymentResponse expected = new PaymentResponse();

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalId(1L)).thenReturn(false);
        when(paymentMapper.toEntity(request, rental)).thenReturn(entity);
        when(paymentRepository.save(entity)).thenReturn(entity);
        when(paymentMapper.toResponse(entity)).thenReturn(expected);

        PaymentResponse actual = paymentService.createPayment(request);

        assertEquals(expected, actual);
        assertNotNull(request.getTransactionId());
        assertEquals(20.0, entity.getCarAmount());
        assertEquals(5.0, entity.getServicesAmount());
        assertEquals(25.0, entity.getAmount());
    }

    @Test
    void createPayment_whenNoServices_shouldCalculateOnlyCarAmount() {
        PaymentCreateRequest request = createRequest();
        request.setTransactionId("TXN-CUSTOM");

        Rental rental = baseCompletedRental();
        rental.setSelectedServices(null);
        Payment entity = new Payment();
        PaymentResponse expected = new PaymentResponse();

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalId(1L)).thenReturn(false);
        when(paymentMapper.toEntity(request, rental)).thenReturn(entity);
        when(paymentRepository.save(entity)).thenReturn(entity);
        when(paymentMapper.toResponse(entity)).thenReturn(expected);

        PaymentResponse actual = paymentService.createPayment(request);

        assertEquals(expected, actual);
        assertEquals("TXN-CUSTOM", request.getTransactionId());
        assertEquals(20.0, entity.getCarAmount());
        assertEquals(0.0, entity.getServicesAmount());
    }

    @Test
    void createPayment_whenDurationLessThanHour_shouldUseMinimumOneHour() {
        PaymentCreateRequest request = createRequest();
        request.setTransactionId("TXN-MIN-HOUR");

        Rental rental = baseCompletedRental();
        rental.setEndTime(rental.getStartTime().plusMinutes(30));
        rental.setSelectedServices(List.of());
        Payment entity = new Payment();
        PaymentResponse expected = new PaymentResponse();

        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalId(1L)).thenReturn(false);
        when(paymentMapper.toEntity(request, rental)).thenReturn(entity);
        when(paymentRepository.save(entity)).thenReturn(entity);
        when(paymentMapper.toResponse(entity)).thenReturn(expected);

        PaymentResponse actual = paymentService.createPayment(request);

        assertEquals(expected, actual);
        assertEquals(10.0, entity.getCarAmount());
        assertEquals(0.0, entity.getServicesAmount());
        assertEquals(10.0, entity.getAmount());
    }

    @Test
    void getPaymentById_whenMissing_shouldThrow() {
        when(paymentRepository.findById(11L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> paymentService.getPaymentById(11L));
    }

    @Test
    void getPaymentById_whenFound_shouldReturnMappedResponse() {
        Payment payment = new Payment();
        PaymentResponse expected = new PaymentResponse();
        when(paymentRepository.findById(11L)).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(expected);

        PaymentResponse actual = paymentService.getPaymentById(11L);

        assertEquals(expected, actual);
    }

    @Test
    void getAllPayments_shouldMapAll() {
        Payment payment = new Payment();
        PaymentResponse expected = new PaymentResponse();
        when(paymentRepository.findAll()).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(expected);

        List<PaymentResponse> actual = paymentService.getAllPayments();

        assertEquals(1, actual.size());
        assertEquals(expected, actual.get(0));
    }

    @Test
    void refundPayment_whenNotCompleted_shouldThrow() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));

        assertThrows(InvalidDataAccessApiUsageException.class, () -> paymentService.refundPayment(5L));
    }

    @Test
    void refundPayment_whenCompleted_shouldSetRefunded() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.COMPLETED);
        PaymentResponse expected = new PaymentResponse();

        when(paymentRepository.findById(5L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(expected);

        PaymentResponse actual = paymentService.refundPayment(5L);

        assertEquals(expected, actual);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void deletePayment_whenRentalLinked_shouldDetachAndDelete() {
        Rental rental = new Rental();
        Payment payment = new Payment();
        payment.setRental(rental);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.deletePayment(1L);

        assertEquals(null, rental.getPayment());
        verify(rentalRepository).save(rental);
        verify(paymentRepository).delete(payment);
    }

    @Test
    void deletePayment_whenRentalMissing_shouldDeleteOnlyPayment() {
        Payment payment = new Payment();
        payment.setRental(null);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        paymentService.deletePayment(1L);

        verify(rentalRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(paymentRepository).delete(payment);
    }

    private PaymentCreateRequest createRequest() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setRentalId(1L);
        request.setUserId(1L);
        request.setAmount(100.0);
        return request;
    }

    private Rental baseCompletedRental() {
        Rental rental = new Rental();
        rental.setStatus(RentalStatus.COMPLETED);
        rental.setStartTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        rental.setEndTime(LocalDateTime.of(2026, 1, 1, 12, 0));

        Car car = new Car();
        car.setPricePerHour(10.0);
        rental.setCar(car);
        return rental;
    }
}
