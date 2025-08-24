package com.buckpal.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@DisplayName("ProjectCategory Entity Tests")
class ProjectCategoryTest {
    
    private ProjectCategory projectCategory;
    private User user;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        
        projectCategory = new ProjectCategory();
        projectCategory.setName("Website Redesign");
        projectCategory.setUser(user);
        projectCategory.setProjectType(ProjectCategory.ProjectType.CLIENT);
        projectCategory.setAllocatedBudget(new BigDecimal("10000.00"));
        projectCategory.setSpentAmount(new BigDecimal("7500.00"));
        projectCategory.setStatus(ProjectCategory.ProjectStatus.IN_PROGRESS);
    }
    
    @Test
    @DisplayName("Should create project category with default values")
    void shouldCreateProjectCategoryWithDefaultValues() {
        ProjectCategory newProject = new ProjectCategory();
        
        assertThat(newProject.getAllocatedBudget()).isEqualTo(BigDecimal.ZERO);
        assertThat(newProject.getSpentAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(newProject.getStatus()).isEqualTo(ProjectCategory.ProjectStatus.PLANNING);
        assertThat(newProject.getProjectType()).isEqualTo(ProjectCategory.ProjectType.INTERNAL);
        assertThat(newProject.getIsBillable()).isFalse();
        assertThat(newProject.getSortOrder()).isEqualTo(0);
        assertThat(newProject.getIsActive()).isTrue();
        assertThat(newProject.getSubProjects()).isNotNull().isEmpty();
        assertThat(newProject.getTransactions()).isNotNull().isEmpty();
    }
    
    @Test
    @DisplayName("Should create project category with constructor")
    void shouldCreateProjectCategoryWithConstructor() {
        ProjectCategory newProject = new ProjectCategory("Mobile App", user, ProjectCategory.ProjectType.INTERNAL);
        
        assertThat(newProject.getName()).isEqualTo("Mobile App");
        assertThat(newProject.getUser()).isEqualTo(user);
        assertThat(newProject.getProjectType()).isEqualTo(ProjectCategory.ProjectType.INTERNAL);
    }
    
    @Test
    @DisplayName("Should calculate remaining budget correctly")
    void shouldCalculateRemainingBudgetCorrectly() {
        BigDecimal remainingBudget = projectCategory.getRemainingBudget();
        
        assertThat(remainingBudget).isEqualTo(new BigDecimal("2500.00"));
    }
    
    @Test
    @DisplayName("Should calculate negative remaining budget when overspent")
    void shouldCalculateNegativeRemainingBudgetWhenOverspent() {
        projectCategory.setSpentAmount(new BigDecimal("12000.00"));
        
        BigDecimal remainingBudget = projectCategory.getRemainingBudget();
        
        assertThat(remainingBudget).isEqualTo(new BigDecimal("-2000.00"));
    }
    
    @Test
    @DisplayName("Should calculate budget usage percentage correctly")
    void shouldCalculateBudgetUsagePercentageCorrectly() {
        BigDecimal usagePercentage = projectCategory.getBudgetUsagePercentage();
        
        assertThat(usagePercentage).isEqualTo(new BigDecimal("75.00"));
    }
    
    @Test
    @DisplayName("Should return zero usage percentage when allocated budget is zero")
    void shouldReturnZeroUsagePercentageWhenAllocatedBudgetIsZero() {
        projectCategory.setAllocatedBudget(BigDecimal.ZERO);
        projectCategory.setSpentAmount(new BigDecimal("500.00"));
        
        BigDecimal usagePercentage = projectCategory.getBudgetUsagePercentage();
        
        assertThat(usagePercentage).isEqualTo(BigDecimal.ZERO);
    }
    
    @Test
    @DisplayName("Should detect over-budget correctly")
    void shouldDetectOverBudgetCorrectly() {
        projectCategory.setSpentAmount(new BigDecimal("11000.00"));
        
        assertThat(projectCategory.isOverBudget()).isTrue();
    }
    
    @Test
    @DisplayName("Should detect within-budget correctly")
    void shouldDetectWithinBudgetCorrectly() {
        assertThat(projectCategory.isOverBudget()).isFalse();
    }
    
    @Test
    @DisplayName("Should detect active project status correctly")
    void shouldDetectActiveProjectStatusCorrectly() {
        projectCategory.setStatus(ProjectCategory.ProjectStatus.ACTIVE);
        assertThat(projectCategory.isActive()).isTrue();
        
        projectCategory.setStatus(ProjectCategory.ProjectStatus.IN_PROGRESS);
        assertThat(projectCategory.isActive()).isTrue();
        
        projectCategory.setStatus(ProjectCategory.ProjectStatus.COMPLETED);
        assertThat(projectCategory.isActive()).isFalse();
        
        projectCategory.setStatus(ProjectCategory.ProjectStatus.CANCELLED);
        assertThat(projectCategory.isActive()).isFalse();
    }
    
    @Test
    @DisplayName("Should generate full path for root project")
    void shouldGenerateFullPathForRootProject() {
        String fullPath = projectCategory.getFullPath();
        
        assertThat(fullPath).isEqualTo("Website Redesign");
    }
    
    @Test
    @DisplayName("Should generate full path for nested project")
    void shouldGenerateFullPathForNestedProject() {
        ProjectCategory parentProject = new ProjectCategory("E-commerce Platform", user, ProjectCategory.ProjectType.CLIENT);
        parentProject.setName("E-commerce Platform");
        
        projectCategory.setParentProject(parentProject);
        
        String fullPath = projectCategory.getFullPath();
        
        assertThat(fullPath).isEqualTo("E-commerce Platform > Website Redesign");
    }
    
    @Test
    @DisplayName("Should auto-generate project code on persist")
    void shouldAutoGenerateProjectCodeOnPersist() {
        projectCategory.setProjectCode(null);
        long timestampBefore = System.currentTimeMillis();
        
        projectCategory.onCreate();
        
        assertThat(projectCategory.getProjectCode()).startsWith("PROJ-");
        String timestamp = projectCategory.getProjectCode().substring(5);
        assertThat(Long.parseLong(timestamp)).isGreaterThanOrEqualTo(timestampBefore);
    }
    
    @Test
    @DisplayName("Should not override existing project code on persist")
    void shouldNotOverrideExistingProjectCodeOnPersist() {
        projectCategory.setProjectCode("CLIENT-001");
        
        projectCategory.onCreate();
        
        assertThat(projectCategory.getProjectCode()).isEqualTo("CLIENT-001");
    }
    
    @Test
    @DisplayName("Should set created timestamp on persist")
    void shouldSetCreatedTimestampOnPersist() {
        LocalDateTime beforePersist = LocalDateTime.now();
        
        projectCategory.onCreate();
        
        assertThat(projectCategory.getCreatedAt()).isAfter(beforePersist.minusSeconds(1));
        assertThat(projectCategory.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    @DisplayName("Should set updated timestamp on update")
    void shouldSetUpdatedTimestampOnUpdate() {
        projectCategory.setCreatedAt(LocalDateTime.now().minusHours(1));
        LocalDateTime beforeUpdate = LocalDateTime.now();
        
        projectCategory.onUpdate();
        
        assertThat(projectCategory.getUpdatedAt()).isAfter(beforeUpdate.minusSeconds(1));
        assertThat(projectCategory.getUpdatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    @DisplayName("Should support hierarchical relationships")
    void shouldSupportHierarchicalRelationships() {
        ProjectCategory parentProject = new ProjectCategory("Main Project", user, ProjectCategory.ProjectType.CLIENT);
        ProjectCategory subProject = new ProjectCategory("Sub Project", user, ProjectCategory.ProjectType.CLIENT);
        
        subProject.setParentProject(parentProject);
        parentProject.getSubProjects().add(subProject);
        
        assertThat(subProject.getParentProject()).isEqualTo(parentProject);
        assertThat(parentProject.getSubProjects()).contains(subProject);
    }
    
    @Test
    @DisplayName("Should handle dates and billable properties")
    void shouldHandleDatesAndBillableProperties() {
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        LocalDate endDate = LocalDate.of(2024, 6, 30);
        BigDecimal hourlyRate = new BigDecimal("75.00");
        
        projectCategory.setStartDate(startDate);
        projectCategory.setEndDate(endDate);
        projectCategory.setIsBillable(true);
        projectCategory.setHourlyRate(hourlyRate);
        
        assertThat(projectCategory.getStartDate()).isEqualTo(startDate);
        assertThat(projectCategory.getEndDate()).isEqualTo(endDate);
        assertThat(projectCategory.getIsBillable()).isTrue();
        assertThat(projectCategory.getHourlyRate()).isEqualTo(hourlyRate);
    }
    
    @Test
    @DisplayName("Should validate project status enum values")
    void shouldValidateProjectStatusEnumValues() {
        ProjectCategory.ProjectStatus[] expectedStatuses = {
            ProjectCategory.ProjectStatus.PLANNING,
            ProjectCategory.ProjectStatus.ACTIVE,
            ProjectCategory.ProjectStatus.IN_PROGRESS,
            ProjectCategory.ProjectStatus.ON_HOLD,
            ProjectCategory.ProjectStatus.COMPLETED,
            ProjectCategory.ProjectStatus.CANCELLED,
            ProjectCategory.ProjectStatus.ARCHIVED
        };
        
        ProjectCategory.ProjectStatus[] actualStatuses = ProjectCategory.ProjectStatus.values();
        
        assertThat(actualStatuses).containsExactly(expectedStatuses);
    }
    
    @Test
    @DisplayName("Should validate project type enum values")
    void shouldValidateProjectTypeEnumValues() {
        ProjectCategory.ProjectType[] expectedTypes = {
            ProjectCategory.ProjectType.INTERNAL,
            ProjectCategory.ProjectType.CLIENT,
            ProjectCategory.ProjectType.PERSONAL,
            ProjectCategory.ProjectType.RESEARCH,
            ProjectCategory.ProjectType.MAINTENANCE,
            ProjectCategory.ProjectType.MARKETING,
            ProjectCategory.ProjectType.OTHER
        };
        
        ProjectCategory.ProjectType[] actualTypes = ProjectCategory.ProjectType.values();
        
        assertThat(actualTypes).containsExactly(expectedTypes);
    }
}