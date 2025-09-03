package com.buckpal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_patterns")
public class MerchantPattern {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "pattern", nullable = false)
    private String pattern;
    
    @Column(name = "category_name", length = 100)
    private String categoryName; // Legacy field for compatibility
    
    @Column(name = "category_id")
    private Long categoryId; // New field for category mapping system
    
    @NotNull
    @Column(name = "specificity_score", nullable = false)
    private Integer specificityScore = 1;
    
    @NotNull
    @Column(name = "confidence_score", precision = 3, scale = 2, nullable = false)
    private BigDecimal confidenceScore = new BigDecimal("0.8");
    
    @Column(name = "total_matches", nullable = false)
    private Integer totalMatches = 0;
    
    @Column(name = "correct_matches", nullable = false)
    private Integer correctMatches = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public MerchantPattern() {}
    
    public MerchantPattern(String pattern, String categoryName, Integer specificityScore) {
        this.pattern = pattern;
        this.categoryName = categoryName;
        this.specificityScore = specificityScore;
    }
    
    public MerchantPattern(String pattern, Long categoryId, Integer specificityScore) {
        this.pattern = pattern;
        this.categoryId = categoryId;
        this.specificityScore = specificityScore;
    }
    
    // Business methods
    public BigDecimal getAccuracyRate() {
        if (totalMatches == 0) return BigDecimal.ZERO;
        return new BigDecimal(correctMatches)
            .divide(new BigDecimal(totalMatches), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    public void recordMatch(boolean wasCorrect) {
        totalMatches++;
        if (wasCorrect) {
            correctMatches++;
        }
        
        // Update confidence score based on accuracy
        BigDecimal accuracy = getAccuracyRate();
        this.confidenceScore = accuracy.multiply(new BigDecimal("0.9"))
            .add(new BigDecimal("0.1")); // Minimum 10% confidence
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    
    public Integer getSpecificityScore() { return specificityScore; }
    public void setSpecificityScore(Integer specificityScore) { this.specificityScore = specificityScore; }
    
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public Integer getTotalMatches() { return totalMatches; }
    public void setTotalMatches(Integer totalMatches) { this.totalMatches = totalMatches; }
    
    public Integer getCorrectMatches() { return correctMatches; }
    public void setCorrectMatches(Integer correctMatches) { this.correctMatches = correctMatches; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}