package com.cfs.BMS.entity;

import com.cfs.BMS.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="show_id",nullable = false)
    private Show show;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "booking_seats",
            joinColumns=@JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "seat_id")
    )
    private List<Seat> seats;

    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime bookedAt;

    //new add
    @Builder.Default
    private Integer pointsRedeemed = 0;

    @Builder.Default
    private Integer pointsEarned = 10;

    @PrePersist
    private void onCreate()
    {
        this.bookedAt=LocalDateTime.now();
        if(this.status==null)
        {
            this.status=BookingStatus.CONFIRMED;
        }
    }

}
