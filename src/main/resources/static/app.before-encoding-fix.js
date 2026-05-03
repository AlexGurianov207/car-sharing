const { useEffect, useMemo, useState } = React;

const rootNode = document.getElementById("root");
const ADMIN_TABS = [
    { id: "cars", label: "Р С’Р Р†РЎвЂљР С•Р С—Р В°РЎР‚Р С”" },
    { id: "rentals", label: "Р С’РЎР‚Р ВµР Р…Р Т‘РЎвЂ№" },
    { id: "users", label: "Р С™Р В»Р С‘Р ВµР Р…РЎвЂљРЎвЂ№" },
    { id: "services", label: "Р Р€РЎРѓР В»РЎС“Р С–Р С‘" },
    { id: "payments", label: "Р СџР В»Р В°РЎвЂљР ВµР В¶Р С‘" }
];
const USER_TABS = [
    { id: "cars", label: "Р С’Р Р†РЎвЂљР С•Р С—Р В°РЎР‚Р С”" },
    { id: "rentals", label: "Р СљР С•Р С‘ Р С—Р С•Р ВµР В·Р Т‘Р С”Р С‘" },
    { id: "payments", label: "Р СљР С•Р С‘ Р С—Р В»Р В°РЎвЂљР ВµР В¶Р С‘" }
];

function el(type, props, ...children) {
    return React.createElement(type, props, ...children.flat());
}

const defaultState = {
    cars: [],
    users: [],
    rentals: [],
    services: [],
    payments: []
};

const defaultForms = {
    car: { id: "", brand: "", model: "", licensePlate: "", year: "", pricePerHour: "", serviceIds: [] },
    rental: { userId: "", carId: "", serviceIds: [] },
    user: {
        id: "",
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
        driverLicense: "",
        status: "ACTIVE",
        originalStatus: "ACTIVE"
    },
    service: { id: "", name: "", description: "", pricePerDay: "", category: "COMFORT", isActive: "true" }
};

const defaultFilters = {
    cars: { query: "", status: "", brand: "", model: "", maxPrice: "" },
    rentals: { query: "", carBrand: "", userId: "", status: "" },
    users: { query: "", status: "" },
    services: { query: "", category: "", onlyActive: "" },
    payments: { query: "", status: "" }
};

const PAGE_SIZE = 6;
const defaultPagination = {
    cars: 1,
    rentals: 1,
    users: 1,
    services: 1,
    payments: 1
};

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^\+?\d{10,15}$/;

