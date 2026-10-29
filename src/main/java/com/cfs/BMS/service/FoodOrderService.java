package com.cfs.BMS.service;

import com.cfs.BMS.dto.FoodOrderItemRequest;
import com.cfs.BMS.dto.FoodOrderRequest;
import com.cfs.BMS.entity.*;
import com.cfs.BMS.enums.BookingStatus;
import com.cfs.BMS.repository.FoodOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodItemService foodItemService;
    private final UserService userService;
    private final BookingService bookingService;

    @Transactional
    public FoodOrder createOrder(FoodOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Cart cannot be empty");
        }

        User user = userService.getUserById(request.getUserId());
        Booking booking = bookingService.getBookingById(request.getBookingId());

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Cannot order food for a cancelled booking");
        }

        FoodOrder order = FoodOrder.builder()
                .booking(booking)
                .user(user)
                .seatNumber(request.getSeatNumber().trim().toUpperCase())
                .deliveryTime(request.getDeliveryTime())
                .status("CONFIRMED")
                .build();

        double totalAmount = 0.0;
        List<FoodOrderItem> orderItems = new ArrayList<>();

        for (FoodOrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() != null && itemReq.getQuantity() > 0) {
                FoodItem foodItem = foodItemService.getFoodItemById(itemReq.getFoodItemId());
                totalAmount += foodItem.getPrice() * itemReq.getQuantity();

                orderItems.add(FoodOrderItem.builder()
                        .foodOrder(order)
                        .foodItem(foodItem)
                        .quantity(itemReq.getQuantity())
                        .price(foodItem.getPrice())
                        .build());
            }
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        return foodOrderRepository.save(order);
    }

    public List<FoodOrder> getOrdersByUser(Long userId) {
        return foodOrderRepository.findByUserIdOrderByOrderedAtDesc(userId);
    }

    public FoodOrder getOrderById(Long id) {
        return foodOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food order not found with id: " + id));
    }

    // Inside FoodOrderService.java
    public List<FoodOrder> getAllOrders() {
        return foodOrderRepository.findAllWithDetails();
    }
}