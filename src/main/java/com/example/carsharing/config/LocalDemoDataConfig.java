package com.example.carsharing.config;

import com.example.carsharing.dto.CarCreateRequest;
import com.example.carsharing.dto.CarResponse;
import com.example.carsharing.dto.ExtraServiceCreateRequest;
import com.example.carsharing.dto.ExtraServiceResponse;
import com.example.carsharing.dto.PaymentResponse;
import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.UserCreateRequest;
import com.example.carsharing.dto.UserResponse;
import com.example.carsharing.model.ServiceCategory;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.service.CarService;
import com.example.carsharing.service.ExtraServiceService;
import com.example.carsharing.service.PaymentService;
import com.example.carsharing.service.RentalService;
import com.example.carsharing.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("local")
public class LocalDemoDataConfig {

    @Bean
    CommandLineRunner seedLocalDemoData(
            CarRepository carRepository,
            CarService carService,
            ExtraServiceService extraServiceService,
            UserService userService,
            RentalService rentalService,
            PaymentService paymentService
    ) {
        return args -> {
            if (carRepository.count() > 0) {
                return;
            }

            ExtraServiceResponse gps = extraServiceService.createService(
                    createService("GPS-навигатор", "Навигатор с актуальными картами и голосовыми подсказками",
                            12.0, ServiceCategory.COMFORT, true)
            );
            ExtraServiceResponse childSeat = extraServiceService.createService(
                    createService("Детское кресло", "Кресло для безопасных поездок с ребёнком",
                            9.0, ServiceCategory.SAFETY, true)
            );
            ExtraServiceResponse casco = extraServiceService.createService(
                    createService("КАСКО+", "Расширенная страховая защита на время аренды",
                            18.5, ServiceCategory.INSURANCE, true)
            );
            ExtraServiceResponse wifi = extraServiceService.createService(
                    createService("Wi-Fi в салоне", "Мобильная точка доступа для работы и поездок",
                            7.5, ServiceCategory.EQUIPMENT, true)
            );
            ExtraServiceResponse secondDriver = extraServiceService.createService(
                    createService("Второй водитель", "Возможность добавить второго допущенного водителя",
                            14.0, ServiceCategory.INSURANCE, true)
            );
            ExtraServiceResponse winterKit = extraServiceService.createService(
                    createService("Зимний комплект", "Щётка, незамерзайка и защита от обледенения",
                            6.0, ServiceCategory.SAFETY, true)
            );
            ExtraServiceResponse phoneHolder = extraServiceService.createService(
                    createService("Держатель для телефона", "Удобный магнитный держатель на панель",
                            4.0, ServiceCategory.EQUIPMENT, true)
            );
            ExtraServiceResponse comfortPack = extraServiceService.createService(
                    createService("Комфорт-пакет", "Плед, вода и салфетки для дальних поездок",
                            8.0, ServiceCategory.COMFORT, false)
            );
            ExtraServiceResponse premiumAudio = extraServiceService.createService(
                    createService("Премиум-аудио", "Улучшенная аудиосистема для поездок по городу и трассе",
                            11.0, ServiceCategory.COMFORT, true)
            );
            ExtraServiceResponse trunkOrganizer = extraServiceService.createService(
                    createService("Органайзер в багажник", "Удобное хранение покупок, сумок и багажа",
                            5.0, ServiceCategory.EQUIPMENT, true)
            );
            ExtraServiceResponse expressWash = extraServiceService.createService(
                    createService("Чистый кузов", "Экспресс-мойка и подготовка автомобиля перед выдачей",
                            10.0, ServiceCategory.COMFORT, true)
            );
            ExtraServiceResponse petKit = extraServiceService.createService(
                    createService("Поездка с питомцем", "Защитная накидка и набор для перевозки питомца",
                            13.0, ServiceCategory.SAFETY, true)
            );

            CarResponse bmw520 = carService.createCar(createCar("BMW", "520d", "AB520D7", 2023, 34.0));
            CarResponse bmw530 = carService.createCar(createCar("BMW", "530i", "AB530I7", 2024, 39.0));
            CarResponse bmwX3 = carService.createCar(createCar("BMW", "X3", "ABX3007", 2022, 36.5));
            CarResponse mercedesE200 = carService.createCar(createCar("Mercedes", "E200", "EC2000A", 2022, 37.5));
            CarResponse mercedesC200 = carService.createCar(createCar("Mercedes", "C200", "MC2007K", 2021, 30.5));
            CarResponse audiA6 = carService.createCar(createCar("Audi", "A6", "AA6006X", 2021, 31.0));
            CarResponse audiA4 = carService.createCar(createCar("Audi", "A4", "AA4004X", 2020, 27.0));
            CarResponse volkswagenPolo = carService.createCar(createCar("Volkswagen", "Polo", "MP3412K", 2020, 21.0));
            CarResponse volkswagenJetta = carService.createCar(createCar("Volkswagen", "Jetta", "MJ7421V", 2021, 24.5));
            CarResponse toyotaCamry = carService.createCar(createCar("Toyota", "Camry", "KT7001M", 2024, 33.5));
            CarResponse toyotaCorolla = carService.createCar(createCar("Toyota", "Corolla", "KT4002M", 2022, 22.5));
            CarResponse kiaRio = carService.createCar(createCar("Kia", "Rio", "BH4421E", 2022, 19.5));
            CarResponse kiaCeed = carService.createCar(createCar("Kia", "Ceed", "BC5151E", 2023, 23.0));
            CarResponse skodaOctavia = carService.createCar(createCar("Skoda", "Octavia", "OP1188P", 2021, 24.0));
            CarResponse skodaRapid = carService.createCar(createCar("Skoda", "Rapid", "OR2099P", 2019, 18.0));
            CarResponse renaultDuster = carService.createCar(createCar("Renault", "Duster", "RD8800N", 2021, 26.0));
            CarResponse hyundaiElantra = carService.createCar(createCar("Hyundai", "Elantra", "HE3131T", 2023, 23.5));
            CarResponse hyundaiCreta = carService.createCar(createCar("Hyundai", "Creta", "HC9090T", 2024, 28.0));

            carService.updateAvailableServices(bmw520.getId(), List.of(
                    gps.getId(), casco.getId(), wifi.getId(), secondDriver.getId(), premiumAudio.getId()
            ));
            carService.updateAvailableServices(bmw530.getId(), List.of(
                    gps.getId(), casco.getId(), expressWash.getId(), premiumAudio.getId(), secondDriver.getId()
            ));
            carService.updateAvailableServices(bmwX3.getId(), List.of(
                    gps.getId(), childSeat.getId(), winterKit.getId(), petKit.getId()
            ));
            carService.updateAvailableServices(mercedesE200.getId(), List.of(
                    gps.getId(), childSeat.getId(), casco.getId(), winterKit.getId()
            ));
            carService.updateAvailableServices(mercedesC200.getId(), List.of(
                    gps.getId(), wifi.getId(), phoneHolder.getId(), expressWash.getId()
            ));
            carService.updateAvailableServices(audiA6.getId(), List.of(
                    childSeat.getId(), casco.getId(), wifi.getId(), secondDriver.getId()
            ));
            carService.updateAvailableServices(audiA4.getId(), List.of(
                    gps.getId(), wifi.getId(), premiumAudio.getId(), trunkOrganizer.getId()
            ));
            carService.updateAvailableServices(volkswagenPolo.getId(), List.of(
                    gps.getId(), phoneHolder.getId(), winterKit.getId()
            ));
            carService.updateAvailableServices(volkswagenJetta.getId(), List.of(
                    gps.getId(), wifi.getId(), childSeat.getId()
            ));
            carService.updateAvailableServices(toyotaCamry.getId(), List.of(
                    gps.getId(), wifi.getId(), casco.getId(), childSeat.getId(), secondDriver.getId()
            ));
            carService.updateAvailableServices(toyotaCorolla.getId(), List.of(
                    gps.getId(), trunkOrganizer.getId(), phoneHolder.getId(), childSeat.getId()
            ));
            carService.updateAvailableServices(kiaRio.getId(), List.of(
                    gps.getId(), phoneHolder.getId()
            ));
            carService.updateAvailableServices(kiaCeed.getId(), List.of(
                    gps.getId(), wifi.getId(), trunkOrganizer.getId(), phoneHolder.getId()
            ));
            carService.updateAvailableServices(skodaOctavia.getId(), List.of(
                    gps.getId(), wifi.getId(), winterKit.getId(), childSeat.getId()
            ));
            carService.updateAvailableServices(skodaRapid.getId(), List.of(
                    gps.getId(), phoneHolder.getId(), expressWash.getId()
            ));
            carService.updateAvailableServices(renaultDuster.getId(), List.of(
                    gps.getId(), childSeat.getId(), winterKit.getId(), petKit.getId()
            ));
            carService.updateAvailableServices(hyundaiElantra.getId(), List.of(
                    gps.getId(), wifi.getId(), expressWash.getId(), phoneHolder.getId()
            ));
            carService.updateAvailableServices(hyundaiCreta.getId(), List.of(
                    gps.getId(), secondDriver.getId(), petKit.getId(), trunkOrganizer.getId()
            ));

            UserResponse alina = userService.createUser(
                    createUser("Алина", "Ковалева", "alina.kovaleva@example.com", "+375291111111", "AB1234567")
            );
            UserResponse alinaMoroz = userService.createUser(
                    createUser("Алина", "Мороз", "alina.moroz@example.com", "+375291111112", "AB1234568")
            );
            UserResponse pavel = userService.createUser(
                    createUser("Павел", "Орлов", "pavel.orlov@example.com", "+375292222222", "MP7654321")
            );
            UserResponse pavelKarpov = userService.createUser(
                    createUser("Павел", "Карпов", "pavel.karpov@example.com", "+375292222223", "MP7654322")
            );
            UserResponse vera = userService.createUser(
                    createUser("Вера", "Соколова", "vera.sokolova@example.com", "+375293333333", "HB9090909")
            );
            UserResponse veraBelova = userService.createUser(
                    createUser("Вера", "Белова", "vera.belova@example.com", "+375293333334", "HB9090910")
            );
            UserResponse nikita = userService.createUser(
                    createUser("Никита", "Лебедев", "nikita.lebedev@example.com", "+375294444444", "BM1112233")
            );
            UserResponse elena = userService.createUser(
                    createUser("Елена", "Громова", "elena.gromova@example.com", "+375295555555", "KH5522881")
            );
            UserResponse dmitry = userService.createUser(
                    createUser("Дмитрий", "Мельник", "dmitry.melnik@example.com", "+375296666666", "IT7788990")
            );
            UserResponse irina = userService.createUser(
                    createUser("Ирина", "Савчук", "irina.savchuk@example.com", "+375297777777", "PP4433221")
            );
            UserResponse igor = userService.createUser(
                    createUser("Игорь", "Савченко", "igor.savchenko@example.com", "+375298888888", "PS5544332")
            );
            UserResponse olga = userService.createUser(
                    createUser("Ольга", "Романова", "olga.romanova@example.com", "+375299999999", "OR2233445")
            );
            UserResponse roman = userService.createUser(
                    createUser("Роман", "Сергеев", "roman.sergeev@example.com", "+375291010101", "RS9988776")
            );
            UserResponse andrey = userService.createUser(
                    createUser("Андрей", "Петренко", "andrey.petrenko@example.com", "+375292020202", "AP1123581")
            );

            rentalService.createRental(
                    createRental(alina.getId(), bmw520.getId(), List.of(gps.getId(), casco.getId()))
            );
            rentalService.createRental(
                    createRental(vera.getId(), audiA6.getId(), List.of(wifi.getId(), secondDriver.getId()))
            );
            rentalService.createRental(
                    createRental(irina.getId(), skodaOctavia.getId(), List.of(childSeat.getId(), winterKit.getId()))
            );
            rentalService.createRental(
                    createRental(igor.getId(), hyundaiCreta.getId(), List.of(petKit.getId(), gps.getId()))
            );

            var completedRentalPavel = rentalService.createRental(
                    createRental(pavel.getId(), mercedesE200.getId(), List.of(childSeat.getId(), winterKit.getId()))
            );
            var completedRentalPavelKarpov = rentalService.createRental(
                    createRental(pavelKarpov.getId(), mercedesC200.getId(), List.of(gps.getId(), expressWash.getId()))
            );
            var completedRentalAlinaMoroz = rentalService.createRental(
                    createRental(alinaMoroz.getId(), bmw530.getId(), List.of(casco.getId(), premiumAudio.getId()))
            );
            var completedRentalVeraBelova = rentalService.createRental(
                    createRental(veraBelova.getId(), audiA4.getId(), List.of(gps.getId(), trunkOrganizer.getId()))
            );
            var completedRentalNikita = rentalService.createRental(
                    createRental(nikita.getId(), volkswagenPolo.getId(), List.of(gps.getId()))
            );
            var completedRentalElena = rentalService.createRental(
                    createRental(elena.getId(), toyotaCamry.getId(), List.of(casco.getId(), wifi.getId()))
            );
            var completedRentalDmitry = rentalService.createRental(
                    createRental(dmitry.getId(), kiaRio.getId(), List.of(phoneHolder.getId()))
            );
            var completedRentalOlga = rentalService.createRental(
                    createRental(olga.getId(), toyotaCorolla.getId(), List.of(gps.getId(), childSeat.getId()))
            );
            var completedRentalRoman = rentalService.createRental(
                    createRental(roman.getId(), renaultDuster.getId(), List.of(winterKit.getId(), petKit.getId()))
            );
            var completedRentalAndrey = rentalService.createRental(
                    createRental(andrey.getId(), hyundaiElantra.getId(), List.of(gps.getId(), expressWash.getId()))
            );
            PaymentResponse pavelPayment = completeWithPayment(rentalService, paymentService, completedRentalPavel.getId());
            completeWithPayment(rentalService, paymentService, completedRentalPavelKarpov.getId());
            completeWithPayment(rentalService, paymentService, completedRentalAlinaMoroz.getId());
            completeWithPayment(rentalService, paymentService, completedRentalVeraBelova.getId());
            completeWithPayment(rentalService, paymentService, completedRentalNikita.getId());
            completeWithPayment(rentalService, paymentService, completedRentalElena.getId());
            completeWithPayment(rentalService, paymentService, completedRentalDmitry.getId());
            completeWithPayment(rentalService, paymentService, completedRentalOlga.getId());
            completeWithPayment(rentalService, paymentService, completedRentalRoman.getId());
            completeWithPayment(rentalService, paymentService, completedRentalAndrey.getId());

            var completedRentalPavelSecond = rentalService.createRental(
                    createRental(pavel.getId(), volkswagenJetta.getId(), List.of(gps.getId(), wifi.getId()))
            );
            var completedRentalElenaSecond = rentalService.createRental(
                    createRental(elena.getId(), kiaCeed.getId(), List.of(phoneHolder.getId(), trunkOrganizer.getId()))
            );
            var completedRentalRomanSecond = rentalService.createRental(
                    createRental(roman.getId(), skodaRapid.getId(), List.of(gps.getId(), expressWash.getId()))
            );
            var completedRentalOlgaSecond = rentalService.createRental(
                    createRental(olga.getId(), bmwX3.getId(), List.of(childSeat.getId(), winterKit.getId()))
            );

            completeWithPayment(rentalService, paymentService, completedRentalPavelSecond.getId());
            completeWithPayment(rentalService, paymentService, completedRentalElenaSecond.getId());
            completeWithPayment(rentalService, paymentService, completedRentalRomanSecond.getId());
            completeWithPayment(rentalService, paymentService, completedRentalOlgaSecond.getId());

            paymentService.refundPayment(pavelPayment.getId());
            userService.updateUserStatus(dmitry.getId(), UserStatus.BLOCKED);
        };
    }

