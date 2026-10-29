package com.cfs.BMS.dto;

import java.util.List;

public record FoodPaymentVerificationRequest(
        Long bookingId,
        Long userId,
        String seatNumber,
        String deliveryTime,
        List<FoodOrderItemRequest> items,
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
) {}