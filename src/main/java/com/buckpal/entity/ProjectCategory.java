package com.buckpal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "project_categories")
public class ProjectCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 100)
    private String name;
    
    @Size(max = 500)
    private String description;
    
    @Column(name = "project_code", unique = true)
    @Size(max = 20)
    private String projectCode; // e.g., "PROJ-001", "CLIENT-A"
    
    @Column(name = "allocated_budget", precision = 15, scale = 2)
    private BigDecimal allocatedBudget = BigDecimal.ZERO;
    
    @Column(name = "spent_amount", precision = 15, scale = 2)
    private BigDecimal spentAmount = BigDecimal.ZERO;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.PLANNING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false)
    private ProjectType projectType = ProjectType.INTERNAL;
    
    @Column(name = "color_code")
    private String colorCode;
    
    @Column(name = "is_billable", nullable = false)
    private Boolean isBillable = false;
    
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private ProjectCategory parentProject;
    
    @OneToMany(mappedBy = "parentProject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ProjectCategory> subProjects = new HashSet<>();
    
    @ManyToMany(mappedBy = "projectCategories", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Transaction> transactions = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (projectCode == null || projectCode.isEmpty()) {
            // Auto-generate project code if not provided
            projectCode = "PROJ-" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Utility methods
    public BigDecimal getRemainingBudget() {
        return allocatedBudget.subtract(spentAmount);
    }
    
    public BigDecimal getBudgetUsagePercentage() {
        if (allocatedBudget.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.divide(allocatedBudget, 2, BigDecimal.ROUND_HALF_UP)
                         .multiply(new BigDecimal("100"));
    }
    
    public boolean isOverBudget() {
        return spentAmount.compareTo(allocatedBudget) > 0;
    }
    
    public boolean isActive() {
        return status == ProjectStatus.ACTIVE || status == ProjectStatus.IN_PROGRESS;
    }
    
    public String getFullPath() {
        if (parentProject != null) {
            return parentProject.getFullPath() + " > " + name;
        }
        return name;
    }
    
    // Constructors
    public ProjectCategory() {}
    
    public ProjectCategory(String name, User user, ProjectType projectType) {
        this.name = name;
        this.user = user;
        this.projectType = projectType;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    
    public BigDecimal getAllocatedBudget() { return allocatedBudget; }
    public void setAllocatedBudget(BigDecimal allocatedBudget) { this.allocatedBudget = allocatedBudget; }
    
    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    
    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType projectType) { this.projectType = projectType; }
    
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
    
    public Boolean getIsBillable() { return isBillable; }
    public void setIsBillable(Boolean isBillable) { this.isBillable = isBillable; }
    
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public ProjectCategory getParentProject() { return parentProject; }
    public void setParentProject(ProjectCategory parentProject) { this.parentProject = parentProject; }
    
    public Set<ProjectCategory> getSubProjects() { return subProjects; }
    public void setSubProjects(Set<ProjectCategory> subProjects) { this.subProjects = subProjects; }
    
    public Set<Transaction> getTransactions() { return transactions; }
    public void setTransactions(Set<Transaction> transactions) { this.transactions = transactions; }
    
    public enum ProjectStatus {
        PLANNING,     // Project is being planned
        ACTIVE,       // Project is currently active
        IN_PROGRESS,  // Work is in progress
        ON_HOLD,      // Project is paused
        COMPLETED,    // Project is finished
        CANCELLED,    // Project was cancelled
        ARCHIVED      // Project is archived
    }
    
    public enum ProjectType {
        INTERNAL,     // Internal company project
        CLIENT,       // Client project  
        PERSONAL,     // Personal project
        RESEARCH,     // Research project
        MAINTENANCE,  // Maintenance work
        MARKETING,    // Marketing campaign
        OTHER         // Other type
    }
}