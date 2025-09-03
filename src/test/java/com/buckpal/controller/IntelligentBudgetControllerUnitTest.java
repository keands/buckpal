package com.buckpal.controller;

import com.buckpal.entity.User;
import com.buckpal.service.IntelligentBudgetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntelligentBudgetController Unit Tests")
class IntelligentBudgetControllerUnitTest {

    @Mock
    private IntelligentBudgetService intelligentBudgetService;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private IntelligentBudgetController controller;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        
        when(authentication.getPrincipal()).thenReturn(testUser);
    }
    
    @Test
    @DisplayName("Should return wizard insights successfully")
    void shouldReturnWizardInsightsSuccessfully() {
        // Given
        IntelligentBudgetService.SmartBudgetTemplate template = 
            new IntelligentBudgetService.SmartBudgetTemplate(Collections.emptyList(), BigDecimal.ZERO);
        
        List<IntelligentBudgetService.IncomePattern> patterns = Collections.emptyList();
        
        when(intelligentBudgetService.generateSmartBudgetTemplate(testUser)).thenReturn(template);
        when(intelligentBudgetService.analyzeIncomePatterns(testUser, 6)).thenReturn(patterns);
        
        // When
        ResponseEntity<Map<String, Object>> response = controller.getWizardInsights(authentication);
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsKeys("hasHistoricalData", "suggestedTemplate", "recentPatterns", "confidence", "message");
        assertThat(response.getBody().get("hasHistoricalData")).isEqualTo(false);
        verify(intelligentBudgetService).generateSmartBudgetTemplate(testUser);
        verify(intelligentBudgetService).analyzeIncomePatterns(testUser, 6);
    }
    
    @Test
    @DisplayName("Should return income patterns successfully")
    void shouldReturnIncomePatternsSuccessfully() {
        // Given
        List<IntelligentBudgetService.IncomePattern> patterns = Collections.emptyList();
        when(intelligentBudgetService.analyzeIncomePatterns(testUser, 12)).thenReturn(patterns);
        
        // When
        ResponseEntity<List<IntelligentBudgetService.IncomePattern>> response = 
            controller.getIncomePatterns(12, authentication);
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
        verify(intelligentBudgetService).analyzeIncomePatterns(testUser, 12);
    }
    
    @Test
    @DisplayName("Should return income patterns with default months back")
    void shouldReturnIncomePatternsWithDefaultMonthsBack() {
        // Given
        List<IntelligentBudgetService.IncomePattern> patterns = Collections.emptyList();
        when(intelligentBudgetService.analyzeIncomePatterns(testUser, 12)).thenReturn(patterns);
        
        // When - using default parameter
        ResponseEntity<List<IntelligentBudgetService.IncomePattern>> response = 
            controller.getIncomePatterns(12, authentication); // Default value from @RequestParam
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(intelligentBudgetService).analyzeIncomePatterns(testUser, 12);
    }
    
    @Test
    @DisplayName("Should suggest income category successfully")
    void shouldSuggestIncomeCategorySuccessfully() {
        // Given
        List<IntelligentBudgetService.IncomeSuggestion> suggestions = Arrays.asList(
            new IntelligentBudgetService.IncomeSuggestion("Salaire", null, 0.9, BigDecimal.ZERO, "Test suggestion")
        );
        when(intelligentBudgetService.suggestIncomeCategories(testUser, "salary")).thenReturn(suggestions);
        
        Map<String, String> request = Map.of("description", "salary");
        
        // When
        ResponseEntity<List<IntelligentBudgetService.IncomeSuggestion>> response = 
            controller.suggestIncomeCategory(request, authentication);
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCategoryName()).isEqualTo("Salaire");
        verify(intelligentBudgetService).suggestIncomeCategories(testUser, "salary");
    }
    
    @Test
    @DisplayName("Should return 400 for empty description")
    void shouldReturn400ForEmptyDescription() {
        // Given
        Map<String, String> request = Map.of("description", "");
        
        // When
        ResponseEntity<List<IntelligentBudgetService.IncomeSuggestion>> response = 
            controller.suggestIncomeCategory(request, authentication);
        
        // Then
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verifyNoInteractions(intelligentBudgetService);
    }
    
    @Test
    @DisplayName("Should return 400 for null description")
    void shouldReturn400ForNullDescription() {
        // Given
        Map<String, String> request = new HashMap<>();
        request.put("description", null);
        
        // When
        ResponseEntity<List<IntelligentBudgetService.IncomeSuggestion>> response = 
            controller.suggestIncomeCategory(request, authentication);
        
        // Then
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verifyNoInteractions(intelligentBudgetService);
    }
    
    @Test
    @DisplayName("Should return smart budget template successfully")
    void shouldReturnSmartBudgetTemplateSuccessfully() {
        // Given
        IntelligentBudgetService.SmartBudgetTemplate template = 
            new IntelligentBudgetService.SmartBudgetTemplate(Collections.emptyList(), BigDecimal.ZERO);
        when(intelligentBudgetService.generateSmartBudgetTemplate(testUser)).thenReturn(template);
        
        // When
        ResponseEntity<IntelligentBudgetService.SmartBudgetTemplate> response = 
            controller.getSmartBudgetTemplate(authentication);
        
        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalSuggestedIncome()).isEqualTo(BigDecimal.ZERO);
        verify(intelligentBudgetService).generateSmartBudgetTemplate(testUser);
    }
    
    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() {
        // Given
        when(intelligentBudgetService.analyzeIncomePatterns(any(), anyInt()))
            .thenThrow(new RuntimeException("Database error"));
        
        // When
        ResponseEntity<List<IntelligentBudgetService.IncomePattern>> response = 
            controller.getIncomePatterns(12, authentication);
        
        // Then
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        verify(intelligentBudgetService).analyzeIncomePatterns(testUser, 12);
    }
    
    @Test
    @DisplayName("Should calculate confidence correctly")
    void shouldCalculateConfidenceCorrectly() {
        // Given
        List<IntelligentBudgetService.IncomePattern> patterns = Arrays.asList(
            createPattern("pattern1", 0.8),
            createPattern("pattern2", 0.6),
            createPattern("pattern3", 0.9)
        );
        
        IntelligentBudgetService.SmartBudgetTemplate template = 
            new IntelligentBudgetService.SmartBudgetTemplate(Collections.emptyList(), BigDecimal.ZERO);
        
        when(intelligentBudgetService.generateSmartBudgetTemplate(testUser)).thenReturn(template);
        when(intelligentBudgetService.analyzeIncomePatterns(testUser, 6)).thenReturn(patterns);
        
        // When
        ResponseEntity<Map<String, Object>> response = controller.getWizardInsights(authentication);
        
        // Then
        double confidence = (Double) response.getBody().get("confidence");
        assertThat(confidence).isCloseTo(0.7666666666666667, org.assertj.core.data.Offset.offset(0.0001)); // Average of 0.8, 0.6, 0.9
    }
    
    @Test
    @DisplayName("Should generate appropriate insight message for new user")
    void shouldGenerateAppropriateInsightMessageForNewUser() {
        // Given
        IntelligentBudgetService.SmartBudgetTemplate template = 
            new IntelligentBudgetService.SmartBudgetTemplate(Collections.emptyList(), BigDecimal.ZERO);
        List<IntelligentBudgetService.IncomePattern> patterns = Collections.emptyList();
        
        when(intelligentBudgetService.generateSmartBudgetTemplate(testUser)).thenReturn(template);
        when(intelligentBudgetService.analyzeIncomePatterns(testUser, 6)).thenReturn(patterns);
        
        // When
        ResponseEntity<Map<String, Object>> response = controller.getWizardInsights(authentication);
        
        // Then
        String message = (String) response.getBody().get("message");
        assertThat(message).contains("premier budget");
    }
    
    // Helper method to create test patterns
    private IntelligentBudgetService.IncomePattern createPattern(String pattern, double confidence) {
        return new IntelligentBudgetService.IncomePattern(
            pattern, "TestCategory", null, confidence, BigDecimal.ZERO, 1
        );
    }
}