package com.buckpal.repository;

import com.buckpal.entity.MerchantPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantPatternRepository extends JpaRepository<MerchantPattern, Long> {
    
    Optional<MerchantPattern> findByPatternAndCategoryName(String pattern, String categoryName);
    
    @Query("SELECT mp FROM MerchantPattern mp WHERE LOWER(:merchantText) LIKE LOWER(CONCAT('%', mp.pattern, '%')) " +
           "AND mp.confidenceScore >= :minConfidence " +
           "ORDER BY mp.specificityScore DESC, mp.confidenceScore DESC")
    List<MerchantPattern> findMatchingPatternsWithMinConfidence(
        @Param("merchantText") String merchantText,
        @Param("minConfidence") BigDecimal minConfidence);
    
    List<MerchantPattern> findByCategoryIdIsNull();
    
    Optional<MerchantPattern> findByPatternAndCategoryId(String pattern, Long categoryId);
}