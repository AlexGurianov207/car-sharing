package com.example.carsharing.service;

import com.example.carsharing.dto.BulkRentalResponse;
import com.example.carsharing.dto.RentalCreateRequest;
import com.example.carsharing.dto.RentalResponse;
import com.example.carsharing.exception.ConflictException;
import com.example.carsharing.exception.NotFoundException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.StringJoiner;

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

    private static final class RentalSearchCacheKey {
        private final String carBrand;
        private final Long userId;
        private final RentalStatus status;
        private final int page;
        private final int size;
        private final String sort;
        private final QueryType queryType;

        private RentalSearchCacheKey(
                String carBrand,
                Long userId,
                RentalStatus status,
                int page,
                int size,
                String sort,
                QueryType queryType
        ) {
            this.carBrand = carBrand;
            this.userId = userId;
            this.status = status;
            this.page = page;
            this.size = size;
            this.sort = sort;
            this.queryType = queryType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RentalSearchCacheKey that = (RentalSearchCacheKey) o;
            return page == that.page
                    && size == that.size
                    && Objects.equals(carBrand, that.carBrand)
                    && Objects.equals(userId, that.userId)
                    && status == that.status
                    && Objects.equals(sort, that.sort)
                    && queryType == that.queryType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(carBrand, userId, status, page, size, sort, queryType);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", RentalSearchCacheKey.class.getSimpleName() + "[", "]")
                    .add("carBrand='" + carBrand + "'")
                    .add("userId=" + userId)
                    .add("status=" + status)
                    .add("page=" + page)
                    .add("size=" + size)
                    .add("sort='" + sort + "'")
                    .add("queryType=" + queryType)
                    .toString();
        }
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
        return rentalRepository.findAllBy(pageable).map(rentalMapper::toResponse);
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

        List<RentalResponse> result = queryType == QueryType.NATIVE
                ? mapNativeRowsToResponses(
                        rentalRepository.searchByFiltersNativeNoPage(
                                normalizedCarBrand,
                                hasUserId,
                                safeUserId,
                                hasStatus,
                                safeStatus.name()
                        )
                )
                : rentalRepository.searchByFiltersJpqlNoPageFetch(
                                normalizedCarBrand,
                                hasUserId,
                                safeUserId,
                                hasStatus,
                                safeStatus
                        ).stream()
                        .map(rentalMapper::toResponse)
                        .toList();

        Page<RentalResponse> resultPage = new PageImpl<>(result);
        putToIndex(key, resultPage);
        return resultPage;
    }

    private List<RentalResponse> mapNativeRowsToResponses(
            List<RentalRepository.RentalNativeSearchProjection> rows
    ) {
        return rows.stream()
                .map(this::mapNativeRowToResponse)
                .toList();
    }

    private RentalResponse mapNativeRowToResponse(RentalRepository.RentalNativeSearchProjection row) {
        RentalResponse response = new RentalResponse();
        response.setId(row.getId());
        response.setUserId(row.getUserId());
        response.setCarId(row.getCarId());
        response.setStartTime(row.getStartTime());
        response.setEndTime(row.getEndTime());
        response.setStatus(row.getStatus());

        boolean completed = isCompletedStatus(row.getStatus());
        response.setUserFullName(resolveUserFullName(row, completed));
        response.setCarInfo(resolveCarInfo(row, completed));
        response.setSelectedServices(resolveSelectedServices(row, completed));
        return response;
    }

    private boolean isCompletedStatus(String status) {
        return RentalStatus.COMPLETED.name().equals(status);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveUserFullName(RentalRepository.RentalNativeSearchProjection row, boolean completed) {
        if (completed && hasText(row.getUserFullNameSnapshot())) {
            return row.getUserFullNameSnapshot();
        }

        String firstName = row.getUserFirstName() == null ? "" : row.getUserFirstName();
        String lastName = row.getUserLastName() == null ? "" : row.getUserLastName();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? row.getUserFullNameSnapshot() : fullName;
    }

    private String resolveCarInfo(RentalRepository.RentalNativeSearchProjection row, boolean completed) {
        if (completed && hasText(row.getCarInfoSnapshot())) {
            return row.getCarInfoSnapshot();
        }

        boolean hasLiveCarInfo = row.getCarBrand() != null
                && row.getCarModel() != null
                && row.getCarLicensePlate() != null;
        if (hasLiveCarInfo) {
            return row.getCarBrand() + " " + row.getCarModel() + " (" + row.getCarLicensePlate() + ")";
        }
        return row.getCarInfoSnapshot();
    }

    private List<String> resolveSelectedServices(RentalRepository.RentalNativeSearchProjection row, boolean completed) {
        String services = completed && hasText(row.getServiceNamesSnapshot())
                ? row.getServiceNamesSnapshot()
                : row.getSelectedServiceNames();
        return splitServiceNames(services);
    }

    private List<String> splitServiceNames(String serviceNames) {
        if (serviceNames == null || serviceNames.isBlank()) {
            return List.of();
        }
        return Arrays.stream(serviceNames.split("[;,]"))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    private List<RentalResponse> mapIdsToResponses(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
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

    @Transactional
    public RentalResponse createRental(RentalCreateRequest request) {
        return createRentalInternal(request);
    }
    @Transactional
    public BulkRentalResponse createRentalsBulk(List<RentalCreateRequest> requests) {
        return processBulkRequests(requests, true);
    }

    public BulkRentalResponse createRentalsBulkWithoutTransaction(List<RentalCreateRequest> requests) {
        return processBulkRequests(requests, false);
    }

    @Transactional
    public BulkRentalResponse createRentalsBulkWithTransaction(List<RentalCreateRequest> requests) {
        return processBulkRequests(requests, true);
    }

    private BulkRentalResponse processBulkRequests(List<RentalCreateRequest> requests, boolean transactionalMode) {
        List<RentalCreateRequest> validRequests = validateBulkRequests(requests);
        String mode = transactionalMode ? "[BULK][TX]" : "[BULK][NO_TX]";
        log.info("{} Processing {} rental requests", mode, validRequests.size());

        List<RentalResponse> createdRentals = validRequests.stream()
                .map(this::createRentalInternal)
                .toList();

        if (transactionalMode) {
            log.info("{} Successfully created {} rentals in one transaction", mode, createdRentals.size());
        } else {
            log.info("{} Successfully created {} rentals", mode, createdRentals.size());
        }

        BulkRentalResponse response = new BulkRentalResponse();
        response.setRequestedCount(validRequests.size());
        response.setCreatedCount(createdRentals.size());
        response.setRentals(createdRentals);
        return response;
    }

    private List<RentalCreateRequest> validateBulkRequests(List<RentalCreateRequest> requests) {
        List<RentalCreateRequest> validRequests = Optional.ofNullable(requests)
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new InvalidDataAccessApiUsageException(
                        "Bulk request must contain at least one rental"));

        if (validRequests.size() > 100) {
            throw new IllegalArgumentException("Bulk request size exceeds limit: 100");
        }
        if (validRequests.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Bulk request contains null item");
        }
        return validRequests;
    }

    private RentalResponse createRentalInternal(RentalCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + request.getUserId()));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidDataAccessApiUsageException("User is not active. Status: " + user.getStatus());
        }

        if (rentalRepository.existsByUserIdAndEndTimeIsNull(user.getId())) {
            throw new ConflictException(
                    "User already has an active rental. Complete it first.");
        }

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new NotFoundException("Car not found with id: " + request.getCarId()));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new ConflictException("Car is not available. Status: " + car.getStatus());
        }

        if (rentalRepository.existsByCarIdAndEndTimeIsNull(car.getId())) {
            throw new DataIntegrityViolationException("Car is already rented");
        }

        Rental rental = rentalMapper.toEntity(user, car);

        List<Long> requestedServiceIds = Optional.ofNullable(request.getServiceIds())
                .orElseGet(List::of);

        if (!requestedServiceIds.isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(requestedServiceIds);
            validateAllRequestedServicesFound(requestedServiceIds, services);
            validateServicesAvailableForCar(car, services);
            rental.setSelectedServices(services);
        }

        Rental savedRental = rentalRepository.save(rental);

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
        invalidateSearchIndex();

        return rentalMapper.toResponse(savedRental);
    }

    private void validateAllRequestedServicesFound(
            List<Long> requestedServiceIds,
            List<ExtraService> foundServices
    ) {
        List<Long> foundIds = foundServices.stream()
                .map(ExtraService::getId)
                .filter(Objects::nonNull)
                .toList();

        List<Long> missingServiceIds = requestedServiceIds.stream()
                .distinct()
                .filter(serviceId -> !foundIds.contains(serviceId))
                .toList();

        if (!missingServiceIds.isEmpty()) {
            throw new NoSuchElementException("Services not found with ids: " + missingServiceIds);
        }
    }

    private void validateServicesAvailableForCar(Car car, List<ExtraService> services) {
        List<String> unavailableServiceNames = services.stream()
                .filter(service -> !car.getAvailableServices().contains(service))
                .map(ExtraService::getName)
                .distinct()
                .toList();

        if (!unavailableServiceNames.isEmpty()) {
            throw new InvalidDataAccessApiUsageException(
                    "Services not available for this car: " + String.join(", ", unavailableServiceNames));
        }
    }

    @Transactional
    public RentalResponse completeRental(Long id) {
        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(RENTAL_NOT_FOUND_MESSAGE + id));

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
                .orElseThrow(() -> new NotFoundException(RENTAL_NOT_FOUND_MESSAGE + id));
        return rentalMapper.toResponse(rental);
    }

    public List<RentalResponse> getUserRentals(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found with id: " + userId);
        }

        return rentalRepository.findByUserId(userId).stream()
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
        List<Long> activeIds = rentalRepository.findActiveRentalIds();

        if (activeIds.isEmpty()) {
            throw new NoSuchElementException("No active rentals found");
        }

        return mapIdsToResponses(activeIds);
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

        log.info("Rental {} and related links were deleted", id);
    }

    public List<RentalResponse> demonstrateNPlus1Problem() {
        log.info("========== N+1 PROBLEM DEMO START ==========");
        List<Rental> rentals = rentalRepository.findAllSlow();
        List<RentalResponse> responses = rentals.stream()
                .map(rentalMapper::toResponse)
                .toList();
        log.info("========== N+1 PROBLEM DEMO END ==========");
        return responses;
    }

    public List<RentalResponse> demonstrateSolutionWithEntityGraph() {
        log.info("========== N+1 SOLUTION DEMO START ==========");
        List<Rental> rentals = rentalRepository.findAll();
        List<RentalResponse> responses = rentals.stream()
                .map(rentalMapper::toResponse)
                .toList();
        log.info("========== N+1 SOLUTION DEMO END ==========");
        return responses;
    }

    public RentalResponse createRentalWithoutTransaction(RentalCreateRequest request) {
        log.info("=== DEMO WITHOUT @Transactional ===");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE));

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new NotFoundException(CAR_NOT_FOUND_MESSAGE));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new InvalidDataAccessApiUsageException("Car is not available");
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setStartTime(LocalDateTime.now());

        Rental savedRental = rentalRepository.save(rental);
        log.info("Rental persisted in DB. ID={}", savedRental.getId());

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
        log.info("Car status updated to RENTED");

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());

            if (services.size() != request.getServiceIds().size()) {
                log.error("Some services were not found. Rental {} is already persisted", savedRental.getId());
                throw new NoSuchElementException("Some services not found");
            }

            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    log.error("Service {} is unavailable. Rental {} is already persisted",
                            service.getName(), savedRental.getId());
                    throw new InvalidDataAccessApiUsageException("Service " + service.getName() +
                            " is not available for this car");
                }
            }

            savedRental.setSelectedServices(services);
            rentalRepository.save(savedRental);
        }

        log.info("Rental {} successfully created (without transaction protection)", savedRental.getId());
        invalidateSearchIndex();
        return rentalMapper.toResponse(savedRental);
    }

    @Transactional
    public RentalResponse createRentalWithTransaction(RentalCreateRequest request) {
        log.info("=== DEMO WITH @Transactional ===");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE));

        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new NotFoundException(CAR_NOT_FOUND_MESSAGE));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new InvalidDataAccessApiUsageException("Car is not available");
        }

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setCar(car);
        rental.setStartTime(LocalDateTime.now());

        Rental savedRental = rentalRepository.save(rental);
        log.info("Rental created in transaction context (not committed yet)");

        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<ExtraService> services = extraServiceRepository.findAllById(request.getServiceIds());

            if (services.size() != request.getServiceIds().size()) {
                log.error("Some services were not found. Transaction will be rolled back");
                throw new NoSuchElementException("Some services not found");
            }

            for (ExtraService service : services) {
                if (!car.getAvailableServices().contains(service)) {
                    log.error("Service {} is unavailable. Transaction will be rolled back", service.getName());
                    throw new InvalidDataAccessApiUsageException("Service " + service.getName() +
                            " is not available for this car");
                }
            }

            rental.setSelectedServices(services);
        }

        log.info("Transaction finished successfully. Rental {} persisted", savedRental.getId());
        invalidateSearchIndex();
        return rentalMapper.toResponse(savedRental);
    }
}
