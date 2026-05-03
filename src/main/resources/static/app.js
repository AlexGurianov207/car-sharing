const { useEffect, useMemo, useState } = React;
const html = htm.bind(React.createElement);

const rootNode = document.getElementById("root");
const appRoot = ReactDOM.createRoot
    ? ReactDOM.createRoot(rootNode)
    : { render: (node) => ReactDOM.render(node, rootNode) };

const PAGE_SIZE = 6;
const ADMIN_TABS = [
    { id: "overview", label: "Обзор" },
    { id: "cars", label: "Автопарк" },
    { id: "rentals", label: "Аренды" },
    { id: "users", label: "Клиенты" },
    { id: "services", label: "Опции" },
    { id: "payments", label: "Платежи" }
];
const USER_TABS = [
    { id: "catalog", label: "Каталог" },
    { id: "rentals", label: "Мои поездки" },
    { id: "payments", label: "Мои платежи" },
    { id: "profile", label: "Профиль" }
];
const CAR_STATUSES = ["AVAILABLE", "RENTED"];
const USER_STATUSES = ["ACTIVE", "BLOCKED"];
const SERVICE_CATEGORIES = ["SAFETY", "COMFORT", "EQUIPMENT", "INSURANCE"];
const PAYMENT_STATUSES = ["COMPLETED", "REFUNDED"];
const RENTAL_STATUSES = ["ACTIVE", "COMPLETED"];
const PRICE_BANDS = ["", "budget", "standard", "premium"];
const STATUS_LABELS = {
    AVAILABLE: "Доступна",
    RENTED: "В аренде",
    DELETED: "Скрыта",
    ACTIVE: "Активна",
    BLOCKED: "Заблокирован",
    COMPLETED: "Завершена",
    REFUNDED: "Возврат",
    INACTIVE: "Неактивна"
};
const PRICE_BAND_LABELS = {
    "": "Любой",
    budget: "До 15 $/час",
    standard: "15–25 $/час",
    premium: "От 25 $/час"
};
const EMPTY_DATA = {
    cars: [],
    services: [],
    users: [],
    rentals: [],
    payments: [],
    profile: null
};

