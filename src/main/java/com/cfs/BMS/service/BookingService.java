package com.cfs.BMS.service;

import com.cfs.BMS.dto.BookingRequest;
import com.cfs.BMS.dto.RescheduleRequest;
import com.cfs.BMS.entity.*;
import com.cfs.BMS.enums.BookingStatus;
import com.cfs.BMS.repository.BookingRepository;
import com.cfs.BMS.repository.SeatRepository;
import com.cfs.BMS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final UserService userService;
    private final ShowService showService;
    private final UserRepository userRepository;
//seee it
    @Value("${app.razorpay.api.key-secret}")
    private String razorpaySecret;


    private boolean isValidSignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null) return false;
        String payload = orderId + "|" + paymentId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(razorpaySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            String expectedSignature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Error verifying payment signature", ex);
        }
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {
        User user = userService.getUserById(request.getUserId());
        Show show = showService.getShowById(request.getShowId());

        // 1. Seat validation[cite: 37]
        List<Long> alreadyBookedSeats = bookingRepository.findBookedSeatIdsByShowId(show.getId());
        for (Long seatId : request.getSeatIds()) {
            if (alreadyBookedSeats.contains(seatId)) {
                throw new RuntimeException("Seat with id " + seatId + " is already Booked");
            }
        }

        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new RuntimeException("Some Seats Are Invalid");
        }

        // 2. Base price calculation[cite: 37]
        double basePrice = seats.size() * show.getTicketPrice();
        int currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        int pointsToRedeem = 0;

        // 3. Apply reward points discount (1 point = ₹1)
        if (Boolean.TRUE.equals(request.getUseRewardPoints()) && currentPoints > 0) {
            pointsToRedeem = (int) Math.min(currentPoints, basePrice);
            basePrice -= pointsToRedeem;
            user.setRewardPoints(currentPoints - pointsToRedeem);
        }

        // 4. Award +10 points for the new booking
        int pointsEarned = 10;
        user.setRewardPoints(user.getRewardPoints() + pointsEarned);
        userRepository.save(user);

        // 5. Save booking[cite: 37]
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .seats(seats)
                .totalPrice(basePrice)
                .pointsRedeemed(pointsToRedeem)
                .pointsEarned(pointsEarned)
                .status(BookingStatus.CONFIRMED)
                .build();

        return bookingRepository.save(booking);
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    public List<Booking> getBookingByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Transactional
    public Booking cancelbooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Rollback points on cancellation:
        // Restore redeemed points and remove earned points
        User user = booking.getUser();
        int userPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        int restored = booking.getPointsRedeemed() != null ? booking.getPointsRedeemed() : 0;
        int earned = booking.getPointsEarned() != null ? booking.getPointsEarned() : 0;

        user.setRewardPoints(Math.max(0, userPoints + restored - earned));
        userRepository.save(user);

        return bookingRepository.save(booking);
    }

    public List<Seat> getAvailableSeats(Long showId) {
        Show show = showService.getShowById(showId);
        List<Seat> allSeats = seatRepository.findByScreenId(show.getScreen().getId());
        List<Long> bookingSeatIds = bookingRepository.findBookedSeatIdsByShowId(showId);
        return allSeats.stream()
                .filter(seat -> !bookingSeatIds.contains(seat.getId()))
                .toList();
    }

    // Inside BookingService.java

    @Transactional
    public Booking rescheduleBooking(Long bookingId, RescheduleRequest request) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Cancelled bookings cannot be rescheduled.");
        }

        Show currentShow = booking.getShow();
        Show newShow = showService.getShowById(request.getNewShowId());

        // 1. Validate same theater[cite: 18, 21]
        Long currentTheaterId = currentShow.getScreen().getTheater().getId();
        Long targetTheaterId = newShow.getScreen().getTheater().getId();
        if (!currentTheaterId.equals(targetTheaterId)) {
            throw new RuntimeException("Rescheduling only allowed within the same theater.");
        }

        // 2. Validate seat count[cite: 18]
        if (request.getNewSeatIds().size() != booking.getSeats().size()) {
            throw new RuntimeException("You must select exactly " + booking.getSeats().size() + " seat(s).");
        }

        // 3. Validate seat availability
        List<Long> bookedSeatIds = bookingRepository.findBookedSeatIdsByShowId(newShow.getId());
        for (Long seatId : request.getNewSeatIds()) {
            if (bookedSeatIds.contains(seatId)) {
                throw new RuntimeException("Seat with id " + seatId + " is already booked.");
            }
        }

        List<Seat> newSeats = seatRepository.findAllById(request.getNewSeatIds());

        // 4. Price Calculations
        double oldTotal = booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0;
        double newTotal = newSeats.size() * (newShow.getTicketPrice() != null ? newShow.getTicketPrice() : 0.0);
        double difference = newTotal - oldTotal;

        User user = booking.getUser();
        int currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;

        // Scenario A: New show is MORE expensive
        if (difference > 0) {
            double payable = difference;
            if (Boolean.TRUE.equals(request.getUseRewardPoints()) && currentPoints > 0) {
                int pointsToDeduct = (int) Math.min(currentPoints, payable);
                payable -= pointsToDeduct;
                currentPoints -= pointsToDeduct;
            }

            // If amount remaining, verify Razorpay signature
            if (payable > 0) {
                if (request.getRazorpayOrderId() == null || request.getRazorpayPaymentId() == null) {
                    throw new RuntimeException("Payment required for price difference.");
                }
                // Signature verification using HMAC-SHA256
                String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
                // Remove paymentService.
                // Remove paymentService.
                if (!isValidSignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature())) {
                    throw new RuntimeException("Payment verification failed for price difference.");
                }
            }
            user.setRewardPoints(currentPoints);
            userRepository.save(user);

            // Scenario B: New show is CHEAPER (Refund difference to Reward Points)
        } else if (difference < 0) {
            int refundPoints = (int) Math.abs(difference);
            user.setRewardPoints(currentPoints + refundPoints);
            userRepository.save(user);
        }
        // Scenario C: difference == 0 -> No financial changes

        // 5. Update and persist booking[cite: 18]
        booking.setShow(newShow);
        booking.setSeats(newSeats);
        booking.setTotalPrice(newTotal);

        return bookingRepository.save(booking);
    }

    // Inside BookingService.java
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllWithDetails();
    }
}