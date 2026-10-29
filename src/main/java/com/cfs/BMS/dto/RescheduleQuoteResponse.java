package com.cfs.BMS.dto;

public record RescheduleQuoteResponse(
        double oldTotal,
        double newTotal,
        double difference, // positive = to pay, negative = refund to points, 0 = free
        int userRewardPoints,
        String keyId,
        String razorpayOrderId,
        int amountToPayInPaise
) {}