function App() {
    const [booting, setBooting] = useState(true);
    const [entryMode, setEntryMode] = useState("guest");
    const [authMode, setAuthMode] = useState("login");
    const [session, setSession] = useState(null);
    const [data, setData] = useState(EMPTY_DATA);
    const [toast, setToast] = useState(null);
    const [authError, setAuthError] = useState("");
    const [activeTab, setActiveTab] = useState("catalog");
    const [selectedCarId, setSelectedCarId] = useState(null);
    const [selectedEntity, setSelectedEntity] = useState(null);
    const [bookingCarId, setBookingCarId] = useState("");
    const [busy, setBusy] = useState(false);
    const [pages, setPages] = useState({
        guestCars: 1,
        catalogCars: 1,
        rentals: 1,
        users: 1,
        services: 1,
        payments: 1
    });
    const [authForm, setAuthForm] = useState({
        login: "",
        password: "",
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
        driverLicense: ""
    });
    const [guestFilters, setGuestFilters] = useState({
        query: "",
        brand: "",
        status: "AVAILABLE",
        priceBand: ""
    });
    const [catalogFilters, setCatalogFilters] = useState({
        query: "",
        brand: "",
        status: "",
        priceBand: ""
    });
    const [rentalFilters, setRentalFilters] = useState({
        query: "",
        status: ""
    });
    const [userFilters, setUserFilters] = useState({
        query: "",
        status: ""
    });
    const [serviceFilters, setServiceFilters] = useState({
        query: "",
        category: "",
        isActive: ""
    });
    const [paymentFilters, setPaymentFilters] = useState({
        query: "",
        status: ""
    });
    const [carForm, setCarForm] = useState(emptyCarForm());
    const [serviceForm, setServiceForm] = useState(emptyServiceForm());
    const [userForm, setUserForm] = useState(emptyUserForm());
    const [adminRentalForm, setAdminRentalForm] = useState(emptyRentalForm());
    const [userRentalForm, setUserRentalForm] = useState(emptyRentalForm());

    useEffect(() => {
        bootstrap();
    }, []);

    useEffect(() => {
        if (!toast) {
            return undefined;
        }
        const timer = window.setTimeout(() => setToast(null), 3200);
        return () => window.clearTimeout(timer);
    }, [toast]);

    const isAdmin = session?.role === "admin";
    const isUser = session?.role === "user";
    const currentTabs = isAdmin ? ADMIN_TABS : USER_TABS;

    const guestCars = useMemo(() => {
        return filterCars(data.cars, guestFilters);
    }, [data.cars, guestFilters]);

    const catalogCars = useMemo(() => {
        return filterCars(data.cars, catalogFilters);
    }, [data.cars, catalogFilters]);

    const filteredRentals = useMemo(() => {
        return filterRentals(data.rentals, rentalFilters);
    }, [data.rentals, rentalFilters]);

    const filteredUsers = useMemo(() => {
        return filterUsers(data.users, userFilters);
    }, [data.users, userFilters]);

    const filteredServices = useMemo(() => {
        return filterServices(data.services, serviceFilters);
    }, [data.services, serviceFilters]);

    const filteredPayments = useMemo(() => {
        return filterPayments(data.payments, paymentFilters);
    }, [data.payments, paymentFilters]);

    const guestPage = paginate(guestCars, pages.guestCars);
    const catalogPage = paginate(catalogCars, pages.catalogCars);
    const rentalPage = paginate(filteredRentals, pages.rentals);
    const userPage = paginate(filteredUsers, pages.users);
    const servicePage = paginate(filteredServices, pages.services);
    const paymentPage = paginate(filteredPayments, pages.payments);

    const selectedCar = data.cars.find((item) => Number(item.id) === Number(selectedCarId)) || null;
    const selectedCardData = resolveSelectedEntity(selectedEntity, data);
    const currentUserActiveRental = data.rentals.find((item) => item.status === "ACTIVE") || null;

    const overview = useMemo(() => {
        return {
            totalCars: data.cars.length,
            freeCars: data.cars.filter((item) => item.status === "AVAILABLE").length,
            totalUsers: data.users.length,
            activeRentals: data.rentals.filter((item) => item.status === "ACTIVE").length,
            activeServices: data.services.filter((item) => item.isActive).length,
            revenue: data.payments
                .filter((item) => item.status === "COMPLETED")
                .reduce((sum, item) => sum + Number(item.amount || 0), 0)
        };
    }, [data]);

    async function bootstrap() {
        setBooting(true);
        try {
            const me = await api("/api/auth/me");
            const nextSession = normalizeSession(me);
            setSession(nextSession);
            setEntryMode("app");
            setActiveTab(nextSession.role === "admin" ? "overview" : "catalog");
            await loadPrivateData(nextSession);
        } catch (error) {
            if (!isUnauthorized(error)) {
                notify(parseErrorMessage(error));
            }
            setSession(null);
            setEntryMode("guest");
            setActiveTab("catalog");
            await loadGuestData();
        } finally {
            setBooting(false);
        }
    }

    async function loadGuestData() {
        try {
            const [cars, services] = await Promise.all([
                api("/api/cars"),
                api("/api/services?onlyActive=true")
            ]);
            setData((current) => ({
                ...current,
                cars,
                services
            }));
            if (!selectedCarId && cars.length) {
                setSelectedCarId(cars[0].id);
            }
        } catch (error) {
            notify(parseErrorMessage(error));
        }
    }

    async function loadPrivateData(nextSession = session) {
        if (!nextSession) {
            return;
        }
        if (nextSession.role === "admin") {
            const [cars, services, users, rentals, payments] = await Promise.all([
                api("/api/cars"),
                api("/api/services"),
                api("/api/users"),
                api("/api/rentals/search/jpql"),
                api("/api/payments")
            ]);
            setData({
                cars,
                services,
                users,
                rentals,
                payments,
                profile: null
            });
            if (!selectedCarId && cars.length) {
                setSelectedCarId(cars[0].id);
            }
            return;
        }

        const [cars, services, profile, rentals, payments] = await Promise.all([
            api("/api/cars"),
            api("/api/services?onlyActive=true"),
            api("/api/users/me"),
            api("/api/rentals/me"),
            api("/api/payments/me")
        ]);
        setData({
            cars,
            services,
            users: [],
            rentals,
            payments,
            profile
        });
        if (!selectedCarId && cars.length) {
            setSelectedCarId(cars[0].id);
        }
    }

    async function refreshEverything() {
        setBusy(true);
        try {
            if (session) {
                await loadPrivateData();
            } else {
                await loadGuestData();
            }
            notify("Данные обновлены");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function submitLogin(event) {
        event.preventDefault();
        setBusy(true);
        setAuthError("");
        try {
            const response = await api("/api/auth/login", "POST", {
                login: authForm.login.trim(),
                password: authForm.password
            });
            const nextSession = normalizeSession(response);
            setSession(nextSession);
            setEntryMode("app");
            setActiveTab(nextSession.role === "admin" ? "overview" : "catalog");
            resetAuthForm();
            await loadPrivateData(nextSession);
            notify(`С возвращением, ${nextSession.firstName || "пользователь"}`);
        } catch (error) {
            setAuthError(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function submitRegister(event) {
        event.preventDefault();
        setBusy(true);
        setAuthError("");
        try {
            const response = await api("/api/auth/register", "POST", {
                firstName: authForm.firstName.trim(),
                lastName: authForm.lastName.trim(),
                email: authForm.email.trim(),
                phoneNumber: authForm.phoneNumber.trim(),
                driverLicense: authForm.driverLicense.trim(),
                password: authForm.password
            });
            const nextSession = normalizeSession(response);
            setSession(nextSession);
            setEntryMode("app");
            setActiveTab("catalog");
            resetAuthForm();
            await loadPrivateData(nextSession);
            notify("Аккаунт создан");
        } catch (error) {
            setAuthError(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function submitLogout() {
        setBusy(true);
        try {
            await api("/api/auth/logout", "POST");
        } catch (error) {
            // Logout should still clear local state even if the session is already gone.
        } finally {
            setSession(null);
            setData(EMPTY_DATA);
            setEntryMode("guest");
            setActiveTab("catalog");
            setSelectedEntity(null);
            setSelectedCarId(null);
            resetAuthForm();
            await loadGuestData();
            setBusy(false);
        }
    }

    async function submitCar(event) {
        event.preventDefault();
        setBusy(true);
        try {
            const body = {
                licensePlate: carForm.licensePlate.trim().toUpperCase(),
                brand: carForm.brand.trim(),
                model: carForm.model.trim(),
                year: Number(carForm.year),
                pricePerHour: Number(carForm.pricePerHour)
            };
            const endpoint = carForm.id ? `/api/cars/${carForm.id}` : "/api/cars";
            const method = carForm.id ? "PUT" : "POST";
            const saved = await api(endpoint, method, body);
            await api(`/api/cars/${saved.id}/available-services`, "PUT", carForm.serviceIds.map(Number));
            setCarForm(emptyCarForm());
            await loadPrivateData();
            notify(carForm.id ? "Машина обновлена" : "Машина добавлена");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function submitService(event) {
        event.preventDefault();
        setBusy(true);
        try {
            const body = {
                name: serviceForm.name.trim(),
                description: serviceForm.description.trim(),
                pricePerDay: Number(serviceForm.pricePerDay),
                category: serviceForm.category,
                isActive: serviceForm.isActive === "true"
            };
            const endpoint = serviceForm.id ? `/api/services/${serviceForm.id}` : "/api/services";
            const method = serviceForm.id ? "PUT" : "POST";
            await api(endpoint, method, body);
            setServiceForm(emptyServiceForm());
            await loadPrivateData();
            notify(serviceForm.id ? "Опция обновлена" : "Опция добавлена");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function submitUser(event) {
        event.preventDefault();
        setBusy(true);
        try {
            const body = {
                firstName: userForm.firstName.trim(),
                lastName: userForm.lastName.trim(),
                email: userForm.email.trim(),
                phoneNumber: userForm.phoneNumber.trim(),
                driverLicense: userForm.driverLicense.trim()
            };
            const endpoint = userForm.id ? `/api/users/${userForm.id}` : "/api/users";
            const method = userForm.id ? "PUT" : "POST";
            await api(endpoint, method, body);
            setUserForm(emptyUserForm());
            await loadPrivateData();
            notify(userForm.id ? "Клиент обновлён" : "Клиент создан");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function submitAdminRental(event) {
        event.preventDefault();
        setBusy(true);
        try {
            await api("/api/rentals", "POST", {
                userId: Number(adminRentalForm.userId),
                carId: Number(adminRentalForm.carId),
                serviceIds: adminRentalForm.serviceIds.map(Number)
            });
            setAdminRentalForm(emptyRentalForm());
            await loadPrivateData();
            notify("Аренда создана");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function submitUserRental(event) {
        event.preventDefault();
        if (!session?.userId) {
            return;
        }
        setBusy(true);
        try {
            await api("/api/rentals", "POST", {
                userId: Number(session.userId),
                carId: Number(userRentalForm.carId),
                serviceIds: userRentalForm.serviceIds.map(Number)
            });
            setUserRentalForm(emptyRentalForm());
            await loadPrivateData();
            setActiveTab("rentals");
            notify("Поездка оформлена");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function deleteEntity(type, id) {
        const urlMap = {
            car: `/api/cars/${id}`,
            rental: `/api/rentals/${id}`,
            user: `/api/users/${id}`,
            service: `/api/services/${id}`,
            payment: `/api/payments/${id}`
        };
        if (!urlMap[type]) {
            return;
        }
        setBusy(true);
        try {
            await api(urlMap[type], "DELETE");
            await loadPrivateData();
            notify("Запись удалена");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function completeRental(id) {
        setBusy(true);
        try {
            await api(`/api/rentals/${id}/complete`, "PATCH");
            await loadPrivateData();
            notify("Аренда завершена");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function refundPayment(id) {
        setBusy(true);
        try {
            await api(`/api/payments/${id}/refund`, "PATCH");
            await loadPrivateData();
            notify("Возврат оформлен");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function verifyPayment(id) {
        setBusy(true);
        try {
            const task = await api(`/api/payments/${id}/verify/async`, "POST");
            notify(`Проверка запущена: ${task.taskId || "новая задача"}`);
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    async function changeUserStatus(id, status) {
        setBusy(true);
        try {
            await api(`/api/users/${id}/status?status=${encodeURIComponent(status)}`, "PATCH");
            await loadPrivateData();
            notify("Статус обновлён");
        } catch (error) {
            notify(parseErrorMessage(error));
        } finally {
            setBusy(false);
        }
    }

    function notify(message) {
        setToast(message);
    }

    function resetAuthForm() {
        setAuthForm({
            login: "",
            password: "",
            firstName: "",
            lastName: "",
            email: "",
            phoneNumber: "",
            driverLicense: ""
        });
    }

    function openCarDetails(carId) {
        setSelectedCarId(carId);
        const target = document.getElementById("car-focus");
        if (target) {
            target.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    }

    function startBooking(car) {
        setSelectedCarId(car.id);
        setBookingCarId(car.id);
        setUserRentalForm({
            ...emptyRentalForm(),
            carId: String(car.id),
            serviceIds: (car.availableServices || []).map((item) => Number(item.id))
        });
        if (session) {
            setActiveTab("catalog");
            window.requestAnimationFrame(() => {
                const target = document.getElementById("booking-panel");
                if (target) {
                    target.scrollIntoView({ behavior: "smooth", block: "start" });
                }
            });
        } else {
            setAuthMode("login");
            setEntryMode("auth");
        }
    }

    function editCar(car) {
        setCarForm({
            id: car.id,
            licensePlate: car.licensePlate || "",
            brand: car.brand || "",
            model: car.model || "",
            year: String(car.year || ""),
            pricePerHour: String(car.pricePerHour || ""),
            serviceIds: (car.availableServices || []).map((item) => Number(item.id))
        });
        setActiveTab("cars");
    }

    function editService(service) {
        setServiceForm({
            id: service.id,
            name: service.name || "",
            description: service.description || "",
            pricePerDay: String(service.pricePerDay || ""),
            category: service.category || "COMFORT",
            isActive: String(Boolean(service.isActive))
        });
        setActiveTab("services");
    }

    function editUser(user) {
        setUserForm({
            id: user.id,
            firstName: user.firstName || "",
            lastName: user.lastName || "",
            email: user.email || "",
            phoneNumber: user.phoneNumber || "",
            driverLicense: user.driverLicense || ""
        });
        setActiveTab("users");
    }

    function toggleListValue(setter, field, value) {
        setter((current) => {
            const list = current[field];
            const normalized = Number(value);
            return {
                ...current,
                [field]: list.includes(normalized)
                    ? list.filter((item) => item !== normalized)
                    : [...list, normalized]
            };
        });
    }

    function renderContent() {
        if (booting) {
            return html`<${LoadingScreen} />`;
        }

        if (!session) {
            if (entryMode === "auth") {
                return html`
                    <${AuthScreen}
                        mode=${authMode}
                        form=${authForm}
                        error=${authError}
                        busy=${busy}
                        onModeChange=${(mode) => {
                            setAuthMode(mode);
                            setAuthError("");
                        }}
                        onBack=${() => {
                            setEntryMode("guest");
                            setAuthError("");
                        }}
                        onFieldChange=${(field, value) => {
                            setAuthForm((current) => ({ ...current, [field]: value }));
                        }}
                        onLogin=${submitLogin}
                        onRegister=${submitRegister}
                    />
                `;
            }

            return html`
                <${GuestView}
                    cars=${guestPage.items}
                    services=${data.services}
                    selectedCar=${selectedCar}
                    filters=${guestFilters}
                    page=${guestPage}
                    onFilterChange=${(field, value) => {
                        setPages((current) => ({ ...current, guestCars: 1 }));
                        setGuestFilters((current) => ({ ...current, [field]: value }));
                    }}
                    onResetFilters=${() => {
                        setPages((current) => ({ ...current, guestCars: 1 }));
                        setGuestFilters({ query: "", brand: "", status: "AVAILABLE", priceBand: "" });
                    }}
                    onSelectCar=${openCarDetails}
                    onBook=${startBooking}
                    onOpenLogin=${() => {
                        setAuthMode("login");
                        setEntryMode("auth");
                    }}
                    onOpenRegister=${() => {
                        setAuthMode("register");
                        setEntryMode("auth");
                    }}
                    onPageChange=${(page) => setPages((current) => ({ ...current, guestCars: page }))}
                />
            `;
        }

        return html`
            <${DashboardShell}
                session=${session}
                busy=${busy}
                tabs=${currentTabs}
                activeTab=${activeTab}
                onTabChange=${setActiveTab}
                onRefresh=${refreshEverything}
                onLogout=${submitLogout}
            >
                ${isAdmin
                    ? renderAdminDashboard()
                    : renderUserDashboard()}
            </${DashboardShell}>
        `;
    }

    function renderAdminDashboard() {
        if (activeTab === "overview") {
            return html`
                <section className="dashboard-stack">
                    <div className="stats-grid">
                        <${StatCard} label="Автомобилей" value=${overview.totalCars} tone="red" />
                        <${StatCard} label="Свободно" value=${overview.freeCars} tone="blue" />
                        <${StatCard} label="Активных поездок" value=${overview.activeRentals} tone="green" />
                        <${StatCard} label="Выручка" value=${formatCurrency(overview.revenue)} tone="gold" />
                    </div>
                    <div className="split-board">
                        <${Panel}
                            kicker="Последние аренды"
                            title="Ситуация по поездкам"
                            text="Здесь удобно отслеживать активные и завершённые поездки."
                        >
                            ${rentalPage.items.length
                                ? html`<div className="stack-list">${rentalPage.items.slice(0, 4).map((rental) => html`
                                    <${MiniRow}
                                        key=${rental.id}
                                        title=${`#${rental.id} · ${rental.carInfo}`}
                                        subtitle=${rental.userFullName}
                                        meta=${rental.status}
                                    />
                                `)}</div>`
                                : html`<${EmptyState} text="Пока нет аренд." />`}
                        </${Panel}>
                        <${Panel}
                            kicker="Дополнительные опции"
                            title="Набор услуг"
                            text="Связи машин и опций проверяются прямо через карточки и формы."
                        >
                            ${data.services.length
                                ? html`<div className="chip-cloud">${data.services.slice(0, 10).map((item) => html`<span key=${item.id} className="service-chip">${item.name}</span>`)}</div>`
                                : html`<${EmptyState} text="Опции ещё не добавлены." />`}
                        </${Panel}>
                    </div>
                </section>
            `;
        }

        if (activeTab === "cars") {
            return html`
                <section className="workspace-grid">
                    <${Panel}
                        kicker="Управление"
                        title=${carForm.id ? "Редактирование машины" : "Новая машина"}
                        text="Добавляйте машины, обновляйте цену и назначайте доступные опции."
                    >
                        <form className="form-stack" onSubmit=${submitCar}>
                            <div className="form-grid two">
                                <${InputField} label="Марка" value=${carForm.brand} onChange=${(value) => setCarForm((current) => ({ ...current, brand: value }))} />
                                <${InputField} label="Модель" value=${carForm.model} onChange=${(value) => setCarForm((current) => ({ ...current, model: value }))} />
                            </div>
                            <div className="form-grid two">
                                <${InputField} label="Номер" value=${carForm.licensePlate} onChange=${(value) => setCarForm((current) => ({ ...current, licensePlate: value }))} />
                                <${InputField} label="Год" type="number" value=${carForm.year} onChange=${(value) => setCarForm((current) => ({ ...current, year: value }))} />
                            </div>
                            <${InputField} label="Цена за час" type="number" step="0.1" value=${carForm.pricePerHour} onChange=${(value) => setCarForm((current) => ({ ...current, pricePerHour: value }))} />
                            <div className="field-block">
                                <span className="field-label">Опции на машине</span>
                                <div className="chips-select">
                                    ${data.services.filter((item) => item.isActive).map((service) => html`
                                        <button
                                            key=${service.id}
                                            className=${`chip-toggle ${carForm.serviceIds.includes(Number(service.id)) ? "active" : ""}`}
                                            type="button"
                                            onClick=${() => toggleListValue(setCarForm, "serviceIds", service.id)}
                                        >
                                            ${service.name}
                                        </button>
                                    `)}
                                </div>
                            </div>
                            <div className="button-row">
                                <button className="primary-button" type="submit">${carForm.id ? "Сохранить" : "Добавить"}</button>
                                <button className="ghost-button" type="button" onClick=${() => setCarForm(emptyCarForm())}>Очистить</button>
                            </div>
                        </form>
                    </${Panel}>
                    <${Panel}
                        kicker="Каталог"
                        title="Машины в системе"
                        text="Поиск и фильтрация работают мгновенно, без перезагрузки списка."
                    >
                        <${CarFilters}
                            filters=${catalogFilters}
                            onChange=${(field, value) => {
                                setPages((current) => ({ ...current, catalogCars: 1 }));
                                setCatalogFilters((current) => ({ ...current, [field]: value }));
                            }}
                            onReset=${() => {
                                setPages((current) => ({ ...current, catalogCars: 1 }));
                                setCatalogFilters({ query: "", brand: "", status: "", priceBand: "" });
                            }}
                        />
                        <div className="card-grid">
                            ${catalogPage.items.map((car) => html`
                                <${CarCard}
                                    key=${car.id}
                                    car=${car}
                                    compact=${true}
                                    onDetails=${() => setSelectedEntity({ type: "car", id: car.id })}
                                    onPrimary=${() => editCar(car)}
                                    primaryLabel="Изменить"
                                    onSecondary=${() => deleteEntity("car", car.id)}
                                    secondaryLabel="Удалить"
                                />
                            `)}
                        </div>
                        <${Pagination}
                            page=${catalogPage.page}
                            totalPages=${catalogPage.totalPages}
                            onChange=${(page) => setPages((current) => ({ ...current, catalogCars: page }))}
                        />
                    </${Panel}>
                </section>
            `;
        }

        if (activeTab === "rentals") {
            return html`
                <section className="workspace-grid">
                    <${Panel}
                        kicker="Новая аренда"
                        title="Оформление поездки"
                        text="Связь клиента, автомобиля и выбранных опций собирается в одном месте."
                    >
                        <form className="form-stack" onSubmit=${submitAdminRental}>
                            <${SelectField}
                                label="Клиент"
                                value=${adminRentalForm.userId}
                                options=${[{ value: "", label: "Выберите клиента" }].concat(data.users.filter((item) => item.status === "ACTIVE").map((item) => ({ value: item.id, label: `${item.firstName} ${item.lastName}` })))}
                                onChange=${(value) => setAdminRentalForm((current) => ({ ...current, userId: value }))}
                            />
                            <${SelectField}
                                label="Машина"
                                value=${adminRentalForm.carId}
                                options=${[{ value: "", label: "Выберите машину" }].concat(data.cars.filter((item) => item.status === "AVAILABLE" || Number(item.id) === Number(adminRentalForm.carId)).map((item) => ({ value: item.id, label: `${item.brand} ${item.model}` })))}
                                onChange=${(value) => setAdminRentalForm((current) => ({ ...current, carId: value }))}
                            />
                            <div className="field-block">
                                <span className="field-label">Дополнительные опции</span>
                                <div className="chips-select">
                                    ${data.services.filter((item) => item.isActive).map((service) => html`
                                        <button
                                            key=${service.id}
                                            className=${`chip-toggle ${adminRentalForm.serviceIds.includes(Number(service.id)) ? "active" : ""}`}
                                            type="button"
                                            onClick=${() => toggleListValue(setAdminRentalForm, "serviceIds", service.id)}
                                        >
                                            ${service.name}
                                        </button>
                                    `)}
                                </div>
                            </div>
                            <div className="button-row">
                                <button className="primary-button" type="submit">Создать аренду</button>
                                <button className="ghost-button" type="button" onClick=${() => setAdminRentalForm(emptyRentalForm())}>Очистить</button>
                            </div>
                        </form>
                    </${Panel}>
                    <${Panel}
                        kicker="История"
                        title="Все аренды"
                        text="Фильтруйте поездки по статусу и тексту поиска."
                    >
                        <${SimpleFilters}
                            filters=${rentalFilters}
                            statusOptions=${RENTAL_STATUSES}
                            onChange=${(field, value) => {
                                setPages((current) => ({ ...current, rentals: 1 }));
                                setRentalFilters((current) => ({ ...current, [field]: value }));
                            }}
                            onReset=${() => {
                                setPages((current) => ({ ...current, rentals: 1 }));
                                setRentalFilters({ query: "", status: "" });
                            }}
                        />
                        <div className="stack-list">
                            ${rentalPage.items.map((rental) => html`
                                <${EntityCard}
                                    key=${rental.id}
                                    title=${rental.carInfo}
                                    subtitle=${rental.userFullName}
                                    status=${rental.status}
                                    accent=${formatDate(rental.startTime)}
                                    chips=${rental.selectedServices || []}
                                    onDetails=${() => setSelectedEntity({ type: "rental", id: rental.id })}
                                    primaryLabel=${rental.status === "ACTIVE" ? "Завершить" : ""}
                                    onPrimary=${rental.status === "ACTIVE" ? () => completeRental(rental.id) : null}
                                    secondaryLabel="Удалить"
                                    onSecondary=${() => deleteEntity("rental", rental.id)}
                                />
                            `)}
                        </div>
                        <${Pagination}
                            page=${rentalPage.page}
                            totalPages=${rentalPage.totalPages}
                            onChange=${(page) => setPages((current) => ({ ...current, rentals: page }))}
                        />
                    </${Panel}>
                </section>
            `;
        }

        if (activeTab === "users") {
            return html`
                <section className="workspace-grid">
                    <${Panel}
                        kicker="Клиент"
                        title=${userForm.id ? "Редактирование клиента" : "Новый клиент"}
                        text="Новый клиент создаётся с системным паролем по умолчанию."
                    >
                        <form className="form-stack" onSubmit=${submitUser}>
                            <div className="form-grid two">
                                <${InputField} label="Имя" value=${userForm.firstName} onChange=${(value) => setUserForm((current) => ({ ...current, firstName: value }))} />
                                <${InputField} label="Фамилия" value=${userForm.lastName} onChange=${(value) => setUserForm((current) => ({ ...current, lastName: value }))} />
                            </div>
                            <${InputField} label="Email" type="email" value=${userForm.email} onChange=${(value) => setUserForm((current) => ({ ...current, email: value }))} />
                            <div className="form-grid two">
                                <${InputField} label="Телефон" value=${userForm.phoneNumber} onChange=${(value) => setUserForm((current) => ({ ...current, phoneNumber: value }))} />
                                <${InputField} label="Водительское удостоверение" value=${userForm.driverLicense} onChange=${(value) => setUserForm((current) => ({ ...current, driverLicense: value }))} />
                            </div>
                            <div className="button-row">
                                <button className="primary-button" type="submit">${userForm.id ? "Сохранить" : "Создать"}</button>
                                <button className="ghost-button" type="button" onClick=${() => setUserForm(emptyUserForm())}>Очистить</button>
                            </div>
                        </form>
                    </${Panel}>
                    <${Panel}
                        kicker="База"
                        title="Все клиенты"
                        text="Статусы и контактные данные доступны прямо из списка."
                    >
                        <${SimpleFilters}
                            filters=${userFilters}
                            statusOptions=${USER_STATUSES}
                            onChange=${(field, value) => {
                                setPages((current) => ({ ...current, users: 1 }));
                                setUserFilters((current) => ({ ...current, [field]: value }));
                            }}
                            onReset=${() => {
                                setPages((current) => ({ ...current, users: 1 }));
                                setUserFilters({ query: "", status: "" });
                            }}
                        />
                        <div className="stack-list">
                            ${userPage.items.map((user) => html`
                                <${EntityCard}
                                    key=${user.id}
                                    title=${`${user.firstName} ${user.lastName}`}
                                    subtitle=${user.email}
                                    status=${user.status}
                                    accent=${user.phoneNumber || "Без телефона"}
                                    chips=${[user.driverLicense]}
                                    onDetails=${() => setSelectedEntity({ type: "user", id: user.id })}
                                    primaryLabel="Изменить"
                                    onPrimary=${() => editUser(user)}
                                    secondaryLabel="Удалить"
                                    onSecondary=${() => deleteEntity("user", user.id)}
                                >
                                    <div className="inline-actions">
                                        ${USER_STATUSES.filter((status) => status !== user.status).map((status) => html`
                                            <button
                                                key=${status}
                                                className="mini-link"
                                                type="button"
                                                onClick=${() => changeUserStatus(user.id, status)}
                                            >
                                                ${status}
                                            </button>
                                        `)}
                                    </div>
                                </${EntityCard}>
                            `)}
                        </div>
                        <${Pagination}
                            page=${userPage.page}
                            totalPages=${userPage.totalPages}
                            onChange=${(page) => setPages((current) => ({ ...current, users: page }))}
                        />
                    </${Panel}>
                </section>
            `;
        }

        if (activeTab === "services") {
            return html`
                <section className="workspace-grid">
                    <${Panel}
                        kicker="Опция"
                        title=${serviceForm.id ? "Редактирование опции" : "Новая опция"}
                        text="Активные опции попадают в автопарк и используются в арендах."
                    >
                        <form className="form-stack" onSubmit=${submitService}>
                            <${InputField} label="Название" value=${serviceForm.name} onChange=${(value) => setServiceForm((current) => ({ ...current, name: value }))} />
                            <${TextAreaField} label="Описание" value=${serviceForm.description} onChange=${(value) => setServiceForm((current) => ({ ...current, description: value }))} />
                            <div className="form-grid two">
                                <${InputField} label="Цена в день" type="number" step="0.1" value=${serviceForm.pricePerDay} onChange=${(value) => setServiceForm((current) => ({ ...current, pricePerDay: value }))} />
                                <${SelectField}
                                    label="Категория"
                                    value=${serviceForm.category}
                                    options=${SERVICE_CATEGORIES.map((item) => ({ value: item, label: item }))}
                                    onChange=${(value) => setServiceForm((current) => ({ ...current, category: value }))}
                                />
                            </div>
                            <${SelectField}
                                label="Статус"
                                value=${serviceForm.isActive}
                                options=${[{ value: "true", label: "Активна" }, { value: "false", label: "Скрыта" }]}
                                onChange=${(value) => setServiceForm((current) => ({ ...current, isActive: value }))}
                            />
                            <div className="button-row">
                                <button className="primary-button" type="submit">${serviceForm.id ? "Сохранить" : "Добавить"}</button>
                                <button className="ghost-button" type="button" onClick=${() => setServiceForm(emptyServiceForm())}>Очистить</button>
                            </div>
                        </form>
                    </${Panel}>
                    <${Panel}
                        kicker="Каталог"
                        title="Все опции"
                        text="Фильтруйте по тексту, категории и активности."
                    >
                        <${ServiceFilters}
                            filters=${serviceFilters}
                            onChange=${(field, value) => {
                                setPages((current) => ({ ...current, services: 1 }));
                                setServiceFilters((current) => ({ ...current, [field]: value }));
                            }}
                            onReset=${() => {
                                setPages((current) => ({ ...current, services: 1 }));
                                setServiceFilters({ query: "", category: "", isActive: "" });
                            }}
                        />
                        <div className="stack-list">
                            ${servicePage.items.map((service) => html`
                                <${EntityCard}
                                    key=${service.id}
                                    title=${service.name}
                                    subtitle=${service.category}
                                    status=${service.isActive ? "ACTIVE" : "INACTIVE"}
                                    accent=${formatCurrency(service.pricePerDay)}
                                    chips=${service.description ? [service.description] : []}
                                    onDetails=${() => setSelectedEntity({ type: "service", id: service.id })}
                                    primaryLabel="Изменить"
                                    onPrimary=${() => editService(service)}
                                    secondaryLabel="Удалить"
                                    onSecondary=${() => deleteEntity("service", service.id)}
                                />
                            `)}
                        </div>
                        <${Pagination}
                            page=${servicePage.page}
                            totalPages=${servicePage.totalPages}
                            onChange=${(page) => setPages((current) => ({ ...current, services: page }))}
                        />
                    </${Panel}>
                </section>
            `;
        }

        return html`
            <section className="dashboard-stack">
                <${Panel}
                    kicker="Финансы"
                    title="Все платежи"
                    text="Просматривайте операции, запускайте проверку и делайте возвраты."
                >
                    <${SimpleFilters}
                        filters=${paymentFilters}
                        statusOptions=${PAYMENT_STATUSES}
                        onChange=${(field, value) => {
                            setPages((current) => ({ ...current, payments: 1 }));
                            setPaymentFilters((current) => ({ ...current, [field]: value }));
                        }}
                        onReset=${() => {
                            setPages((current) => ({ ...current, payments: 1 }));
                            setPaymentFilters({ query: "", status: "" });
                        }}
                    />
                    <div className="stack-list">
                        ${paymentPage.items.map((payment) => html`
                            <${EntityCard}
                                key=${payment.id}
                                title=${`Платёж #${payment.id}`}
                                subtitle=${`Аренда #${payment.rentalId}`}
                                status=${payment.status}
                                accent=${formatCurrency(payment.amount)}
                                chips=${[
                                    `Авто: ${formatCurrency(payment.carAmount)}`,
                                    `Опции: ${formatCurrency(payment.servicesAmount)}`
                                ]}
                                onDetails=${() => setSelectedEntity({ type: "payment", id: payment.id })}
                                primaryLabel=${payment.status === "COMPLETED" ? "Возврат" : "Проверить"}
                                onPrimary=${payment.status === "COMPLETED" ? () => refundPayment(payment.id) : () => verifyPayment(payment.id)}
                                secondaryLabel="Удалить"
                                onSecondary=${() => deleteEntity("payment", payment.id)}
                            />
                        `)}
                    </div>
                    <${Pagination}
                        page=${paymentPage.page}
                        totalPages=${paymentPage.totalPages}
                        onChange=${(page) => setPages((current) => ({ ...current, payments: page }))}
                    />
                </${Panel}>
            </section>
        `;
    }

    function renderUserDashboard() {
        if (activeTab === "catalog") {
            return html`
                <section className="catalog-layout">
                    <div className="catalog-main">
                        <${Panel}
                            kicker="Каталог"
                            title="Выберите автомобиль"
                            text="Для поездки доступны только открытые к бронированию автомобили."
                        >
                            <${CarFilters}
                                filters=${catalogFilters}
                                onChange=${(field, value) => {
                                    setPages((current) => ({ ...current, catalogCars: 1 }));
                                    setCatalogFilters((current) => ({ ...current, [field]: value }));
                                }}
                                onReset=${() => {
                                    setPages((current) => ({ ...current, catalogCars: 1 }));
                                setCatalogFilters({ query: "", brand: "", status: "", priceBand: "" });
                                }}
                            />
                            <div className="card-grid">
                                ${catalogPage.items.map((car) => html`
                                    <${CarCard}
                                        key=${car.id}
                                        car=${car}
                                        compact=${false}
                                        onDetails=${() => openCarDetails(car.id)}
                                        onPrimary=${() => startBooking(car)}
                                        primaryLabel="Арендовать"
                                    />
                                `)}
                            </div>
                            <${Pagination}
                                page=${catalogPage.page}
                                totalPages=${catalogPage.totalPages}
                                onChange=${(page) => setPages((current) => ({ ...current, catalogCars: page }))}
                            />
                        </${Panel}>
                    </div>
                    <div className="catalog-side">
                        <${Panel}
                            id="car-focus"
                            kicker="Выбранный автомобиль"
                            title=${selectedCar ? `${selectedCar.brand} ${selectedCar.model}` : "Выберите машину"}
                            text=${selectedCar ? selectedCar.licensePlate : "Нажмите «Подробнее», чтобы посмотреть характеристики и доступные опции."}
                        >
                            ${selectedCar
                                ? html`
                                    <img className="showcase-image" src=${carArt(selectedCar)} alt=${`${selectedCar.brand} ${selectedCar.model}`} />
                                    <div className="detail-list">
                                        <${DetailLine} label="Статус" value=${selectedCar.status} />
                                        <${DetailLine} label="Год" value=${String(selectedCar.year)} />
                                        <${DetailLine} label="Цена" value=${`${formatCurrency(selectedCar.pricePerHour)} / час`} />
                                    </div>
                                    <div className="chip-cloud">
                                        ${(selectedCar.availableServices || []).length
                                            ? selectedCar.availableServices.map((item) => html`<span key=${item.id} className="service-chip">${item.name}</span>`)
                                            : html`<span className="service-chip muted">Без дополнительных опций</span>`}
                                    </div>
                                `
                                : html`<${EmptyState} text="Каталог пуст." />`}
                        </${Panel}>
                        <${Panel}
                            id="booking-panel"
                            kicker="Оформление"
                            title=${currentUserActiveRental ? "У вас уже есть активная поездка" : "Новая поездка"}
                            text=${currentUserActiveRental
                                ? "Завершите текущую аренду, чтобы оформить следующую."
                                : "После подтверждения поездка сразу появится в истории."}
                        >
                            ${currentUserActiveRental
                                ? html`
                                    <${EntityCard}
                                        title=${currentUserActiveRental.carInfo}
                                        subtitle=${formatDate(currentUserActiveRental.startTime)}
                                        status=${currentUserActiveRental.status}
                                        accent="Активна"
                                        chips=${currentUserActiveRental.selectedServices || []}
                                        primaryLabel="Завершить аренду"
                                        onPrimary=${() => completeRental(currentUserActiveRental.id)}
                                    />
                                `
                                : html`
                                    <form className="form-stack" onSubmit=${submitUserRental}>
                                        <${SelectField}
                                            label="Машина"
                                            value=${userRentalForm.carId}
                                            options=${[{ value: "", label: "Выберите машину" }].concat(data.cars.filter((item) => item.status === "AVAILABLE" || Number(item.id) === Number(userRentalForm.carId)).map((item) => ({ value: item.id, label: `${item.brand} ${item.model}` })))}
                                            onChange=${(value) => {
                                                const nextCar = data.cars.find((item) => String(item.id) === String(value));
                                                setBookingCarId(value);
                                                setUserRentalForm((current) => ({
                                                    ...current,
                                                    carId: value,
                                                    serviceIds: nextCar ? (nextCar.availableServices || []).map((item) => Number(item.id)) : []
                                                }));
                                            }}
                                        />
                                        <div className="field-block">
                                            <span className="field-label">Опции поездки</span>
                                            <div className="chips-select">
                                                ${availableServicesForCar(userRentalForm.carId, data.cars).map((service) => html`
                                                    <button
                                                        key=${service.id}
                                                        className=${`chip-toggle ${userRentalForm.serviceIds.includes(Number(service.id)) ? "active" : ""}`}
                                                        type="button"
                                                        onClick=${() => toggleListValue(setUserRentalForm, "serviceIds", service.id)}
                                                    >
                                                        ${service.name}
                                                    </button>
                                                `)}
                                            </div>
                                        </div>
                                        <button className="primary-button" type="submit">Подтвердить аренду</button>
                                    </form>
                                `}
                        </${Panel}>
                    </div>
                </section>
            `;
        }

        if (activeTab === "rentals") {
            return html`
                <section className="dashboard-stack">
                    <${Panel}
                        kicker="История"
                        title="Мои поездки"
                        text="Все поездки в одном списке: активные, завершённые и связанные с ними опции."
                    >
                        <${SimpleFilters}
                            filters=${rentalFilters}
                            statusOptions=${RENTAL_STATUSES}
                            onChange=${(field, value) => {
                                setPages((current) => ({ ...current, rentals: 1 }));
                                setRentalFilters((current) => ({ ...current, [field]: value }));
                            }}
                            onReset=${() => {
                                setPages((current) => ({ ...current, rentals: 1 }));
                                setRentalFilters({ query: "", status: "" });
                            }}
                        />
                        <div className="stack-list">
                            ${rentalPage.items.map((rental) => html`
                                <${EntityCard}
                                    key=${rental.id}
                                    title=${rental.carInfo}
                                    subtitle=${formatDate(rental.startTime)}
                                    status=${rental.status}
                                    accent=${rental.endTime ? formatDate(rental.endTime) : "В пути"}
                                    chips=${rental.selectedServices || []}
                                    onDetails=${() => setSelectedEntity({ type: "rental", id: rental.id })}
                                    primaryLabel=${rental.status === "ACTIVE" ? "Завершить аренду" : ""}
                                    onPrimary=${rental.status === "ACTIVE" ? () => completeRental(rental.id) : null}
                                />
                            `)}
                        </div>
                        <${Pagination}
                            page=${rentalPage.page}
                            totalPages=${rentalPage.totalPages}
                            onChange=${(page) => setPages((current) => ({ ...current, rentals: page }))}
                        />
                    </${Panel}>
                </section>
            `;
        }

        if (activeTab === "payments") {
            return html`
                <section className="dashboard-stack">
                    <${Panel}
                        kicker="Платежи"
                        title="История оплат"
                        text="Список автоматически ограничен только вашими операциями."
                    >
                        <${SimpleFilters}
                            filters=${paymentFilters}
                            statusOptions=${PAYMENT_STATUSES}
                            onChange=${(field, value) => {
                                setPages((current) => ({ ...current, payments: 1 }));
                                setPaymentFilters((current) => ({ ...current, [field]: value }));
                            }}
                            onReset=${() => {
                                setPages((current) => ({ ...current, payments: 1 }));
                                setPaymentFilters({ query: "", status: "" });
                            }}
                        />
                        <div className="stack-list">
                            ${paymentPage.items.map((payment) => html`
                                <${EntityCard}
                                    key=${payment.id}
                                    title=${`Платёж #${payment.id}`}
                                    subtitle=${formatDate(payment.paymentDate)}
                                    status=${payment.status}
                                    accent=${formatCurrency(payment.amount)}
                                    chips=${[
                                        `Авто: ${formatCurrency(payment.carAmount)}`,
                                        `Опции: ${formatCurrency(payment.servicesAmount)}`
                                    ]}
                                    onDetails=${() => setSelectedEntity({ type: "payment", id: payment.id })}
                                />
                            `)}
                        </div>
                        <${Pagination}
                            page=${paymentPage.page}
                            totalPages=${paymentPage.totalPages}
                            onChange=${(page) => setPages((current) => ({ ...current, payments: page }))}
                        />
                    </${Panel}>
                </section>
            `;
        }

        return html`
            <section className="dashboard-stack">
                <${Panel}
                    kicker="Профиль"
                    title=${data.profile ? `${data.profile.firstName} ${data.profile.lastName}` : "Личный кабинет"}
                    text="Контактные данные и статус аккаунта."
                >
                    ${data.profile
                        ? html`
                            <div className="profile-grid">
                                <${DetailLine} label="Email" value=${data.profile.email} />
                                <${DetailLine} label="Телефон" value=${data.profile.phoneNumber || "Не указан"} />
                                <${DetailLine} label="Права" value=${data.profile.driverLicense || "Не указаны"} />
                                <${DetailLine} label="Статус" value=${data.profile.status} />
                            </div>
                        `
                        : html`<${EmptyState} text="Профиль пока недоступен." />`}
                </${Panel}>
            </section>
        `;
    }

    return html`
        <div className="app-shell">
            ${renderContent()}
            ${toast ? html`<div className="toast">${toast}</div>` : null}
            ${selectedCardData && session ? html`<${FloatingDetail} entity=${selectedCardData} onClose=${() => setSelectedEntity(null)} />` : null}
        </div>
    `;
}

function LoadingScreen() {
    return html`
        <main className="loading-screen">
            <div className="loading-card">
                <span className="eyebrow">DriveFlow</span>
                <h1>Загружаем интерфейс</h1>
                <p>Подключаем каталог, поездки и рабочие разделы.</p>
            </div>
        </main>
    `;
}

function GuestView(props) {
    const {
        cars,
        services,
        selectedCar,
        filters,
        page,
        onFilterChange,
        onResetFilters,
        onSelectCar,
        onBook,
        onOpenLogin,
        onOpenRegister,
        onPageChange
    } = props;

    return html`
        <main className="guest-page">
            <section className="hero-block">
                <div className="hero-copy">
                    <span className="eyebrow">Городской каршеринг</span>
                    <h1>Автомобиль на час, день или весь уикенд</h1>
                    <p>
                        Витрина открыта сразу: можно сравнить машины, посмотреть комплектацию
                        и подобрать удобный вариант до входа в аккаунт.
                    </p>
                    <div className="hero-actions">
                        <button className="primary-button" onClick=${onOpenLogin}>Войти</button>
                        <button className="secondary-button" onClick=${onOpenRegister}>Регистрация</button>
                    </div>
                    <div className="hero-tags">
                        <span>Без офиса</span>
                        <span>По минутам и часам</span>
                        <span>Опции уже на машине</span>
                    </div>
                </div>
                <div className="hero-stage">
                    ${selectedCar
                        ? html`
                            <div className="hero-car-card">
                                <img className="showcase-image" src=${carArt(selectedCar)} alt=${`${selectedCar.brand} ${selectedCar.model}`} />
                                <div className="hero-car-meta">
                                    <strong>${selectedCar.brand} ${selectedCar.model}</strong>
                                    <span>${selectedCar.licensePlate}</span>
                                    <span>${formatCurrency(selectedCar.pricePerHour)} / час</span>
                                </div>
                            </div>
                        `
                        : html`
                            <div className="hero-placeholder">
                                <strong>Каталог загружается</strong>
                                <span>Выберите машину, и она появится здесь.</span>
                            </div>
                        `}
                </div>
            </section>

            <section className="surface-panel">
                <div className="surface-head">
                    <div>
                        <span className="eyebrow">Каталог</span>
                        <h2>Доступные автомобили</h2>
                    </div>
                    <div className="stats-inline">
                        <span>${cars.length} машин на странице</span>
                        <span>${services.length} доступных опций</span>
                    </div>
                </div>

                <div className="filter-row">
                    <${InputField} label="Поиск" value=${filters.query} onChange=${(value) => onFilterChange("query", value)} />
                    <${InputField} label="Марка" value=${filters.brand} onChange=${(value) => onFilterChange("brand", value)} />
                    <${SelectField}
                        label="Статус"
                        value=${filters.status}
                        options=${[{ value: "", label: "Все" }].concat(CAR_STATUSES.map((item) => ({ value: item, label: item })))}
                        onChange=${(value) => onFilterChange("status", value)}
                    />
                    <button className="ghost-button wide" type="button" onClick=${onResetFilters}>Сбросить</button>
                </div>

                <div className="card-grid spacious">
                    ${cars.map((car) => html`
                        <${CarCard}
                            key=${car.id}
                            car=${car}
                            onDetails=${() => onSelectCar(car.id)}
                            onPrimary=${() => onBook(car)}
                            primaryLabel="Выбрать"
                        />
                    `)}
                </div>

                <${Pagination}
                    page=${page.page}
                    totalPages=${page.totalPages}
                    onChange=${onPageChange}
                />
            </section>

            <section className="surface-panel detail-surface">
                <div className="surface-head">
                    <div>
                        <span className="eyebrow">Подробнее</span>
                        <h2>${selectedCar ? `${selectedCar.brand} ${selectedCar.model}` : "Выберите автомобиль"}</h2>
                    </div>
                </div>
                ${selectedCar
                    ? html`
                        <div className="detail-layout">
                            <div className="detail-gallery">
                                <img className="showcase-image large" src=${carArt(selectedCar)} alt=${`${selectedCar.brand} ${selectedCar.model}`} />
                            </div>
                            <div className="detail-copy">
                                <div className="detail-list">
                                    <${DetailLine} label="Номер" value=${selectedCar.licensePlate} />
                                    <${DetailLine} label="Год" value=${String(selectedCar.year)} />
                                        <${DetailLine} label="Статус" value=${displayStatus(selectedCar.status)} />
                                    <${DetailLine} label="Цена" value=${`${formatCurrency(selectedCar.pricePerHour)} / час`} />
                                </div>
                                <div className="feature-block">
                                    <span className="field-label">Опции на автомобиле</span>
                                    <div className="chip-cloud">
                                        ${(selectedCar.availableServices || []).length
                                            ? selectedCar.availableServices.map((item) => html`<span key=${item.id} className="service-chip">${item.name}</span>`)
                                            : html`<span className="service-chip muted">Без дополнительных опций</span>`}
                                    </div>
                                </div>
                                <div className="button-row">
                                    <button className="primary-button" onClick=${() => onBook(selectedCar)}>Оформить после входа</button>
                                    <button className="secondary-button" onClick=${onOpenRegister}>Создать аккаунт</button>
                                </div>
                            </div>
                        </div>
                    `
                    : html`<${EmptyState} text="Нажмите «Подробнее» на карточке машины." />`}
            </section>
        </main>
    `;
}

function AuthScreen(props) {
    const { mode, form, error, busy, onModeChange, onBack, onFieldChange, onLogin, onRegister } = props;
    const isRegister = mode === "register";
    return html`
        <main className="auth-page">
            <section className="auth-stage">
                <div className="auth-brand">
                    <span className="eyebrow">DriveFlow</span>
                    <h1>${isRegister ? "Создайте аккаунт" : "Добро пожаловать"}</h1>
                    <p>
                        Войдите, чтобы бронировать машины, смотреть историю поездок
                        и управлять своими платежами.
                    </p>
                    <button className="ghost-button" onClick=${onBack}>Вернуться к просмотру</button>
                </div>
                <div className="auth-card">
                    <div className="toggle-row">
                        <button className=${`toggle-button ${!isRegister ? "active" : ""}`} onClick=${() => onModeChange("login")}>Вход</button>
                        <button className=${`toggle-button ${isRegister ? "active" : ""}`} onClick=${() => onModeChange("register")}>Регистрация</button>
                    </div>
                    ${error ? html`<div className="error-box">${error}</div>` : null}
                    ${isRegister
                        ? html`
                            <form className="form-stack" onSubmit=${onRegister}>
                                <div className="form-grid two">
                                    <${InputField} label="Имя" value=${form.firstName} onChange=${(value) => onFieldChange("firstName", value)} />
                                    <${InputField} label="Фамилия" value=${form.lastName} onChange=${(value) => onFieldChange("lastName", value)} />
                                </div>
                                <${InputField} label="Email" type="email" value=${form.email} onChange=${(value) => onFieldChange("email", value)} />
                                <div className="form-grid two">
                                    <${InputField} label="Телефон" value=${form.phoneNumber} onChange=${(value) => onFieldChange("phoneNumber", value)} hint="Формат: +375291112233" />
                                    <${InputField} label="Права" value=${form.driverLicense} onChange=${(value) => onFieldChange("driverLicense", value)} />
                                </div>
                                <${InputField} label="Пароль" type="password" value=${form.password} onChange=${(value) => onFieldChange("password", value)} hint="Минимум 6 символов" />
                                <button className="primary-button wide" type="submit" disabled=${busy}>Создать аккаунт</button>
                            </form>
                        `
                        : html`
                            <form className="form-stack" onSubmit=${onLogin}>
                                <${InputField} label="Email или логин" value=${form.login} onChange=${(value) => onFieldChange("login", value)} />
                                <${InputField} label="Пароль" type="password" value=${form.password} onChange=${(value) => onFieldChange("password", value)} />
                                <button className="primary-button wide" type="submit" disabled=${busy}>Войти</button>
                            </form>
                        `}
                </div>
            </section>
        </main>
    `;
}

function DashboardShell(props) {
    const { session, busy, tabs, activeTab, onTabChange, onRefresh, onLogout, children } = props;
    return html`
        <main className="dashboard-page">
            <header className="topbar">
                <div className="brand-block">
                    <span className="brand-mark">DF</span>
                    <div>
                        <strong>DriveFlow</strong>
                        <span>${session.role === "admin" ? "Панель управления" : "Личный кабинет"}</span>
                    </div>
                </div>
                <nav className="tab-row">
                    ${tabs.map((tab) => html`
                        <button
                            key=${tab.id}
                            className=${`tab-pill ${tab.id === activeTab ? "active" : ""}`}
                            onClick=${() => onTabChange(tab.id)}
                        >
                            ${tab.label}
                        </button>
                    `)}
                </nav>
                <div className="topbar-actions">
                    <button className="ghost-button" onClick=${onRefresh} disabled=${busy}>Обновить</button>
                    <button className="primary-button" onClick=${onLogout} disabled=${busy}>Выйти</button>
                </div>
            </header>
            <section className="dashboard-content">${children}</section>
        </main>
    `;
}

function Panel(props) {
    return html`
        <section id=${props.id || null} className="panel-card">
            <div className="panel-head">
                <div>
                    <span className="eyebrow">${props.kicker}</span>
                    <h2>${props.title}</h2>
                    ${props.text ? html`<p className="panel-text">${props.text}</p>` : null}
                </div>
            </div>
            <div className="panel-body">${props.children}</div>
        </section>
    `;
}

function StatCard(props) {
    return html`
        <article className=${`stat-card tone-${props.tone || "red"}`}>
            <span>${props.label}</span>
            <strong>${props.value}</strong>
        </article>
    `;
}

function CarCard(props) {
    const { car, onDetails, onPrimary, primaryLabel, onSecondary, secondaryLabel, compact } = props;
    return html`
        <article className=${`car-card ${compact ? "compact" : ""}`}>
            <img className="car-card-image" src=${carArt(car)} alt=${`${car.brand} ${car.model}`} />
            <div className="car-card-body">
                <div className="card-topline">
                <span className=${`status-badge ${statusTone(car.status)}`}>${displayStatus(car.status)}</span>
                    <span className="price-tag">${formatCurrency(car.pricePerHour)} / час</span>
                </div>
                <h3>${car.brand} ${car.model}</h3>
                <p>${car.licensePlate}</p>
                <div className="chip-cloud">
                    ${(car.availableServices || []).length
                        ? car.availableServices.slice(0, compact ? 3 : 5).map((item) => html`<span key=${item.id} className="service-chip">${item.name}</span>`)
                        : html`<span className="service-chip muted">Без опций</span>`}
                </div>
                <div className="button-row">
                    <button className="ghost-button" onClick=${onDetails}>Подробнее</button>
                    ${onPrimary ? html`<button className="primary-button" onClick=${onPrimary}>${primaryLabel}</button>` : null}
                    ${onSecondary ? html`<button className="secondary-button" onClick=${onSecondary}>${secondaryLabel}</button>` : null}
                </div>
            </div>
        </article>
    `;
}

function EntityCard(props) {
    return html`
        <article className="entity-card">
            <div className="card-topline">
                ${props.status ? html`<span className=${`status-badge ${statusTone(props.status)}`}>${displayStatus(props.status)}</span>` : null}
                ${props.accent ? html`<span className="meta-accent">${props.accent}</span>` : null}
            </div>
            <h3>${props.title}</h3>
            ${props.subtitle ? html`<p>${props.subtitle}</p>` : null}
            ${props.chips && props.chips.length
                ? html`<div className="chip-cloud">${props.chips.map((chipText, index) => html`<span key=${index} className="service-chip">${chipText}</span>`)}</div>`
                : null}
            ${props.children}
            ${(props.onDetails || props.onPrimary || props.onSecondary)
                ? html`
                    <div className="button-row">
                        ${props.onDetails ? html`<button className="ghost-button" onClick=${props.onDetails}>Подробнее</button>` : null}
                        ${props.onPrimary && props.primaryLabel ? html`<button className="primary-button" onClick=${props.onPrimary}>${props.primaryLabel}</button>` : null}
                        ${props.onSecondary && props.secondaryLabel ? html`<button className="secondary-button" onClick=${props.onSecondary}>${props.secondaryLabel}</button>` : null}
                    </div>
                `
                : null}
        </article>
    `;
}

function FloatingDetail(props) {
    const { entity, onClose } = props;
    return html`
        <aside className="floating-detail">
            <div className="floating-head">
                <div>
                    <span className="eyebrow">${entity.kicker}</span>
                    <h3>${entity.title}</h3>
                </div>
                <button className="icon-button" onClick=${onClose}>×</button>
            </div>
            <div className="detail-list">
                ${entity.lines.map((line, index) => html`<${DetailLine} key=${index} label=${line.label} value=${line.value} />`)}
            </div>
        </aside>
    `;
}

function MiniRow(props) {
    return html`
        <div className="mini-row">
            <div>
                <strong>${props.title}</strong>
                <span>${props.subtitle}</span>
            </div>
            <span className="mini-meta">${props.meta}</span>
        </div>
    `;
}

function DetailLine(props) {
    return html`
        <div className="detail-line">
            <span>${props.label}</span>
            <strong>${props.value}</strong>
        </div>
    `;
}

function EmptyState(props) {
    return html`<div className="empty-state">${props.text}</div>`;
}

function InputField(props) {
    return html`
        <label className="field-block">
            <span className="field-label">${props.label}</span>
            <input
                type=${props.type || "text"}
                step=${props.step || null}
                value=${props.value}
                placeholder=${props.placeholder || ""}
                onChange=${(event) => props.onChange(event.target.value)}
            />
            ${props.hint ? html`<small>${props.hint}</small>` : null}
        </label>
    `;
}

function TextAreaField(props) {
    return html`
        <label className="field-block">
            <span className="field-label">${props.label}</span>
            <textarea rows="4" value=${props.value} onChange=${(event) => props.onChange(event.target.value)} />
        </label>
    `;
}

function SelectField(props) {
    return html`
        <label className="field-block">
            <span className="field-label">${props.label}</span>
            <select value=${props.value} onChange=${(event) => props.onChange(event.target.value)}>
                ${props.options.map((option) => html`
                    <option key=${String(option.value)} value=${option.value}>${option.label}</option>
                `)}
            </select>
        </label>
    `;
}

function CarFilters(props) {
    return html`
        <div className="filter-row">
            <${InputField} label="Поиск по модели, номеру или опции" value=${props.filters.query} onChange=${(value) => props.onChange("query", value)} />
            <${InputField} label="Марка" value=${props.filters.brand} onChange=${(value) => props.onChange("brand", value)} />
            <${SelectField}
                label="Статус"
                value=${props.filters.status}
                options=${[{ value: "", label: "Все" }].concat(CAR_STATUSES.map((item) => ({ value: item, label: displayStatus(item) })))}
                onChange=${(value) => props.onChange("status", value)}
            />
            <${SelectField}
                label="Тариф"
                value=${props.filters.priceBand}
                options=${PRICE_BANDS.map((item) => ({ value: item, label: PRICE_BAND_LABELS[item] }))}
                onChange=${(value) => props.onChange("priceBand", value)}
            />
            <button className="ghost-button wide" type="button" onClick=${props.onReset}>Сбросить</button>
        </div>
    `;
}

function ServiceFilters(props) {
    return html`
        <div className="filter-row">
            <${InputField} label="Поиск" value=${props.filters.query} onChange=${(value) => props.onChange("query", value)} />
            <${SelectField}
                label="Категория"
                value=${props.filters.category}
                options=${[{ value: "", label: "Все" }].concat(SERVICE_CATEGORIES.map((item) => ({ value: item, label: item })))}
                onChange=${(value) => props.onChange("category", value)}
            />
            <${SelectField}
                label="Активность"
                value=${props.filters.isActive}
                options=${[
                    { value: "", label: "Все" },
                    { value: "true", label: "Активные" },
                    { value: "false", label: "Скрытые" }
                ]}
                onChange=${(value) => props.onChange("isActive", value)}
            />
            <button className="ghost-button wide" type="button" onClick=${props.onReset}>Сбросить</button>
        </div>
    `;
}

function SimpleFilters(props) {
    return html`
        <div className="filter-row">
            <${InputField} label="Поиск по названию или номеру" value=${props.filters.query} onChange=${(value) => props.onChange("query", value)} />
            <${SelectField}
                label="Статус"
                value=${props.filters.status}
                options=${[{ value: "", label: "Все" }].concat(props.statusOptions.map((item) => ({ value: item, label: displayStatus(item) })))}
                onChange=${(value) => props.onChange("status", value)}
            />
            <button className="ghost-button wide" type="button" onClick=${props.onReset}>Сбросить</button>
        </div>
    `;
}

function Pagination(props) {
    if (props.totalPages <= 1) {
        return null;
    }
    return html`
        <div className="pagination">
            <button className="ghost-button" disabled=${props.page <= 1} onClick=${() => props.onChange(props.page - 1)}>Назад</button>
            <span>Страница ${props.page} из ${props.totalPages}</span>
            <button className="ghost-button" disabled=${props.page >= props.totalPages} onClick=${() => props.onChange(props.page + 1)}>Вперёд</button>
        </div>
    `;
}

function emptyCarForm() {
    return {
        id: null,
        licensePlate: "",
        brand: "",
        model: "",
        year: "",
        pricePerHour: "",
        serviceIds: []
    };
}

function emptyServiceForm() {
    return {
        id: null,
        name: "",
        description: "",
        pricePerDay: "",
        category: "COMFORT",
        isActive: "true"
    };
}

function emptyUserForm() {
    return {
        id: null,
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
        driverLicense: ""
    };
}

function emptyRentalForm() {
    return {
        userId: "",
        carId: "",
        serviceIds: []
    };
}

function filterCars(items, filters) {
    const query = normalize(filters.query);
    const brand = normalize(filters.brand);
    return items.filter((item) => {
        if (item.status === "DELETED") {
            return false;
        }
        if (filters.status && item.status !== filters.status) {
            return false;
        }
        if (filters.priceBand) {
            const price = Number(item.pricePerHour || 0);
            if (filters.priceBand === "budget" && price > 15) {
                return false;
            }
            if (filters.priceBand === "standard" && (price < 15 || price > 25)) {
                return false;
            }
            if (filters.priceBand === "premium" && price < 25) {
                return false;
            }
        }
        if (brand && !normalize(item.brand).includes(brand)) {
            return false;
        }
        if (query) {
            const haystack = `${item.brand} ${item.model} ${item.licensePlate} ${(item.availableServices || []).map((service) => service.name).join(" ")}`;
            if (!normalize(haystack).includes(query)) {
                return false;
            }
        }
        return true;
    });
}

function filterRentals(items, filters) {
    const query = normalize(filters.query);
    return items.filter((item) => {
        if (filters.status && item.status !== filters.status) {
            return false;
        }
        if (!query) {
            return true;
        }
        return normalize(`${item.carInfo} ${item.userFullName} ${(item.selectedServices || []).join(" ")}`).includes(query);
    });
}

function filterUsers(items, filters) {
    const query = normalize(filters.query);
    return items.filter((item) => {
        if (filters.status && item.status !== filters.status) {
            return false;
        }
        if (!query) {
            return true;
        }
        return normalize(`${item.firstName} ${item.lastName} ${item.email} ${item.phoneNumber} ${item.driverLicense}`).includes(query);
    });
}

function filterServices(items, filters) {
    const query = normalize(filters.query);
    return items.filter((item) => {
        if (filters.category && item.category !== filters.category) {
            return false;
        }
        if (filters.isActive && String(Boolean(item.isActive)) !== filters.isActive) {
            return false;
        }
        if (!query) {
            return true;
        }
        return normalize(`${item.name} ${item.description} ${item.category}`).includes(query);
    });
}

function filterPayments(items, filters) {
    const query = normalize(filters.query);
    return items.filter((item) => {
        if (filters.status && item.status !== filters.status) {
            return false;
        }
        if (!query) {
            return true;
        }
        return normalize(`${item.id} ${item.transactionId} ${item.rentalId}`).includes(query);
    });
}

function paginate(items, page) {
    const safePage = Math.max(1, page || 1);
    const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
    const normalizedPage = Math.min(safePage, totalPages);
    const start = (normalizedPage - 1) * PAGE_SIZE;
    return {
        page: normalizedPage,
        totalPages,
        items: items.slice(start, start + PAGE_SIZE)
    };
}

function availableServicesForCar(carId, cars) {
    const car = cars.find((item) => String(item.id) === String(carId));
    return car ? car.availableServices || [] : [];
}

function resolveSelectedEntity(selected, data) {
    if (!selected) {
        return null;
    }
    if (selected.type === "car") {
        const car = data.cars.find((item) => Number(item.id) === Number(selected.id));
        if (!car) {
            return null;
        }
        return {
            kicker: "Автомобиль",
            title: `${car.brand} ${car.model}`,
            lines: [
                { label: "Номер", value: car.licensePlate },
                { label: "Статус", value: displayStatus(car.status) },
                { label: "Год", value: String(car.year) },
                { label: "Цена", value: `${formatCurrency(car.pricePerHour)} / час` },
                {
                    label: "Опции",
                    value: (car.availableServices || []).length
                        ? car.availableServices.map((item) => item.name).join(", ")
                        : "Без опций"
                }
            ]
        };
    }
    if (selected.type === "rental") {
        const rental = data.rentals.find((item) => Number(item.id) === Number(selected.id));
        if (!rental) {
            return null;
        }
        return {
            kicker: "Аренда",
            title: rental.carInfo,
            lines: [
                { label: "Клиент", value: rental.userFullName },
                { label: "Статус", value: displayStatus(rental.status) },
                { label: "Начало", value: formatDate(rental.startTime) },
                { label: "Завершение", value: rental.endTime ? formatDate(rental.endTime) : "В пути" },
                { label: "Опции", value: (rental.selectedServices || []).join(", ") || "Без опций" }
            ]
        };
    }
    if (selected.type === "user") {
        const user = data.users.find((item) => Number(item.id) === Number(selected.id));
        if (!user) {
            return null;
        }
        return {
            kicker: "Клиент",
            title: `${user.firstName} ${user.lastName}`,
            lines: [
                { label: "Email", value: user.email },
                { label: "Телефон", value: user.phoneNumber || "Не указан" },
                { label: "Права", value: user.driverLicense || "Не указаны" },
                { label: "Статус", value: displayStatus(user.status) },
                { label: "Роль", value: user.role || "USER" }
            ]
        };
    }
    if (selected.type === "service") {
        const service = data.services.find((item) => Number(item.id) === Number(selected.id));
        if (!service) {
            return null;
        }
        return {
            kicker: "Опция",
            title: service.name,
            lines: [
                { label: "Категория", value: service.category },
                { label: "Статус", value: displayStatus(service.isActive ? "ACTIVE" : "INACTIVE") },
                { label: "Цена", value: `${formatCurrency(service.pricePerDay)} / день` },
                { label: "Описание", value: service.description || "Без описания" }
            ]
        };
    }
    if (selected.type === "payment") {
        const payment = data.payments.find((item) => Number(item.id) === Number(selected.id));
        if (!payment) {
            return null;
        }
        return {
            kicker: "Платёж",
            title: `Платёж #${payment.id}`,
            lines: [
                { label: "Статус", value: displayStatus(payment.status) },
                { label: "Сумма", value: formatCurrency(payment.amount) },
                { label: "Авто", value: formatCurrency(payment.carAmount) },
                { label: "Опции", value: formatCurrency(payment.servicesAmount) },
                { label: "Дата", value: formatDate(payment.paymentDate) },
                { label: "Транзакция", value: payment.transactionId || "Нет данных" }
            ]
        };
    }
    return null;
}

function normalizeSession(response) {
    return {
        role: String(response.role || "").toLowerCase(),
        userId: response.userId,
        firstName: response.firstName || "",
        lastName: response.lastName || "",
        email: response.email || ""
    };
}

async function api(url, method, body) {
    const options = {
        method: method || "GET",
        credentials: "same-origin",
        headers: {}
    };
    if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    if (!response.ok) {
        const text = await response.text();
        throw new Error(parseApiError(text, response.status));
    }
    if (response.status === 204) {
        return null;
    }
    const contentType = response.headers.get("content-type") || "";
    return contentType.includes("application/json") ? response.json() : response.text();
}

function parseApiError(text, status) {
    try {
        const parsed = JSON.parse(text);
        if (Array.isArray(parsed.details) && parsed.details.length) {
            return parsed.details.join(" ");
        }
        return parsed.message || parsed.error || `Ошибка ${status}`;
    } catch (error) {
        return text || `Ошибка ${status}`;
    }
}

function parseErrorMessage(error) {
    return String(error?.message || "Не удалось выполнить операцию");
}

function isUnauthorized(error) {
    const message = String(error?.message || "");
    return message.includes("401") || message.toLowerCase().includes("unauthorized");
}

function normalize(value) {
    return String(value || "").toLowerCase().trim();
}

function formatCurrency(value) {
    return new Intl.NumberFormat("ru-RU", {
        style: "currency",
        currency: "USD",
        maximumFractionDigits: 2
    }).format(Number(value || 0));
}

function formatDate(value) {
    if (!value) {
        return "—";
    }
    return new Intl.DateTimeFormat("ru-RU", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

function statusTone(status) {
    const value = String(status || "").toUpperCase();
    if (["AVAILABLE", "ACTIVE", "COMPLETED"].includes(value)) {
        return "ok";
    }
    if (["RENTED", "REFUNDED", "BLOCKED"].includes(value)) {
        return "warn";
    }
    return "danger";
}

function displayStatus(status) {
    const value = String(status || "").toUpperCase();
    return STATUS_LABELS[value] || status || "—";
}

function carArt(car) {
    const brand = escapeSvg(car.brand || "Drive");
    const model = escapeSvg(car.model || "Flow");
    const plate = escapeSvg(car.licensePlate || "");
    const palettes = [
        ["#0f172a", "#1e293b", "#ef4444"],
        ["#111827", "#1f2937", "#f59e0b"],
        ["#172554", "#1d4ed8", "#38bdf8"],
        ["#1f2937", "#111827", "#22c55e"]
    ];
    const palette = palettes[Number(car.id || 0) % palettes.length];
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 520"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="${palette[0]}"/><stop offset="100%" stop-color="${palette[1]}"/></linearGradient></defs><rect width="900" height="520" rx="36" fill="url(#g)"/><circle cx="730" cy="110" r="150" fill="${palette[2]}" fill-opacity="0.18"/><path d="M168 312h430c30 0 58-18 70-45l22-51c8-19 27-31 47-31h52c13 0 24 11 24 24v65c0 21-17 38-38 38h-50c-9 34-40 58-76 58-35 0-66-24-75-58H362c-9 34-40 58-76 58-35 0-66-24-75-58h-43c-26 0-46-20-46-46 0-25 20-45 46-45Zm123 70a39 39 0 1 0 0-78 39 39 0 0 0 0 78Zm354 0a39 39 0 1 0 0-78 39 39 0 0 0 0 78Z" fill="#f8fafc" fill-opacity="0.95"/><path d="M308 208h200c24 0 46 13 57 34l16 30H260l23-41c5-13 14-23 25-23Z" fill="#e2e8f0" fill-opacity="0.95"/><text x="62" y="92" fill="#fff" font-size="34" font-family="Segoe UI, Arial, sans-serif" opacity="0.7">${brand}</text><text x="62" y="144" fill="#fff" font-size="56" font-weight="700" font-family="Segoe UI, Arial, sans-serif">${model}</text><text x="62" y="460" fill="#fff" font-size="28" font-family="Segoe UI, Arial, sans-serif" opacity="0.76">${plate}</text></svg>`;
    return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function escapeSvg(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;");
}

appRoot.render(html`<${App} />`);
