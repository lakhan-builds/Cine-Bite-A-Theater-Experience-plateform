package com.cfs.BMS.service;

import com.cfs.BMS.dto.FoodOrderItemRequest;
import com.cfs.BMS.dto.FoodOrderRequest;
import com.cfs.BMS.dto.FoodPaymentOrderResponse;
import com.cfs.BMS.dto.FoodPaymentVerificationRequest;
import com.cfs.BMS.entity.FoodItem;
import com.cfs.BMS.entity.FoodOrder;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class FoodPaymentService {

    private final RazorpayClient razorpayClient;
    private final FoodItemService foodItemService;
    private final FoodOrderService foodOrderService;
    private final String keyId;
    private final String secret;

    public FoodPaymentService(
            RazorpayClient razorpayClient,
            FoodItemService foodItemService,
            FoodOrderService foodOrderService,
            @Value("${app.razorpay.api.key-id}") String keyId,
            @Value("${app.razorpay.api.key-secret}") String secret
    ) {
        this.razorpayClient = razorpayClient;
        this.foodItemService = foodItemService;
        this.foodOrderService = foodOrderService;
        this.keyId = keyId;
        this.secret = secret;
    }

    public FoodPaymentOrderResponse createFoodOrderPayment(List<FoodOrderItemRequest> items) throws RazorpayException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty");
        }

        double totalAmount = 0.0;
        for (FoodOrderItemRequest itemReq : items) {
            if (itemReq.getQuantity() != null && itemReq.getQuantity() > 0) {
                FoodItem foodItem = foodItemService.getFoodItemById(itemReq.getFoodItemId());
                totalAmount += foodItem.getPrice() * itemReq.getQuantity();
            }
        }

        int amountInPaise = (int) Math.round(totalAmount * 100);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "food_rcpt_" + System.currentTimeMillis());
        orderRequest.put("payment_capture", 1);

        Order order = razorpayClient.orders.create(orderRequest);

        return new FoodPaymentOrderResponse(
                keyId,
                order.get("id"),
                amountInPaise,
                "INR",
                totalAmount
        );
    }

    @Transactional
    public FoodOrder verifyAndPlaceFoodOrder(FoodPaymentVerificationRequest request) {
        if (!isValidSignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new IllegalArgumentException("Invalid Razorpay payment signature for food order");
        }

        FoodOrderRequest orderReq = new FoodOrderRequest(
                request.bookingId(),
                request.userId(),
                request.seatNumber(),
                request.deliveryTime(),
                request.items()
        );

        return foodOrderService.createOrder(orderReq);
    }

    private boolean isValidSignature(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            String expectedSignature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Error verifying Razorpay food payment signature", ex);
        }
    }
}