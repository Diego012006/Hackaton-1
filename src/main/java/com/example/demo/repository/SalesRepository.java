package com.example.demo.repository;



import java.time.LocalDateTime;
import java.util.List;

public interface SalesRepository extends JpaRepository<Sale, String> {

    @Query("SELECT s FROM Sale s WHERE s.soldAt BETWEEN :from AND :to")
    List<Sale> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Sale> findByBranch(String branch);
}
