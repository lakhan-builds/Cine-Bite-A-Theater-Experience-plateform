// ===== API BASE URL =====
const API = 'http://localhost:8080/api';

// ===== GENERIC FETCH HELPERS =====
async function apiGet(endpoint) {
    const res = await fetch(`${API}${endpoint}`);
    if (!res.ok) {
        const err = await res.json().catch(() => ({ message: res.statusText }));
        throw new Error(err.message || 'Request failed');
    }
    return res.json();
}

async function apiPost(endpoint, data) {
    const res = await fetch(`${API}${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    if (!res.ok) {
        const err = await res.json().catch(() => ({ message: res.statusText }));
        throw new Error(err.message || 'Request failed');
    }
    return res.json();
}

async function apiPut(endpoint, data) {
    const res = await fetch(`${API}${endpoint}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: data ? JSON.stringify(data) : undefined
    });
    if (!res.ok) {
        const err = await res.json().catch(() => ({ message: res.statusText }));
        throw new Error(err.message || 'Request failed');
    }
    return res.json();
}

async function apiDelete(endpoint) {
    const res = await fetch(`${API}${endpoint}`, { method: 'DELETE' });
    if (!res.ok) {
        const err = await res.json().catch(() => ({ message: res.statusText }));
        throw new Error(err.message || 'Request failed');
    }
    // might return text
    const text = await res.text();
    try { return JSON.parse(text); } catch { return text; }
}

// ===== USER APIs =====
const UserAPI = {
    register: (data) => apiPost('/users/register', data),
    login: (data) => apiPost('/users/login', data),
    getAll: () => apiGet('/users'),
    getById: (id) => apiGet(`/users/${id}`)
};

// ===== CITY APIs =====
const CityAPI = {
    add: (data) => apiPost('/cities', data),
    getAll: () => apiGet('/cities'),
    getById: (id) => apiGet(`/cities/${id}`),
    delete: (id) => apiDelete(`/cities/${id}`)
};

// ===== MOVIE APIs =====
const MovieAPI = {
    add: (data) => apiPost('/movies', data),
    getAll: () => apiGet('/movies'),
    getById: (id) => apiGet(`/movies/${id}`),
    search: (title) => apiGet(`/movies/search?title=${encodeURIComponent(title)}`),
    getByGenre: (genre) => apiGet(`/movies/genre/${genre}`),
    getByLanguage: (lang) => apiGet(`/movies/language/${lang}`),
    update: (id, data) => apiPut(`/movies/${id}`, data),
    delete: (id) => apiDelete(`/movies/${id}`)
};

// ===== THEATER APIs =====
const TheaterAPI = {
    add: (data) => apiPost('/theaters', data),
    getAll: () => apiGet('/theaters'),
    getById: (id) => apiGet(`/theaters/${id}`),
    getByCity: (cityId) => apiGet(`/theaters/city/${cityId}`),
    delete: (id) => apiDelete(`/theaters/${id}`) // <--- Added
};

// ===== SCREEN APIs =====
const ScreenAPI = {
    add: (data) => apiPost('/screens', data),
    getAll: () => apiGet('/screens'),
    getById: (id) => apiGet(`/screens/${id}`),
    getByTheater: (theaterId) => apiGet(`/screens/theater/${theaterId}`),
    delete: (id) => apiDelete(`/screens/${id}`) // <--- Added
};

// ===== SEAT APIs =====
const SeatAPI = {
    add: (data) => apiPost('/seats', data),
    getByScreen: (screenId) => apiGet(`/seats/screen/${screenId}`),
    getById: (id) => apiGet(`/seats/${id}`),
    delete: (id) => apiDelete(`/seats/${id}`) // <--- Added
};

// ===== SHOW APIs =====
const ShowAPI = {
    add: (data) => apiPost('/shows', data),
    getAll: () => apiGet('/shows'),
    getById: (id) => apiGet(`/shows/${id}`),
    getByMovie: (movieId) => apiGet(`/shows/movie/${movieId}`),
    getByMovieAndDate: (movieId, date) => apiGet(`/shows/movie/${movieId}/date?date=${date}`),
    delete: (id) => apiDelete(`/shows/${id}`) // <--- Added
};

// ===== BOOKING APIs =====
const BookingAPI = {
    getAll: () => apiGet('/bookings'),
    create: (data) => apiPost('/bookings', data),
    getById: (id) => apiGet(`/bookings/${id}`),
    getByUser: (userId) => apiGet(`/bookings/user/${userId}`),
    cancel: (id) => apiPut(`/bookings/${id}/cancel`),
    getAvailableSeats: (showId) => apiGet(`/bookings/show/${showId}/available-seats`),
    reschedule: (id, data) => apiPut(`/bookings/${id}/reschedule`, data) // Added
};

// ===== FOOD ITEM & FOOD ORDER APIs =====
const FoodAPI = {
    getAvailableItems: () => apiGet('/food-items'),
    getAllItems: () => apiGet('/food-items/all'),
    addItem: (data) => apiPost('/food-items', data),
    deleteItem: (id) => apiDelete(`/food-items/${id}`),
    placeOrder: (data) => apiPost('/food-orders', data),
    getOrdersByUser: (userId) => apiGet(`/food-orders/user/${userId}`),
    getOrderById: (id) => apiGet(`/food-orders/${id}`)
};


// Append to BMS/UI/UI/js/api.js[cite: 46]
const PaymentAPI = {
    createOrder: (userId, showId, seatIds, useRewardPoints) => {
        const query = new URLSearchParams({
            userId: userId,
            showId: showId,
            seatIds: seatIds.join(','),
            useRewardPoints: useRewardPoints
        });
        return apiPost(`/payments/create-order?${query.toString()}`);
    },
    verifyAndBook: (data) => apiPost('/payments/verify-and-book', data),
    getRescheduleQuote: (bookingId, newShowId, seatCount, useRewardPoints) => {
        return apiGet(`/payments/reschedule-quote?bookingId=${bookingId}&newShowId=${newShowId}&seatCount=${seatCount}&useRewardPoints=${useRewardPoints}`);
    }
};

// Add into FoodAPI in js/api.js
const FoodAPII = {
    getAvailableItems: () => apiGet('/food-items'),
    getAllItems: () => apiGet('/food-items/all'),
    addItem: (data) => apiPost('/food-items', data),
    deleteItem: (id) => apiDelete(`/food-items/${id}`),
    placeOrder: (data) => apiPost('/food-orders', data),
    getAllOrders: () => apiGet('/food-orders'), // <--- Added
    getOrdersByUser: (userId) => apiGet(`/food-orders/user/${userId}`),
    getOrderById: (id) => apiGet(`/food-orders/${id}`),
    // Razorpay Integration
    createPaymentOrder: (items) => apiPost('/food-payments/create-order', items),
    verifyAndOrder: (data) => apiPost('/food-payments/verify-and-order', data)
};

// In js/api.js
PaymentAPI.getRescheduleQuote = (bookingId, newShowId, seatCount, useRewardPoints) => {
    return apiGet(`/payments/reschedule-quote?bookingId=${bookingId}&newShowId=${newShowId}&seatCount=${seatCount}&useRewardPoints=${useRewardPoints}`);
};