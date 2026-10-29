package com.cfs.BMS.dto;

import java.util.List;

public record BookingPaymentVerificationRequest(
        Long userId,
        Long showId,
        List<Long> seatIds,
        Boolean useRewardPoints,
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
) {}