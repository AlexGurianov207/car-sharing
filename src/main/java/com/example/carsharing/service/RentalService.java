package com.example.carsharing.service;

import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.model.Car;
import com.example.carsharing.model.CarStatus;
import com.example.carsharing.model.ExtraService;
import com.example.carsharing.model.Payment;
import com.example.carsharing.model.PaymentStatus;
import com.example.carsharing.model.Rental;
import com.example.carsharing.model.RentalStatus;
import com.example.carsharing.model.User;
import com.example.carsharing.model.UserStatus;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.ExtraServiceRepository;
import com.example.carsharing.repository.PaymentRepository;
import com.example.carsharing.repository.RentalRepository;
import com.example.carsharing.repository.UserRepository;
import com.example.carsharing.service.mapper.RentalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final CarRepository carRepository;
    private final ExtraServiceRepository extraServiceRepository;
    private final RentalMapper rentalMapper;
    private final PaymentRepository paymentRepository;

    private static final String CAR_NOT_FOUND_MESSAGE = "Car not found";
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";
    private static final String RENTAL_NOT_FOUND_MESSAGE = "Rental not found with id: ";
    private final Map<RentalSearchCacheKey, Page<RentalResponse>> rentalSearchIndex =
            Collections.synchronizedMap(new HashMap<>());

    private enum QueryType {
        JPQL,
        NATIVE
    }

    private record RentalSearchCacheKey(
            String carBrand,
            Long userId,
            RentalStatus status,
            int page,
            int size,
            String sort,
            QueryType queryType
    ) {
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> searchRentalsJpql(
            String carBrand,
            Long userId,
            RentalStatus status
    ) {
        return searchRentalsListAsPageInternal(carBrand, userId, status, QueryType.JPQL).getContent();
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> searchRentalsNative(
            String carBrand,
            Long userId,
            RentalStatus status
    ) {
        return searchRentalsListAsPageInternal(carBrand, userId, status, QueryType.NATIVE).getContent();
    }

    @Transactional(readOnly = true)
    public Page<RentalResponse> getRentalsPage(Pageable pageable) {
        log.info("[PAGE] Request page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<Rental> rentalsPage = rentalRepository.findAll(pageable);
        return mapPageWithDetails(rentalsPage, pageable);
    }

    @Transactional(readOnly = true)
    public Page<RentalResponse> searchRentals(
            String carBrand,
            Long userId,
            RentalStatus status,
            boolean useNative,
            Pageable pageable
    ) {
        return searchRentalsInternal(
                carBrand,
                userId,
                status,
                useNative ? QueryType.NATIVE : QueryType.JPQL,
                pageable
        );
    }

    private Page<RentalResponse> searchRentalsListAsPageInternal(
            String carBrand,
            Long userId,
            RentalStatus status,
            QueryType queryType
    ) {
        String normalizedCarBrand = normalize(carBrand);
        boolean hasUserId = userId != null;
        boolean hasStatus = status != null;
        Long safeUserId = hasUserId ? userId : -1L;
        RentalStatus safeStatus = hasStatus ? status : RentalStatus.ACTIVE;
        RentalSearchCacheKey key = new RentalSearchCacheKey(
                normalizedCarBrand,
                userId,
                status,
                0,
                -1,
                "LIST[startTime,DESC]",
                queryType
        );

        Page<RentalResponse> cachedPage = rentalSearchIndex.get(key);
        if (cachedPage != null) {
            log.info("[CACHE] HIT key={}", key);
            return cachedPage;
        }
        log.info("[CACHE] MISS key={}", key);

        List<Rental> rentals = queryType == QueryType.NATIVE
                ? rentalRepository.searchByFiltersNativeNoPage(
                        normalizedCarBrand,
                        hasUserId,
                        safeUserId,
                        hasStatus,
                        safeStatus.name()
                )
                : rentalRepository.searchByFiltersJpqlNoPage(
                        normalizedCarBrand,
                        hasUserId,
                        safeUserId,
                        hasStatus,
                        safeStatus
                );

        List<RentalResponse> result = mapListWithDetails(rentals);
        Page<RentalResponse> resultPage = new PageImpl<>(result);
        putToIndex(key, resultPage);
        return resultPage;
    }

    private Page<RentalResponse> searchRentalsInternal(
            String carBrand,
            Long userId,
            RentalStatus status,
            QueryType queryType,
            Pageable pageable
    ) {
        String normalizedCarBrand = normalize(carBrand);
        boolean hasUserId = userId != null;
        boolean hasStatus = status != null;
        Long safeUserId = hasUserId ? userId : -1L;
        RentalStatus safeStatus = hasStatus ? status : RentalStatus.ACTIVE;
        RentalSearchCacheKey key = new RentalSearchCacheKey(
                normalizedCarBrand,
                userId,
                status,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString(),
                queryType
        );

        Page<RentalResponse> cachedPage = rentalSearchIndex.get(key);
        if (cachedPage != null) {
            log.info("[CACHE] HIT key={}", key);
            return cachedPage;
        }
        log.info("[CACHE] MISS key={}", key);

        Page<Rental> rentalsPage = queryType == QueryType.NATIVE
                ? rentalRepository.searchByFiltersNative(
                        normalizedCarBrand,
                        hasUserId,
                        safeUserId,
                        hasStatus,
                        safeStatus.name(),
                        toNativePageable(pageable)
                )
                : rentalRepository.searchByFiltersJpql(
                        normalizedCarBrand,
                        hasUserId,
                        safeUserId,
                        hasStatus,
                        safeStatus,
                        pageable
                );

        Page<RentalResponse> resultPage = mapPageWithDetails(rentalsPage, pageable);
        putToIndex(key, resultPage);
        return resultPage;
    }

    private Page<RentalResponse> mapPageWithDetails(Page<Rental> rentalsPage, Pageable pageable) {
        List<RentalResponse> content = mapListWithDetails(rentalsPage.getContent());
        return new PageImpl<>(content, pageable, rentalsPage.getTotalElements());
    }

    private List<RentalResponse> mapListWithDetails(List<Rental> rentals) {
        if (rentals.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rentals.stream()
                .map(Rental::getId)
                .toList();
        Map<Long, Rental> rentalsById = rentalRepository.findAllWithDetailsByIdIn(ids).stream()
                .collect(Collectors.toMap(Rental::getId, Function.identity()));

        return ids.stream()
                .map(rentalsById::get)
                .filter(Objects::nonNull)
                .map(rentalMapper::toResponse)
                .toList();
    }

    public void invalidateSearchIndex() {
        int removedEntries = rentalSearchIndex.size();
        rentalSearchIndex.clear();
        log.info("[CACHE] INVALIDATE removedEntries={}", removedEntries);
    }

    private synchronized Page<RentalResponse> putToIndex(
            RentalSearchCacheKey key,
            Page<RentalResponse> value
    ) {
        if (rentalSearchIndex.containsKey(key)) {
            log.info("[CACHE] UPDATE key={}", key);
        } else {
            log.info("[CACHE] PUT key={}", key);
        }
        Page<RentalResponse> previousValue = rentalSearchIndex.put(key, value);
        log.info("[CACHE] SIZE={}", rentalSearchIndex.size());
        return previousValue;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed.toLowerCase(Locale.ROOT);
    }

    private Pageable toNativePageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        Sort nativeSort = Sort.by(
                pageable.getSort().stream()
                        .map(order -> new Sort.Order(order.getDirection(), toSnakeCase(order.getProperty())))
                        .toList()
        );
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), nativeSort);
    }

    private String toSnakeCase(String value) {
        return value
                .replaceAll("([a-z])([A-Z]+)", "$1_$2")
                .toLowerCase();
    }

    @Transactional
    public RentalResponse createRental(RentalCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidDataAccessApiUsageException("User is not active. Status: " + user.getStatus());
        }

        if (rentalRepository.existsByUserIdAndEndTimeIsNull(user.getId())) {
            throw new InvalidDataAccessApiUsageException(
                    "User already has an active rental. Complete it first.");
        }

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + request.getCarId()));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new InvalidDataAccessApiUsageException("Car is not available. Status: " + car.getStatus());
        }

        if (rentalRepository.existsByCarIdAndEndTimeIsNull(car.getId())) {
            throw new DataIntegrityViolationException("Car is already rented");
        }

        Rental rental = rentalMapper.toEntity(user, car);

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());
            if (services.size() != request.getServiceIds().size()) {
                throw new NoSuchElementException("Some services not found");
            }
            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    throw new InvalidDataAccessApiUsageException("Service " + service.getName() +
                            " is not available for this car");
                }
            }
            rental.setSelectedServices(services);
        }

        Rental savedRental = rentalRepository.save(rental);

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
        invalidateSearchIndex();

        return rentalMapper.toResponse(savedRental);
    }

    @Transactional
    public RentalResponse completeRental(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(RENTAL_NOT_FOUND_MESSAGE + id));

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new InvalidDataAccessApiUsageException("Rental is not active. Status: " + rental.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(rental.getStartTime())) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot complete rental before it started. Start time: " +
                            rental.getStartTime() + ", current time: " + now);
        }

        rental.setEndTime(now);

        User user = rental.getUser();
        rental.setUserFullName(user.getFirstName() + " " + user.getLastName());

        Car car = rental.getCar();
        rental.setCarInfo(car.getBrand() + " " + car.getModel() + " (" + car.getLicensePlate() + ")");

        if (rental.getSelectedServices() != null && !rental.getSelectedServices().isEmpty()) {
            String servicesString = rental.getSelectedServices().stream()
                    .map(ExtraService::getName)
                    .collect(Collectors.joining(";"));
            rental.setServiceNames(servicesString);
        }

        long hours = Duration.between(rental.getStartTime(), now).toHours();
        if (hours < 1) {
            hours = 1;
        }
        long days = hours / 24 + (hours % 24 == 0 ? 0 : 1);

        double carPrice = car.getPricePerHour() * hours;
        double servicesPrice = rental.getSelectedServices().stream()
                .mapToDouble(s -> s.getPricePerDay() * days)
                .sum();

        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setAmount(carPrice + servicesPrice);
        payment.setCarAmount(carPrice);
        payment.setServicesAmount(servicesPrice);

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        rental.setStatus(RentalStatus.COMPLETED);

        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);

        rental.setPayment(payment);
        Rental updatedRental = rentalRepository.save(rental);
        invalidateSearchIndex();

        return rentalMapper.toResponse(updatedRental);
    }

    public RentalResponse getRentalById(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(RENTAL_NOT_FOUND_MESSAGE + id));
        return rentalMapper.toResponse(rental);
    }

    public List<RentalResponse> getUserRentals(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found with id: " + userId);
        }

        List<Rental> rentals = rentalRepository.findByUserId(userId);

        if (rentals.isEmpty()) {
            throw new NoSuchElementException("No rentals found for user with id: " + userId);
        }

        return rentals.stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    public List<RentalResponse> getCarRentals(Long carId) {
        if (!carRepository.existsById(carId)) {
            throw new NoSuchElementException("Car not found with id: " + carId);
        }

        List<Rental> rentals = rentalRepository.findByCarId(carId);

        if (rentals.isEmpty()) {
            throw new NoSuchElementException("No rentals found for car with id: " + carId);
        }

        return rentals.stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    public List<RentalResponse> getActiveRentals() {
        List<Rental> rentals = rentalRepository.findByEndTimeIsNull();

        if (rentals.isEmpty()) {
            throw new NoSuchElementException("No active rentals found");
        }

        return rentals.stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteRental(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Rental not found"));

        if (rental.getStatus() == RentalStatus.ACTIVE) {
            throw new InvalidDataAccessApiUsageException(
                    "Cannot delete active rental. Complete or cancel it first.");
        }

        Payment payment = rental.getPayment();
        if (payment != null) {
            payment.setRental(null);
            paymentRepository.save(payment);
        }

        rentalRepository.delete(rental);
        invalidateSearchIndex();

        log.info("Аренда {} и связанный платеж удалены", id);
    }

    public List<RentalResponse> demonstrateNPlus1Problem() {
        log.info("========== ДЕМОНСТРАЦИЯ N+1 ПРОБЛЕМЫ ==========");
        List<Rental> rentals = rentalRepository.findAllSlow();
        List<RentalResponse> responses = rentals.stream()
                .map(rentalMapper::toResponse)
                .toList();
        log.info("========== КОНЕЦ ДЕМОНСТРАЦИИ ==========");
        return responses;
    }

    public List<RentalResponse> demonstrateSolutionWithEntityGraph() {
        log.info("========== РЕШЕНИЕ N+1 ПРОБЛЕМЫ ==========");
        List<Rental> rentals = rentalRepository.findAll();
        List<RentalResponse> responses = rentals.stream()
                .map(rentalMapper::toResponse)
                .toList();
        log.info("========== КОНЕЦ РЕШЕНИЯ ==========");
        return responses;
    }

    public RentalResponse createRentalWithoutTransaction(RentalCreateRequest request) {
        log.info("=== ДЕМОНСТРАЦИЯ БЕЗ @Transactional ===");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException(CAR_NOT_FOUND_MESSAGE));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new InvalidDataAccessApiUsageException("Car is not available");
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setStartTime(LocalDateTime.now());

        Rental savedRental = rentalRepository.save(rental);
        log.info("Аренда сохранена в БД! ID: {}", savedRental.getId());

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
        log.info("Статус машины обновлен на RENTED");

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());

            if (services.size() != request.getServiceIds().size()) {
                log.error("ОШИБКА: Не все сервисы найдены. Аренда {} уже в БД!", savedRental.getId());
                throw new NoSuchElementException("Some services not found");
            }

            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    log.error("ОШИБКА: Сервис {} недоступен. Аренда {} уже в БД!",
                            service.getName(), savedRental.getId());
                    throw new InvalidDataAccessApiUsageException("Service " + service.getName() +
                            " is not available for this car");
                }
            }

            savedRental.setSelectedServices(services);
            rentalRepository.save(savedRental);
        }

        log.info("Аренда {} успешно создана (НО ЕСЛИ БЫЛА ОШИБКА - ОНА БЫ ОСТАЛАСЬ!)", savedRental.getId());
        invalidateSearchIndex();
        return rentalMapper.toResponse(savedRental);
    }

    @Transactional
    public RentalResponse createRentalWithTransaction(RentalCreateRequest request) {
        log.info("=== ДЕМОНСТРАЦИЯ С @Transactional ===");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new RuntimeException(CAR_NOT_FOUND_MESSAGE));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new InvalidDataAccessApiUsageException("Car is not available");
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setStartTime(LocalDateTime.now());

        Rental savedRental = rentalRepository.save(rental);
        log.info("Аренда создана в памяти, но еще не закоммичена в БД");

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
        
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());

            if (services.size() != request.getServiceIds().size()) {
                log.error("ОШИБКА: Не все сервисы найдены. Транзакция откатится!");
                throw new NoSuchElementException("Some services not found");
            }

            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    log.error("ОШИБКА: Сервис {} недоступен. Транзакция откатится!", service.getName());
                    throw new InvalidDataAccessApiUsageException("Service " + service.getName() +
                            " is not available for this car");
                }
            }

            rental.setSelectedServices(services);
        }

        log.info("Транзакция успешно завершена. Аренда {} сохранена в БД", savedRental.getId());
        invalidateSearchIndex();
        return rentalMapper.toResponse(savedRental);
    }
}
