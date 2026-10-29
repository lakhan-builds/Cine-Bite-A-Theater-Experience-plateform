package com.cfs.BMS.repository;

import com.cfs.BMS.entity.Booking;
import com.cfs.BMS.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking,Long> {

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.show s " +
            "LEFT JOIN FETCH s.movie " +
            "LEFT JOIN FETCH s.screen sc " +
            "LEFT JOIN FETCH sc.theater t " +
            "LEFT JOIN FETCH t.city " +
            "LEFT JOIN FETCH b.seats " +
            "WHERE b.user.id = :userId")
  List<Booking> findByUserId(Long userId);
  List<Booking> findByShowId(Long showId);

  //find all seat ids that are already booked for given show
  @Query("SELECT s.id FROM Booking b JOIN b.seats s WHERE b.show.id=:showId AND b.status='CONFIRMED'")
  List<Long> findBookedSeatIdsByShowId(@Param("showId") Long showId);

    // Inside BookingRepository.java
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.show s " +
            "LEFT JOIN FETCH s.movie " +
            "LEFT JOIN FETCH s.screen sc " +
            "LEFT JOIN FETCH sc.theater t " +
            "LEFT JOIN FETCH t.city " +
            "LEFT JOIN FETCH b.seats " +
            "ORDER BY b.id DESC")
    List<Booking> findAllWithDetails();
}
