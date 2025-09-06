package com.buckpal.controller;

import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.service.SmartTransactionAssignmentService;
import com.buckpal.service.TransactionAssignmentService;
import com.buckpal.service.TransactionRevisionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionAssignmentController Unit Tests")
class TransactionAssignmentControllerTest {

    @Mock
    private TransactionAssignmentService transactionAssignmentService;

    @Mock
    private TransactionRevisionService transactionRevisionService;

    @Mock
    private SmartTransactionAssignmentService smartTransactionAssignmentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionAssignmentController transactionAssignmentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionAssignmentController).build();
        objectMapper = new ObjectMapper();
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setDescription("Test Transaction");
    }

    @Nested
    @DisplayName("POST /api/transaction-assignments/manual-assign")
    class ManualAssign {
        
        @Test
        @DisplayName("Should manually assign transaction successfully")
        void shouldManuallyAssignTransactionSuccessfully() throws Exception {
            // Given
            Map<String, Object> assignmentRequest = Map.of(
                "transactionId", 1L,
                "categoryId", 10L
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionAssignmentService.manuallyAssignTransaction(eq(1L), eq(10L), eq(testUser)))
                .thenReturn(testTransaction);
            
            // When & Then
            mockMvc.perform(post("/api/transaction-assignments/manual-assign")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(assignmentRequest)))
                    .andExpect(status().isOk());
            
            verify(transactionAssignmentService).manuallyAssignTransaction(1L, 10L, testUser);
        }
    }

    @Nested
    @DisplayName("PUT /api/transaction-assignments/override/{transactionId}")
    class OverrideAssignment {
        
        @Test
        @DisplayName("Should override assignment successfully")
        void shouldOverrideAssignmentSuccessfully() throws Exception {
            // Given
            Map<String, Object> overrideRequest = Map.of(
                "categoryId", 20L,
                "reason", "User preference"
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionAssignmentService.overrideAssignment(eq(1L), eq(20L), eq("User preference"), eq(testUser)))
                .thenReturn(testTransaction);
            
            // When & Then
            mockMvc.perform(put("/api/transaction-assignments/override/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(overrideRequest)))
                    .andExpect(status().isOk());
            
            verify(transactionAssignmentService).overrideAssignment(1L, 20L, "User preference", testUser);
        }
    }

    @Nested
    @DisplayName("GET /api/transaction-assignments/needs-review")
    class GetTransactionsNeedingReview {
        
        @Test
        @DisplayName("Should return transactions needing review")
        void shouldReturnTransactionsNeedingReview() throws Exception {
            // Given
            List<Transaction> transactions = List.of(testTransaction);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRevisionService.getTransactionsNeedingReview(testUser))
                .thenReturn(transactions);
            
            // When & Then
            mockMvc.perform(get("/api/transaction-assignments/needs-review")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));
            
            verify(transactionRevisionService).getTransactionsNeedingReview(testUser);
        }
        
        @Test
        @DisplayName("Should return transactions needing review by budget")
        void shouldReturnTransactionsNeedingReviewByBudget() throws Exception {
            // Given
            List<Transaction> transactions = List.of(testTransaction);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRevisionService.getTransactionsNeedingReview(1L, testUser))
                .thenReturn(transactions);
            
            // When & Then
            mockMvc.perform(get("/api/transaction-assignments/needs-review/1")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
            
            verify(transactionRevisionService).getTransactionsNeedingReview(1L, testUser);
        }
    }

    @Nested
    @DisplayName("GET /api/transaction-assignments/unassigned")
    class GetUnassignedTransactions {
        
        @Test
        @DisplayName("Should return unassigned transactions")
        void shouldReturnUnassignedTransactions() throws Exception {
            // Given
            List<Transaction> transactions = List.of(testTransaction);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionAssignmentService.getUnassignedTransactions(testUser))
                .thenReturn(transactions);
            
            // When & Then
            mockMvc.perform(get("/api/transaction-assignments/unassigned")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
            
            verify(transactionAssignmentService).getUnassignedTransactions(testUser);
        }
        
        @Test
        @DisplayName("Should return empty array when no unassigned transactions")
        void shouldReturnEmptyArrayWhenNoUnassignedTransactions() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionAssignmentService.getUnassignedTransactions(testUser))
                .thenReturn(Collections.emptyList());
            
            // When & Then
            mockMvc.perform(get("/api/transaction-assignments/unassigned")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpected(jsonPath("$").isArray())
                    .andExpected(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/transaction-assignments/smart-suggest/{transactionId}")
    class SmartSuggest {
        
        @Test
        @DisplayName("Should provide smart category suggestion")
        void shouldProvideSmartCategorySuggestion() throws Exception {
            // Given
            Map<String, Object> suggestion = Map.of(
                "categoryId", 15L,
                "categoryName", "Groceries",
                "confidence", 0.85
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(smartTransactionAssignmentService.suggestCategoryForTransaction(1L, testUser))
                .thenReturn(suggestion);
            
            // When & Then
            mockMvc.perform(post("/api/transaction-assignments/smart-suggest/1")
                    .principal(authentication))
                    .andExpected(status().isOk())
                    .andExpected(jsonPath("$.categoryId").value(15))
                    .andExpected(jsonPath("$.confidence").value(0.85));
            
            verify(smartTransactionAssignmentService).suggestCategoryForTransaction(1L, testUser);
        }
    }

    @Nested
    @DisplayName("POST /api/transaction-assignments/smart-feedback/{transactionId}")
    class SmartFeedback {
        
        @Test
        @DisplayName("Should submit smart assignment feedback")
        void shouldSubmitSmartAssignmentFeedback() throws Exception {
            // Given
            Map<String, Object> feedbackRequest = Map.of(
                "wasAccepted", true,
                "actualCategoryId", 15L
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            
            // When & Then
            mockMvc.perform(post("/api/transaction-assignments/smart-feedback/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(feedbackRequest)))
                    .andExpected(status().isOk());
            
            verify(smartTransactionAssignmentService).recordAssignmentFeedback(eq(1L), eq(true), eq(15L), eq(testUser));
        }
    }

    @Nested
    @DisplayName("GET /api/transaction-assignments/recently-assigned")
    class GetRecentlyAssignedTransactions {
        
        @Test
        @DisplayName("Should return recently assigned transactions")
        void shouldReturnRecentlyAssignedTransactions() throws Exception {
            // Given
            List<Transaction> transactions = List.of(testTransaction);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionAssignmentService.getRecentlyAssignedTransactions(testUser))
                .thenReturn(transactions);
            
            // When & Then
            mockMvc.perform(get("/api/transaction-assignments/recently-assigned")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpected(jsonPath("$").isArray());
            
            verify(transactionAssignmentService).getRecentlyAssignedTransactions(testUser);
        }
    }

    @Nested
    @DisplayName("POST /api/transaction-assignments/mark-for-revision/{transactionId}")
    class MarkForRevision {
        
        @Test
        @DisplayName("Should mark transaction for revision")
        void shouldMarkTransactionForRevision() throws Exception {
            // Given
            Map<String, Object> revisionRequest = Map.of(
                "reason", "Incorrect category assignment"
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            
            // When & Then
            mockMvc.perform(post("/api/transaction-assignments/mark-for-revision/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(revisionRequest)))
                    .andExpected(status().isOk());
            
            verify(transactionRevisionService).markTransactionForRevision(eq(1L), eq("Incorrect category assignment"), eq(testUser));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {
        
        @Test
        @DisplayName("Should handle service errors gracefully")
        void shouldHandleServiceErrorsGracefully() throws Exception {
            // Given
            Map<String, Object> assignmentRequest = Map.of(
                "transactionId", 1L,
                "categoryId", 10L
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionAssignmentService.manuallyAssignTransaction(eq(1L), eq(10L), eq(testUser)))
                .thenThrow(new RuntimeException("Transaction not found"));
            
            // When & Then
            mockMvc.perform(post("/api/transaction-assignments/manual-assign")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(assignmentRequest)))
                    .andExpected(status().is5xxServerError());
        }
    }
}