package com.cfs.BMS.service;

import com.cfs.BMS.dto.BookingPaymentVerificationRequest;
import com.cfs.BMS.dto.BookingRequest;
import com.cfs.BMS.dto.PaymentOrderResponse;
import com.cfs.BMS.dto.RescheduleQuoteResponse;
import com.cfs.BMS.entity.Booking;
import com.cfs.BMS.entity.Show;
import com.cfs.BMS.entity.User;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final ShowService showService;
    private final UserService userService;
    private final BookingService bookingService;
    private final String keyId;
    private final String secret;



    public PaymentService(
            RazorpayClient razorpayClient,
            ShowService showService,
            UserService userService,
            BookingService bookingService,
            @Value("${app.razorpay.api.key-id}") String keyId,
            @Value("${app.razorpay.api.key-secret}") String secret
    ) {
        this.razorpayClient = razorpayClient;
        this.showService = showService;
        this.userService = userService;
        this.bookingService = bookingService;
        this.keyId = keyId;
        this.secret = secret;
    }

    public PaymentOrderResponse createBookingOrder(Long userId, Long showId, List<Long> seatIds, Boolean useRewardPoints) throws RazorpayException {
        Show show = showService.getShowById(showId);
        double totalTicketPrice = seatIds.size() * (show.getTicketPrice() != null ? show.getTicketPrice() : 0.0);

        if (Boolean.TRUE.equals(useRewardPoints)) {
            User user = userService.getUserById(userId);
            int points = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
            totalTicketPrice = Math.max(0, totalTicketPrice - points);
        }

        int amountInPaise = (int) Math.round(totalTicketPrice * 100);

        // If total price after rewards is 0, no Razorpay order is needed
        if (amountInPaise <= 0) {
            return new PaymentOrderResponse(keyId, "FREE_ORDER", 0, "INR", 0.0);
        }

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "show_rcpt_" + System.currentTimeMillis());
        orderRequest.put("payment_capture", 1);

        Order order = razorpayClient.orders.create(orderRequest);

        return new PaymentOrderResponse(
                keyId,
                order.get("id"),
                amountInPaise,
                "INR",
                totalTicketPrice
        );
    }

    @Transactional
    public Booking verifyAndConfirmBooking(BookingPaymentVerificationRequest request) {
        // Skip signature check if amount was zero
        if (!"FREE_ORDER".equals(request.razorpayOrderId())) {
            if (!isValidSignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
                throw new IllegalArgumentException("Razorpay payment signature verification failed");
            }
        }

        // Finalize the booking in database
        BookingRequest bookingReq = new BookingRequest(
                request.userId(),
                request.showId(),
                request.seatIds(),
                request.useRewardPoints()
        );
        return bookingService.createBooking(bookingReq);
    }

    private boolean isValidSignature(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            String expectedSignature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Error verifying payment signature", ex);
        }
    }
    // Inside PaymentService.java

    public RescheduleQuoteResponse getRescheduleQuote(Long bookingId, Long newShowId, int seatCount, Boolean useRewardPoints) throws RazorpayException {
        Booking booking = bookingService.getBookingById(bookingId);
        Show newShow = showService.getShowById(newShowId);

        double oldTotal = booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0;
        double newTotal = seatCount * (newShow.getTicketPrice() != null ? newShow.getTicketPrice() : 0.0);
        double rawDifference = newTotal - oldTotal;

        User user = booking.getUser();
        int rewardPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;

        double payableDifference = rawDifference;
        if (payableDifference > 0 && Boolean.TRUE.equals(useRewardPoints) && rewardPoints > 0) {
            int pointsUsed = (int) Math.min(rewardPoints, payableDifference);
            payableDifference -= pointsUsed;
        }

        String rzpOrderId = null;
        int amountInPaise = (int) Math.round(payableDifference * 100);

        if (payableDifference > 0) {
            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", amountInPaise);
            orderReq.put("currency", "INR");
            orderReq.put("receipt", "resched_" + System.currentTimeMillis());
            orderReq.put("payment_capture", 1);

            Order order = razorpayClient.orders.create(orderReq);
            rzpOrderId = order.get("id");
        }

        return new RescheduleQuoteResponse(
                oldTotal,
                newTotal,
                rawDifference,
                rewardPoints,
                keyId,
                rzpOrderId,
                amountInPaise
        );
    }
}