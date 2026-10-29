//package com.cfs.BMS.repository;
//
//import com.cfs.BMS.entity.FoodOrder;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
//    List<FoodOrder> findByUserIdOrderByOrderedAtDesc(Long userId);
//
//    @Query("SELECT DISTINCT fo FROM FoodOrder fo " +
//            "LEFT JOIN FETCH fo.user " +
//            "LEFT JOIN FETCH fo.items i " +
//            "LEFT JOIN FETCH i.foodItem " +
//            "WHERE fo.user.id = :userId ORDER BY fo.id DESC")
//    List<FoodOrder> findByUserIdWithItems(Long userId);
//
//    @Query("SELECT DISTINCT fo FROM FoodOrder fo " +
//            "LEFT JOIN FETCH fo.user " +
//            "LEFT JOIN FETCH fo.items i " +
//            "LEFT JOIN FETCH i.foodItem " +
//            "ORDER BY fo.id DESC")
//    List<FoodOrder> findAllWithDetails();
//}
package com.cfs.BMS.repository;

import com.cfs.BMS.entity.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    List<FoodOrder> findByUserIdOrderByOrderedAtDesc(Long userId);
    @Query("SELECT DISTINCT fo FROM FoodOrder fo " +
            "LEFT JOIN FETCH fo.user " +
            "LEFT JOIN FETCH fo.booking " +
            "LEFT JOIN FETCH fo.items i " +
            "LEFT JOIN FETCH i.foodItem " +
            "WHERE fo.user.id = :userId ORDER BY fo.id DESC")
    List<FoodOrder> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("SELECT DISTINCT fo FROM FoodOrder fo " +
            "LEFT JOIN FETCH fo.user " +
            "LEFT JOIN FETCH fo.booking " +
            "LEFT JOIN FETCH fo.items i " +
            "LEFT JOIN FETCH i.foodItem " +
            "ORDER BY fo.id DESC")
    List<FoodOrder> findAllWithDetails();
}