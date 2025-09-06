package com.buckpal.controller;

import com.buckpal.dto.TransactionDto;
import com.buckpal.entity.Account;
import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import com.buckpal.service.CategoryService;
import com.buckpal.service.CsvImportService;
import com.buckpal.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController Unit Tests")
class TransactionControllerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CsvImportService csvImportService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Account testAccount;
    private Transaction testTransaction;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        objectMapper = new ObjectMapper();
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setUser(testUser);
        
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");
        
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setDescription("Test Transaction");
        testTransaction.setTransactionDate(LocalDate.now());
        testTransaction.setTransactionType(Transaction.TransactionType.EXPENSE);
        testTransaction.setAccount(testAccount);
        testTransaction.setCategory(testCategory);
    }

    @Nested
    @DisplayName("GET /api/transactions")
    class GetTransactions {
        
        @Test
        @DisplayName("Should return paginated transactions for authenticated user")
        void shouldReturnPaginatedTransactions() throws Exception {
            // Given
            List<Account> userAccounts = List.of(testAccount);
            Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findByUser(testUser)).thenReturn(userAccounts);
            when(transactionRepository.findByAccountsOrderByTransactionDateDesc(eq(userAccounts), any(Pageable.class)))
                .thenReturn(transactionPage);
            
            // When & Then
            mockMvc.perform(get("/api/transactions")
                    .principal(authentication)
                    .param("page", "0")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].amount").value(100.00))
                    .andExpect(jsonPath("$.content[0].description").value("Test Transaction"));
            
            verify(accountRepository).findByUser(testUser);
            verify(transactionRepository).findByAccountsOrderByTransactionDateDesc(eq(userAccounts), any(Pageable.class));
        }
        
        @Test
        @DisplayName("Should use default pagination parameters")
        void shouldUseDefaultPagination() throws Exception {
            // Given
            List<Account> userAccounts = List.of(testAccount);
            Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList());
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findByUser(testUser)).thenReturn(userAccounts);
            when(transactionRepository.findByAccountsOrderByTransactionDateDesc(eq(userAccounts), any(Pageable.class)))
                .thenReturn(emptyPage);
            
            // When & Then
            mockMvc.perform(get("/api/transactions")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty());
            
            verify(transactionRepository).findByAccountsOrderByTransactionDateDesc(
                eq(userAccounts), 
                eq(PageRequest.of(0, 20))
            );
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/account/{accountId}")
    class GetTransactionsByAccount {
        
        @Test
        @DisplayName("Should return transactions for account owner")
        void shouldReturnTransactionsByAccount() throws Exception {
            // Given
            List<Transaction> transactions = List.of(testTransaction);
            Page<Transaction> transactionPage = new PageImpl<>(transactions);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findByAccount(eq(testAccount), any(Pageable.class)))
                .thenReturn(transactionPage);
            
            // When & Then
            mockMvc.perform(get("/api/transactions/account/1")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].accountId").value(1));
            
            verify(accountRepository).findById(1L);
        }
        
        @Test
        @DisplayName("Should return 404 when account not found")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(get("/api/transactions/account/999")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
            
            verify(accountRepository).findById(999L);
        }
        
        @Test
        @DisplayName("Should return 403 when accessing other user's account")
        void shouldReturn403WhenAccessingOtherUsersAccount() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Account otherUserAccount = new Account();
            otherUserAccount.setId(2L);
            otherUserAccount.setUser(otherUser);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(2L)).thenReturn(Optional.of(otherUserAccount));
            
            // When & Then
            mockMvc.perform(get("/api/transactions/account/2")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
        
        @Test
        @DisplayName("Should filter by date range when provided")
        void shouldFilterByDateRange() throws Exception {
            // Given
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 1, 31);
            List<Transaction> transactions = List.of(testTransaction);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(transactionRepository.findByAccountAndTransactionDateBetween(testAccount, startDate, endDate))
                .thenReturn(transactions);
            
            // When & Then
            mockMvc.perform(get("/api/transactions/account/1")
                    .principal(authentication)
                    .param("startDate", "2024-01-01")
                    .param("endDate", "2024-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
            
            verify(transactionRepository).findByAccountAndTransactionDateBetween(testAccount, startDate, endDate);
        }
    }

    @Nested
    @DisplayName("POST /api/transactions/import-csv/{accountId}")
    class ImportCsvTransactions {
        
        @Test
        @DisplayName("Should import CSV transactions successfully")
        void shouldImportCsvTransactions() throws Exception {
            // Given
            MockMultipartFile csvFile = new MockMultipartFile(
                "file", 
                "transactions.csv", 
                "text/csv", 
                "date,amount,description\n2024-01-01,100.00,Test".getBytes()
            );
            
            List<Transaction> importedTransactions = List.of(testTransaction);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(csvImportService.importTransactionsFromCsv(any(MultipartFile.class), eq(1L)))
                .thenReturn(importedTransactions);
            when(categoryService.categorizeTransaction(any(Transaction.class))).thenReturn(testCategory);
            when(transactionService.createTransaction(any(Transaction.class))).thenReturn(testTransaction);
            
            // When & Then
            mockMvc.perform(multipart("/api/transactions/import-csv/1")
                    .file(csvFile)
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Imported 1 transactions"));
            
            verify(csvImportService).importTransactionsFromCsv(any(MultipartFile.class), eq(1L));
            verify(categoryService).categorizeTransaction(any(Transaction.class));
            verify(transactionService).createTransaction(any(Transaction.class));
        }
        
        @Test
        @DisplayName("Should return 403 when importing to other user's account")
        void shouldReturn403WhenImportingToOtherUsersAccount() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Account otherUserAccount = new Account();
            otherUserAccount.setId(2L);
            otherUserAccount.setUser(otherUser);
            
            MockMultipartFile csvFile = new MockMultipartFile(
                "file", 
                "transactions.csv", 
                "text/csv", 
                "test".getBytes()
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(2L)).thenReturn(Optional.of(otherUserAccount));
            
            // When & Then
            mockMvc.perform(multipart("/api/transactions/import-csv/2")
                    .file(csvFile)
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/csv-template")
    class GetCsvTemplate {
        
        @Test
        @DisplayName("Should return CSV template")
        void shouldReturnCsvTemplate() throws Exception {
            // Given
            String csvTemplate = "date,amount,description,category\n";
            when(csvImportService.generateCsvTemplate()).thenReturn(csvTemplate);
            
            // When & Then
            mockMvc.perform(get("/api/transactions/csv-template"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv"))
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"transaction_template.csv\""))
                    .andExpect(content().string(csvTemplate));
            
            verify(csvImportService).generateCsvTemplate();
        }
    }

    @Nested
    @DisplayName("POST /api/transactions")
    class CreateTransaction {
        
        @Test
        @DisplayName("Should create transaction successfully")
        void shouldCreateTransaction() throws Exception {
            // Given
            TransactionDto transactionDto = new TransactionDto();
            transactionDto.setAmount(new BigDecimal("100.00"));
            transactionDto.setDescription("New Transaction");
            transactionDto.setTransactionDate(LocalDate.now());
            transactionDto.setTransactionType(Transaction.TransactionType.EXPENSE);
            transactionDto.setAccountId(1L);
            transactionDto.setCategoryId(1L);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(testCategory));
            when(transactionService.createTransaction(any(Transaction.class))).thenReturn(testTransaction);
            
            // When & Then
            mockMvc.perform(post("/api/transactions")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transactionDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.amount").value(100.00));
            
            verify(transactionService).createTransaction(any(Transaction.class));
        }
        
        @Test
        @DisplayName("Should create transaction without category")
        void shouldCreateTransactionWithoutCategory() throws Exception {
            // Given
            TransactionDto transactionDto = new TransactionDto();
            transactionDto.setAmount(new BigDecimal("100.00"));
            transactionDto.setDescription("New Transaction");
            transactionDto.setTransactionDate(LocalDate.now());
            transactionDto.setTransactionType(Transaction.TransactionType.EXPENSE);
            transactionDto.setAccountId(1L);
            // No categoryId set
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(transactionService.createTransaction(any(Transaction.class))).thenReturn(testTransaction);
            
            // When & Then
            mockMvc.perform(post("/api/transactions")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transactionDto)))
                    .andExpect(status().isOk());
            
            verify(categoryService, never()).getCategoryById(any());
        }
        
        @Test
        @DisplayName("Should return 403 when creating transaction for other user's account")
        void shouldReturn403WhenCreatingForOtherUsersAccount() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Account otherUserAccount = new Account();
            otherUserAccount.setId(2L);
            otherUserAccount.setUser(otherUser);
            
            TransactionDto transactionDto = new TransactionDto();
            transactionDto.setAccountId(2L);
            transactionDto.setAmount(new BigDecimal("100.00"));
            transactionDto.setDescription("Test");
            transactionDto.setTransactionDate(LocalDate.now());
            transactionDto.setTransactionType(Transaction.TransactionType.EXPENSE);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(2L)).thenReturn(Optional.of(otherUserAccount));
            
            // When & Then
            mockMvc.perform(post("/api/transactions")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transactionDto)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/transactions/{transactionId}")
    class UpdateTransaction {
        
        @Test
        @DisplayName("Should update transaction successfully")
        void shouldUpdateTransaction() throws Exception {
            // Given
            TransactionDto updateDto = new TransactionDto();
            updateDto.setAmount(new BigDecimal("200.00"));
            updateDto.setDescription("Updated Transaction");
            updateDto.setTransactionDate(LocalDate.now());
            updateDto.setTransactionType(Transaction.TransactionType.INCOME);
            updateDto.setAccountId(1L);
            updateDto.setCategoryId(1L);
            
            Transaction updatedTransaction = new Transaction();
            updatedTransaction.setId(1L);
            updatedTransaction.setAmount(new BigDecimal("200.00"));
            updatedTransaction.setDescription("Updated Transaction");
            updatedTransaction.setAccount(testAccount);
            updatedTransaction.setCategory(testCategory);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(testCategory));
            when(transactionService.updateTransaction(eq(1L), any(Transaction.class))).thenReturn(updatedTransaction);
            
            // When & Then
            mockMvc.perform(put("/api/transactions/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.amount").value(200.00));
            
            verify(transactionService).updateTransaction(eq(1L), any(Transaction.class));
        }
        
        @Test
        @DisplayName("Should return 404 when transaction not found")
        void shouldReturn404WhenTransactionNotFound() throws Exception {
            // Given
            TransactionDto updateDto = new TransactionDto();
            updateDto.setAccountId(1L);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(put("/api/transactions/999")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("DELETE /api/transactions/{transactionId}")
    class DeleteTransaction {
        
        @Test
        @DisplayName("Should delete transaction successfully")
        void shouldDeleteTransaction() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
            
            // When & Then
            mockMvc.perform(delete("/api/transactions/1")
                    .principal(authentication))
                    .andExpect(status().isOk());
            
            verify(transactionService).deleteTransaction(1L);
        }
        
        @Test
        @DisplayName("Should return 404 when transaction not found")
        void shouldReturn404WhenTransactionNotFoundForDelete() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(delete("/api/transactions/999")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
        
        @Test
        @DisplayName("Should return 403 when deleting other user's transaction")
        void shouldReturn403WhenDeletingOtherUsersTransaction() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Account otherUserAccount = new Account();
            otherUserAccount.setUser(otherUser);
            
            Transaction otherUserTransaction = new Transaction();
            otherUserTransaction.setId(1L);
            otherUserTransaction.setAccount(otherUserAccount);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(otherUserTransaction));
            
            // When & Then
            mockMvc.perform(delete("/api/transactions/1")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
            
            verify(transactionService, never()).deleteTransaction(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/transactions/{transactionId}/category")
    class UpdateTransactionCategory {
        
        @Test
        @DisplayName("Should update transaction category successfully")
        void shouldUpdateTransactionCategory() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
            when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(testCategory));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
            
            // When & Then
            mockMvc.perform(put("/api/transactions/1/category")
                    .principal(authentication)
                    .param("categoryId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryId").value(1));
            
            verify(categoryService).getCategoryById(1L);
            verify(transactionRepository).save(testTransaction);
        }
        
        @Test
        @DisplayName("Should return 404 when transaction not found for category update")
        void shouldReturn404WhenTransactionNotFoundForCategoryUpdate() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(put("/api/transactions/999/category")
                    .principal(authentication)
                    .param("categoryId", "1"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/date/{date}")
    class GetTransactionsByDate {
        
        @Test
        @DisplayName("Should return transactions for specific date")
        void shouldReturnTransactionsByDate() throws Exception {
            // Given
            LocalDate testDate = LocalDate.of(2024, 1, 15);
            List<Account> userAccounts = List.of(testAccount);
            List<Transaction> transactions = List.of(testTransaction);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findByUser(testUser)).thenReturn(userAccounts);
            when(transactionRepository.findByAccountInAndTransactionDateOrderByTransactionDateDesc(userAccounts, testDate))
                .thenReturn(transactions);
            
            // When & Then
            mockMvc.perform(get("/api/transactions/date/2024-01-15")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
            
            verify(transactionRepository).findByAccountInAndTransactionDateOrderByTransactionDateDesc(userAccounts, testDate);
        }
        
        @Test
        @DisplayName("Should return 400 for invalid date format")
        void shouldReturn400ForInvalidDateFormat() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            
            // When & Then
            mockMvc.perform(get("/api/transactions/date/invalid-date")
                    .principal(authentication))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("Should return empty array when no transactions for date")
        void shouldReturnEmptyArrayWhenNoTransactionsForDate() throws Exception {
            // Given
            LocalDate testDate = LocalDate.of(2024, 1, 15);
            List<Account> userAccounts = List.of(testAccount);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findByUser(testUser)).thenReturn(userAccounts);
            when(transactionRepository.findByAccountInAndTransactionDateOrderByTransactionDateDesc(userAccounts, testDate))
                .thenReturn(Collections.emptyList());
            
            // When & Then
            mockMvc.perform(get("/api/transactions/date/2024-01-15")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}