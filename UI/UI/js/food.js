let userBookings = [];
let activeBooking = null;
let foodItems = [];
let cart = {}; // { foodItemId: quantity }

document.addEventListener('DOMContentLoaded', initFoodPage);

async function initFoodPage() {
    const user = getLoggedInUser();
    if (!user) {
        document.getElementById('booking-context-container').innerHTML = `
            <div class="empty-state">
                <div class="icon">🔐</div>
                <p>Please <a href="login.html" style="color:var(--primary);font-weight:700;">login</a> to order food.</p>
            </div>`;
        return;
    }

    try {
        const bookings = await BookingAPI.getByUser(user.id);
        userBookings = (bookings || []).filter(b => b.status === 'CONFIRMED');

        if (userBookings.length === 0) {
            document.getElementById('booking-context-container').innerHTML = `
                <div class="empty-state">
                    <div class="icon">🎟️</div>
                    <h3 style="color:var(--white);">No Active Movie Bookings Found</h3>
                    <p style="margin-top:0.5rem; color:var(--text-muted);">You can only order snacks after booking a movie ticket.</p>
                    <a href="movies.html" class="btn btn-primary" style="margin-top:1.2rem;">Book Movies Now</a>
                </div>`;
            return;
        }

        const urlParams = new URLSearchParams(window.location.search);
        const reqBookingId = urlParams.get('bookingId');
        if (reqBookingId) {
            activeBooking = userBookings.find(b => b.id == reqBookingId);
        }
        if (!activeBooking) {
            activeBooking = userBookings[0];
        }

        renderBookingBanner();
        document.getElementById('food-catalog-section').style.display = 'block';
        loadFoodCatalog();
    } catch (e) {
        document.getElementById('booking-context-container').innerHTML = 
            '<div class="empty-state"><p>Error verifying bookings</p></div>';
    }
}

function renderBookingBanner() {
    const show = activeBooking.show || {};
    const screen = show.screen || {};
    const theater = screen.theater || {};

    document.getElementById('booking-context-container').innerHTML = `
        <div class="booking-banner">
            <div>
                <span class="badge" style="background:rgba(46,204,113,0.2);color:var(--success);margin-bottom:0.4rem;">Delivering To Your Booking</span>
                <h3 style="color:var(--white);margin:0.2rem 0;">🎬 ${show.movie?.title || 'Movie'}</h3>
                <p style="color:var(--text-muted);font-size:0.85rem;">
                    🏛️ ${theater.name || 'Theater'} (${screen.name || ''}) &nbsp;|&nbsp; 📅 ${show.showDate} 🕐 ${show.startTime}
                </p>
            </div>
            <div>
                <label style="font-size:0.8rem;color:var(--text-muted);display:block;margin-bottom:0.3rem;">Switch Booking:</label>
                <select onchange="onSwitchBooking(this.value)" style="padding:0.4rem 0.8rem;border-radius:6px;background:var(--bg-dark);color:var(--text);border:1px solid rgba(255,255,255,0.2);">
                    ${userBookings.map(b => `<option value="${b.id}" ${b.id === activeBooking.id ? 'selected' : ''}>${b.show?.movie?.title} (${b.show?.showDate})</option>`).join('')}
                </select>
            </div>
        </div>
    `;
}

function onSwitchBooking(newBookingId) {
    activeBooking = userBookings.find(b => b.id == newBookingId);
    cart = {};
    updateCartBar();
    renderBookingBanner();
    renderFoodGrid();
}

async function loadFoodCatalog() {
    try {
        foodItems = await FoodAPI.getAvailableItems();
        renderFoodGrid();
    } catch (e) {
        showEmpty('food-grid', 'Failed to load menu items');
    }
}

function renderFoodGrid() {
    const grid = document.getElementById('food-grid');
    if (foodItems.length === 0) {
        showEmpty('food-grid', 'No snacks available right now');
        return;
    }

    grid.innerHTML = foodItems.map(item => `
        <div class="food-card">
            <img class="food-img" src="${item.imageUrl || '../assets/default-snack.png'}" alt="${item.name}" onerror="this.src='https://images.unsplash.com/photo-1572177812156-58036aae439c?w=400'">
            <div class="food-body">
                <span class="badge badge-genre" style="font-size:0.7rem;margin-bottom:0.4rem;">${item.category || 'SNACK'}</span>
                <h3 style="color:var(--white);font-size:1.05rem;">${item.name}</h3>
                <p style="color:var(--text-muted);font-size:0.85rem;margin-top:0.3rem;">${item.description || ''}</p>
            </div>
            <div class="food-footer">
                <span style="font-size:1.15rem;font-weight:700;color:var(--success);">₹${item.price}</span>
                <div class="qty-counter">
                    <button class="btn btn-sm btn-outline" onclick="changeQty(${item.id}, -1)">-</button>
                    <span id="qty-${item.id}" style="font-weight:700;min-width:18px;text-align:center;">${cart[item.id] || 0}</span>
                    <button class="btn btn-sm btn-primary" onclick="changeQty(${item.id}, 1)">+</button>
                </div>
            </div>
        </div>
    `).join('');
}

