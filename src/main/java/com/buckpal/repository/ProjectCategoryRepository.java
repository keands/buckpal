package com.buckpal.repository;

import com.buckpal.entity.ProjectCategory;
import com.buckpal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectCategoryRepository extends JpaRepository<ProjectCategory, Long> {
    
    List<ProjectCategory> findByUserOrderBySortOrderAscNameAsc(User user);
    
    List<ProjectCategory> findByUserAndIsActiveTrueOrderBySortOrderAscNameAsc(User user);
    
    List<ProjectCategory> findByUserAndParentProjectIsNullOrderBySortOrderAscNameAsc(User user);
    
    List<ProjectCategory> findByUserAndParentProjectIsNullAndIsActiveTrueOrderBySortOrderAscNameAsc(User user);
    
    List<ProjectCategory> findByParentProjectOrderBySortOrderAscNameAsc(ProjectCategory parentProject);
    
    List<ProjectCategory> findByUserAndProjectTypeOrderBySortOrderAscNameAsc(User user, ProjectCategory.ProjectType projectType);
    
    Optional<ProjectCategory> findByUserAndName(User user, String name);
    
    Optional<ProjectCategory> findByUserAndProjectCode(User user, String projectCode);
    
    Optional<ProjectCategory> findByProjectCodeIgnoreCase(String projectCode);
    
    List<ProjectCategory> findByUserAndStatusOrderBySortOrderAscNameAsc(User user, ProjectCategory.ProjectStatus status);
    
    @Query("SELECT pc FROM ProjectCategory pc WHERE pc.user = :user AND pc.spentAmount > pc.allocatedBudget")
    List<ProjectCategory> findOverBudgetProjects(@Param("user") User user);
    
    @Query("SELECT pc FROM ProjectCategory pc WHERE pc.user = :user AND " +
           "(pc.spentAmount / pc.allocatedBudget) >= :threshold")
    List<ProjectCategory> findProjectsNearBudgetLimit(@Param("user") User user, 
                                                     @Param("threshold") BigDecimal threshold);
    
    @Query("SELECT pc FROM ProjectCategory pc WHERE pc.user = :user AND " +
           "pc.endDate IS NOT NULL AND pc.endDate < :currentDate AND " +
           "pc.status NOT IN ('COMPLETED', 'CANCELLED', 'ARCHIVED')")
    List<ProjectCategory> findOverdueProjects(@Param("user") User user, 
                                             @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT pc FROM ProjectCategory pc WHERE pc.user = :user AND " +
           "pc.endDate IS NOT NULL AND pc.endDate BETWEEN :startDate AND :endDate AND " +
           "pc.status IN ('ACTIVE', 'IN_PROGRESS')")
    List<ProjectCategory> findProjectsDueInPeriod(@Param("user") User user,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(pc.allocatedBudget) FROM ProjectCategory pc WHERE pc.user = :user AND pc.projectType = :projectType")
    BigDecimal sumAllocatedBudgetByType(@Param("user") User user, 
                                       @Param("projectType") ProjectCategory.ProjectType projectType);
    
    @Query("SELECT SUM(pc.spentAmount) FROM ProjectCategory pc WHERE pc.user = :user AND pc.projectType = :projectType")
    BigDecimal sumSpentAmountByType(@Param("user") User user, 
                                   @Param("projectType") ProjectCategory.ProjectType projectType);
    
    @Query("SELECT SUM(pc.allocatedBudget) FROM ProjectCategory pc WHERE pc.user = :user AND pc.status = :status")
    BigDecimal sumAllocatedBudgetByStatus(@Param("user") User user, 
                                         @Param("status") ProjectCategory.ProjectStatus status);
    
    @Query("SELECT SUM(pc.spentAmount) FROM ProjectCategory pc WHERE pc.user = :user AND pc.status = :status")
    BigDecimal sumSpentAmountByStatus(@Param("user") User user, 
                                     @Param("status") ProjectCategory.ProjectStatus status);
    
    @Query("SELECT pc FROM ProjectCategory pc WHERE pc.user = :user AND " +
           "pc.startDate IS NOT NULL AND pc.startDate <= :date AND " +
           "(pc.endDate IS NULL OR pc.endDate >= :date)")
    List<ProjectCategory> findActiveProjectsOnDate(@Param("user") User user, 
                                                  @Param("date") LocalDate date);
    
    @Query("SELECT pc FROM ProjectCategory pc WHERE pc.user = :user AND pc.isBillable = true")
    List<ProjectCategory> findBillableProjects(@Param("user") User user);
    
    Long countByParentProject(ProjectCategory parentProject);
    
    Long countByUserAndStatus(User user, ProjectCategory.ProjectStatus status);
    
    Long countByUserAndProjectType(User user, ProjectCategory.ProjectType projectType);
    
    @Query("SELECT COUNT(t) > 0 FROM ProjectCategory pc JOIN pc.transactions t WHERE pc = :project")
    boolean hasLinkedTransactions(@Param("project") ProjectCategory project);
    
    @Query("SELECT pc FROM ProjectCategory pc JOIN pc.transactions t WHERE t.transactionDate BETWEEN :startDate AND :endDate GROUP BY pc ORDER BY COUNT(t) DESC")
    List<ProjectCategory> findMostUsedProjectsInPeriod(@Param("startDate") LocalDate startDate, 
                                                      @Param("endDate") LocalDate endDate);
    
    @Query("SELECT DISTINCT pc FROM ProjectCategory pc JOIN pc.transactions t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    List<ProjectCategory> findProjectsWithTransactionsInPeriod(@Param("startDate") LocalDate startDate, 
                                                              @Param("endDate") LocalDate endDate);
    
    default List<ProjectCategory> findActiveProjects(User user) {
        return findByUserAndStatusOrderBySortOrderAscNameAsc(user, ProjectCategory.ProjectStatus.ACTIVE);
    }
    
    default List<ProjectCategory> findCurrentProjects(User user) {
        LocalDate today = LocalDate.now();
        return findActiveProjectsOnDate(user, today);
    }
    
    default List<ProjectCategory> findProjectsDueSoon(User user, int daysAhead) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead);
        return findProjectsDueInPeriod(user, startDate, endDate);
    }
}