package com.cfs.BMS.controller;

import com.cfs.BMS.dto.FoodOrderItemRequest;
import com.cfs.BMS.dto.FoodPaymentOrderResponse;
import com.cfs.BMS.dto.FoodPaymentVerificationRequest;
import com.cfs.BMS.entity.FoodOrder;
import com.cfs.BMS.service.FoodPaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food-payments")
@RequiredArgsConstructor
public class FoodPaymentController {

    private final FoodPaymentService foodPaymentService;

    @PostMapping("/create-order")
    public ResponseEntity<FoodPaymentOrderResponse> createOrder(@RequestBody List<FoodOrderItemRequest> items) throws RazorpayException {
        return ResponseEntity.ok(foodPaymentService.createFoodOrderPayment(items));
    }

    @PostMapping("/verify-and-order")
    public ResponseEntity<FoodOrder> verifyAndOrder(@RequestBody FoodPaymentVerificationRequest request) {
        return ResponseEntity.ok(foodPaymentService.verifyAndPlaceFoodOrder(request));
    }
}