    private static PaymentResponse completeWithPayment(
            RentalService rentalService,
            PaymentService paymentService,
            Long rentalId
    ) {
        rentalService.completeRental(rentalId);
        return paymentService.getAllPayments().stream()
                .filter(payment -> payment.getRentalId().equals(rentalId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Payment not found for rental " + rentalId));
    }

    private static ExtraServiceCreateRequest createService(
            String name,
            String description,
            double pricePerDay,
            ServiceCategory category,
            boolean isActive
    ) {
        ExtraServiceCreateRequest request = new ExtraServiceCreateRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPricePerDay(pricePerDay);
        request.setCategory(category);
        request.setIsActive(isActive);
        return request;
    }

    private static CarCreateRequest createCar(
            String brand,
            String model,
            String plate,
            int year,
            double pricePerHour
    ) {
        CarCreateRequest request = new CarCreateRequest();
        request.setBrand(brand);
        request.setModel(model);
        request.setLicensePlate(plate);
        request.setYear(year);
        request.setPricePerHour(pricePerHour);
        return request;
    }

    private static UserCreateRequest createUser(
            String firstName,
            String lastName,
            String email,
            String phone,
            String license
    ) {
        UserCreateRequest request = new UserCreateRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setPhoneNumber(phone);
        request.setDriverLicense(license);
        return request;
    }

    private static RentalCreateRequest createRental(Long userId, Long carId, List<Long> serviceIds) {
        RentalCreateRequest request = new RentalCreateRequest();
        request.setUserId(userId);
        request.setCarId(carId);
        request.setServiceIds(serviceIds);
        return request;
    }
}
