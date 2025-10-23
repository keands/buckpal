package com.buckpal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Patterns de marchands personnalisés par utilisateur
 * Priorité supérieure aux patterns globaux
 */
@Entity
@Table(name = "user_merchant_patterns", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "pattern", "category_id"}))
public class UserMerchantPattern {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank
    @Column(name = "pattern", nullable = false, length = 100)
    private String pattern;
    
    @NotNull
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    // Statistiques personnelles d'usage
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 1;
    
    @Column(name = "success_count", nullable = false)
    private Integer successCount = 1;
    
    @Column(name = "confidence_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal confidenceScore = new BigDecimal("0.90");
    
    // Source de création du pattern
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private PatternSource source = PatternSource.MANUAL;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Source de création du pattern
    public enum PatternSource {
        MANUAL("Assignation manuelle"),
        LEARNED("Apprentissage automatique"), 
        CONFIRMED("Confirmé par feedback");
        
        private final String description;
        
        PatternSource(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUsedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastUsedAt = LocalDateTime.now();
    }
    
    // Constructors
    public UserMerchantPattern() {}
    
    public UserMerchantPattern(User user, String pattern, Long categoryId, PatternSource source) {
        this.user = user;
        this.pattern = pattern.toUpperCase().trim();
        this.categoryId = categoryId;
        this.source = source;
    }
    
    // Business methods
    public BigDecimal getAccuracyRate() {
        if (usageCount == 0) return BigDecimal.ZERO;
        return new BigDecimal(successCount)
            .divide(new BigDecimal(usageCount), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    public void recordUsage(boolean wasSuccessful) {
        usageCount++;
        if (wasSuccessful) {
            successCount++;
        }
        
        // Mettre à jour la confiance basée sur l'accuracy
        BigDecimal accuracy = getAccuracyRate();
        this.confidenceScore = accuracy.multiply(new BigDecimal("0.9"))
            .add(new BigDecimal("0.1")); // Minimum 10% de confiance
        
        lastUsedAt = LocalDateTime.now();
    }
    
    public boolean isHighConfidence() {
        return confidenceScore.compareTo(new BigDecimal("0.7")) >= 0;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern != null ? pattern.toUpperCase().trim() : null; }
    
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public PatternSource getSource() { return source; }
    public void setSource(PatternSource source) { this.source = source; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}