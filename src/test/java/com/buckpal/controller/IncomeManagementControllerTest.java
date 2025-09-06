package com.buckpal.controller;

import com.buckpal.entity.IncomeCategory;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.service.IncomeManagementService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncomeManagementController Unit Tests")
class IncomeManagementControllerTest {

    @Mock
    private IncomeManagementService incomeManagementService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private IncomeManagementController incomeManagementController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private IncomeCategory testIncomeCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(incomeManagementController).build();
        objectMapper = new ObjectMapper();
        
        testUser = new User();
        testUser.setId(1L);
        
        testIncomeCategory = new IncomeCategory();
        testIncomeCategory.setId(1L);
        testIncomeCategory.setName("Salary");
        testIncomeCategory.setExpectedAmount(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Should get income categories for budget")
    void shouldGetIncomeCategoriesForBudget() throws Exception {
        // Given
        List<IncomeCategory> categories = List.of(testIncomeCategory);
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(incomeManagementService.getIncomeCategories(1L)).thenReturn(categories);
        
        // When & Then
        mockMvc.perform(get("/api/income/budgets/1/categories")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Salary"));
        
        verify(incomeManagementService).getIncomeCategories(1L);
    }

    @Test
    @DisplayName("Should create income category")
    void shouldCreateIncomeCategory() throws Exception {
        // Given
        Map<String, Object> createRequest = Map.of(
            "name", "Freelance",
            "expectedAmount", 1000.00,
            "incomeType", "VARIABLE"
        );
        
        IncomeCategory createdCategory = new IncomeCategory();
        createdCategory.setId(2L);
        createdCategory.setName("Freelance");
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(incomeManagementService.createIncomeCategory(eq(1L), any(), eq(testUser)))
            .thenReturn(createdCategory);
        
        // When & Then
        mockMvc.perform(post("/api/income/budgets/1/categories")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Freelance"));
        
        verify(incomeManagementService).createIncomeCategory(eq(1L), any(), eq(testUser));
    }

    @Test
    @DisplayName("Should get available income transactions")
    void shouldGetAvailableIncomeTransactions() throws Exception {
        // Given
        List<Transaction> transactions = List.of(new Transaction());
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(incomeManagementService.getAvailableIncomeTransactions(testUser))
            .thenReturn(transactions);
        
        // When & Then
        mockMvc.perform(get("/api/income/available-transactions")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
        
        verify(incomeManagementService).getAvailableIncomeTransactions(testUser);
    }

    @Test
    @DisplayName("Should update income category")
    void shouldUpdateIncomeCategory() throws Exception {
        // Given
        Map<String, Object> updateRequest = Map.of(
            "name", "Updated Salary",
            "expectedAmount", 5500.00
        );
        
        IncomeCategory updatedCategory = new IncomeCategory();
        updatedCategory.setId(1L);
        updatedCategory.setName("Updated Salary");
        
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(incomeManagementService.updateIncomeCategory(eq(1L), any(), eq(testUser)))
            .thenReturn(updatedCategory);
        
        // When & Then
        mockMvc.perform(put("/api/income/categories/1")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Salary"));
        
        verify(incomeManagementService).updateIncomeCategory(eq(1L), any(), eq(testUser));
    }

    @Test
    @DisplayName("Should delete income category")
    void shouldDeleteIncomeCategory() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        
        // When & Then
        mockMvc.perform(delete("/api/income/categories/1")
                .principal(authentication))
                .andExpect(status().isNoContent());
        
        verify(incomeManagementService).deleteIncomeCategory(1L, testUser);
    }
}