function App() {
    const [data, setData] = useState(defaultState);
    const [forms, setForms] = useState(defaultForms);
    const [filters, setFilters] = useState(defaultFilters);
    const [activeTab, setActiveTab] = useState("cars");
    const [selected, setSelected] = useState(null);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState("");
    const [session, setSession] = useState(null);
    const [entryMode, setEntryMode] = useState("guest");
    const [pagination, setPagination] = useState(defaultPagination);
    const [authReady, setAuthReady] = useState(false);
    const [authBusy, setAuthBusy] = useState(false);
    const [authTouched, setAuthTouched] = useState({});
    const [authMessage, setAuthMessage] = useState(null);
    const [authDraft, setAuthDraft] = useState({
        mode: "login",
        email: "",
        password: "",
        firstName: "",
        lastName: "",
        phoneNumber: "",
        driverLicense: ""
    });

    useEffect(() => {
        bootstrapSession().catch(showError);
    }, []);

    useEffect(() => {
        if (!toast) {
            return undefined;
        }
        const timer = setTimeout(() => setToast(""), 3000);
        return () => clearTimeout(timer);
    }, [toast]);

    useEffect(() => {
        const tabs = session?.role === "admin" ? ADMIN_TABS : USER_TABS;
        if (!tabs.some((tab) => tab.id === activeTab)) {
            setActiveTab("cars");
        }
    }, [session, activeTab]);

    const authValidation = useMemo(() => buildAuthValidation(authDraft), [authDraft]);

    async function bootstrapSession() {
        try {
            const currentSession = await api("/api/auth/me");
            const nextSession = toClientSession(currentSession);
            setSession(nextSession);
            setEntryMode("auth");
            await refreshAll(nextSession);
        } catch (error) {
            setSession(null);
            if (!isUnauthorized(error)) {
                showError(error);
            }
            await loadGuestPreview();
        } finally {
            setAuthReady(true);
        }
    }

    async function loadGuestPreview() {
        try {
            const [cars, services] = await Promise.all([
                api("/api/cars"),
                api("/api/services")
            ]);
            setData({
                cars,
                users: [],
                rentals: [],
                services,
                payments: []
            });
        } catch (error) {
            showError(error);
            setData(defaultState);
        }
    }

    const currentUser = useMemo(() => {
        if (!session || session.role !== "user") {
            return null;
        }
        return data.users.find((item) => item.id === Number(session.userId)) || null;
    }, [data.users, session]);

    const rentalsById = useMemo(() => {
        const map = new Map();
        data.rentals.forEach((item) => map.set(item.id, item));
        return map;
    }, [data.rentals]);

    const scopedRentals = useMemo(() => {
        if (session?.role !== "user" || !currentUser) {
            return data.rentals;
        }
        return data.rentals.filter((item) => item.userId === currentUser.id);
    }, [data.rentals, session, currentUser]);

    const scopedPayments = useMemo(() => {
        if (session?.role !== "user" || !currentUser) {
            return data.payments;
        }
        return data.payments.filter((item) => rentalsById.get(item.rentalId)?.userId === currentUser.id);
    }, [data.payments, rentalsById, session, currentUser]);

    const currentRental = useMemo(
        () => scopedRentals.find((item) => item.status === "ACTIVE") || null,
        [scopedRentals]
    );

    const rentalHistory = useMemo(
        () => scopedRentals.filter((item) => item.status !== "ACTIVE"),
        [scopedRentals]
    );

    const metrics = useMemo(() => {
        if (session?.role === "user" && currentUser) {
            return {
                availableCars: data.cars.filter((item) => item.status === "AVAILABLE").length,
                activeRentals: scopedRentals.filter((item) => item.status === "ACTIVE").length,
                rentals: scopedRentals.length,
                payments: scopedPayments.length
            };
        }
        return {
            cars: data.cars.length,
            availableCars: data.cars.filter((item) => item.status === "AVAILABLE").length,
            rentals: data.rentals.length,
            activeRentals: data.rentals.filter((item) => item.status === "ACTIVE").length,
            users: data.users.length,
            services: data.services.filter((item) => item.isActive).length
        };
    }, [data, session, currentUser, scopedRentals, scopedPayments]);

    const filteredUsers = useMemo(() => {
        const query = filters.users.query.trim().toLowerCase();
        return data.users.filter((item) => {
            const haystack = [
                item.firstName,
                item.lastName,
                item.email,
                item.driverLicense,
                item.phoneNumber
            ].join(" ").toLowerCase();
            return (!query || haystack.includes(query))
                && (!filters.users.status || item.status === filters.users.status);
        });
    }, [data.users, filters.users]);

    const filteredCars = useMemo(() => {
        const source = session?.role === "user"
            ? data.cars.filter((item) => item.status === "AVAILABLE" || item.status === "RENTED")
            : data.cars;
        const query = filters.cars.query.trim().toLowerCase();
        const brand = filters.cars.brand.trim().toLowerCase();
        const model = filters.cars.model.trim().toLowerCase();
        const maxPrice = Number(filters.cars.maxPrice);

        return source.filter((item) => {
            const haystack = [
                item.brand,
                item.model,
                item.licensePlate,
                item.year,
                item.status,
                (item.availableServices || []).map((service) => service.name).join(" ")
            ].join(" ").toLowerCase();

            return (!query || haystack.includes(query))
                && (!filters.cars.status || item.status === filters.cars.status)
                && (!brand || String(item.brand || "").toLowerCase().includes(brand))
                && (!model || String(item.model || "").toLowerCase().includes(model))
                && (!filters.cars.maxPrice || (!Number.isNaN(maxPrice) && Number(item.pricePerHour) <= maxPrice));
        });
    }, [data.cars, filters.cars, session]);

    const filteredRentals = useMemo(() => {
        const source = session?.role === "user" ? scopedRentals : data.rentals;
        const query = filters.rentals.query.trim().toLowerCase();
        const brand = filters.rentals.carBrand.trim().toLowerCase();

        return source.filter((item) => {
            const haystack = [
                item.id,
                item.userFullName,
                item.carInfo,
                item.status,
                ...(item.selectedServices || [])
            ].join(" ").toLowerCase();

            return (!query || haystack.includes(query))
                && (!brand || String(item.carInfo || "").toLowerCase().includes(brand))
                && (!filters.rentals.userId || String(item.userId) === String(filters.rentals.userId))
                && (!filters.rentals.status || item.status === filters.rentals.status);
        });
    }, [data.rentals, scopedRentals, filters.rentals, session]);

    const filteredServices = useMemo(() => {
        const query = filters.services.query.trim().toLowerCase();
        return data.services.filter((item) => {
            const haystack = [item.name, item.description, item.category].join(" ").toLowerCase();
            return (!query || haystack.includes(query))
                && (!filters.services.category || item.category === filters.services.category)
                && (filters.services.onlyActive === ""
                    || String(Boolean(item.isActive)) === String(filters.services.onlyActive));
        });
    }, [data.services, filters.services]);

    const filteredPayments = useMemo(() => {
        const source = session?.role === "user" ? scopedPayments : data.payments;
        const query = filters.payments.query.trim().toLowerCase();
        return source.filter((item) => {
            const haystack = [item.id, item.rentalId, item.transactionId, item.status].join(" ").toLowerCase();
            return (!query || haystack.includes(query))
                && (!filters.payments.status || item.status === filters.payments.status);
        });
    }, [data.payments, scopedPayments, filters.payments, session]);

    const paginatedCars = useMemo(
        () => paginateItems(filteredCars, pagination.cars, PAGE_SIZE),
        [filteredCars, pagination.cars]
    );
    const paginatedRentals = useMemo(
        () => paginateItems(filteredRentals, pagination.rentals, PAGE_SIZE),
        [filteredRentals, pagination.rentals]
    );
    const paginatedUsers = useMemo(
        () => paginateItems(filteredUsers, pagination.users, PAGE_SIZE),
        [filteredUsers, pagination.users]
    );
    const paginatedServices = useMemo(
        () => paginateItems(filteredServices, pagination.services, PAGE_SIZE),
        [filteredServices, pagination.services]
    );
    const paginatedPayments = useMemo(
        () => paginateItems(filteredPayments, pagination.payments, PAGE_SIZE),
        [filteredPayments, pagination.payments]
    );

    async function refreshAll(activeSession = session) {
        if (!activeSession) {
            setData(defaultState);
            return;
        }
        setLoading(true);
        try {
            const requests = activeSession.role === "admin"
                ? [
                    api("/api/cars"),
                    api("/api/users"),
                    api("/api/rentals/search/jpql"),
                    api("/api/services"),
                    api("/api/payments")
                ]
                : [
                    api("/api/cars"),
                    api("/api/users/me"),
                    api("/api/rentals/me"),
                    api("/api/services"),
                    api("/api/payments/me")
                ];
            const [cars, usersOrProfile, rentals, services, payments] = await Promise.all(requests);
            const users = activeSession.role === "admin" ? usersOrProfile : [usersOrProfile];
            setData({ cars, users, rentals, services, payments });
        } finally {
            setLoading(false);
        }
    }

    function showError(error) {
        console.error(error);
        setToast(error?.message || "Р СџРЎР‚Р С•Р С‘Р В·Р С•РЎв‚¬Р В»Р В° Р С•РЎв‚¬Р С‘Р В±Р С”Р В°");
    }

    function patchAuthDraft(patch) {
        setAuthDraft((current) => ({ ...current, ...patch }));
        setAuthMessage(null);
    }

    function touchAuthFields(fieldNames) {
        setAuthTouched((current) => ({
            ...current,
            ...Object.fromEntries(fieldNames.map((fieldName) => [fieldName, true]))
        }));
    }

    function showAuthError(error) {
        const message = humanizeAuthError(error?.message || "Р СњР Вµ РЎС“Р Т‘Р В°Р В»Р С•РЎРѓРЎРЉ Р Р†РЎвЂ№Р С—Р С•Р В»Р Р…Р С‘РЎвЂљРЎРЉ Р Т‘Р ВµР в„–РЎРѓРЎвЂљР Р†Р С‘Р Вµ");
        setAuthMessage({ type: "error", text: message });
        setToast(message);
    }

    function patchForm(name, patch) {
        setForms((current) => ({
            ...current,
            [name]: { ...current[name], ...patch }
        }));
    }

    function resetForm(name) {
        setForms((current) => ({ ...current, [name]: defaultForms[name] }));
    }

    function updateFilter(name, patch) {
        setFilters((current) => ({
            ...current,
            [name]: { ...current[name], ...patch }
        }));
        setPagination((current) => ({ ...current, [name]: 1 }));
    }

    function resetFilter(name) {
        setFilters((current) => ({
            ...current,
            [name]: { ...defaultFilters[name] }
        }));
        setPagination((current) => ({ ...current, [name]: 1 }));
    }

    function changePage(name, nextPage) {
        setPagination((current) => ({
            ...current,
            [name]: Math.max(1, nextPage)
        }));
    }

    function requireAdmin(action) {
        return async (...args) => {
            if (session?.role !== "admin") {
                setToast("Р СњР ВµР Т‘Р С•РЎРѓРЎвЂљР В°РЎвЂљР С•РЎвЂЎР Р…Р С• Р С—РЎР‚Р В°Р Р† Р Т‘Р В»РЎРЏ РЎРЊРЎвЂљР С•Р С–Р С• Р Т‘Р ВµР в„–РЎРѓРЎвЂљР Р†Р С‘РЎРЏ");
                return;
            }
            return action(...args);
        };
    }

    async function submitCar(event) {
        event.preventDefault();
        const payload = {
            brand: forms.car.brand.trim(),
            model: forms.car.model.trim(),
            licensePlate: forms.car.licensePlate.trim().toUpperCase(),
            year: Number(forms.car.year),
            pricePerHour: Number(forms.car.pricePerHour)
        };
        const saved = forms.car.id
            ? await api(`/api/cars/${forms.car.id}`, "PUT", payload)
            : await api("/api/cars", "POST", payload);
        await api(`/api/cars/${saved.id}/available-services`, "PUT", forms.car.serviceIds.map(Number));
        resetForm("car");
        setSelected({ type: "cars", id: saved.id });
        await refreshAll();
        setToast(forms.car.id ? "Р С’Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»РЎРЉ Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…" : "Р С’Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»РЎРЉ РЎРѓР С•Р В·Р Т‘Р В°Р Р…");
    }

    async function submitRental(event) {
        event.preventDefault();
        const userId = session?.role === "user" && currentUser
            ? currentUser.id
            : Number(forms.rental.userId);
        const saved = await api("/api/rentals", "POST", {
            userId,
            carId: Number(forms.rental.carId),
            serviceIds: forms.rental.serviceIds.map(Number)
        });
        resetForm("rental");
        setSelected({ type: "rentals", id: saved.id });
        await refreshAll();
        setToast("Р СџР С•Р ВµР В·Р Т‘Р С”Р В° Р С•РЎвЂћР С•РЎР‚Р СР В»Р ВµР Р…Р В°");
    }

    async function submitUser(event) {
        event.preventDefault();
        const payload = {
            firstName: forms.user.firstName.trim(),
            lastName: forms.user.lastName.trim(),
            email: forms.user.email.trim(),
            phoneNumber: forms.user.phoneNumber.trim(),
            driverLicense: forms.user.driverLicense.trim()
        };
        const saved = forms.user.id
            ? await api(`/api/users/${forms.user.id}`, "PUT", payload)
            : await api("/api/users", "POST", payload);
        const originalStatus = forms.user.id ? forms.user.originalStatus : "ACTIVE";
        if (forms.user.status !== originalStatus) {
            await api(`/api/users/${saved.id}/status?status=${encodeURIComponent(forms.user.status)}`, "PATCH");
        }
        resetForm("user");
        setSelected({ type: "users", id: saved.id });
        await refreshAll();
        setToast(forms.user.id ? "Р С™Р В»Р С‘Р ВµР Р…РЎвЂљ Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…" : "Р С™Р В»Р С‘Р ВµР Р…РЎвЂљ РЎРѓР С•Р В·Р Т‘Р В°Р Р…");
    }

    async function submitService(event) {
        event.preventDefault();
        const payload = {
            name: forms.service.name.trim(),
            description: forms.service.description.trim(),
            pricePerDay: Number(forms.service.pricePerDay),
            category: forms.service.category,
            isActive: forms.service.isActive === "true"
        };
        const saved = forms.service.id
            ? await api(`/api/services/${forms.service.id}`, "PUT", payload)
            : await api("/api/services", "POST", payload);
        resetForm("service");
        setSelected({ type: "services", id: saved.id });
        await refreshAll();
        setToast(forms.service.id ? "Р Р€РЎРѓР В»РЎС“Р С–Р В° Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р В°" : "Р Р€РЎРѓР В»РЎС“Р С–Р В° РЎРѓР С•Р В·Р Т‘Р В°Р Р…Р В°");
    }

    async function removeEntity(type, id) {
        const endpointMap = {
            cars: `/api/cars/${id}`,
            rentals: `/api/rentals/${id}`,
            users: `/api/users/${id}`,
            services: `/api/services/${id}`,
            payments: `/api/payments/${id}`
        };
        await api(endpointMap[type], "DELETE");
        if (selected && selected.type === type && selected.id === id) {
            setSelected(null);
        }
        await refreshAll();
        setToast("Р вЂ”Р В°Р С—Р С‘РЎРѓРЎРЉ РЎС“Р Т‘Р В°Р В»Р ВµР Р…Р В°");
    }

    async function completeRental(id) {
        await api(`/api/rentals/${id}/complete`, "PATCH");
        await refreshAll();
        setToast("Р СџР С•Р ВµР В·Р Т‘Р С”Р В° Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬Р ВµР Р…Р В°");
    }

    async function refundPayment(id) {
        await api(`/api/payments/${id}/refund`, "PATCH");
        await refreshAll();
        setToast("Р вЂ™Р С•Р В·Р Р†РЎР‚Р В°РЎвЂљ Р Р†РЎвЂ№Р С—Р С•Р В»Р Р…Р ВµР Р…");
    }

    async function verifyPayment(id) {
        const task = await api(`/api/payments/${id}/verify/async`, "POST");
        setToast(`Р вЂ”Р В°Р С—РЎС“РЎвЂ°Р ВµР Р…Р В° Р С—РЎР‚Р С•Р Р†Р ВµРЎР‚Р С”Р В°: ${task.taskId}`);
    }

    async function loginAsUser() {
        if (authBusy) {
            return;
        }
        touchAuthFields(["email", "password"]);
        if (!authValidation.login.isValid) {
            setAuthMessage({ type: "error", text: "Р вЂ™Р Р†Р ВµР Т‘Р С‘РЎвЂљР Вµ Р С”Р С•РЎР‚РЎР‚Р ВµР С”РЎвЂљР Р…РЎвЂ№Р в„– Р В»Р С•Р С–Р С‘Р Р… Р С‘ Р С—Р В°РЎР‚Р С•Р В»РЎРЉ." });
            return;
        }
        try {
            setAuthBusy(true);
            setAuthMessage(null);
            const response = await api("/api/auth/login", "POST", {
                login: authDraft.email.trim(),
                password: authDraft.password
            });
            const nextSession = toClientSession(response);
            setSession(nextSession);
            setEntryMode("auth");
            setSelected(null);
            setActiveTab("cars");
            setAuthTouched({});
            await refreshAll(nextSession);
            setToast("Р вЂ™РЎвЂ¦Р С•Р Т‘ Р Р†РЎвЂ№Р С—Р С•Р В»Р Р…Р ВµР Р…. Р В Р В°Р Т‘РЎвЂ№ Р Р†Р С‘Р Т‘Р ВµРЎвЂљРЎРЉ Р Р†Р В°РЎРѓ РЎРѓР Р…Р С•Р Р†Р В°.");
        } catch (error) {
            showAuthError(error);
        } finally {
            setAuthBusy(false);
        }
    }

    async function registerUser() {
        if (authBusy) {
            return;
        }
        touchAuthFields(["firstName", "lastName", "email", "phoneNumber", "driverLicense", "password"]);
        if (!authValidation.register.isValid) {
            setAuthMessage({ type: "error", text: "Р СџРЎР‚Р С•Р Р†Р ВµРЎР‚РЎРЉРЎвЂљР Вµ Р В·Р В°Р С—Р С•Р В»Р Р…Р ВµР Р…Р С‘Р Вµ Р С—Р С•Р В»Р ВµР в„– РЎР‚Р ВµР С–Р С‘РЎРѓРЎвЂљРЎР‚Р В°РЎвЂ Р С‘Р С‘." });
            return;
        }
        try {
            setAuthBusy(true);
            setAuthMessage(null);
            const response = await api("/api/auth/register", "POST", {
                firstName: authDraft.firstName.trim(),
                lastName: authDraft.lastName.trim(),
                email: authDraft.email.trim(),
                phoneNumber: authDraft.phoneNumber.trim(),
                driverLicense: authDraft.driverLicense.trim(),
                password: authDraft.password
            });
            const nextSession = toClientSession(response);
            setSession(nextSession);
            setEntryMode("auth");
            setSelected(null);
            setActiveTab("cars");
            setAuthTouched({});
            await refreshAll(nextSession);
            setToast("Р С’Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ РЎРѓР С•Р В·Р Т‘Р В°Р Р…. Р вЂ™РЎвЂ№ РЎС“Р В¶Р Вµ Р Р†Р С•РЎв‚¬Р В»Р С‘ Р Р† Р В»Р С‘РЎвЂЎР Р…РЎвЂ№Р в„– Р С”Р В°Р В±Р С‘Р Р…Р ВµРЎвЂљ.");
        } catch (error) {
            showAuthError(error);
        } finally {
            setAuthBusy(false);
        }
    }

    async function logout() {
        try {
            await api("/api/auth/logout", "POST");
        } catch (error) {
            if (!isUnauthorized(error)) {
                showError(error);
            }
        } finally {
            setSession(null);
            setEntryMode("guest");
            setData(defaultState);
            setSelected(null);
            setActiveTab("cars");
            setToast("Р вЂ™РЎвЂ№ Р Р†РЎвЂ№РЎв‚¬Р В»Р С‘ Р С‘Р В· РЎРѓР С‘РЎРѓРЎвЂљР ВµР СРЎвЂ№");
            await loadGuestPreview();
        }
    }

    const availableTabs = session?.role === "admin" ? ADMIN_TABS : USER_TABS;
    const dashboardTitle = session?.role === "admin"
        ? "Р СџР В°Р Р…Р ВµР В»РЎРЉ РЎС“Р С—РЎР‚Р В°Р Р†Р В»Р ВµР Р…Р С‘РЎРЏ Р С”Р В°РЎР‚РЎв‚¬Р ВµРЎР‚Р С‘Р Р…Р С–Р С•Р С"
        : "Р вЂ™Р В°РЎв‚¬ Р В»Р С‘РЎвЂЎР Р…РЎвЂ№Р в„– Р С”Р В°Р В±Р С‘Р Р…Р ВµРЎвЂљ";
    const dashboardLead = session?.role === "admin"
        ? "Р С™Р С•Р Р…РЎвЂљРЎР‚Р С•Р В»Р С‘РЎР‚РЎС“Р в„–РЎвЂљР Вµ Р В°Р Р†РЎвЂљР С•Р С—Р В°РЎР‚Р С”, Р С”Р В»Р С‘Р ВµР Р…РЎвЂљР С•Р Р†, Р С—Р С•Р ВµР В·Р Т‘Р С”Р С‘, РЎС“РЎРѓР В»РЎС“Р С–Р С‘ Р С‘ Р С—Р В»Р В°РЎвЂљР ВµР В¶Р С‘ Р С‘Р В· Р С•Р Т‘Р Р…Р С•Р С–Р С• РЎвЂ Р ВµР Р…РЎвЂљРЎР‚Р В° РЎС“Р С—РЎР‚Р В°Р Р†Р В»Р ВµР Р…Р С‘РЎРЏ."
        : "Р вЂ™РЎвЂ№Р В±Р С‘РЎР‚Р В°Р в„–РЎвЂљР Вµ Р В°Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»Р С‘, Р С•РЎвЂћР С•РЎР‚Р СР В»РЎРЏР в„–РЎвЂљР Вµ Р С—Р С•Р ВµР В·Р Т‘Р С”Р С‘, РЎРѓР В»Р ВµР Т‘Р С‘РЎвЂљР Вµ Р В·Р В° Р В°Р С”РЎвЂљР С‘Р Р†Р Р…Р С•Р в„– Р В°РЎР‚Р ВµР Р…Р Т‘Р С•Р в„– Р С‘ Р С—РЎР‚Р С•РЎРѓР СР В°РЎвЂљРЎР‚Р С‘Р Р†Р В°Р в„–РЎвЂљР Вµ Р С‘РЎРѓРЎвЂљР С•РЎР‚Р С‘РЎР‹ Р С—Р В»Р В°РЎвЂљР ВµР В¶Р ВµР в„–.";

    if (!authReady) {
        return el("div", { className: "page-shell auth-shell" }, [
            el("section", { className: "hero auth-hero", key: "auth-loading" }, [
                el("div", { className: "hero-panel-card auth-card", key: "loading-card" }, [
                    el("p", { className: "eyebrow", key: "loading-eyebrow" }, "Р вЂ”Р В°Р С–РЎР‚РЎС“Р В·Р С”Р В°"),
                    el("h1", { key: "loading-title" }, "Р СџРЎР‚Р С•Р Р†Р ВµРЎР‚РЎРЏР ВµР С Р Р†Р В°РЎв‚¬РЎС“ РЎРѓР ВµРЎРѓРЎРѓР С‘РЎР‹"),
                    el("p", { className: "hero-text", key: "loading-text" }, "Р вЂўРЎРѓР В»Р С‘ Р Р†РЎвЂ№ РЎС“Р В¶Р Вµ Р Р†РЎвЂ¦Р С•Р Т‘Р С‘Р В»Р С‘ РЎР‚Р В°Р Р…РЎРЉРЎв‚¬Р Вµ, Р С”Р В°Р В±Р С‘Р Р…Р ВµРЎвЂљ Р С•РЎвЂљР С”РЎР‚Р С•Р ВµРЎвЂљРЎРѓРЎРЏ Р В°Р Р†РЎвЂљР С•Р СР В°РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”Р С‘.")
                ])
            ])
        ]);
    }

    if (!session && entryMode === "guest") {
        return renderGuestScreen({
            data,
            setEntryMode,
            setAuthDraft
        });
    }

    if (!session) {
        return renderAuthScreen({
            authDraft,
            patchAuthDraft,
            touchAuthFields,
            authTouched,
            authValidation,
            authBusy,
            authMessage,
            setEntryMode,
            loginAsUser,
            registerUser
        });
    }

    return el("div", { className: "page-shell" }, [
    el("div", { className: "utility-bar", key: "utility" }, [
        el("div", { className: "utility-copy", key: "utility-copy" }, [
            el("strong", { key: "u1" }, session.role === "admin" ? "\u0420\u0435\u0436\u0438\u043c \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440\u0430" : `\u041b\u0438\u0447\u043d\u044b\u0439 \u043a\u0430\u0431\u0438\u043d\u0435\u0442 ${currentUser ? `${currentUser.firstName} ${currentUser.lastName}` : ""}`),
            el("span", { key: "u2" }, session.role === "admin" ? "\u041f\u043e\u043b\u043d\u044b\u0439 \u0434\u043e\u0441\u0442\u0443\u043f \u043a \u0443\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u044e \u0441\u0435\u0440\u0432\u0438\u0441\u043e\u043c" : "\u0412\u0430\u0448\u0438 \u043f\u043e\u0435\u0437\u0434\u043a\u0438, \u0430\u0440\u0435\u043d\u0434\u044b \u0438 \u043f\u043b\u0430\u0442\u0435\u0436\u0438 \u0432 \u043e\u0434\u043d\u043e\u043c \u043a\u0430\u0431\u0438\u043d\u0435\u0442\u0435")
        ]),
        el("div", { className: "utility-actions", key: "utility-actions" }, [
            button(loading ? "\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435..." : "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c", () => refreshAll().catch(showError), "ghost-button small utility-button", "refresh-top"),
            button("\u0412\u044b\u0439\u0442\u0438", logout, "primary-button small utility-button utility-logout", "logout", "button")
        ])
    ]),

    el("header", { className: "hero", key: "hero" }, [
        el("div", { className: "hero-grid", key: "hero-grid" }, [
            el("section", { className: "hero-copy", key: "hero-copy" }, [
                el("p", { className: "eyebrow", key: "eyebrow" }, session.role === "admin" ? "\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0441\u0435\u0440\u0432\u0438\u0441\u043e\u043c" : "\u041a\u043e\u043c\u0444\u043e\u0440\u0442 \u0432 \u043a\u0430\u0436\u0434\u043e\u0439 \u043f\u043e\u0435\u0437\u0434\u043a\u0435"),
                el("h1", { key: "title" }, dashboardTitle),
                el("p", { className: "hero-text", key: "lead" }, dashboardLead),
                el("div", { className: "hero-actions", key: "hero-actions" }, [
                    button(availableTabs[0].label, () => setActiveTab(availableTabs[0].id), "primary-button", "hero-first"),
                    button(availableTabs[1].label, () => setActiveTab(availableTabs[1].id), "secondary-button", "hero-second")
                ]),
                el("div", { className: "hero-tags", key: "hero-tags" }, session.role === "admin"
                    ? [
                        tag("\u041f\u043e\u043b\u043d\u044b\u0439 \u043a\u043e\u043d\u0442\u0440\u043e\u043b\u044c \u0430\u0432\u0442\u043e\u043f\u0430\u0440\u043a\u0430", "t1"),
                        tag("\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u043a\u043b\u0438\u0435\u043d\u0442\u0430\u043c\u0438", "t2"),
                        tag("\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u0443\u0441\u043b\u0443\u0433", "t3"),
                        tag("\u041f\u043b\u0430\u0442\u0451\u0436\u043d\u0430\u044f \u0438\u0441\u0442\u043e\u0440\u0438\u044f", "t4")
                    ]
                    : [
                        tag(`${metrics.availableCars} \u0430\u0432\u0442\u043e \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b \u0441\u0435\u0439\u0447\u0430\u0441`, "t1"),
                        tag(currentRental ? "\u0415\u0441\u0442\u044c \u0430\u043a\u0442\u0438\u0432\u043d\u0430\u044f \u043f\u043e\u0435\u0437\u0434\u043a\u0430" : "\u041c\u043e\u0436\u043d\u043e \u043e\u0444\u043e\u0440\u043c\u0438\u0442\u044c \u043d\u043e\u0432\u0443\u044e \u043f\u043e\u0435\u0437\u0434\u043a\u0443", "t2"),
                        tag(`${scopedPayments.length} \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439 \u0432 \u0438\u0441\u0442\u043e\u0440\u0438\u0438`, "t3"),
                        tag("\u041f\u043e\u0434\u0434\u0435\u0440\u0436\u043a\u0430 24/7", "t4")
                    ])
            ]),
            el("section", { className: "hero-panel", key: "hero-panel" }, [
                el("div", { className: "hero-panel-card", key: "hero-card" }, session.role === "admin"
                    ? [
                        el("div", { className: "panel-caption", key: "hc" }, "\u041a\u043b\u044e\u0447\u0435\u0432\u044b\u0435 \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u0435\u043b\u0438"),
                        el("div", { className: "feature-stack", key: "metrics-admin" }, [
                            heroInfoCard("\u0410\u0432\u0442\u043e\u043f\u0430\u0440\u043a", `${metrics.cars} \u043c\u0430\u0448\u0438\u043d, \u0438\u0437 \u043d\u0438\u0445 ${metrics.availableCars} \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b.`, "a1"),
                            heroInfoCard("\u041f\u043e\u0435\u0437\u0434\u043a\u0438", `${metrics.rentals} \u043f\u043e\u0435\u0437\u0434\u043e\u043a \u0432\u0441\u0435\u0433\u043e, ${metrics.activeRentals} \u0430\u043a\u0442\u0438\u0432\u043d\u044b \u0441\u0435\u0439\u0447\u0430\u0441.`, "a2"),
                            heroInfoCard("\u041a\u043b\u0438\u0435\u043d\u0442\u044b \u0438 \u0443\u0441\u043b\u0443\u0433\u0438", `${metrics.users} \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432 \u0438 ${metrics.services} \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445 \u0443\u0441\u043b\u0443\u0433 \u0432 \u0441\u0438\u0441\u0442\u0435\u043c\u0435.`, "a3")
                        ])
                    ]
                    : [
                        el("div", { className: "panel-caption", key: "hc" }, "\u0412\u0430\u0448\u0438 \u043f\u043e\u0435\u0437\u0434\u043a\u0438"),
                        el("div", { className: "feature-stack", key: "metrics-user" }, [
                            heroInfoCard("\u0422\u0435\u043a\u0443\u0449\u0430\u044f \u043f\u043e\u0435\u0437\u0434\u043a\u0430", currentRental ? currentRental.carInfo || `\u0410\u0440\u0435\u043d\u0434\u0430 #${currentRental.id}` : "\u0421\u0435\u0439\u0447\u0430\u0441 \u0430\u043a\u0442\u0438\u0432\u043d\u043e\u0439 \u043f\u043e\u0435\u0437\u0434\u043a\u0438 \u043d\u0435\u0442.", "u1"),
                            heroInfoCard("\u0418\u0441\u0442\u043e\u0440\u0438\u044f", `${rentalHistory.length} \u0437\u0430\u0432\u0435\u0440\u0448\u0451\u043d\u043d\u044b\u0445 \u043f\u043e\u0435\u0437\u0434\u043e\u043a \u0432 \u0438\u0441\u0442\u043e\u0440\u0438\u0438 \u0430\u043a\u043a\u0430\u0443\u043d\u0442\u0430.`, "u2"),
                            heroInfoCard("\u041f\u043b\u0430\u0442\u0435\u0436\u0438", `${scopedPayments.length} \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u0439 \u0432 \u0432\u0430\u0448\u0435\u043c \u0436\u0443\u0440\u043d\u0430\u043b\u0435 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439.`, "u3")
                        ])
                    ]),
                el("div", { className: "hero-aside-card", key: "hero-aside" }, session.role === "admin"
                    ? [
                        el("div", { className: "aside-title", key: "hat" }, "\u0411\u044b\u0441\u0442\u0440\u044b\u0439 \u043a\u043e\u043d\u0442\u0440\u043e\u043b\u044c"),
                        showcaseSection(
                            "\u0410\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u0438",
                            data.cars.slice(0, 3).map((car) => showcaseRow(
                                `${car.brand} ${car.model}`,
                                `${car.year} вЂў ${formatCurrency(car.pricePerHour)}/\u0447\u0430\u0441`,
                                car.status === "AVAILABLE" ? "\u0414\u043e\u0441\u0442\u0443\u043f\u0435\u043d" : car.status,
                                `car-${car.id}`
                            )),
                            "cars-showcase"
                        ),
                        showcaseSection(
                            "\u0423\u0441\u043b\u0443\u0433\u0438",
                            data.services.slice(0, 3).map((service) => showcaseRow(
                                service.name,
                                `${service.category} вЂў ${formatCurrency(service.pricePerDay)}/\u0434\u0435\u043d\u044c`,
                                service.isActive ? "\u0410\u043a\u0442\u0438\u0432\u043d\u0430" : "\u0421\u043a\u0440\u044b\u0442\u0430",
                                `service-${service.id}`
                            )),
                            "services-showcase"
                        )
                    ]
                    : [
                        el("div", { className: "aside-title", key: "hat" }, "\u0427\u0442\u043e \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u0432\u0430\u043c"),
                        showcaseSection(
                            "\u0412\u0430\u0448\u0438 \u0440\u0430\u0437\u0434\u0435\u043b\u044b",
                            [
                                showcaseRow("\u0410\u0432\u0442\u043e\u043f\u0430\u0440\u043a", "\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b\u0445 \u0430\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u0435\u0439 \u0438 \u0443\u0441\u043b\u043e\u0432\u0438\u0439 \u0430\u0440\u0435\u043d\u0434\u044b.", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c", "s1"),
                                showcaseRow("\u041c\u043e\u0438 \u043f\u043e\u0435\u0437\u0434\u043a\u0438", "\u0422\u0435\u043a\u0443\u0449\u0430\u044f \u0430\u0440\u0435\u043d\u0434\u0430 \u0438 \u0438\u0441\u0442\u043e\u0440\u0438\u044f \u0432\u0430\u0448\u0438\u0445 \u043f\u043e\u0435\u0437\u0434\u043e\u043a.", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c", "s2"),
                                showcaseRow("\u041c\u043e\u0438 \u043f\u043b\u0430\u0442\u0435\u0436\u0438", "\u0418\u0441\u0442\u043e\u0440\u0438\u044f \u043e\u043f\u043b\u0430\u0442 \u0438 \u0441\u0442\u0430\u0442\u0443\u0441\u044b \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u0439.", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c", "s3")
                            ],
                            "user-showcase"
                        )
                    ])
            ])
        ]),

        el("main", { className: "content-grid", key: "main" }, [
            el("section", { className: "workspace", key: "workspace" }, [
                el("section", { className: "showcase", key: "showcase" }, [
                    el("div", { className: "section-head", key: "head" }, [
                        el("div", { key: "head-copy" }, [
                            el("p", { className: "section-kicker", key: "sk" }, session.role === "admin" ? "\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435" : "\u041a\u0430\u0431\u0438\u043d\u0435\u0442 \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044f"),
                            el("h2", { key: "sh" }, session.role === "admin" ? "\u041f\u0430\u043d\u0435\u043b\u044c \u0430\u0434\u043c\u0438\u043d\u0438\u0441\u0442\u0440\u0430\u0442\u043e\u0440\u0430" : "\u0412\u0430\u0448\u0438 \u0441\u0435\u0440\u0432\u0438\u0441\u044b")
                        ]),
                        button(loading ? "\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435..." : "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0434\u0430\u043d\u043d\u044b\u0435", () => refreshAll().catch(showError), "ghost-button", "refresh")
                    ]),
                    el("div", { className: "category-strip", key: "tabs" },
                        availableTabs.map((tab) => el("button", {
                            key: tab.id,
                            className: `category-card ${activeTab === tab.id ? "active" : ""}`,
                            onClick: () => setActiveTab(tab.id)
                        }, [
                            el("strong", { key: `${tab.id}-title` }, tab.label),
                            el("span", { key: `${tab.id}-subtitle` }, tabSubtitle(tab.id, session.role))
                        ]))
                    )
                ]),
                renderTab({
                    activeTab,
                    role: session.role,
                    currentUser,
                    currentRental,
                    rentalHistory,
                    data,
                    forms,
                    filters,
                    pagination,
                    filteredUsers,
                    filteredCars,
                    filteredRentals,
                    filteredServices,
                    filteredPayments,
                    paginatedCars,
                    paginatedRentals,
                    paginatedUsers,
                    paginatedServices,
                    paginatedPayments,
                    scopedRentals,
                    scopedPayments,
                    patchForm,
                    resetForm,
                    updateFilter,
                    resetFilter,
                    changePage,
                    setSelected,
                    submitCar: requireAdmin(submitCar),
                    submitRental,
                    submitUser: requireAdmin(submitUser),
                    submitService: requireAdmin(submitService),
                    removeEntity: requireAdmin(removeEntity),
                    completeRental: session.role === "admin" ? completeRental : null,
                    refundPayment: session.role === "admin" ? refundPayment : null,
                    verifyPayment: session.role === "admin" ? verifyPayment : null
                })
            ]),
            el("aside", { className: "side-panel", key: "side" }, [
                el("section", { className: "relation-card", key: "details" }, [
                    el("p", { className: "section-kicker light", key: "dk" }, "\u041f\u043e\u0434\u0440\u043e\u0431\u043d\u043e\u0441\u0442\u0438"),
                    el("h3", { key: "dh" }, "\u0418\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044f"),
                    detailView(selected, session.role === "user" ? { ...data, rentals: scopedRentals, payments: scopedPayments } : data)
                ]),
                el("section", { className: "relation-card alt", key: "access" }, [
                    el("p", { className: "section-kicker", key: "ak" }, "\u041f\u0440\u0430\u0432\u0430 \u0434\u043e\u0441\u0442\u0443\u043f\u0430"),
                    el("ul", { className: "panel-list", key: "al" }, session.role === "admin"
                        ? [
                            li("\u041f\u043e\u043b\u043d\u044b\u0439 \u0434\u043e\u0441\u0442\u0443\u043f \u043a \u0430\u0432\u0442\u043e\u043f\u0430\u0440\u043a\u0443", "p1"),
                            li("\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0432\u0441\u0435\u0445 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432 \u0438 \u043f\u043e\u0435\u0437\u0434\u043e\u043a", "p2"),
                            li("\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0443\u0441\u043b\u0443\u0433\u0430\u043c\u0438 \u0438 \u043f\u043b\u0430\u0442\u0435\u0436\u0430\u043c\u0438", "p3"),
                            li("\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0438 \u0443\u0434\u0430\u043b\u0435\u043d\u0438\u0435 \u0437\u0430\u043f\u0438\u0441\u0435\u0439", "p4")
                        ]
                        : [
                            li("\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b\u0445 \u0430\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u0435\u0439", "p1"),
                            li("\u041e\u0444\u043e\u0440\u043c\u043b\u0435\u043d\u0438\u0435 \u0441\u0432\u043e\u0435\u0439 \u043f\u043e\u0435\u0437\u0434\u043a\u0438", "p2"),
                            li("\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u043e\u043b\u044c\u043a\u043e \u0441\u0432\u043e\u0438\u0445 \u043f\u043e\u0435\u0437\u0434\u043e\u043a", "p3"),
                            li("\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440 \u0442\u043e\u043b\u044c\u043a\u043e \u0441\u0432\u043e\u0438\u0445 \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439", "p4")
                        ])
                ])
            ])
        ]),

        el("div", { className: `toast ${toast ? "show" : ""}`, key: "toast" }, toast)
    ]);
}
function renderAuthScreen(ctx) {
    const registerErrors = ctx.authValidation.register.errors;
    const loginErrors = ctx.authValidation.login.errors;

    return el("div", { className: "page-shell auth-shell" }, [
        el("section", { className: "hero auth-hero", key: "auth" }, [
            el("div", { className: "hero-grid", key: "auth-grid" }, [
                el("section", { className: "hero-copy", key: "auth-copy" }, [
                    el("p", { className: "eyebrow", key: "ae" }, "Р вЂќР С•Р В±РЎР‚Р С• Р С—Р С•Р В¶Р В°Р В»Р С•Р Р†Р В°РЎвЂљРЎРЉ"),
                    el("h1", { key: "ah" }, ctx.authDraft.mode === "register" ? "Р РЋР С•Р В·Р Т‘Р В°Р в„–РЎвЂљР Вµ Р В°Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ" : "Р вЂ™Р С•Р в„–Р Т‘Р С‘РЎвЂљР Вµ Р Р† Р В°Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ"),
                    el("p", { className: "hero-text", key: "at" }, "Р вЂ™Р С•Р в„–Р Т‘Р С‘РЎвЂљР Вµ Р Р† Р В°Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ Р С‘Р В»Р С‘ РЎРѓР С•Р В·Р Т‘Р В°Р в„–РЎвЂљР Вµ Р Р…Р С•Р Р†РЎвЂ№Р в„– Р С—РЎР‚Р С•РЎвЂћР С‘Р В»РЎРЉ, РЎвЂЎРЎвЂљР С•Р В±РЎвЂ№ Р С—РЎР‚Р С•Р Т‘Р С•Р В»Р В¶Р С‘РЎвЂљРЎРЉ.")
                ]),
                el("section", { className: "hero-panel", key: "auth-panel" }, [
                    el("div", { className: "hero-panel-card auth-card", key: "user-card" }, [
                        actionRow([
                            button("Р СџРЎР‚Р С•Р Т‘Р С•Р В»Р В¶Р С‘РЎвЂљРЎРЉ Р С—РЎР‚Р С•РЎРѓР СР С•РЎвЂљРЎР‚", () => ctx.setEntryMode("guest"), "ghost-button", "back-to-guest")
                        ], "auth-back"),
                        el("div", { className: "auth-switch", key: "auth-switch" }, [
                            button("Р вЂ™Р С•Р в„–РЎвЂљР С‘", () => ctx.patchAuthDraft({ mode: "login" }), `ghost-button small ${ctx.authDraft.mode === "login" ? "active-auth" : ""}`, "switch-login"),
                            button("Р вЂ”Р В°РЎР‚Р ВµР С–Р С‘РЎРѓРЎвЂљРЎР‚Р С‘РЎР‚Р С•Р Р†Р В°РЎвЂљРЎРЉРЎРѓРЎРЏ", () => ctx.patchAuthDraft({ mode: "register" }), `ghost-button small ${ctx.authDraft.mode === "register" ? "active-auth" : ""}`, "switch-register")
                        ]),
                        ctx.authMessage
                            ? el("div", { className: `auth-message ${ctx.authMessage.type || "info"}`, key: "auth-message" }, ctx.authMessage.text)
                            : null,
                        ctx.authDraft.mode === "register"
                            ? el("div", { key: "register-form" }, [
                                formRow([
                                    textField("Р ВР СРЎРЏ", ctx.authDraft.firstName, (value) => ctx.patchAuthDraft({ firstName: value }), "register-first", {
                                        hint: "Р СњР Вµ Р СР ВµР Р…Р ВµР Вµ 2 РЎРѓР С‘Р СР Р†Р С•Р В»Р С•Р Р†",
                                        error: ctx.authTouched.firstName ? registerErrors.firstName : "",
                                        onBlur: () => ctx.touchAuthFields(["firstName"]),
                                        autoComplete: "given-name"
                                    }),
                                    textField("Р В¤Р В°Р СР С‘Р В»Р С‘РЎРЏ", ctx.authDraft.lastName, (value) => ctx.patchAuthDraft({ lastName: value }), "register-last", {
                                        hint: "Р СњР Вµ Р СР ВµР Р…Р ВµР Вµ 2 РЎРѓР С‘Р СР Р†Р С•Р В»Р С•Р Р†",
                                        error: ctx.authTouched.lastName ? registerErrors.lastName : "",
                                        onBlur: () => ctx.touchAuthFields(["lastName"]),
                                        autoComplete: "family-name"
                                    })
                                ], "register-row1"),
                                textField("Email", ctx.authDraft.email, (value) => ctx.patchAuthDraft({ email: value }), "register-email", {
                                    hint: "Р В¤Р С•РЎР‚Р СР В°РЎвЂљ: name@example.com",
                                    error: ctx.authTouched.email ? registerErrors.email : "",
                                    onBlur: () => ctx.touchAuthFields(["email"]),
                                    autoComplete: "email",
                                    placeholder: "name@example.com"
                                }),
                                formRow([
                                    textField("Р СћР ВµР В»Р ВµРЎвЂћР С•Р Р…", ctx.authDraft.phoneNumber, (value) => ctx.patchAuthDraft({ phoneNumber: value }), "register-phone", {
                                        hint: "Р СњР В°Р С—РЎР‚Р С‘Р СР ВµРЎР‚: +375291234567",
                                        error: ctx.authTouched.phoneNumber ? registerErrors.phoneNumber : "",
                                        onBlur: () => ctx.touchAuthFields(["phoneNumber"]),
                                        autoComplete: "tel",
                                        placeholder: "+375291234567"
                                    }),
                                    textField("Р вЂ™Р С•Р Т‘Р С‘РЎвЂљР ВµР В»РЎРЉРЎРѓР С”Р С•Р Вµ РЎС“Р Т‘Р С•РЎРѓРЎвЂљР С•Р Р†Р ВµРЎР‚Р ВµР Р…Р С‘Р Вµ", ctx.authDraft.driverLicense, (value) => ctx.patchAuthDraft({ driverLicense: value }), "register-license", {
                                        hint: "Р С›РЎвЂљ 5 Р Т‘Р С• 20 РЎРѓР С‘Р СР Р†Р С•Р В»Р С•Р Р†",
                                        error: ctx.authTouched.driverLicense ? registerErrors.driverLicense : "",
                                        onBlur: () => ctx.touchAuthFields(["driverLicense"]),
                                        autoComplete: "off"
                                    })
                                ], "register-row2"),
                                textField("Р СџР В°РЎР‚Р С•Р В»РЎРЉ", ctx.authDraft.password, (value) => ctx.patchAuthDraft({ password: value }), "register-password", {
                                    type: "password",
                                    hint: "Р СљР С‘Р Р…Р С‘Р СРЎС“Р С 6 РЎРѓР С‘Р СР Р†Р С•Р В»Р С•Р Р†",
                                    error: ctx.authTouched.password ? registerErrors.password : "",
                                    onBlur: () => ctx.touchAuthFields(["password"]),
                                    autoComplete: "new-password"
                                }),
                                actionRow([
                                    button(ctx.authBusy ? "Р РЋР С•Р В·Р Т‘Р В°Р ВµР С Р В°Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ..." : "Р вЂ”Р В°РЎР‚Р ВµР С–Р С‘РЎРѓРЎвЂљРЎР‚Р С‘РЎР‚Р С•Р Р†Р В°РЎвЂљРЎРЉРЎРѓРЎРЏ", ctx.registerUser, "primary-button", "register-submit"),
                                    button("Р вЂ™Р С•Р в„–РЎвЂљР С‘", () => ctx.patchAuthDraft({ mode: "login" }), "secondary-button", "go-login")
                                ], "register-actions")
                            ])
                            : el("div", { key: "login-form" }, [
                                textField("Р вЂєР С•Р С–Р С‘Р Р…", ctx.authDraft.email, (value) => ctx.patchAuthDraft({ email: value }), "login-email", {
                                    hint: "Р Р€Р С”Р В°Р В¶Р С‘РЎвЂљР Вµ Р Т‘Р В°Р Р…Р Р…РЎвЂ№Р Вµ Р Р†Р В°РЎв‚¬Р ВµР в„– РЎС“РЎвЂЎР ВµРЎвЂљР Р…Р С•Р в„– Р В·Р В°Р С—Р С‘РЎРѓР С‘",
                                    error: ctx.authTouched.email ? loginErrors.email : "",
                                    onBlur: () => ctx.touchAuthFields(["email"]),
                                    autoComplete: "username",
                                    placeholder: "Р вЂ™Р Р†Р ВµР Т‘Р С‘РЎвЂљР Вµ Р В»Р С•Р С–Р С‘Р Р…"
                                }),
                                textField("Р СџР В°РЎР‚Р С•Р В»РЎРЉ", ctx.authDraft.password, (value) => ctx.patchAuthDraft({ password: value }), "login-password", {
                                    type: "password",
                                    hint: "Р вЂўРЎРѓР В»Р С‘ Р В°Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ РЎвЂљР С•Р В»РЎРЉР С”Р С• РЎвЂЎРЎвЂљР С• РЎРѓР С•Р В·Р Т‘Р В°Р Р…, Р С‘РЎРѓР С—Р С•Р В»РЎРЉР В·РЎС“Р в„–РЎвЂљР Вµ Р Р…Р С•Р Р†РЎвЂ№Р в„– Р С—Р В°РЎР‚Р С•Р В»РЎРЉ",
                                    error: ctx.authTouched.password ? loginErrors.password : "",
                                    onBlur: () => ctx.touchAuthFields(["password"]),
                                    autoComplete: "current-password"
                                }),
                                actionRow([
                                    button(ctx.authBusy ? "Р вЂ™РЎвЂ¦Р С•Р Т‘..." : "Р вЂ™Р С•Р в„–РЎвЂљР С‘", ctx.loginAsUser, "primary-button", "login-submit"),
                                    button("Р вЂ”Р В°РЎР‚Р ВµР С–Р С‘РЎРѓРЎвЂљРЎР‚Р С‘РЎР‚Р С•Р Р†Р В°РЎвЂљРЎРЉРЎРѓРЎРЏ", () => ctx.patchAuthDraft({ mode: "register" }), "secondary-button", "go-register")
                                ], "login-actions")
                            ])
                    ])
                ])
            ])
        ])
    ]);
}

function renderGuestScreen(ctx) {
    return el("div", { className: "page-shell auth-shell" }, [
        el("header", { className: "hero", key: "guest-hero" }, [
            el("div", { className: "hero-grid", key: "guest-grid" }, [
                el("section", { className: "hero-copy", key: "guest-copy" }, [
                    el("p", { className: "eyebrow", key: "guest-eyebrow" }, "Car Sharing"),
                    el("h1", { key: "guest-title" }, "Р вЂ™РЎвЂ№Р В±Р С‘РЎР‚Р В°Р в„–РЎвЂљР Вµ Р В°Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»РЎРЉ Р В·Р В°РЎР‚Р В°Р Р…Р ВµР Вµ"),
                    el("p", { className: "hero-text", key: "guest-text" }, "Р С™Р В°РЎвЂљР В°Р В»Р С•Р С– Р Т‘Р С•РЎРѓРЎвЂљРЎС“Р С—Р ВµР Р… РЎРѓРЎР‚Р В°Р В·РЎС“. Р В§РЎвЂљР С•Р В±РЎвЂ№ Р С•РЎвЂћР С•РЎР‚Р СР С‘РЎвЂљРЎРЉ Р С—Р С•Р ВµР В·Р Т‘Р С”РЎС“, Р С•РЎвЂљР С”РЎР‚РЎвЂ№РЎвЂљРЎРЉ Р С‘РЎРѓРЎвЂљР С•РЎР‚Р С‘РЎР‹ Р В°РЎР‚Р ВµР Р…Р Т‘ Р С‘ Р С—Р С•РЎРѓР СР С•РЎвЂљРЎР‚Р ВµРЎвЂљРЎРЉ Р С—Р В»Р В°РЎвЂљР ВµР В¶Р С‘, Р С—Р С•РЎвЂљРЎР‚Р ВµР В±РЎС“Р ВµРЎвЂљРЎРѓРЎРЏ Р Р†РЎвЂ¦Р С•Р Т‘ Р Р† Р В°Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ."),
                    el("div", { className: "hero-actions", key: "guest-actions" }, [
                        button("Р вЂ™Р С•Р в„–РЎвЂљР С‘", () => {
                            ctx.setAuthDraft((current) => ({ ...current, mode: "login" }));
                            ctx.setEntryMode("auth");
                        }, "primary-button", "guest-login"),
                        button("Р вЂ”Р В°РЎР‚Р ВµР С–Р С‘РЎРѓРЎвЂљРЎР‚Р С‘РЎР‚Р С•Р Р†Р В°РЎвЂљРЎРЉРЎРѓРЎРЏ", () => {
                            ctx.setAuthDraft((current) => ({ ...current, mode: "register" }));
                            ctx.setEntryMode("auth");
                        }, "secondary-button", "guest-register")
                    ]),
                    el("div", { className: "hero-tags", key: "guest-tags" }, [
                        tag("Р С™Р В°РЎвЂљР В°Р В»Р С•Р С– Р В°Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»Р ВµР в„–", "guest-tag-1"),
                        tag("Р вЂќР С•Р С—Р С•Р В»Р Р…Р С‘РЎвЂљР ВµР В»РЎРЉР Р…РЎвЂ№Р Вµ Р С•Р С—РЎвЂ Р С‘Р С‘", "guest-tag-2"),
                        tag("Р вЂєР С‘РЎвЂЎР Р…РЎвЂ№Р в„– Р С”Р В°Р В±Р С‘Р Р…Р ВµРЎвЂљ Р С—Р С•РЎРѓР В»Р Вµ Р Р†РЎвЂ¦Р С•Р Т‘Р В°", "guest-tag-3")
                    ])
                ]),
                el("section", { className: "hero-panel", key: "guest-panel" }, [
                    el("div", { className: "hero-panel-card", key: "guest-info" }, [
                        el("p", { className: "panel-caption", key: "guest-caption" }, "Р вЂќР С•РЎРѓРЎвЂљРЎС“Р С—Р Р…Р С• Р С–Р С•РЎРѓРЎвЂљРЎР‹"),
                        el("div", { className: "feature-stack", key: "guest-feature-stack" }, [
                            heroInfoCard("Р С’Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»Р С‘", `${ctx.data.cars.length} Р СР С•Р Т‘Р ВµР В»Р ВµР в„– Р Т‘Р С•РЎРѓРЎвЂљРЎС“Р С—Р Р…РЎвЂ№ Р Т‘Р В»РЎРЏ Р С—РЎР‚Р С•РЎРѓР СР С•РЎвЂљРЎР‚Р В°.`, "guest-info-1"),
                            heroInfoCard("Р Р€РЎРѓР В»РЎС“Р С–Р С‘", `${ctx.data.services.filter((item) => item.isActive).length} Р В°Р С”РЎвЂљР С‘Р Р†Р Р…РЎвЂ№РЎвЂ¦ Р С•Р С—РЎвЂ Р С‘Р в„– Р СР С•Р В¶Р Р…Р С• Р С‘Р В·РЎС“РЎвЂЎР С‘РЎвЂљРЎРЉ Р В·Р В°РЎР‚Р В°Р Р…Р ВµР Вµ.`, "guest-info-2"),
                            heroInfoCard("Р вЂќР ВµР в„–РЎРѓРЎвЂљР Р†Р С‘РЎРЏ", "Р С›РЎвЂћР С•РЎР‚Р СР В»Р ВµР Р…Р С‘Р Вµ Р С—Р С•Р ВµР В·Р Т‘Р С”Р С‘ Р С‘ Р С‘РЎРѓРЎвЂљР С•РЎР‚Р С‘РЎРЏ РЎРѓРЎвЂљР В°Р Р…РЎС“РЎвЂљ Р Т‘Р С•РЎРѓРЎвЂљРЎС“Р С—Р Р…РЎвЂ№ Р С—Р С•РЎРѓР В»Р Вµ Р Р†РЎвЂ¦Р С•Р Т‘Р В°.", "guest-info-3")
                        ])
                    ])
                ])
            ])
        ]),
        el("section", { className: "content-grid", key: "guest-content" }, [
            el("div", { className: "workspace", key: "guest-workspace" }, [
                listCard("Р С™Р В°РЎвЂљР В°Р В»Р С•Р С–", "Р С’Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»Р С‘", "Р СџРЎР‚Р С•РЎРѓР СР В°РЎвЂљРЎР‚Р С‘Р Р†Р В°Р в„–РЎвЂљР Вµ Р Т‘Р С•РЎРѓРЎвЂљРЎС“Р С—Р Р…РЎвЂ№Р Вµ Р В°Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»Р С‘ Р С‘ РЎС“РЎРѓР В»Р С•Р Р†Р С‘РЎРЏ Р В°РЎР‚Р ВµР Р…Р Т‘РЎвЂ№.", [
                    entityList(ctx.data.cars.map((car) => entityCard({
                        key: `guest-car-${car.id}`,
                        title: `${car.brand} ${car.model}`,
                        subtitle: car.licensePlate,
                        status: car.status,
                        accent: `${formatCurrency(car.pricePerHour)}/РЎвЂЎР В°РЎРѓ`,
                        chips: (car.availableServices || []).slice(0, 3).map((service) => service.name),
                        meta: [`Р вЂњР С•Р Т‘: ${car.year}`],
                        onSelect: null,
                        onEdit: () => {
                            ctx.setAuthDraft((current) => ({ ...current, mode: "login" }));
                            ctx.setEntryMode("auth");
                        },
                        editLabel: "Р вЂ™Р С•Р в„–РЎвЂљР С‘ Р Т‘Р В»РЎРЏ Р В°РЎР‚Р ВµР Р…Р Т‘РЎвЂ№"
                    })), "guest-cars-list")
                ], "guest-cars-card"),
                listCard("Р С›Р С—РЎвЂ Р С‘Р С‘", "Р вЂќР С•Р С—Р С•Р В»Р Р…Р С‘РЎвЂљР ВµР В»РЎРЉР Р…РЎвЂ№Р Вµ РЎС“РЎРѓР В»РЎС“Р С–Р С‘", "Р РЋРЎР‚Р В°Р Р†Р Р…Р С‘РЎвЂљР Вµ Р Т‘Р С•РЎРѓРЎвЂљРЎС“Р С—Р Р…РЎвЂ№Р Вµ Р Т‘Р С•Р С—Р С•Р В»Р Р…Р ВµР Р…Р С‘РЎРЏ Р С—Р ВµРЎР‚Р ВµР Т‘ Р Р†РЎвЂ¦Р С•Р Т‘Р С•Р С Р Р† Р В°Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ.", [
                    entityList(ctx.data.services.filter((service) => service.isActive).map((service) => entityCard({
                        key: `guest-service-${service.id}`,
                        title: service.name,
                        subtitle: service.description || "Р вЂР ВµР В· Р С•Р С—Р С‘РЎРѓР В°Р Р…Р С‘РЎРЏ",
                        status: service.isActive ? "ACTIVE" : "INACTIVE",
                        accent: `${formatCurrency(service.pricePerDay)}/Р Т‘Р ВµР Р…РЎРЉ`,
                        chips: [service.category],
                        meta: [],
                        onSelect: null
                    })), "guest-services-list")
                ], "guest-services-card")
            ]),
            el("aside", { className: "side-panel", key: "guest-side" }, [
                el("section", { className: "relation-card alt", key: "guest-locked" }, [
                    el("p", { className: "section-kicker", key: "guest-locked-kicker" }, "Р СџР С•РЎРѓР В»Р Вµ Р Р†РЎвЂ¦Р С•Р Т‘Р В°"),
                    el("h3", { key: "guest-locked-title" }, "Р В§РЎвЂљР С• Р С•РЎвЂљР С”РЎР‚Р С•Р ВµРЎвЂљРЎРѓРЎРЏ Р Р† Р С”Р В°Р В±Р С‘Р Р…Р ВµРЎвЂљР Вµ"),
                    el("div", { className: "detail-lines", key: "guest-locked-lines" }, [
                        el("div", { className: "detail-line", key: "guest-line-1" }, "Р С›РЎвЂћР С•РЎР‚Р СР В»Р ВµР Р…Р С‘Р Вµ Р С‘ Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬Р ВµР Р…Р С‘Р Вµ Р С—Р С•Р ВµР В·Р Т‘Р С•Р С”"),
                        el("div", { className: "detail-line", key: "guest-line-2" }, "Р ВРЎРѓРЎвЂљР С•РЎР‚Р С‘РЎРЏ Р В°РЎР‚Р ВµР Р…Р Т‘ Р С‘ РЎРѓРЎвЂљР В°РЎвЂљРЎС“РЎРѓРЎвЂ№ Р С—Р В»Р В°РЎвЂљР ВµР В¶Р ВµР в„–"),
                        el("div", { className: "detail-line", key: "guest-line-3" }, "Р вЂєР С‘РЎвЂЎР Р…РЎвЂ№Р в„– Р С”Р В°Р В±Р С‘Р Р…Р ВµРЎвЂљ РЎРѓ Р В°Р С”РЎвЂљРЎС“Р В°Р В»РЎРЉР Р…РЎвЂ№Р СР С‘ Р Т‘Р В°Р Р…Р Р…РЎвЂ№Р СР С‘")
                    ]),
                    actionRow([
                        button("Р вЂ™Р С•Р в„–РЎвЂљР С‘ Р Р† Р В°Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ", () => {
                            ctx.setAuthDraft((current) => ({ ...current, mode: "login" }));
                            ctx.setEntryMode("auth");
                        }, "primary-button", "guest-side-login")
                    ], "guest-side-actions")
                ])
            ])
        ])
    ]);
}

function renderTab(ctx) {
    if (ctx.role === "user") {
        if (ctx.activeTab === "cars") {
            return el("section", { className: "module-grid", key: "user-cars" }, [
                formCard("\u0411\u0440\u043e\u043d\u044c", "\u041d\u043e\u0432\u0430\u044f \u043f\u043e\u0435\u0437\u0434\u043a\u0430", "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0430\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u044c \u0438 \u0434\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u043e\u043f\u0446\u0438\u0438 \u0434\u043b\u044f \u043e\u0444\u043e\u0440\u043c\u043b\u0435\u043d\u0438\u044f \u0430\u0440\u0435\u043d\u0434\u044b.", [
                    el("form", { onSubmit: wrapSubmit(ctx.submitRental), className: "stack-form", key: "user-rental-form" }, [
                        ctx.currentRental ? el("div", { className: "auth-message info", key: "rental-lock" }, "\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0438\u0442\u0435 \u0442\u0435\u043a\u0443\u0449\u0443\u044e \u043f\u043e\u0435\u0437\u0434\u043a\u0443, \u0447\u0442\u043e\u0431\u044b \u043e\u0444\u043e\u0440\u043c\u0438\u0442\u044c \u043d\u043e\u0432\u0443\u044e.") : null,
                        infoLine(`\u041a\u043b\u0438\u0435\u043d\u0442: ${ctx.currentUser ? `${ctx.currentUser.firstName} ${ctx.currentUser.lastName}` : "\u0413\u043e\u0441\u0442\u044c"}`, "user-rental-user"),
                        selectField("\u0410\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u044c", ctx.forms.rental.carId, [{ value: "", label: "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0430\u0432\u0442\u043e" }].concat(ctx.data.cars.filter((item) => item.status === "AVAILABLE").map((item) => ({ value: String(item.id), label: `${item.brand} ${item.model} (${item.licensePlate})` }))), (value) => ctx.patchForm("rental", { carId: value }), "user-rental-car"),
                        checkboxGroup("\u0414\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u043e\u043f\u0446\u0438\u0438", ctx.data.services.filter((item) => item.isActive).map((item) => ({ value: String(item.id), title: item.name, subtitle: `${item.category} • ${formatCurrency(item.pricePerDay)}/\u0434\u0435\u043d\u044c` })), ctx.forms.rental.serviceIds, (value) => ctx.patchForm("rental", { serviceIds: toggleId(ctx.forms.rental.serviceIds, value) }), "user-rental-services"),
                        actionRow([ submitButton("\u041e\u0444\u043e\u0440\u043c\u0438\u0442\u044c \u043f\u043e\u0435\u0437\u0434\u043a\u0443", "user-rental-submit"), button("\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c", () => ctx.resetForm("rental"), "secondary-button", "user-rental-reset") ], "user-rental-actions")
                    ])
                ], "user-booking-card"),
                listCard("\u0410\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u0438", "\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u044b\u0439 \u0430\u0432\u0442\u043e\u043f\u0430\u0440\u043a", "\u041f\u0440\u043e\u0441\u043c\u0430\u0442\u0440\u0438\u0432\u0430\u0439\u0442\u0435 \u043c\u0430\u0448\u0438\u043d\u044b, \u0438\u0445 \u0441\u0442\u0430\u0442\u0443\u0441\u044b \u0438 \u0434\u043e\u0441\u0442\u0443\u043f\u043d\u044b\u0435 \u043e\u043f\u0446\u0438\u0438.", [
                    el("div", { className: "toolbar", key: "user-cars-toolbar" }, [ textField("\u041f\u043e\u0438\u0441\u043a", ctx.filters.cars.query, (v) => ctx.updateFilter("cars", { query: v }), "user-cars-query"), selectField("\u0421\u0442\u0430\u0442\u0443\u0441", ctx.filters.cars.status, ["", "AVAILABLE", "RENTED"], (v) => ctx.updateFilter("cars", { status: v }), "user-cars-status"), textField("\u041c\u0430\u0440\u043a\u0430", ctx.filters.cars.brand, (v) => ctx.updateFilter("cars", { brand: v }), "user-cars-brand"), textField("\u041c\u0430\u043a\u0441. \u0446\u0435\u043d\u0430", ctx.filters.cars.maxPrice, (v) => ctx.updateFilter("cars", { maxPrice: v }), "user-cars-price", "number"), button("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c", () => ctx.resetFilter("cars"), "ghost-button", "user-cars-reset") ]),
                    pagedEntityList(ctx.paginatedCars, (car) => entityCard({ key: `uc-${car.id}`, title: `${car.brand} ${car.model}`, subtitle: `${car.licensePlate} • ${car.year}`, status: car.status, accent: `${formatCurrency(car.pricePerHour)}/\u0447\u0430\u0441`, chips: (car.availableServices || []).map((item) => item.name), meta: [car.status === "AVAILABLE" ? "\u041c\u043e\u0436\u043d\u043e \u0431\u0440\u043e\u043d\u0438\u0440\u043e\u0432\u0430\u0442\u044c" : "\u0421\u0435\u0439\u0447\u0430\u0441 \u0432\u0440\u0435\u043c\u0435\u043d\u043d\u043e \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"], onSelect: () => ctx.setSelected({ type: "cars", id: car.id }) }), "\u041f\u043e \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u043c \u0444\u0438\u043b\u044c\u0442\u0440\u0430\u043c \u043c\u0430\u0448\u0438\u043d \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e.", "user-cars-list", ctx.changePage, "cars")
                ], "user-cars-card")
            ]);
        }
        if (ctx.activeTab === "rentals") {
            return el("section", { className: "module-grid", key: "user-rentals" }, [ formCard("\u0422\u0435\u043a\u0443\u0449\u0430\u044f \u043f\u043e\u0435\u0437\u0434\u043a\u0430", "\u0410\u043a\u0442\u0438\u0432\u043d\u0430\u044f \u0430\u0440\u0435\u043d\u0434\u0430", "\u0417\u0434\u0435\u0441\u044c \u043e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0435\u0442\u0441\u044f \u0432\u0430\u0448\u0430 \u0442\u0435\u043a\u0443\u0449\u0430\u044f \u043f\u043e\u0435\u0437\u0434\u043a\u0430, \u0435\u0441\u043b\u0438 \u043e\u043d\u0430 \u0435\u0441\u0442\u044c.", [ ctx.currentRental ? entityCard({ key: `active-rental-${ctx.currentRental.id}`, title: `\u0410\u0440\u0435\u043d\u0434\u0430 #${ctx.currentRental.id}`, subtitle: ctx.currentRental.carInfo || "\u0410\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u044c", status: ctx.currentRental.status, accent: formatDate(ctx.currentRental.startTime), chips: ctx.currentRental.selectedServices || ["\u0411\u0435\u0437 \u0443\u0441\u043b\u0443\u0433"], meta: [ctx.currentRental.userFullName || ""], onSelect: () => ctx.setSelected({ type: "rentals", id: ctx.currentRental.id }) }) : el("div", { className: "empty-inline", key: "no-current-rental" }, "\u0421\u0435\u0439\u0447\u0430\u0441 \u0443 \u0432\u0430\u0441 \u043d\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u043d\u043e\u0439 \u043f\u043e\u0435\u0437\u0434\u043a\u0438.") ], "current-rental-card"), listCard("\u0418\u0441\u0442\u043e\u0440\u0438\u044f", "\u0412\u0430\u0448\u0438 \u043f\u043e\u0435\u0437\u0434\u043a\u0438", "\u0412\u0441\u0435 \u0442\u0435\u043a\u0443\u0449\u0438\u0435 \u0438 \u0437\u0430\u0432\u0435\u0440\u0448\u0451\u043d\u043d\u044b\u0435 \u043f\u043e\u0435\u0437\u0434\u043a\u0438 \u0432 \u043e\u0434\u043d\u043e\u043c \u0441\u043f\u0438\u0441\u043a\u0435.", [ el("div", { className: "toolbar", key: "user-rentals-toolbar" }, [ textField("\u041f\u043e\u0438\u0441\u043a", ctx.filters.rentals.query, (v) => ctx.updateFilter("rentals", { query: v }), "user-rentals-query"), selectField("\u0421\u0442\u0430\u0442\u0443\u0441", ctx.filters.rentals.status, ["", "ACTIVE", "COMPLETED"], (v) => ctx.updateFilter("rentals", { status: v }), "user-rentals-status"), button("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c", () => ctx.resetFilter("rentals"), "ghost-button", "user-rentals-reset") ]), pagedEntityList(ctx.paginatedRentals, (rental) => entityCard({ key: `ur-${rental.id}`, title: `\u0410\u0440\u0435\u043d\u0434\u0430 #${rental.id}`, subtitle: rental.carInfo || "\u0410\u0432\u0442\u043e\u043c\u043e\u0431\u0438\u043b\u044c", status: rental.status, accent: formatDate(rental.startTime), chips: rental.selectedServices || ["\u0411\u0435\u0437 \u0443\u0441\u043b\u0443\u0433"], meta: [rental.userFullName || ""], onSelect: () => ctx.setSelected({ type: "rentals", id: rental.id }) }), "\u041f\u043e \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u043c \u0444\u0438\u043b\u044c\u0442\u0440\u0430\u043c \u043f\u043e\u0435\u0437\u0434\u043e\u043a \u043d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e.", "user-rentals-list", ctx.changePage, "rentals") ], "user-rentals-card") ]);
        }
        return el("section", { className: "module-grid single-column", key: "user-payments" }, [ listCard("\u041f\u043b\u0430\u0442\u0435\u0436\u0438", "\u0418\u0441\u0442\u043e\u0440\u0438\u044f \u043f\u043b\u0430\u0442\u0435\u0436\u0435\u0439", "\u0417\u0434\u0435\u0441\u044c \u0441\u043e\u0431\u0440\u0430\u043d\u044b \u0442\u043e\u043b\u044c\u043a\u043e \u0432\u0430\u0448\u0438 \u043f\u043b\u0430\u0442\u0435\u0436\u0438 \u043f\u043e \u043f\u043e\u0435\u0437\u0434\u043a\u0430\u043c.", [ el("div", { className: "toolbar", key: "user-payments-toolbar" }, [ textField("\u041f\u043e\u0438\u0441\u043a", ctx.filters.payments.query, (v) => ctx.updateFilter("payments", { query: v }), "user-payments-query"), selectField("\u0421\u0442\u0430\u0442\u0443\u0441", ctx.filters.payments.status, ["", "COMPLETED", "REFUNDED"], (v) => ctx.updateFilter("payments", { status: v }), "user-payments-status"), button("\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c", () => ctx.resetFilter("payments"), "ghost-button", "user-payments-reset") ]), pagedEntityList(ctx.paginatedPayments, (payment) => entityCard({ key: `up-${payment.id}`, title: `\u041f\u043b\u0430\u0442\u0451\u0436 #${payment.id}`, subtitle: `\u0410\u0440\u0435\u043d\u0434\u0430 #${payment.rentalId}`, status: payment.status, accent: formatCurrency(payment.amount), chips: [`\u0410\u0432\u0442\u043e: ${formatCurrency(payment.carAmount)}`, `\u0423\u0441\u043b\u0443\u0433\u0438: ${formatCurrency(payment.servicesAmount)}`], meta: [payment.transactionId || "\u041d\u0435\u0442 transactionId", formatDate(payment.paymentDate)], onSelect: () => ctx.setSelected({ type: "payments", id: payment.id }) }), "\u041f\u043b\u0430\u0442\u0435\u0436\u0438 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u044b.", "user-payments-list", ctx.changePage, "payments") ], "user-payments-card") ]);
    }
    return el("section", { className: "module-grid single-column", key: "admin-placeholder" }, [ el("div", { className: "list-card" }, [ el("div", { className: "section-title" }, [ el("p", { className: "section-kicker" }, "\u041f\u0430\u043d\u0435\u043b\u044c"), el("h3", null, "\u0420\u0430\u0437\u0434\u0435\u043b \u0432 \u0440\u0430\u0431\u043e\u0442\u0435"), el("p", { className: "section-text" }, "\u0421\u0435\u0439\u0447\u0430\u0441 \u0434\u043e\u0447\u0438\u0449\u0430\u044e \u043a\u043e\u0434\u0438\u0440\u043e\u0432\u043a\u0443 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430 \u0432 \u0430\u0434\u043c\u0438\u043d\u0441\u043a\u0438\u0445 \u0432\u043a\u043b\u0430\u0434\u043a\u0430\u0445.") ]) ]) ]);
}function detailView(selected, data) {
    if (!selected) {
        return el("p", { className: "panel-text" }, "Р вЂ™РЎвЂ№Р В±Р ВµРЎР‚Р С‘РЎвЂљР Вµ Р С”Р В°РЎР‚РЎвЂљР С•РЎвЂЎР С”РЎС“, РЎвЂЎРЎвЂљР С•Р В±РЎвЂ№ Р С—Р С•РЎРѓР СР С•РЎвЂљРЎР‚Р ВµРЎвЂљРЎРЉ Р С—Р С•Р Т‘РЎР‚Р С•Р В±Р Р…РЎС“РЎР‹ Р С‘Р Р…РЎвЂћР С•РЎР‚Р СР В°РЎвЂ Р С‘РЎР‹.");
    }

    if (selected.type === "cars") {
        const car = data.cars.find((item) => item.id === selected.id);
        if (!car) {
            return null;
        }
        return detailBlock(`${car.brand} ${car.model}`, [
            `Р вЂњР С•РЎРѓР Р…Р С•Р СР ВµРЎР‚: ${car.licensePlate}`,
            `Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ: ${car.status}`,
            `Р вЂќР С•РЎРѓРЎвЂљРЎС“Р С—Р Р…РЎвЂ№Р Вµ РЎС“РЎРѓР В»РЎС“Р С–Р С‘: ${(car.availableServices || []).map((item) => item.name).join(", ") || "Р Р…Р ВµРЎвЂљ"}`,
            `Р ВРЎРѓРЎвЂљР С•РЎР‚Р С‘РЎРЏ Р В°РЎР‚Р ВµР Р…Р Т‘: ${data.rentals.filter((item) => item.carId === car.id).length}`
        ]);
    }

    if (selected.type === "users") {
        const user = data.users.find((item) => item.id === selected.id);
        if (!user) {
            return null;
        }
        return detailBlock(`${user.firstName} ${user.lastName}`, [
            `Email: ${user.email}`,
            `Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ: ${user.status}`,
            `Р С™Р С•Р В»Р С‘РЎвЂЎР ВµРЎРѓРЎвЂљР Р†Р С• Р В°РЎР‚Р ВµР Р…Р Т‘: ${data.rentals.filter((item) => item.userId === user.id).length}`
        ]);
    }

    if (selected.type === "rentals") {
        const rental = data.rentals.find((item) => item.id === selected.id);
        if (!rental) {
            return null;
        }
        return detailBlock(`Р С’РЎР‚Р ВµР Р…Р Т‘Р В° #${rental.id}`, [
            `Р С™Р В»Р С‘Р ВµР Р…РЎвЂљ: ${rental.userFullName || "Р Р…Р Вµ Р Р…Р В°Р в„–Р Т‘Р ВµР Р…"}`,
            `Р С’Р Р†РЎвЂљР С•: ${rental.carInfo || "Р Р…Р Вµ Р Р…Р В°Р в„–Р Т‘Р ВµР Р…Р С•"}`,
            `Р вЂ™РЎвЂ№Р В±РЎР‚Р В°Р Р…Р Р…РЎвЂ№Р Вµ РЎС“РЎРѓР В»РЎС“Р С–Р С‘: ${(rental.selectedServices || []).join(", ") || "Р Р…Р ВµРЎвЂљ"}`
        ]);
    }

    if (selected.type === "services") {
        const service = data.services.find((item) => item.id === selected.id);
        if (!service) {
            return null;
        }
        return detailBlock(service.name, [
            `Р С™Р В°РЎвЂљР ВµР С–Р С•РЎР‚Р С‘РЎРЏ: ${service.category}`,
            `Р вЂќР С•РЎРѓРЎвЂљРЎС“Р С—Р Р…Р В° Р Т‘Р В»РЎРЏ Р В°Р Р†РЎвЂљР С•: ${data.cars.filter((item) => (item.availableServices || []).some((s) => s.id === service.id)).length}`,
            `Р ВРЎРѓР С—Р С•Р В»РЎРЉР В·Р С•Р Р†Р В°Р Р…Р В° Р Р† Р В°РЎР‚Р ВµР Р…Р Т‘Р В°РЎвЂ¦: ${data.rentals.filter((item) => (item.selectedServices || []).includes(service.name)).length}`
        ]);
    }

    const payment = data.payments.find((item) => item.id === selected.id);
    if (!payment) {
        return null;
    }
    return detailBlock(`Р СџР В»Р В°РЎвЂљР ВµР В¶ #${payment.id}`, [
        `Р С’РЎР‚Р ВµР Р…Р Т‘Р В°: ${payment.rentalId}`,
        `Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ: ${payment.status}`,
        `Р РЋРЎС“Р СР СР В°: ${formatCurrency(payment.amount)}`
    ]);
}

function wrapSubmit(handler) {
    return async (event) => {
        event.preventDefault();
        try {
            await handler(event);
        } catch (error) {
            console.error(error);
        }
    };
}

function showcaseSection(title, rows, key) {
    return el("div", { className: "showcase-block", key }, [
        el("div", { className: "showcase-heading", key: `${key}-title` }, title),
        rows.length ? rows : el("div", { className: "showcase-empty", key: `${key}-empty` }, "Р СџР С•Р С”Р В° Р Р…Р ВµРЎвЂљ Р Т‘Р В°Р Р…Р Р…РЎвЂ№РЎвЂ¦")
    ]);
}

function infoLine(text, key) {
    return el("div", { className: "detail-line auth-note", key }, text);
}

function formCard(kicker, title, text, children, key) {
    return el("div", { className: "form-card", key }, [
        sectionTitle(kicker, title, text, `${key}-title`),
        children
    ]);
}

function listCard(kicker, title, text, children, key) {
    return el("div", { className: "list-card", key }, [
        sectionTitle(kicker, title, text, `${key}-title`),
        children
    ]);
}

function sectionTitle(kicker, title, text, key) {
    return el("div", { className: "section-title", key }, [
        el("p", { className: "section-kicker", key: `${key}-k` }, kicker),
        el("h3", { key: `${key}-h` }, title),
        el("p", { className: "section-text", key: `${key}-t` }, text)
    ]);
}

function formRow(children, key) {
    return el("div", { className: "form-row", key }, children);
}

function actionRow(children, key) {
    return el("div", { className: "button-row", key }, children);
}

function textField(label, value, onChange, key, typeOrOptions = "text") {
    const config = typeof typeOrOptions === "object" ? typeOrOptions : { type: typeOrOptions };
    const type = config.type || "text";
    return el("label", { className: `field ${config.error ? "field-invalid" : ""}`, key }, [
        el("span", { key: `${key}-label` }, label),
        el("input", {
            key: `${key}-input`,
            type,
            value,
            placeholder: config.placeholder,
            autoComplete: config.autoComplete,
            onBlur: config.onBlur,
            onChange: (event) => onChange(event.target.value)
        }),
        config.error
            ? el("small", { className: "field-error", key: `${key}-error` }, config.error)
            : config.hint
                ? el("small", { className: "field-hint", key: `${key}-hint` }, config.hint)
                : null
    ]);
}

function textareaField(label, value, onChange, key) {
    return el("label", { className: "field", key }, [
        el("span", { key: `${key}-label` }, label),
        el("textarea", {
            key: `${key}-textarea`,
            value,
            onChange: (event) => onChange(event.target.value)
        })
    ]);
}

function selectField(label, value, options, onChange, key) {
    const normalized = options.map((option) => typeof option === "object" ? option : { value: option, label: option || "Р вЂ™РЎРѓР Вµ" });
    return el("label", { className: "field", key }, [
        el("span", { key: `${key}-label` }, label),
        el("select", {
            key: `${key}-select`,
            value,
            onChange: (event) => onChange(event.target.value)
        }, normalized.map((option) => el("option", { key: `${key}-${option.value}`, value: option.value }, option.label)))
    ]);
}

function checkboxGroup(label, options, values, onToggle, key) {
    return el("div", { className: "checkbox-block", key }, [
        el("div", { className: "checkbox-label", key: `${key}-label` }, label),
        el("div", { className: "checkbox-list", key: `${key}-list` },
            options.length
                ? options.map((option) => el("label", { className: "checkbox-item", key: `${key}-${option.value}` }, [
                    el("input", {
                        key: `${key}-${option.value}-input`,
                        type: "checkbox",
                        checked: values.includes(option.value),
                        onChange: () => onToggle(option.value)
                    }),
                    el("div", { key: `${key}-${option.value}-copy` }, [
                        el("strong", { key: `${key}-${option.value}-title` }, option.title),
                        el("span", { key: `${key}-${option.value}-subtitle` }, option.subtitle)
                    ])
                ]))
                : el("div", { className: "empty-inline", key: `${key}-empty` }, "Р СњР ВµРЎвЂљ Р Т‘Р С•РЎРѓРЎвЂљРЎС“Р С—Р Р…РЎвЂ№РЎвЂ¦ РЎС“РЎРѓР В»РЎС“Р С–")
        )
    ]);
}

function pagedEntityList(pageData, renderItem, emptyText, key, onPageChange, pageKey) {
    return el("div", { className: "list-stack", key }, [
        entityList(
            pageData.items.length
                ? pageData.items.map(renderItem)
                : [el("div", { className: "empty-inline list-empty", key: `${key}-empty` }, emptyText)],
            `${key}-items`
        ),
        pageData.totalPages > 1
            ? paginationBar(pageData, onPageChange, pageKey, `${key}-pagination`)
            : null
    ]);
}

function entityList(children, key) {
    return el("div", { className: "entity-list", key }, children);
}

function entityCard(props) {
    const chips = props.chips && props.chips.length ? props.chips : ["Р вЂР ВµР В· Р Т‘Р С•Р С—Р С•Р В»Р Р…Р С‘РЎвЂљР ВµР В»РЎРЉР Р…РЎвЂ№РЎвЂ¦ Р Т‘Р В°Р Р…Р Р…РЎвЂ№РЎвЂ¦"];
    const actions = [];
    if (props.onEdit) {
        actions.push(button(props.editLabel || "Р ВР В·Р СР ВµР Р…Р С‘РЎвЂљРЎРЉ", props.onEdit, "ghost-button small", `${props.key}-edit`));
    }
    if (props.onDelete) {
        actions.push(button("Р Р€Р Т‘Р В°Р В»Р С‘РЎвЂљРЎРЉ", props.onDelete, "danger-button small", `${props.key}-delete`));
    }

    return el("article", { className: "entity-card", key: props.key, onClick: props.onSelect }, [
        el("div", { className: "entity-top", key: `${props.key}-top` }, [
            el("div", { key: `${props.key}-copy` }, [
                el("h4", { key: `${props.key}-title` }, props.title),
                el("p", { key: `${props.key}-subtitle` }, props.subtitle)
            ]),
            el("span", { className: statusClass(props.status), key: `${props.key}-status` }, props.status)
        ]),
        el("div", { className: "entity-price", key: `${props.key}-accent` }, props.accent),
        el("div", { className: "chip-row", key: `${props.key}-chips` }, chips.map((chip, index) => el("span", { className: "chip", key: `${props.key}-chip-${index}` }, chip))),
        el("div", { className: "meta-row", key: `${props.key}-meta` }, (props.meta || []).map((meta, index) => el("span", { key: `${props.key}-meta-${index}` }, meta))),
        actions.length
            ? el("div", { className: "button-row compact", key: `${props.key}-actions`, onClick: (event) => event.stopPropagation() }, actions)
            : null
    ]);
}

function detailBlock(title, lines) {
    return el("div", { key: title }, [
        el("div", { className: "detail-title", key: `${title}-title` }, title),
        el("div", { className: "detail-lines", key: `${title}-lines` }, lines.map((line, index) => el("div", { className: "detail-line", key: `${title}-${index}` }, line)))
    ]);
}

function heroInfoCard(title, text, key) {
    return el("div", { className: "hero-info-card", key }, [
        el("div", { className: "hero-info-title", key: `${key}-title` }, title),
        el("div", { className: "hero-info-text", key: `${key}-text` }, text)
    ]);
}

function showcaseRow(title, subtitle, badge, key) {
    return el("div", { className: "showcase-row", key }, [
        el("div", { className: "showcase-copy", key: `${key}-copy` }, [
            el("strong", { key: `${key}-title` }, title),
            el("span", { key: `${key}-subtitle` }, subtitle)
        ]),
        el("span", { className: "showcase-badge", key: `${key}-badge` }, badge)
    ]);
}

function tag(text, key) {
    return el("span", { key }, text);
}

function li(text, key) {
    return el("li", { key }, text);
}

function button(text, onClick, className, key, type = "button") {
    return el("button", { className, onClick, key, type }, text);
}

function submitButton(text, key) {
    return button(text, null, "primary-button", key, "submit");
}

function tabSubtitle(tabId, role) {
    if (role === "user") {
        const map = {
            cars: "Р вЂ™РЎвЂ№Р В±Р С•РЎР‚ Р В°Р Р†РЎвЂљР С•Р СР С•Р В±Р С‘Р В»РЎРЏ",
            rentals: "Р СћР ВµР С”РЎС“РЎвЂ°Р В°РЎРЏ Р С—Р С•Р ВµР В·Р Т‘Р С”Р В° Р С‘ Р С‘РЎРѓРЎвЂљР С•РЎР‚Р С‘РЎРЏ",
            payments: "Р ВРЎРѓРЎвЂљР С•РЎР‚Р С‘РЎРЏ Р С•Р С—Р В»Р В°РЎвЂљ"
        };
        return map[tabId];
    }
    const map = {
        cars: "Р Р€Р С—РЎР‚Р В°Р Р†Р В»Р ВµР Р…Р С‘Р Вµ Р В°Р Р†РЎвЂљР С•Р С—Р В°РЎР‚Р С”Р С•Р С",
        rentals: "Р вЂ™РЎРѓР Вµ Р С—Р С•Р ВµР В·Р Т‘Р С”Р С‘",
        users: "Р вЂР В°Р В·Р В° Р С”Р В»Р С‘Р ВµР Р…РЎвЂљР С•Р Р†",
        services: "Р С›Р С—РЎвЂ Р С‘Р С‘ Р С‘ Р Т‘Р С•Р С—Р С•Р В»Р Р…Р ВµР Р…Р С‘РЎРЏ",
        payments: "Р вЂ™Р С•Р В·Р Р†РЎР‚Р В°РЎвЂљРЎвЂ№ Р С‘ Р С—РЎР‚Р С•Р Р†Р ВµРЎР‚Р С”Р С‘"
    };
    return map[tabId];
}

function statusClass(status) {
    const normalized = String(status || "").toUpperCase();
    if (["AVAILABLE", "ACTIVE", "COMPLETED"].includes(normalized)) {
        return "status-badge success";
    }
    if (["RENTED", "REFUNDED", "BLOCKED", "SERVICE"].includes(normalized)) {
        return "status-badge warning";
    }
    return "status-badge danger";
}

function toggleId(list, value) {
    return list.includes(value) ? list.filter((item) => item !== value) : [...list, value];
}

function paginationBar(pageData, onPageChange, pageKey, key) {
    return el("div", { className: "pagination-bar", key }, [
        el("span", { className: "pagination-meta", key: `${key}-meta` }, `Р РЋРЎвЂљРЎР‚Р В°Р Р…Р С‘РЎвЂ Р В° ${pageData.page} Р С‘Р В· ${pageData.totalPages}`),
        el("div", { className: "pagination-actions", key: `${key}-actions` }, [
            button("Р СњР В°Р В·Р В°Р Т‘", () => onPageChange(pageKey, pageData.page - 1), "ghost-button small", `${key}-prev`),
            button("Р вЂ™Р С—Р ВµРЎР‚РЎвЂР Т‘", () => onPageChange(pageKey, pageData.page + 1), "ghost-button small", `${key}-next`)
        ].map((action, index) => React.cloneElement(action, {
            disabled: index === 0 ? !pageData.hasPrev : !pageData.hasNext
        })))
    ]);
}

function paginateItems(items, page, pageSize) {
    const totalItems = items.length;
    const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
    const safePage = Math.min(Math.max(1, page), totalPages);
    const start = (safePage - 1) * pageSize;
    return {
        page: safePage,
        totalItems,
        totalPages,
        hasPrev: safePage > 1,
        hasNext: safePage < totalPages,
        items: items.slice(start, start + pageSize)
    };
}

function buildAuthValidation(draft) {
    const loginErrors = {};
    const registerErrors = {};

    const loginValue = draft.email.trim();

    if (!loginValue) {
        loginErrors.email = "Р Р€Р С”Р В°Р В¶Р С‘РЎвЂљР Вµ email Р С‘Р В»Р С‘ Р В»Р С•Р С–Р С‘Р Р….";
        registerErrors.email = "Р Р€Р С”Р В°Р В¶Р С‘РЎвЂљР Вµ email.";
    } else if (!EMAIL_REGEX.test(draft.email.trim())) {
        registerErrors.email = "Р вЂ™Р Р†Р ВµР Т‘Р С‘РЎвЂљР Вµ email Р Р† РЎвЂћР С•РЎР‚Р СР В°РЎвЂљР Вµ name@example.com.";
        if (loginValue.includes("@")) {
            loginErrors.email = "Р вЂ™Р Р†Р ВµР Т‘Р С‘РЎвЂљР Вµ email Р Р† РЎвЂћР С•РЎР‚Р СР В°РЎвЂљР Вµ name@example.com.";
        } else if (loginValue.length < 3) {
            loginErrors.email = "Р вЂєР С•Р С–Р С‘Р Р… Р Т‘Р С•Р В»Р В¶Р ВµР Р… РЎРѓР С•Р Т‘Р ВµРЎР‚Р В¶Р В°РЎвЂљРЎРЉ Р СР С‘Р Р…Р С‘Р СРЎС“Р С 3 РЎРѓР С‘Р СР Р†Р С•Р В»Р В°.";
        }
    }

    if (!draft.password) {
        loginErrors.password = "Р вЂ™Р Р†Р ВµР Т‘Р С‘РЎвЂљР Вµ Р С—Р В°РЎР‚Р С•Р В»РЎРЉ.";
        registerErrors.password = "Р вЂ™Р Р†Р ВµР Т‘Р С‘РЎвЂљР Вµ Р С—Р В°РЎР‚Р С•Р В»РЎРЉ.";
    } else if (draft.password.length < 6) {
        loginErrors.password = "Р СџР В°РЎР‚Р С•Р В»РЎРЉ Р Т‘Р С•Р В»Р В¶Р ВµР Р… РЎРѓР С•Р Т‘Р ВµРЎР‚Р В¶Р В°РЎвЂљРЎРЉ Р СР С‘Р Р…Р С‘Р СРЎС“Р С 6 РЎРѓР С‘Р СР Р†Р С•Р В»Р С•Р Р†.";
        registerErrors.password = "Р СџР В°РЎР‚Р С•Р В»РЎРЉ Р Т‘Р С•Р В»Р В¶Р ВµР Р… РЎРѓР С•Р Т‘Р ВµРЎР‚Р В¶Р В°РЎвЂљРЎРЉ Р СР С‘Р Р…Р С‘Р СРЎС“Р С 6 РЎРѓР С‘Р СР Р†Р С•Р В»Р С•Р Р†.";
    }

    if (!draft.firstName.trim()) {
        registerErrors.firstName = "Р Р€Р С”Р В°Р В¶Р С‘РЎвЂљР Вµ Р С‘Р СРЎРЏ.";
    } else if (draft.firstName.trim().length < 2) {
        registerErrors.firstName = "Р ВР СРЎРЏ Р Т‘Р С•Р В»Р В¶Р Р…Р С• РЎРѓР С•Р Т‘Р ВµРЎР‚Р В¶Р В°РЎвЂљРЎРЉ Р СР С‘Р Р…Р С‘Р СРЎС“Р С 2 РЎРѓР С‘Р СР Р†Р С•Р В»Р В°.";
    }

    if (!draft.lastName.trim()) {
        registerErrors.lastName = "Р Р€Р С”Р В°Р В¶Р С‘РЎвЂљР Вµ РЎвЂћР В°Р СР С‘Р В»Р С‘РЎР‹.";
    } else if (draft.lastName.trim().length < 2) {
        registerErrors.lastName = "Р В¤Р В°Р СР С‘Р В»Р С‘РЎРЏ Р Т‘Р С•Р В»Р В¶Р Р…Р В° РЎРѓР С•Р Т‘Р ВµРЎР‚Р В¶Р В°РЎвЂљРЎРЉ Р СР С‘Р Р…Р С‘Р СРЎС“Р С 2 РЎРѓР С‘Р СР Р†Р С•Р В»Р В°.";
    }

    if (!draft.driverLicense.trim()) {
        registerErrors.driverLicense = "Р Р€Р С”Р В°Р В¶Р С‘РЎвЂљР Вµ Р Р…Р С•Р СР ВµРЎР‚ Р Р†Р С•Р Т‘Р С‘РЎвЂљР ВµР В»РЎРЉРЎРѓР С”Р С•Р С–Р С• РЎС“Р Т‘Р С•РЎРѓРЎвЂљР С•Р Р†Р ВµРЎР‚Р ВµР Р…Р С‘РЎРЏ.";
    } else if (draft.driverLicense.trim().length < 5 || draft.driverLicense.trim().length > 20) {
        registerErrors.driverLicense = "Р СњР С•Р СР ВµРЎР‚ Р Р†Р С•Р Т‘Р С‘РЎвЂљР ВµР В»РЎРЉРЎРѓР С”Р С•Р С–Р С• РЎС“Р Т‘Р С•РЎРѓРЎвЂљР С•Р Р†Р ВµРЎР‚Р ВµР Р…Р С‘РЎРЏ Р Т‘Р С•Р В»Р В¶Р ВµР Р… Р В±РЎвЂ№РЎвЂљРЎРЉ Р Т‘Р В»Р С‘Р Р…Р С•Р в„– Р С•РЎвЂљ 5 Р Т‘Р С• 20 РЎРѓР С‘Р СР Р†Р С•Р В»Р С•Р Р†.";
    }

    if (draft.phoneNumber.trim() && !PHONE_REGEX.test(draft.phoneNumber.trim())) {
        registerErrors.phoneNumber = "Р вЂ™Р Р†Р ВµР Т‘Р С‘РЎвЂљР Вµ РЎвЂљР ВµР В»Р ВµРЎвЂћР С•Р Р… Р Р† Р СР ВµР В¶Р Т‘РЎС“Р Р…Р В°РЎР‚Р С•Р Т‘Р Р…Р С•Р С РЎвЂћР С•РЎР‚Р СР В°РЎвЂљР Вµ, Р Р…Р В°Р С—РЎР‚Р С‘Р СР ВµРЎР‚ +375291234567.";
    }

    return {
        login: { errors: loginErrors, isValid: !Object.keys(loginErrors).length },
        register: { errors: registerErrors, isValid: !Object.keys(registerErrors).length }
    };
}

function humanizeAuthError(message) {
    const text = String(message || "");
    if (text.includes("User with email") && text.includes("already exists")) {
        return "Р СџР С•Р В»РЎРЉР В·Р С•Р Р†Р В°РЎвЂљР ВµР В»РЎРЉ РЎРѓ РЎвЂљР В°Р С”Р С‘Р С email РЎС“Р В¶Р Вµ Р В·Р В°РЎР‚Р ВµР С–Р С‘РЎРѓРЎвЂљРЎР‚Р С‘РЎР‚Р С•Р Р†Р В°Р Р…. Р СџР С•Р С—РЎР‚Р С•Р В±РЎС“Р в„–РЎвЂљР Вµ Р Р†Р С•Р в„–РЎвЂљР С‘.";
    }
    if (text.includes("driver license") && text.includes("already exists")) {
        return "Р СџР С•Р В»РЎРЉР В·Р С•Р Р†Р В°РЎвЂљР ВµР В»РЎРЉ РЎРѓ РЎвЂљР В°Р С”Р С‘Р С Р Р†Р С•Р Т‘Р С‘РЎвЂљР ВµР В»РЎРЉРЎРѓР С”Р С‘Р С РЎС“Р Т‘Р С•РЎРѓРЎвЂљР С•Р Р†Р ВµРЎР‚Р ВµР Р…Р С‘Р ВµР С РЎС“Р В¶Р Вµ РЎРѓРЎС“РЎвЂ°Р ВµРЎРѓРЎвЂљР Р†РЎС“Р ВµРЎвЂљ.";
    }
    if (text.includes("User not found")) {
        return "Р С’Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ РЎРѓ РЎвЂљР В°Р С”Р С‘Р С Р В»Р С•Р С–Р С‘Р Р…Р С•Р С Р Р…Р Вµ Р Р…Р В°Р в„–Р Т‘Р ВµР Р…. Р СџРЎР‚Р С•Р Р†Р ВµРЎР‚РЎРЉРЎвЂљР Вµ Р Т‘Р В°Р Р…Р Р…РЎвЂ№Р Вµ Р С‘Р В»Р С‘ Р В·Р В°РЎР‚Р ВµР С–Р С‘РЎРѓРЎвЂљРЎР‚Р С‘РЎР‚РЎС“Р в„–РЎвЂљР ВµРЎРѓРЎРЉ.";
    }
    if (text.includes("Invalid login or password")) {
        return "Р СњР ВµР Р†Р ВµРЎР‚Р Р…РЎвЂ№Р в„– Р В»Р С•Р С–Р С‘Р Р… Р С‘Р В»Р С‘ Р С—Р В°РЎР‚Р С•Р В»РЎРЉ.";
    }
    if (text.includes("User account is not active")) {
        return "Р С’Р С”Р С”Р В°РЎС“Р Р…РЎвЂљ Р Р†РЎР‚Р ВµР СР ВµР Р…Р Р…Р С• Р Р…Р ВµР Т‘Р С•РЎРѓРЎвЂљРЎС“Р С—Р ВµР Р…. Р С›Р В±РЎР‚Р В°РЎвЂљР С‘РЎвЂљР ВµРЎРѓРЎРЉ Р Р† Р С—Р С•Р Т‘Р Т‘Р ВµРЎР‚Р В¶Р С”РЎС“.";
    }
    return text;
}

async function api(url, method = "GET", body) {
    const options = { method, headers: {} };
    if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }

    const response = await fetch(url, options);
    if (!response.ok) {
        const text = await response.text();
        throw new Error(parseError(text, response.status));
    }
    if (response.status === 204) {
        return null;
    }
    const contentType = response.headers.get("content-type") || "";
    return contentType.includes("application/json") ? response.json() : response.text();
}

function parseError(text, status) {
    try {
        const parsed = JSON.parse(text);
        if (Array.isArray(parsed.details) && parsed.details.length) {
            return parsed.message === "Validation failed"
                ? parsed.details.join(" ")
                : `${parsed.message}: ${parsed.details.join(" ")}`;
        }
        return parsed.message || parsed.error || `Request failed with status ${status}`;
    } catch {
        return text || `Request failed with status ${status}`;
    }
}

function toQuery(values) {
    const params = new URLSearchParams();
    Object.entries(values)
        .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== "")
        .forEach(([key, value]) => params.set(key, value));
    const query = params.toString();
    return query ? `?${query}` : "";
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
        return "Р вЂќР В°РЎвЂљР В° Р Р…Р Вµ РЎС“Р С”Р В°Р В·Р В°Р Р…Р В°";
    }
    return new Intl.DateTimeFormat("ru-RU", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

function toClientSession(response) {
    return {
        role: String(response.role || "").toLowerCase(),
        userId: response.userId,
        firstName: response.firstName,
        lastName: response.lastName,
        email: response.email
    };
}

function isUnauthorized(error) {
    const text = String(error?.message || "");
    return text.includes("401") || text.toLowerCase().includes("unauthorized");
}

window.addEventListener("error", (event) => {
    if (!rootNode.innerHTML.trim()) {
        rootNode.innerHTML = `<pre style="padding:24px;background:#111;color:#fff;white-space:pre-wrap;">${escapeHtml(event.message || String(event.error || ""))}</pre>`;
    }
});

window.addEventListener("unhandledrejection", (event) => {
    if (!rootNode.innerHTML.trim()) {
        rootNode.innerHTML = `<pre style="padding:24px;background:#111;color:#fff;white-space:pre-wrap;">${escapeHtml(String(event.reason || ""))}</pre>`;
    }
});

function escapeHtml(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;");
}

try {
    ReactDOM.render(el(App), rootNode);
} catch (error) {
    rootNode.innerHTML = `<pre style="padding:24px;background:#111;color:#fff;white-space:pre-wrap;">${escapeHtml(error.stack || error.message || String(error))}</pre>`;
}
