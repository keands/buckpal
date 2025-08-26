package com.buckpal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_assignment_feedback")
public class UserAssignmentFeedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @Column(name = "suggested_category_name", nullable = false, length = 100)
    private String suggestedCategoryName;
    
    @NotNull
    @Column(name = "user_chosen_category_name", nullable = false, length = 100)
    private String userChosenCategoryName;
    
    @NotNull
    @Column(name = "was_accepted", nullable = false)
    private Boolean wasAccepted;
    
    @Column(name = "confidence_score_at_time", precision = 3, scale = 2)
    private java.math.BigDecimal confidenceScoreAtTime;
    
    @Column(name = "pattern_matched", length = 255)
    private String patternMatched;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public UserAssignmentFeedback() {}
    
    public UserAssignmentFeedback(Transaction transaction, User user, String suggestedCategoryName, 
                                 String userChosenCategoryName, Boolean wasAccepted) {
        this.transaction = transaction;
        this.user = user;
        this.suggestedCategoryName = suggestedCategoryName;
        this.userChosenCategoryName = userChosenCategoryName;
        this.wasAccepted = wasAccepted;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getSuggestedCategoryName() { return suggestedCategoryName; }
    public void setSuggestedCategoryName(String suggestedCategoryName) { this.suggestedCategoryName = suggestedCategoryName; }
    
    public String getUserChosenCategoryName() { return userChosenCategoryName; }
    public void setUserChosenCategoryName(String userChosenCategoryName) { this.userChosenCategoryName = userChosenCategoryName; }
    
    public Boolean getWasAccepted() { return wasAccepted; }
    public void setWasAccepted(Boolean wasAccepted) { this.wasAccepted = wasAccepted; }
    
    public java.math.BigDecimal getConfidenceScoreAtTime() { return confidenceScoreAtTime; }
    public void setConfidenceScoreAtTime(java.math.BigDecimal confidenceScoreAtTime) { this.confidenceScoreAtTime = confidenceScoreAtTime; }
    
    public String getPatternMatched() { return patternMatched; }
    public void setPatternMatched(String patternMatched) { this.patternMatched = patternMatched; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}