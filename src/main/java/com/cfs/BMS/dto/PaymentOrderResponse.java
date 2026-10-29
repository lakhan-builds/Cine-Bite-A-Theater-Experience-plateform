package com.cfs.BMS.dto;

public record PaymentOrderResponse(
        String keyId,
        String orderId,
        int amountInPaise,
        String currency,
        double amountInRupees
) {}