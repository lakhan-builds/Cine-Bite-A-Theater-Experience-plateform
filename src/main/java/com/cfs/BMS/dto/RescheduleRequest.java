package com.cfs.BMS.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleRequest {
    private Long newShowId;
    private List<Long> newSeatIds;
    private Boolean useRewardPoints;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}