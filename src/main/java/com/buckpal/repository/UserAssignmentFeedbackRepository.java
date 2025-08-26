package com.buckpal.repository;

import com.buckpal.entity.User;
import com.buckpal.entity.UserAssignmentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserAssignmentFeedbackRepository extends JpaRepository<UserAssignmentFeedback, Long> {
    
    List<UserAssignmentFeedback> findByUser(User user);
    
    List<UserAssignmentFeedback> findByUserOrderByCreatedAtDesc(User user);
    
    List<UserAssignmentFeedback> findByTransactionId(Long transactionId);
    
    List<UserAssignmentFeedback> findByUserAndWasAccepted(User user, Boolean wasAccepted);
    
    @Query("SELECT uaf FROM UserAssignmentFeedback uaf WHERE uaf.user = :user " +
           "AND uaf.patternMatched = :pattern")
    List<UserAssignmentFeedback> findByUserAndPatternMatched(
        @Param("user") User user,
        @Param("pattern") String pattern);
    
    @Query("SELECT uaf FROM UserAssignmentFeedback uaf WHERE uaf.user = :user " +
           "AND uaf.suggestedCategoryName = :categoryName")
    List<UserAssignmentFeedback> findByUserAndSuggestedCategory(
        @Param("user") User user,
        @Param("categoryName") String categoryName);
    
    @Query("SELECT uaf FROM UserAssignmentFeedback uaf WHERE uaf.user = :user " +
           "AND uaf.userChosenCategoryName = :categoryName")
    List<UserAssignmentFeedback> findByUserAndChosenCategory(
        @Param("user") User user,
        @Param("categoryName") String categoryName);
    
    @Query("SELECT COUNT(uaf) FROM UserAssignmentFeedback uaf WHERE uaf.user = :user " +
           "AND uaf.wasAccepted = true")
    Long countAcceptedFeedbackByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(uaf) FROM UserAssignmentFeedback uaf WHERE uaf.user = :user " +
           "AND uaf.wasAccepted = false")
    Long countRejectedFeedbackByUser(@Param("user") User user);
    
    @Query("SELECT uaf FROM UserAssignmentFeedback uaf WHERE uaf.user = :user " +
           "AND uaf.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY uaf.createdAt DESC")
    List<UserAssignmentFeedback> findByUserAndDateRange(
        @Param("user") User user,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT uaf.suggestedCategoryName as category, " +
           "COUNT(uaf) as totalSuggestions, " +
           "SUM(CASE WHEN uaf.wasAccepted = true THEN 1 ELSE 0 END) as acceptedCount " +
           "FROM UserAssignmentFeedback uaf WHERE uaf.user = :user " +
           "GROUP BY uaf.suggestedCategoryName " +
           "ORDER BY totalSuggestions DESC")
    List<Object[]> getCategoryAccuracyStats(@Param("user") User user);
}