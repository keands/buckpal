package com.buckpal.service;

import com.buckpal.entity.Category;
import com.buckpal.entity.User;
import com.buckpal.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryInitializationServiceTest {
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @InjectMocks
    private CategoryInitializationService categoryInitializationService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }
    
    @Test
    void createPredefinedTransactionCategories_ShouldCreateCategoriesForUser() {
        // Given
        when(categoryRepository.save(any(Category.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        categoryInitializationService.createPredefinedTransactionCategories(testUser);
        
        // Then
        verify(categoryRepository, atLeast(20)).save(argThat(category -> 
            category.getUser().equals(testUser) &&
            category.getIsDefault() == true &&
            category.getName() != null &&
            !category.getName().isEmpty() &&
            category.getDescription() != null &&
            category.getColorCode() != null &&
            category.getIconName() != null
        ));
    }
    
    @Test
    void hasCategories_ShouldReturnTrue_WhenUserHasCategories() {
        // Given
        when(categoryRepository.countByUser(testUser))
            .thenReturn(5L);
        
        // When
        boolean result = categoryInitializationService.hasCategories(testUser);
        
        // Then
        assertTrue(result);
        verify(categoryRepository).countByUser(testUser);
    }
    
    @Test
    void hasCategories_ShouldReturnFalse_WhenUserHasNoCategories() {
        // Given
        when(categoryRepository.countByUser(testUser))
            .thenReturn(0L);
        
        // When
        boolean result = categoryInitializationService.hasCategories(testUser);
        
        // Then
        assertFalse(result);
        verify(categoryRepository).countByUser(testUser);
    }
    
    @Test
    void getPredefinedBudgetCategoryTemplates_ShouldReturnValidTemplates() {
        // When
        List<CategoryInitializationService.BudgetCategoryTemplate> templates = 
            categoryInitializationService.getPredefinedBudgetCategoryTemplates();
        
        // Then
        assertNotNull(templates);
        assertFalse(templates.isEmpty());
        assertTrue(templates.size() > 15); // Should have multiple predefined categories
        
        // Verify template structure
        for (var template : templates) {
            assertNotNull(template.getName());
            assertNotNull(template.getDescription());
            assertNotNull(template.getCategoryType());
            assertNotNull(template.getColorCode());
            assertNotNull(template.getIconName());
            assertNotNull(template.getSuggestedPercentage());
            assertTrue(template.getSuggestedPercentage() > 0);
            assertTrue(template.getSuggestedPercentage() <= 50); // Reasonable percentage range
        }
        
        // Verify we have categories of all types
        boolean hasNeeds = templates.stream().anyMatch(t -> 
            t.getCategoryType() == CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS);
        boolean hasWants = templates.stream().anyMatch(t -> 
            t.getCategoryType() == CategoryInitializationService.BudgetCategoryTemplate.CategoryType.WANTS);
        boolean hasSavings = templates.stream().anyMatch(t -> 
            t.getCategoryType() == CategoryInitializationService.BudgetCategoryTemplate.CategoryType.SAVINGS);
        
        assertTrue(hasNeeds, "Should have NEEDS categories");
        assertTrue(hasWants, "Should have WANTS categories");
        assertTrue(hasSavings, "Should have SAVINGS categories");
        
        // Verify total percentages are reasonable (templates provide suggestions, user will customize)
        double totalPercentage = templates.stream()
            .mapToDouble(CategoryInitializationService.BudgetCategoryTemplate::getSuggestedPercentage)
            .sum();
        assertTrue(totalPercentage > 90 && totalPercentage <= 150, 
            "Total suggested percentages should be reasonable for guidance, got: " + totalPercentage);
    }
    
    @Test
    void budgetCategoryTemplate_ShouldHaveCorrectGetters() {
        // Given
        String name = "Test Category";
        String description = "Test Description";
        CategoryInitializationService.BudgetCategoryTemplate.CategoryType type = 
            CategoryInitializationService.BudgetCategoryTemplate.CategoryType.NEEDS;
        String colorCode = "#ff0000";
        String iconName = "🏠";
        Double percentage = 25.0;
        
        // When
        CategoryInitializationService.BudgetCategoryTemplate template = 
            new CategoryInitializationService.BudgetCategoryTemplate(
                name, description, type, colorCode, iconName, percentage
            );
        
        // Then
        assertEquals(name, template.getName());
        assertEquals(description, template.getDescription());
        assertEquals(type, template.getCategoryType());
        assertEquals(colorCode, template.getColorCode());
        assertEquals(iconName, template.getIconName());
        assertEquals(percentage, template.getSuggestedPercentage());
    }
    
    @Test
    void createPredefinedTransactionCategories_ShouldIncludeEssentialCategories() {
        // Given
        when(categoryRepository.save(any(Category.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        categoryInitializationService.createPredefinedTransactionCategories(testUser);
        
        // Then - verify with translation keys
        verify(categoryRepository).save(argThat(category -> 
            "categories.salary".equals(category.getName()) && 
            category.getColorCode().equals("#22c55e")
        ));
        verify(categoryRepository).save(argThat(category -> 
            "categories.groceries".equals(category.getName()) &&
            "🛒".equals(category.getIconName())
        ));
        verify(categoryRepository).save(argThat(category -> 
            "categories.transportation".equals(category.getName()) &&
            "categories.transportation.description".equals(category.getDescription())
        ));
        verify(categoryRepository).save(argThat(category -> 
            "categories.housing".equals(category.getName()) &&
            "🏠".equals(category.getIconName())
        ));
    }
    
    @Test
    void createPredefinedTransactionCategories_ShouldUseTranslationKeys() {
        // Given
        when(categoryRepository.save(any(Category.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        categoryInitializationService.createPredefinedTransactionCategories(testUser);
        
        // Then - verify all categories use translation keys
        verify(categoryRepository, atLeast(20)).save(argThat(category -> 
            category.getName().startsWith("categories.") &&
            category.getDescription().startsWith("categories.") &&
            category.getUser().equals(testUser) &&
            category.getIsDefault() == true
        ));
    }
    
    @Test
    void getPredefinedBudgetCategoryTemplates_ShouldUseTranslationKeys() {
        // When
        List<CategoryInitializationService.BudgetCategoryTemplate> templates = 
            categoryInitializationService.getPredefinedBudgetCategoryTemplates();
        
        // Then - verify all templates use translation keys
        for (var template : templates) {
            assertTrue(template.getName().startsWith("budgetCategories."), 
                "Template name should be a translation key: " + template.getName());
            assertTrue(template.getDescription().startsWith("budgetCategories."), 
                "Template description should be a translation key: " + template.getDescription());
        }
    }
}