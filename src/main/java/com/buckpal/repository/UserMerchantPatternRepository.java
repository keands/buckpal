package com.buckpal.repository;

import com.buckpal.entity.User;
import com.buckpal.entity.UserMerchantPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMerchantPatternRepository extends JpaRepository<UserMerchantPattern, Long> {
    
    /**
     * Trouve le meilleur pattern personnel pour un utilisateur et un texte marchand
     * Ordonnés par confiance décroissante puis par usage décroissant
     */
    @Query("SELECT ump FROM UserMerchantPattern ump WHERE ump.user = :user " +
           "AND UPPER(:merchantText) LIKE CONCAT('%', ump.pattern, '%') " +
           "ORDER BY ump.confidenceScore DESC, ump.usageCount DESC, ump.lastUsedAt DESC")
    List<UserMerchantPattern> findMatchingPatterns(@Param("user") User user, 
                                                  @Param("merchantText") String merchantText);
    
    /**
     * Trouve le pattern exact pour un utilisateur, pattern et catégorie
     */
    Optional<UserMerchantPattern> findByUserAndPatternAndCategoryId(User user, String pattern, Long categoryId);
    
    /**
     * Trouve tous les patterns d'un utilisateur pour une catégorie
     */
    List<UserMerchantPattern> findByUserAndCategoryIdOrderByConfidenceScoreDesc(User user, Long categoryId);
    
    /**
     * Trouve tous les patterns d'un utilisateur ordonnés par usage
     */
    List<UserMerchantPattern> findByUserOrderByUsageCountDescLastUsedAtDesc(User user);
    
    /**
     * Trouve les patterns d'un utilisateur avec confiance élevée
     */
    @Query("SELECT ump FROM UserMerchantPattern ump WHERE ump.user = :user " +
           "AND ump.confidenceScore >= :minConfidence " +
           "ORDER BY ump.confidenceScore DESC")
    List<UserMerchantPattern> findByUserWithHighConfidence(@Param("user") User user, 
                                                           @Param("minConfidence") java.math.BigDecimal minConfidence);
    
    /**
     * Compte les patterns d'un utilisateur par source
     */
    @Query("SELECT ump.source, COUNT(ump) FROM UserMerchantPattern ump " +
           "WHERE ump.user = :user GROUP BY ump.source")
    List<Object[]> countPatternsBySource(@Param("user") User user);
    
    /**
     * Trouve les patterns récemment utilisés (derniers 30 jours)
     */
    @Query("SELECT ump FROM UserMerchantPattern ump WHERE ump.user = :user " +
           "AND ump.lastUsedAt >= :since " +
           "ORDER BY ump.lastUsedAt DESC")
    List<UserMerchantPattern> findRecentlyUsed(@Param("user") User user, 
                                              @Param("since") LocalDateTime since);
    
    /**
     * Trouve les patterns candidats pour apprentissage automatique
     * (patterns avec beaucoup d'usage mais faible confiance)
     */
    @Query("SELECT ump FROM UserMerchantPattern ump WHERE ump.user = :user " +
           "AND ump.usageCount >= :minUsage " +
           "AND ump.confidenceScore < :maxConfidence " +
           "ORDER BY ump.usageCount DESC")
    List<UserMerchantPattern> findCandidatesForImprovement(@Param("user") User user,
                                                           @Param("minUsage") Integer minUsage,
                                                           @Param("maxConfidence") java.math.BigDecimal maxConfidence);
    
    /**
     * Supprime les patterns peu utilisés et anciens (nettoyage)
     */
    @Query("DELETE FROM UserMerchantPattern ump WHERE ump.user = :user " +
           "AND ump.usageCount <= :maxUsage " +
           "AND ump.lastUsedAt < :before")
    void deleteUnusedPatterns(@Param("user") User user,
                             @Param("maxUsage") Integer maxUsage,
                             @Param("before") LocalDateTime before);
    
    /**
     * Statistiques globales pour un utilisateur
     */
    @Query("SELECT COUNT(ump), AVG(ump.confidenceScore), SUM(ump.usageCount) " +
           "FROM UserMerchantPattern ump WHERE ump.user = :user")
    Object[] getUserPatternStats(@Param("user") User user);
}