function changeQty(foodId, delta) {
    const current = cart[foodId] || 0;
    const next = Math.max(0, current + delta);
    if (next === 0) {
        delete cart[foodId];
    } else {
        cart[foodId] = next;
    }

    const span = document.getElementById(`qty-${foodId}`);
    if (span) span.textContent = next;
    updateCartBar();
}

function updateCartBar() {
    const ids = Object.keys(cart);
    const count = ids.reduce((acc, id) => acc + cart[id], 0);

    const bar = document.getElementById('cart-bar');
    if (count === 0) {
        bar.style.display = 'none';
        return;
    }

    let total = 0;
    ids.forEach(id => {
        const item = foodItems.find(f => f.id == id);
        if (item) total += item.price * cart[id];
    });

    bar.style.display = 'flex';
    document.getElementById('cart-item-count').textContent = `${count} item${count > 1 ? 's' : ''} in cart`;
    document.getElementById('cart-total-price').textContent = `₹${total}`;
}

function openCheckoutModal() {
    const seatSelect = document.getElementById('seat-select');
    if (activeBooking.seats && activeBooking.seats.length > 0) {
        seatSelect.innerHTML = activeBooking.seats.map(s => `
            <option value="${s.seatNumber}">Seat ${s.seatNumber} (${s.seatType || 'REGULAR'})</option>
        `).join('');
    } else {
        seatSelect.innerHTML = '<option value="Current Seat">Current Seat</option>';
    }

    let summaryHtml = '';
    let total = 0;
    Object.keys(cart).forEach(id => {
        const item = foodItems.find(f => f.id == id);
        const sub = item.price * cart[id];
        total += sub;
        summaryHtml += `<div class="summary-row"><span>${item.name} (x${cart[id]})</span><span>₹${sub}</span></div>`;
    });
    summaryHtml += `<div class="summary-row"><span>Total</span><span>₹${total}</span></div>`;
    document.getElementById('order-summary-content').innerHTML = summaryHtml;

    openModal('checkout-modal');
}

async function placeFoodOrder() {
    const user = getLoggedInUser();
    if (!user) {
        showToast('Please login to place order', 'error');
        return;
    }

    // const seat = document.getElementById('seat-select').value;
    // const time = document.getElementById('delivery-time-select').value;
    // const payBtn = document.getElementById('food-pay-btn');

    const seat = document.getElementById('seat-select')?.value || 'Current Seat';
    const timeElement = document.getElementById('delivery-time-select');
    const time = timeElement ? timeElement.value : 'In 20 mins';

    const payBtn = document.getElementById('food-pay-btn');

    const items = Object.keys(cart).map(id => ({
        foodItemId: parseInt(id),
        quantity: cart[id]
    }));

    if (items.length === 0) {
        showToast('Cart is empty', 'error');
        return;
    }

    payBtn.disabled = true;
    payBtn.textContent = 'Opening Payment Gateway...';

    try {
        // 1. Create Razorpay order on backend
        const orderData = await FoodAPII.createPaymentOrder(items);

        // 2. Open Razorpay Checkout modal
        const options = {
            key: orderData.keyId,
            amount: orderData.amountInPaise,
            currency: orderData.currency,
            name: "BookMyShow Snacks",
            description: `Cinema Seat Delivery - ${seat}`,
            order_id: orderData.orderId,
            handler: async function (response) {
                showToast('Verifying payment...', 'info');  
                try {
                    await FoodAPII.verifyAndOrder({
                        bookingId: activeBooking.id,
                        userId: user.id,
                        seatNumber: seat,
                        deliveryTime: time,
                        items: items,
                        razorpayOrderId: response.razorpay_order_id,
                        razorpayPaymentId: response.razorpay_payment_id,
                        razorpaySignature: response.razorpay_signature
                    });

                    showToast('🎉 Payment successful! Snacks ordered to seat ' + seat, 'success');
                    closeModal('checkout-modal');
                    setTimeout(() => window.location.href = 'bookings.html', 1200);
                } catch (err) {
                    showToast(err.message, 'error');
                    payBtn.disabled = false;
                    payBtn.textContent = 'Pay & Confirm Order';
                }
            },
            prefill: {
                name: user.name,
                email: user.email,
                contact: user.phone || ""
            },
            theme: {
                color: "#e23744"
            },
            modal: {
                ondismiss: function () {
                    payBtn.disabled = false;
                    payBtn.textContent = 'Pay & Confirm Order';
                    showToast('Payment cancelled', 'info');
                }
            }
        };

        const rzp = new Razorpay(options);
        rzp.open();

    } catch (e) {
        showToast(e.message, 'error');
        payBtn.disabled = false;
        payBtn.textContent = 'Pay & Confirm Order';
    }
}