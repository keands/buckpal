package com.buckpal.dto;

import com.buckpal.entity.ProjectCategory.ProjectStatus;
import com.buckpal.entity.ProjectCategory.ProjectType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectCategoryDto {
    
    private Long id;
    
    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Size(max = 20, message = "Project code must not exceed 20 characters")
    private String projectCode;
    
    @DecimalMin(value = "0.0", message = "Allocated budget must be positive")
    private BigDecimal allocatedBudget = BigDecimal.ZERO;
    
    @DecimalMin(value = "0.0", message = "Spent amount must be positive")
    private BigDecimal spentAmount = BigDecimal.ZERO;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    @NotNull(message = "Project status is required")
    private ProjectStatus status = ProjectStatus.PLANNING;
    
    @NotNull(message = "Project type is required")
    private ProjectType projectType = ProjectType.INTERNAL;
    
    private String colorCode;
    
    private Boolean isBillable = false;
    
    @DecimalMin(value = "0.0", message = "Hourly rate must be positive")
    private BigDecimal hourlyRate;
    
    private Integer sortOrder = 0;
    
    private Boolean isActive = true;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private Long parentProjectId;
    
    private String parentProjectName;
    
    private List<ProjectCategoryDto> subProjects;
    
    private BigDecimal remainingBudget;
    private BigDecimal budgetUsagePercentage;
    private Boolean isOverBudget;
    private Integer transactionCount;
    private String fullPath;
    private Boolean hasActiveSubProjects;
    
    public ProjectCategoryDto() {}
    
    public ProjectCategoryDto(String name, ProjectType projectType) {
        this.name = name;
        this.projectType = projectType;
    }
    
    // Computed properties
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
    
    public Boolean getIsOverBudget() {
        return spentAmount.compareTo(allocatedBudget) > 0;
    }
    
    public String getFullPath() {
        if (parentProjectName != null) {
            return parentProjectName + " > " + name;
        }
        return name;
    }
    
    public Boolean getIsActive() {
        return status == ProjectStatus.ACTIVE || status == ProjectStatus.IN_PROGRESS;
    }
    
    public String getProjectDuration() {
        if (startDate != null && endDate != null) {
            return startDate + " - " + endDate;
        } else if (startDate != null) {
            return "Started: " + startDate;
        } else if (endDate != null) {
            return "Due: " + endDate;
        }
        return "No dates set";
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

    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getParentProjectId() { return parentProjectId; }
    public void setParentProjectId(Long parentProjectId) { this.parentProjectId = parentProjectId; }
    
    public String getParentProjectName() { return parentProjectName; }
    public void setParentProjectName(String parentProjectName) { this.parentProjectName = parentProjectName; }
    
    public List<ProjectCategoryDto> getSubProjects() { return subProjects; }
    public void setSubProjects(List<ProjectCategoryDto> subProjects) { this.subProjects = subProjects; }
    
    public void setRemainingBudget(BigDecimal remainingBudget) { this.remainingBudget = remainingBudget; }
    public void setBudgetUsagePercentage(BigDecimal budgetUsagePercentage) { this.budgetUsagePercentage = budgetUsagePercentage; }
    public void setIsOverBudget(Boolean isOverBudget) { this.isOverBudget = isOverBudget; }
    
    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
    
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }
    
    public Boolean getHasActiveSubProjects() { return hasActiveSubProjects; }
    public void setHasActiveSubProjects(Boolean hasActiveSubProjects) { this.hasActiveSubProjects = hasActiveSubProjects; }
}