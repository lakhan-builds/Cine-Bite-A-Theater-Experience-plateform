package com.cfs.BMS.controller;

import com.cfs.BMS.dto.BookingPaymentVerificationRequest;
import com.cfs.BMS.dto.PaymentOrderResponse;
import com.cfs.BMS.dto.RescheduleQuoteResponse;
import com.cfs.BMS.entity.Booking;
import com.cfs.BMS.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @RequestParam Long userId,
            @RequestParam Long showId,
            @RequestParam List<Long> seatIds,
            @RequestParam(defaultValue = "false") Boolean useRewardPoints
    ) throws RazorpayException {
        return ResponseEntity.ok(paymentService.createBookingOrder(userId, showId, seatIds, useRewardPoints));
    }

    @PostMapping("/verify-and-book")
    public ResponseEntity<Booking> verifyAndBook(@RequestBody BookingPaymentVerificationRequest request) {
        return ResponseEntity.ok(paymentService.verifyAndConfirmBooking(request));
    }

    @GetMapping("/reschedule-quote")
    public ResponseEntity<RescheduleQuoteResponse> getRescheduleQuote(
            @RequestParam Long bookingId,
            @RequestParam Long newShowId,
            @RequestParam int seatCount,
            @RequestParam(defaultValue = "false") Boolean useRewardPoints
    ) throws RazorpayException {
        return ResponseEntity.ok(paymentService.getRescheduleQuote(bookingId, newShowId, seatCount, useRewardPoints));
    }
}