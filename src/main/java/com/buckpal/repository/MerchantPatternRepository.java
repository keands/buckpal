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
    
    List<MerchantPattern> findByPatternContainingIgnoreCase(String pattern);
    
    Optional<MerchantPattern> findByPatternAndCategoryName(String pattern, String categoryName);
    
    List<MerchantPattern> findByCategoryNameOrderBySpecificityScoreDescConfidenceScoreDesc(String categoryName);
    
    @Query("SELECT mp FROM MerchantPattern mp WHERE LOWER(:merchantText) LIKE LOWER(CONCAT('%', mp.pattern, '%')) " +
           "ORDER BY mp.specificityScore DESC, mp.confidenceScore DESC")
    List<MerchantPattern> findMatchingPatterns(@Param("merchantText") String merchantText);
    
    @Query("SELECT mp FROM MerchantPattern mp WHERE LOWER(:merchantText) LIKE LOWER(CONCAT('%', mp.pattern, '%')) " +
           "AND mp.confidenceScore >= :minConfidence " +
           "ORDER BY mp.specificityScore DESC, mp.confidenceScore DESC")
    List<MerchantPattern> findMatchingPatternsWithMinConfidence(
        @Param("merchantText") String merchantText,
        @Param("minConfidence") BigDecimal minConfidence);
    
    @Query("SELECT mp FROM MerchantPattern mp WHERE mp.specificityScore >= :minSpecificity " +
           "ORDER BY mp.specificityScore DESC, mp.confidenceScore DESC")
    List<MerchantPattern> findByMinSpecificityScore(@Param("minSpecificity") Integer minSpecificity);
    
    @Query("SELECT mp FROM MerchantPattern mp WHERE mp.confidenceScore >= :minConfidence " +
           "ORDER BY mp.confidenceScore DESC, mp.specificityScore DESC")
    List<MerchantPattern> findByMinConfidenceScore(@Param("minConfidence") BigDecimal minConfidence);
    
    @Query("SELECT DISTINCT mp.categoryName FROM MerchantPattern mp ORDER BY mp.categoryName")
    List<String> findAllDistinctCategoryNames();
    
    @Query("SELECT COUNT(mp) FROM MerchantPattern mp WHERE mp.categoryName = :categoryName")
    Long countByCategoryName(@Param("categoryName") String categoryName);
    
    @Query("SELECT AVG(mp.confidenceScore) FROM MerchantPattern mp WHERE mp.categoryName = :categoryName")
    BigDecimal getAverageConfidenceForCategory(@Param("categoryName") String categoryName);
    
    @Query("SELECT mp FROM MerchantPattern mp WHERE mp.totalMatches = 0 ORDER BY mp.createdAt DESC")
    List<MerchantPattern> findUntestedPatterns();
    
    @Query("SELECT mp FROM MerchantPattern mp WHERE mp.correctMatches < mp.totalMatches * :minAccuracy " +
           "AND mp.totalMatches >= :minMatches ORDER BY mp.confidenceScore ASC")
    List<MerchantPattern> findLowAccuracyPatterns(
        @Param("minAccuracy") BigDecimal minAccuracy,
        @Param("minMatches") Integer minMatches);
    
    // New methods for categoryId support
    List<MerchantPattern> findByCategoryIdIsNull();
    
    Optional<MerchantPattern> findByPatternAndCategoryId(String pattern, Long categoryId);
    
    List<MerchantPattern> findByCategoryIdOrderBySpecificityScoreDescConfidenceScoreDesc(Long categoryId);
    
    @Query("SELECT DISTINCT mp.categoryId FROM MerchantPattern mp WHERE mp.categoryId IS NOT NULL ORDER BY mp.categoryId")
    List<Long> findAllDistinctCategoryIds();
    
    @Query("SELECT COUNT(mp) FROM MerchantPattern mp WHERE mp.categoryId = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("SELECT AVG(mp.confidenceScore) FROM MerchantPattern mp WHERE mp.categoryId = :categoryId")
    BigDecimal getAverageConfidenceForCategoryId(@Param("categoryId") Long categoryId);
}