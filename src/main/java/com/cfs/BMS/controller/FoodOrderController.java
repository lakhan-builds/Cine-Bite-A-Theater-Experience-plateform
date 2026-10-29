package com.cfs.BMS.controller;

import com.cfs.BMS.dto.FoodOrderRequest;
import com.cfs.BMS.entity.FoodOrder;
import com.cfs.BMS.service.FoodOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food-orders")
@RequiredArgsConstructor
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    @PostMapping
    public ResponseEntity<FoodOrder> createOrder(@RequestBody FoodOrderRequest request) {
        return ResponseEntity.ok(foodOrderService.createOrder(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FoodOrder>> getOrdersByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(foodOrderService.getOrdersByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodOrder> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(foodOrderService.getOrderById(id));
    }

    // Inside FoodOrderController.java
    @GetMapping
    public ResponseEntity<List<FoodOrder>> getAllOrders() {
        return ResponseEntity.ok(foodOrderService.getAllOrders());
    }
}