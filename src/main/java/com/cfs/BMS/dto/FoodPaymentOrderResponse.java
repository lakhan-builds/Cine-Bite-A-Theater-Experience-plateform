package com.cfs.BMS.dto;

public record FoodPaymentOrderResponse(
        String keyId,
        String orderId,
        int amountInPaise,
        String currency,
        double amountInRupees
) {}