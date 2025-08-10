package com.buckpal.controller;

import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerDeleteAllTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private User testUser;
    private Account testAccount;
    private List<Transaction> testTransactions;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setUser(testUser);
        
        // Create test transactions
        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setDescription("Test transaction 1");
        transaction1.setTransactionDate(LocalDate.now());
        transaction1.setTransactionType(Transaction.TransactionType.EXPENSE);
        transaction1.setAccount(testAccount);
        
        Transaction transaction2 = new Transaction();
        transaction2.setId(2L);
        transaction2.setAmount(new BigDecimal("50.00"));
        transaction2.setDescription("Test transaction 2");
        transaction2.setTransactionDate(LocalDate.now().minusDays(1));
        transaction2.setTransactionType(Transaction.TransactionType.INCOME);
        transaction2.setAccount(testAccount);
        
        testTransactions = List.of(transaction1, transaction2);
    }

    @Test
    void deleteAllTransactionsByAccount_ShouldDeleteAllTransactions_WhenAccountExistsAndBelongsToUser() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccount(testAccount)).thenReturn(testTransactions);

        // When & Then
        mockMvc.perform(delete("/api/transactions/account/1/all")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All transactions permanently deleted. This action cannot be undone."))
                .andExpect(jsonPath("$.deletedCount").value(2));

        // Verify that all transactions were deleted
        verify(transactionRepository).deleteAll(testTransactions);
        verify(transactionRepository).findByAccount(testAccount);
    }

    @Test
    void deleteAllTransactionsByAccount_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/transactions/account/999/all")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(transactionRepository, never()).deleteAll(any());
    }

    @Test
    void deleteAllTransactionsByAccount_ShouldReturnForbidden_WhenAccountDoesNotBelongToUser() throws Exception {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        
        Account otherAccount = new Account();
        otherAccount.setId(1L);
        otherAccount.setName("Other Account");
        otherAccount.setUser(otherUser);

        when(authentication.getPrincipal()).thenReturn(testUser);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(otherAccount));

        // When & Then
        mockMvc.perform(delete("/api/transactions/account/1/all")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(transactionRepository, never()).deleteAll(any());
    }

    @Test
    void deleteAllTransactionsByAccount_ShouldReturnOk_WhenNoTransactionsExist() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccount(testAccount)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(delete("/api/transactions/account/1/all")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All transactions deleted successfully"))
                .andExpect(jsonPath("$.deletedCount").value(0));

        verify(transactionRepository, never()).deleteAll(any());
    }

    @Test
    void getTransactionCountByAccount_ShouldReturnCount() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.countByAccount(testAccount)).thenReturn(2L);

        // When & Then
        mockMvc.perform(get("/api/transactions/account/1/count")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }
}