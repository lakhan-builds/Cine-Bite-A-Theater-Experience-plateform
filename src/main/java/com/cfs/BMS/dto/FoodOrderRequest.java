package com.cfs.BMS.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FoodOrderRequest {
    private Long bookingId;
    private Long userId;
    private String seatNumber;
    private String deliveryTime;
    private List<FoodOrderItemRequest> items;
}