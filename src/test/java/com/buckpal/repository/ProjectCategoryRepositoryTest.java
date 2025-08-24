package com.buckpal.repository;

import com.buckpal.entity.ProjectCategory;
import com.buckpal.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProjectCategoryRepository Integration Tests")
class ProjectCategoryRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ProjectCategoryRepository projectCategoryRepository;
    
    private User testUser;
    private ProjectCategory parentProject;
    private ProjectCategory subProject1;
    private ProjectCategory subProject2;
    private ProjectCategory clientProject;
    private ProjectCategory internalProject;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedpassword");
        entityManager.persistAndFlush(testUser);
        
        // Parent project
        parentProject = new ProjectCategory("E-commerce Platform", testUser, ProjectCategory.ProjectType.CLIENT);
        parentProject.setAllocatedBudget(new BigDecimal("50000.00"));
        parentProject.setSpentAmount(new BigDecimal("30000.00"));
        parentProject.setStatus(ProjectCategory.ProjectStatus.ACTIVE);
        parentProject.setProjectCode("CLIENT-001");
        parentProject.setSortOrder(1);
        parentProject.setStartDate(LocalDate.of(2024, 1, 1));
        parentProject.setEndDate(LocalDate.of(2024, 12, 31));
        entityManager.persistAndFlush(parentProject);
        
        // Sub projects
        subProject1 = new ProjectCategory("Frontend Development", testUser, ProjectCategory.ProjectType.CLIENT);
        subProject1.setParentProject(parentProject);
        subProject1.setAllocatedBudget(new BigDecimal("25000.00"));
        subProject1.setSpentAmount(new BigDecimal("28000.00")); // Over budget
        subProject1.setStatus(ProjectCategory.ProjectStatus.IN_PROGRESS);
        subProject1.setProjectCode("CLIENT-001-FE");
        subProject1.setSortOrder(1);
        entityManager.persistAndFlush(subProject1);
        
        subProject2 = new ProjectCategory("Backend API", testUser, ProjectCategory.ProjectType.CLIENT);
        subProject2.setParentProject(parentProject);
        subProject2.setAllocatedBudget(new BigDecimal("20000.00"));
        subProject2.setSpentAmount(new BigDecimal("15000.00"));
        subProject2.setStatus(ProjectCategory.ProjectStatus.ACTIVE);
        subProject2.setProjectCode("CLIENT-001-BE");
        subProject2.setSortOrder(2);
        entityManager.persistAndFlush(subProject2);
        
        // Standalone client project
        clientProject = new ProjectCategory("Mobile App", testUser, ProjectCategory.ProjectType.CLIENT);
        clientProject.setAllocatedBudget(new BigDecimal("30000.00"));
        clientProject.setSpentAmount(new BigDecimal("25000.00"));
        clientProject.setStatus(ProjectCategory.ProjectStatus.COMPLETED);
        clientProject.setProjectCode("CLIENT-002");
        clientProject.setSortOrder(2);
        clientProject.setStartDate(LocalDate.of(2023, 6, 1));
        clientProject.setEndDate(LocalDate.of(2023, 12, 31));
        clientProject.setIsBillable(true);
        clientProject.setHourlyRate(new BigDecimal("100.00"));
        entityManager.persistAndFlush(clientProject);
        
        // Internal project
        internalProject = new ProjectCategory("Internal Tool", testUser, ProjectCategory.ProjectType.INTERNAL);
        internalProject.setAllocatedBudget(new BigDecimal("10000.00"));
        internalProject.setSpentAmount(new BigDecimal("8000.00"));
        internalProject.setStatus(ProjectCategory.ProjectStatus.ACTIVE);
        internalProject.setProjectCode("INT-001");
        internalProject.setSortOrder(3);
        internalProject.setStartDate(LocalDate.of(2024, 2, 1));
        internalProject.setEndDate(LocalDate.of(2024, 8, 31));
        entityManager.persistAndFlush(internalProject);
    }
    
    @Test
    @DisplayName("Should find projects by user ordered by sort order and name")
    void shouldFindProjectsByUserOrderedBySortOrderAndName() {
        List<ProjectCategory> projects = projectCategoryRepository.findByUserOrderBySortOrderAscNameAsc(testUser);
        
        assertThat(projects).hasSize(5);
        assertThat(projects.get(0).getName()).isEqualTo("E-commerce Platform"); // Sort order 1
        assertThat(projects.get(1).getName()).isEqualTo("Frontend Development"); // Sort order 1
    }
    
    @Test
    @DisplayName("Should find only active projects")
    void shouldFindOnlyActiveProjects() {
        internalProject.setIsActive(false);
        entityManager.persistAndFlush(internalProject);
        
        List<ProjectCategory> activeProjects = projectCategoryRepository.findByUserAndIsActiveTrueOrderBySortOrderAscNameAsc(testUser);
        
        assertThat(activeProjects).hasSize(4);
        assertThat(activeProjects).extracting(ProjectCategory::getName)
            .doesNotContain("Internal Tool");
    }
    
    @Test
    @DisplayName("Should find only parent projects")
    void shouldFindOnlyParentProjects() {
        List<ProjectCategory> parentProjects = projectCategoryRepository.findByUserAndParentProjectIsNullOrderBySortOrderAscNameAsc(testUser);
        
        assertThat(parentProjects).hasSize(3);
        assertThat(parentProjects).extracting(ProjectCategory::getName)
            .containsExactly("E-commerce Platform", "Mobile App", "Internal Tool");
    }
    
    @Test
    @DisplayName("Should find active parent projects only")
    void shouldFindActiveParentProjectsOnly() {
        internalProject.setIsActive(false);
        entityManager.persistAndFlush(internalProject);
        
        List<ProjectCategory> activeParentProjects = projectCategoryRepository.findByUserAndParentProjectIsNullAndIsActiveTrueOrderBySortOrderAscNameAsc(testUser);
        
        assertThat(activeParentProjects).hasSize(2);
        assertThat(activeParentProjects).extracting(ProjectCategory::getName)
            .containsExactly("E-commerce Platform", "Mobile App");
    }
    
    @Test
    @DisplayName("Should find sub-projects by parent project")
    void shouldFindSubProjectsByParentProject() {
        List<ProjectCategory> subProjects = projectCategoryRepository.findByParentProjectOrderBySortOrderAscNameAsc(parentProject);
        
        assertThat(subProjects).hasSize(2);
        assertThat(subProjects).extracting(ProjectCategory::getName)
            .containsExactly("Frontend Development", "Backend API");
    }
    
    @Test
    @DisplayName("Should find projects by type")
    void shouldFindProjectsByType() {
        List<ProjectCategory> clientProjects = projectCategoryRepository.findByUserAndProjectTypeOrderBySortOrderAscNameAsc(
            testUser, ProjectCategory.ProjectType.CLIENT
        );
        
        assertThat(clientProjects).hasSize(4);
        assertThat(clientProjects).allMatch(project -> 
            project.getProjectType() == ProjectCategory.ProjectType.CLIENT
        );
    }
    
    @Test
    @DisplayName("Should find project by user and name")
    void shouldFindProjectByUserAndName() {
        Optional<ProjectCategory> project = projectCategoryRepository.findByUserAndName(testUser, "Mobile App");
        
        assertThat(project).isPresent();
        assertThat(project.get().getAllocatedBudget()).isEqualTo(new BigDecimal("30000.00"));
    }
    
    @Test
    @DisplayName("Should find project by user and project code")
    void shouldFindProjectByUserAndProjectCode() {
        Optional<ProjectCategory> project = projectCategoryRepository.findByUserAndProjectCode(testUser, "CLIENT-001");
        
        assertThat(project).isPresent();
        assertThat(project.get().getName()).isEqualTo("E-commerce Platform");
    }
    
    @Test
    @DisplayName("Should find project by project code ignoring case")
    void shouldFindProjectByProjectCodeIgnoringCase() {
        Optional<ProjectCategory> project = projectCategoryRepository.findByProjectCodeIgnoreCase("client-001");
        
        assertThat(project).isPresent();
        assertThat(project.get().getName()).isEqualTo("E-commerce Platform");
    }
    
    @Test
    @DisplayName("Should find projects by status")
    void shouldFindProjectsByStatus() {
        List<ProjectCategory> activeProjects = projectCategoryRepository.findByUserAndStatusOrderBySortOrderAscNameAsc(
            testUser, ProjectCategory.ProjectStatus.ACTIVE
        );
        
        assertThat(activeProjects).hasSize(3);
        assertThat(activeProjects).allMatch(project -> 
            project.getStatus() == ProjectCategory.ProjectStatus.ACTIVE
        );
    }
    
    @Test
    @DisplayName("Should find over-budget projects")
    void shouldFindOverBudgetProjects() {
        List<ProjectCategory> overBudgetProjects = projectCategoryRepository.findOverBudgetProjects(testUser);
        
        assertThat(overBudgetProjects).hasSize(1);
        assertThat(overBudgetProjects.get(0).getName()).isEqualTo("Frontend Development");
        assertThat(overBudgetProjects.get(0).getSpentAmount()).isGreaterThan(overBudgetProjects.get(0).getAllocatedBudget());
    }
    
    @Test
    @DisplayName("Should find projects near budget limit")
    void shouldFindProjectsNearBudgetLimit() {
        BigDecimal threshold = new BigDecimal("0.75"); // 75% threshold
        
        List<ProjectCategory> nearLimitProjects = projectCategoryRepository.findProjectsNearBudgetLimit(testUser, threshold);
        
        assertThat(nearLimitProjects).hasSize(3); // Frontend (112%), Internal (80%), Mobile (83.3%)
    }
    
    @Test
    @DisplayName("Should find overdue projects")
    void shouldFindOverdueProjects() {
        // Create an overdue project
        ProjectCategory overdueProject = new ProjectCategory("Overdue Project", testUser, ProjectCategory.ProjectType.CLIENT);
        overdueProject.setStatus(ProjectCategory.ProjectStatus.ACTIVE);
        overdueProject.setEndDate(LocalDate.now().minusDays(10));
        entityManager.persistAndFlush(overdueProject);
        
        List<ProjectCategory> overdueProjects = projectCategoryRepository.findOverdueProjects(testUser, LocalDate.now());
        
        assertThat(overdueProjects).hasSize(1);
        assertThat(overdueProjects.get(0).getName()).isEqualTo("Overdue Project");
    }
    
    @Test
    @DisplayName("Should find projects due in period")
    void shouldFindProjectsDueInPeriod() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(30);
        
        // Create a project due soon
        ProjectCategory dueSoonProject = new ProjectCategory("Due Soon Project", testUser, ProjectCategory.ProjectType.CLIENT);
        dueSoonProject.setStatus(ProjectCategory.ProjectStatus.ACTIVE);
        dueSoonProject.setEndDate(LocalDate.now().plusDays(15));
        entityManager.persistAndFlush(dueSoonProject);
        
        List<ProjectCategory> projectsDueSoon = projectCategoryRepository.findProjectsDueInPeriod(testUser, startDate, endDate);
        
        assertThat(projectsDueSoon).hasSize(1);
        assertThat(projectsDueSoon.get(0).getName()).isEqualTo("Due Soon Project");
    }
    
    @Test
    @DisplayName("Should sum allocated budget by project type")
    void shouldSumAllocatedBudgetByProjectType() {
        BigDecimal totalClientBudget = projectCategoryRepository.sumAllocatedBudgetByType(
            testUser, ProjectCategory.ProjectType.CLIENT
        );
        
        assertThat(totalClientBudget).isEqualTo(new BigDecimal("125000.00")); // 50000 + 25000 + 20000 + 30000
    }
    
    @Test
    @DisplayName("Should sum spent amount by project type")
    void shouldSumSpentAmountByProjectType() {
        BigDecimal totalClientSpent = projectCategoryRepository.sumSpentAmountByType(
            testUser, ProjectCategory.ProjectType.CLIENT
        );
        
        assertThat(totalClientSpent).isEqualTo(new BigDecimal("98000.00")); // 30000 + 28000 + 15000 + 25000
    }
    
    @Test
    @DisplayName("Should find active projects on date using default method")
    void shouldFindActiveProjectsOnDateUsingDefaultMethod() {
        List<ProjectCategory> currentProjects = projectCategoryRepository.findCurrentProjects(testUser);
        
        assertThat(currentProjects).hasSize(2); // Parent project and internal project are active today
    }
    
    @Test
    @DisplayName("Should find projects due soon using default method")
    void shouldFindProjectsDueSoonUsingDefaultMethod() {
        // Create a project due in 5 days
        ProjectCategory dueSoonProject = new ProjectCategory("Due Soon", testUser, ProjectCategory.ProjectType.CLIENT);
        dueSoonProject.setStatus(ProjectCategory.ProjectStatus.ACTIVE);
        dueSoonProject.setEndDate(LocalDate.now().plusDays(5));
        entityManager.persistAndFlush(dueSoonProject);
        
        List<ProjectCategory> projectsDueSoon = projectCategoryRepository.findProjectsDueSoon(testUser, 7);
        
        assertThat(projectsDueSoon).hasSize(1);
        assertThat(projectsDueSoon.get(0).getName()).isEqualTo("Due Soon");
    }
    
    @Test
    @DisplayName("Should find billable projects")
    void shouldFindBillableProjects() {
        List<ProjectCategory> billableProjects = projectCategoryRepository.findBillableProjects(testUser);
        
        assertThat(billableProjects).hasSize(1);
        assertThat(billableProjects.get(0).getName()).isEqualTo("Mobile App");
        assertThat(billableProjects.get(0).getIsBillable()).isTrue();
    }
    
    @Test
    @DisplayName("Should count sub-projects")
    void shouldCountSubProjects() {
        Long subProjectCount = projectCategoryRepository.countByParentProject(parentProject);
        
        assertThat(subProjectCount).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("Should count projects by status and type")
    void shouldCountProjectsByStatusAndType() {
        Long activeProjectCount = projectCategoryRepository.countByUserAndStatus(testUser, ProjectCategory.ProjectStatus.ACTIVE);
        Long clientProjectCount = projectCategoryRepository.countByUserAndProjectType(testUser, ProjectCategory.ProjectType.CLIENT);
        
        assertThat(activeProjectCount).isEqualTo(3L);
        assertThat(clientProjectCount).isEqualTo(4L);
    }
    
    @Test
    @DisplayName("Should check if project has linked transactions")
    void shouldCheckIfProjectHasLinkedTransactions() {
        boolean hasTransactions = projectCategoryRepository.hasLinkedTransactions(clientProject);
        
        assertThat(hasTransactions).isFalse(); // No transactions linked in test setup
    }
}