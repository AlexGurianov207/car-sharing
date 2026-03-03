package com.example.carsharing.config;

import com.example.carsharing.model.Car;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.Tariff;
import com.example.carsharing.model.User;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.PaymentRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.TariffRepository;
import com.example.carsharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final TariffRepository tariffRepository;
    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;  // Добавили

    @Override
    public void run(String... args) {
        // Создаем тарифы
        if (tariffRepository.count() == 0) {
            Tariff economy = new Tariff();
            economy.setName("Economy");
            economy.setPricePerHour(10.0);
            economy.setMinRentalHours(1);
            tariffRepository.save(economy);

            Tariff comfort = new Tariff();
            comfort.setName("Comfort");
            comfort.setPricePerHour(20.0);
            comfort.setMinRentalHours(1);
            tariffRepository.save(comfort);
        }

        // Создаем машины
        if (carRepository.count() == 0) {
            Car car1 = new Car();
            car1.setBrand("Toyota");
            car1.setModel("Camry");
            car1.setLicensePlate("A001AA");
            car1.setYear(2022);
            car1.setPricePerHour(15.0);
            car1.setStatus("AVAILABLE");
            carRepository.save(car1);

            Car car2 = new Car();
            car2.setBrand("BMW");
            car2.setModel("X5");
            car2.setLicensePlate("B002BB");
            car2.setYear(2023);
            car2.setPricePerHour(25.0);
            car2.setStatus("AVAILABLE");
            carRepository.save(car2);
        }

        // Создаем пользователей
        if (userRepository.count() == 0) {
            User user1 = new User();
            user1.setFirstName("Иван");
            user1.setLastName("Иванов");
            user1.setEmail("ivan@example.com");
            user1.setPhoneNumber("+79001234567");
            user1.setDriverLicense("7712345678");
            user1.setStatus("ACTIVE");
            userRepository.save(user1);

            User user2 = new User();
            user2.setFirstName("Петр");
            user2.setLastName("Петров");
            user2.setEmail("petr@example.com");
            user2.setPhoneNumber("+79007654321");
            user2.setDriverLicense("7787654321");
            user2.setStatus("ACTIVE");
            userRepository.save(user2);
        }

        // Создаем аренду и платеж для примера
        if (rentalRepository.count() == 0 && paymentRepository.count() == 0) {
            // Берем первого пользователя и первую машину
            User user = userRepository.findAll().get(0);
            Car car = carRepository.findAll().get(0);

            // Создаем аренду
            Rental rental = new Rental();
            rental.setUser(user);
            rental.setCar(car);
            rental.setStartTime(LocalDateTime.now().minusHours(3));
            rental.setEndTime(LocalDateTime.now().minusHours(1));
            rental.setTotalPrice(30.0);
            rental.setStatus("COMPLETED");
            Rental savedRental = rentalRepository.save(rental);

            // Меняем статус машины обратно на AVAILABLE
            car.setStatus("AVAILABLE");
            carRepository.save(car);

            // Создаем платеж за аренду
            Payment payment = new Payment();
            payment.setRental(savedRental);
            payment.setUser(user);
            payment.setAmount(30.0);
            payment.setPaymentMethod("CARD");
            payment.setStatus("COMPLETED");
            payment.setTransactionId("TXN-" + System.currentTimeMillis());
            paymentRepository.save(payment);
        }
    }
}