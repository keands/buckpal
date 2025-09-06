package com.buckpal.controller;

import com.buckpal.entity.User;
import com.buckpal.service.CategoryMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryMappingController Unit Tests")
class CategoryMappingControllerTest {

    @Mock
    private CategoryMappingService categoryMappingService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CategoryMappingController categoryMappingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryMappingController).build();
        objectMapper = new ObjectMapper();
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should get categories grouped by budget category")
    void shouldGetCategoriesGroupedByBudgetCategory() throws Exception {
        // Given
        Map<String, List<Map<String, Object>>> groupedCategories = Map.of(
            "needs", List.of(Map.of("id", 1L, "name", "Groceries")),
            "wants", List.of(Map.of("id", 2L, "name", "Entertainment"))
        );
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(categoryMappingService.getCategoriesGroupedByBudgetCategory(testUser))
            .thenReturn(groupedCategories);
        
        // When & Then
        mockMvc.perform(get("/api/category-mappings/grouped")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needs").isArray())
                .andExpect(jsonPath("$.wants").isArray())
                .andExpect(jsonPath("$.needs[0].name").value("Groceries"))
                .andExpect(jsonPath("$.wants[0].name").value("Entertainment"));
        
        verify(categoryMappingService).getCategoriesGroupedByBudgetCategory(testUser);
    }

    @Test
    @DisplayName("Should get budget category for detailed category")
    void shouldGetBudgetCategoryForDetailedCategory() throws Exception {
        // Given
        Map<String, Object> budgetCategoryInfo = Map.of(
            "budgetCategoryKey", "needs",
            "displayName", "Needs",
            "description", "Essential expenses"
        );
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(categoryMappingService.getBudgetCategoryForDetailed(1L, testUser))
            .thenReturn(budgetCategoryInfo);
        
        // When & Then
        mockMvc.perform(get("/api/category-mappings/budget-category/1")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetCategoryKey").value("needs"))
                .andExpect(jsonPath("$.displayName").value("Needs"));
        
        verify(categoryMappingService).getBudgetCategoryForDetailed(1L, testUser);
    }

    @Test
    @DisplayName("Should update category mapping")
    void shouldUpdateCategoryMapping() throws Exception {
        // Given
        Map<String, Object> updateRequest = Map.of(
            "detailedCategoryId", 1L,
            "newBudgetCategoryKey", "wants"
        );
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(categoryMappingService.updateCategoryMapping(eq(1L), eq("wants"), eq(testUser)))
            .thenReturn(Map.of("success", true, "message", "Mapping updated"));
        
        // When & Then
        mockMvc.perform(put("/api/category-mappings/update-mapping")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mapping updated"));
        
        verify(categoryMappingService).updateCategoryMapping(1L, "wants", testUser);
    }

    @Test
    @DisplayName("Should create custom category")
    void shouldCreateCustomCategory() throws Exception {
        // Given
        Map<String, Object> createRequest = Map.of(
            "name", "Custom Category",
            "budgetCategoryKey", "wants",
            "iconName", "🎯",
            "colorCode", "#22c55e"
        );
        
        Map<String, Object> createdCategory = Map.of(
            "id", 10L,
            "name", "Custom Category",
            "budgetCategoryKey", "wants"
        );
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(categoryMappingService.createCustomCategory(any(), eq(testUser)))
            .thenReturn(createdCategory);
        
        // When & Then
        mockMvc.perform(post("/api/category-mappings/create-custom-category")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Custom Category"));
        
        verify(categoryMappingService).createCustomCategory(any(), eq(testUser));
    }

    @Test
    @DisplayName("Should delete custom category")
    void shouldDeleteCustomCategory() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(categoryMappingService.deleteCustomCategory(1L, testUser))
            .thenReturn(Map.of("success", true, "message", "Category deleted"));
        
        // When & Then
        mockMvc.perform(delete("/api/category-mappings/delete-custom-category/1")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        
        verify(categoryMappingService).deleteCustomCategory(1L, testUser);
    }

    @Test
    @DisplayName("Should get unmapped categories")
    void shouldGetUnmappedCategories() throws Exception {
        // Given
        List<Map<String, Object>> unmappedCategories = List.of(
            Map.of("id", 5L, "name", "Unmapped Category")
        );
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(categoryMappingService.getUnmappedCategories(testUser))
            .thenReturn(unmappedCategories);
        
        // When & Then
        mockMvc.perform(get("/api/category-mappings/unmapped")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Unmapped Category"));
        
        verify(categoryMappingService).getUnmappedCategories(testUser);
    }

    @Test
    @DisplayName("Should get mapping statistics")
    void shouldGetMappingStatistics() throws Exception {
        // Given
        Map<String, Object> stats = Map.of(
            "totalCategories", 25,
            "mappedCategories", 20,
            "unmappedCategories", 5,
            "mappingCompleteness", 80.0
        );
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(categoryMappingService.getMappingStatistics(testUser)).thenReturn(stats);
        
        // When & Then
        mockMvc.perform(get("/api/category-mappings/stats")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCategories").value(25))
                .andExpect(jsonPath("$.mappingCompleteness").value(80.0));
        
        verify(categoryMappingService).getMappingStatistics(testUser);
    }

    @Test
    @DisplayName("Should initialize default mappings")
    void shouldInitializeDefaultMappings() throws Exception {
        // Given
        Map<String, Object> initResult = Map.of(
            "success", true,
            "mappingsCreated", 15,
            "message", "Default mappings initialized"
        );
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(categoryMappingService.initializeDefaultMappings(testUser))
            .thenReturn(initResult);
        
        // When & Then
        mockMvc.perform(post("/api/category-mappings/initialize-defaults")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.mappingsCreated").value(15));
        
        verify(categoryMappingService).initializeDefaultMappings(testUser);
    }
}