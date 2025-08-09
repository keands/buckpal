package com.buckpal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "csv_mapping_templates", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "bank_name"}))
public class CsvMappingTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "bank_name", nullable = false)
    private String bankName;
    
    @NotNull
    @Column(name = "date_column_index")
    private Integer dateColumnIndex;
    
    @Column(name = "amount_column_index")
    private Integer amountColumnIndex;
    
    @Column(name = "debit_column_index")
    private Integer debitColumnIndex;
    
    @Column(name = "credit_column_index")
    private Integer creditColumnIndex;
    
    @Column(name = "description_column_index")
    private Integer descriptionColumnIndex;
    
    @Column(name = "category_column_index")
    private Integer categoryColumnIndex;
    
    @Size(max = 50)
    @Column(name = "date_format")
    private String dateFormat;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public CsvMappingTemplate() {}
    
    public CsvMappingTemplate(String bankName, User user) {
        this.bankName = bankName;
        this.user = user;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    
    public Integer getDateColumnIndex() { return dateColumnIndex; }
    public void setDateColumnIndex(Integer dateColumnIndex) { this.dateColumnIndex = dateColumnIndex; }
    
    public Integer getAmountColumnIndex() { return amountColumnIndex; }
    public void setAmountColumnIndex(Integer amountColumnIndex) { this.amountColumnIndex = amountColumnIndex; }
    
    public Integer getDebitColumnIndex() { return debitColumnIndex; }
    public void setDebitColumnIndex(Integer debitColumnIndex) { this.debitColumnIndex = debitColumnIndex; }
    
    public Integer getCreditColumnIndex() { return creditColumnIndex; }
    public void setCreditColumnIndex(Integer creditColumnIndex) { this.creditColumnIndex = creditColumnIndex; }
    
    public Integer getDescriptionColumnIndex() { return descriptionColumnIndex; }
    public void setDescriptionColumnIndex(Integer descriptionColumnIndex) { this.descriptionColumnIndex = descriptionColumnIndex; }
    
    public Integer getCategoryColumnIndex() { return categoryColumnIndex; }
    public void setCategoryColumnIndex(Integer categoryColumnIndex) { this.categoryColumnIndex = categoryColumnIndex; }
    
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}