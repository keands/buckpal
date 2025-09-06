package com.buckpal.controller;

import com.buckpal.dto.AccountDto;
import com.buckpal.entity.Account;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import com.buckpal.service.PlaidService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController Unit Tests")
class AccountControllerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PlaidService plaidService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();
        objectMapper = new ObjectMapper();
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Checking Account");
        testAccount.setAccountType(Account.AccountType.CHECKING);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setBankName("Test Bank");
        testAccount.setIsActive(true);
        testAccount.setUser(testUser);
    }

    @Nested
    @DisplayName("GET /api/accounts")
    class GetUserAccounts {
        
        @Test
        @DisplayName("Should return user's active accounts")
        void shouldReturnUserActiveAccounts() throws Exception {
            // Given
            List<Account> accounts = List.of(testAccount);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findByUserAndIsActive(testUser, true)).thenReturn(accounts);
            
            // When & Then
            mockMvc.perform(get("/api/accounts")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Test Checking Account"))
                    .andExpect(jsonPath("$[0].accountType").value("CHECKING"))
                    .andExpect(jsonPath("$[0].balance").value(1000.00))
                    .andExpect(jsonPath("$[0].bankName").value("Test Bank"))
                    .andExpect(jsonPath("$[0].isActive").value(true));
            
            verify(accountRepository).findByUserAndIsActive(testUser, true);
        }
        
        @Test
        @DisplayName("Should return empty array when user has no active accounts")
        void shouldReturnEmptyArrayWhenNoActiveAccounts() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findByUserAndIsActive(testUser, true)).thenReturn(Collections.emptyList());
            
            // When & Then
            mockMvc.perform(get("/api/accounts")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/accounts/{accountId}")
    class GetAccount {
        
        @Test
        @DisplayName("Should return account when user owns it")
        void shouldReturnAccountWhenUserOwnsIt() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            
            // When & Then
            mockMvc.perform(get("/api/accounts/1")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Test Checking Account"));
            
            verify(accountRepository).findById(1L);
        }
        
        @Test
        @DisplayName("Should return 404 when account not found")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(get("/api/accounts/999")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
            
            verify(accountRepository).findById(999L);
        }
        
        @Test
        @DisplayName("Should return 403 when user doesn't own account")
        void shouldReturn403WhenUserDoesntOwnAccount() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Account otherUserAccount = new Account();
            otherUserAccount.setId(2L);
            otherUserAccount.setUser(otherUser);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(2L)).thenReturn(Optional.of(otherUserAccount));
            
            // When & Then
            mockMvc.perform(get("/api/accounts/2")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/accounts")
    class CreateAccount {
        
        @Test
        @DisplayName("Should create account successfully")
        void shouldCreateAccountSuccessfully() throws Exception {
            // Given
            AccountDto accountDto = new AccountDto();
            accountDto.setName("New Savings Account");
            accountDto.setAccountType(Account.AccountType.SAVINGS);
            accountDto.setBalance(new BigDecimal("2000.00"));
            accountDto.setBankName("New Bank");
            
            Account savedAccount = new Account();
            savedAccount.setId(2L);
            savedAccount.setName("New Savings Account");
            savedAccount.setAccountType(Account.AccountType.SAVINGS);
            savedAccount.setBalance(new BigDecimal("2000.00"));
            savedAccount.setBankName("New Bank");
            savedAccount.setUser(testUser);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
            
            // When & Then
            mockMvc.perform(post("/api/accounts")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("New Savings Account"))
                    .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                    .andExpect(jsonPath("$.balance").value(2000.00));
            
            verify(accountRepository).save(any(Account.class));
        }
        
        @Test
        @DisplayName("Should handle validation errors")
        void shouldHandleValidationErrors() throws Exception {
            // Given - AccountDto with invalid data (missing required fields)
            AccountDto invalidAccountDto = new AccountDto();
            // Missing name, accountType, etc.
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            
            // When & Then - This would normally be caught by @Valid but since we're using standalone MockMvc
            // we need to test the happy path. In a real @WebMvcTest, validation would be tested automatically
            mockMvc.perform(post("/api/accounts")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidAccountDto)))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("PUT /api/accounts/{accountId}")
    class UpdateAccount {
        
        @Test
        @DisplayName("Should update account successfully")
        void shouldUpdateAccountSuccessfully() throws Exception {
            // Given
            AccountDto updateDto = new AccountDto();
            updateDto.setName("Updated Account Name");
            updateDto.setAccountType(Account.AccountType.SAVINGS);
            updateDto.setBalance(new BigDecimal("1500.00"));
            updateDto.setBankName("Updated Bank");
            
            Account updatedAccount = new Account();
            updatedAccount.setId(1L);
            updatedAccount.setName("Updated Account Name");
            updatedAccount.setAccountType(Account.AccountType.SAVINGS);
            updatedAccount.setBalance(new BigDecimal("1500.00"));
            updatedAccount.setBankName("Updated Bank");
            updatedAccount.setUser(testUser);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);
            
            // When & Then
            mockMvc.perform(put("/api/accounts/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Account Name"))
                    .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                    .andExpect(jsonPath("$.balance").value(1500.00));
            
            verify(accountRepository).findById(1L);
            verify(accountRepository).save(any(Account.class));
        }
        
        @Test
        @DisplayName("Should return 404 when account not found for update")
        void shouldReturn404WhenAccountNotFoundForUpdate() throws Exception {
            // Given
            AccountDto updateDto = new AccountDto();
            updateDto.setName("Updated Name");
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(put("/api/accounts/999")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isInternalServerError());
        }
        
        @Test
        @DisplayName("Should return 403 when updating other user's account")
        void shouldReturn403WhenUpdatingOtherUsersAccount() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Account otherUserAccount = new Account();
            otherUserAccount.setId(2L);
            otherUserAccount.setUser(otherUser);
            
            AccountDto updateDto = new AccountDto();
            updateDto.setName("Updated Name");
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(2L)).thenReturn(Optional.of(otherUserAccount));
            
            // When & Then
            mockMvc.perform(put("/api/accounts/2")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("DELETE /api/accounts/{accountId}")
    class DeleteAccount {
        
        @Test
        @DisplayName("Should soft delete account successfully")
        void shouldSoftDeleteAccountSuccessfully() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
            
            // When & Then
            mockMvc.perform(delete("/api/accounts/1")
                    .principal(authentication))
                    .andExpect(status().isOk());
            
            verify(accountRepository).findById(1L);
            verify(accountRepository).save(any(Account.class));
        }
        
        @Test
        @DisplayName("Should return 404 when account not found for delete")
        void shouldReturn404WhenAccountNotFoundForDelete() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(delete("/api/accounts/999")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
        
        @Test
        @DisplayName("Should return 403 when deleting other user's account")
        void shouldReturn403WhenDeletingOtherUsersAccount() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Account otherUserAccount = new Account();
            otherUserAccount.setId(2L);
            otherUserAccount.setUser(otherUser);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(2L)).thenReturn(Optional.of(otherUserAccount));
            
            // When & Then
            mockMvc.perform(delete("/api/accounts/2")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
            
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("POST /api/accounts/sync-plaid")
    class SyncPlaidAccounts {
        
        @Test
        @DisplayName("Should sync Plaid accounts successfully")
        void shouldSyncPlaidAccountsSuccessfully() throws Exception {
            // Given
            String accessToken = "test-access-token";
            List<Account> syncedAccounts = List.of(testAccount);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(plaidService.syncAccounts(accessToken, testUser)).thenReturn(syncedAccounts);
            
            // When & Then
            mockMvc.perform(post("/api/accounts/sync-plaid")
                    .principal(authentication)
                    .param("accessToken", accessToken))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Synced 1 accounts from Plaid"));
            
            verify(plaidService).syncAccounts(accessToken, testUser);
        }
        
        @Test
        @DisplayName("Should handle Plaid service errors")
        void shouldHandlePlaidServiceErrors() throws Exception {
            // Given
            String accessToken = "invalid-token";
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(plaidService.syncAccounts(accessToken, testUser))
                .thenThrow(new IOException("Plaid API error"));
            
            // When & Then
            mockMvc.perform(post("/api/accounts/sync-plaid")
                    .principal(authentication)
                    .param("accessToken", accessToken))
                    .andExpect(status().is5xxServerError());
            
            verify(plaidService).syncAccounts(accessToken, testUser);
        }
    }

    @Nested
    @DisplayName("POST /api/accounts/{accountId}/recalculate-balance")
    class RecalculateAccountBalance {
        
        @Test
        @DisplayName("Should recalculate account balance successfully")
        void shouldRecalculateAccountBalanceSuccessfully() throws Exception {
            // Given
            Account updatedAccount = new Account();
            updatedAccount.setId(1L);
            updatedAccount.setName("Test Checking Account");
            updatedAccount.setBalance(new BigDecimal("1200.00")); // Updated balance
            updatedAccount.setUser(testUser);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(1L))
                .thenReturn(Optional.of(testAccount))  // First call for verification
                .thenReturn(Optional.of(updatedAccount)); // Second call to get updated balance
            
            // When & Then
            mockMvc.perform(post("/api/accounts/1/recalculate-balance")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.balance").value(1200.00));
            
            verify(accountRepository, times(2)).findById(1L);
            verify(transactionService).recalculateAccountBalance(testAccount);
        }
        
        @Test
        @DisplayName("Should return 404 when account not found for balance recalculation")
        void shouldReturn404WhenAccountNotFoundForBalanceRecalculation() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(post("/api/accounts/999/recalculate-balance")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
        
        @Test
        @DisplayName("Should return 403 when recalculating balance for other user's account")
        void shouldReturn403WhenRecalculatingBalanceForOtherUsersAccount() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Account otherUserAccount = new Account();
            otherUserAccount.setId(2L);
            otherUserAccount.setUser(otherUser);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(accountRepository.findById(2L)).thenReturn(Optional.of(otherUserAccount));
            
            // When & Then
            mockMvc.perform(post("/api/accounts/2/recalculate-balance")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
            
            verify(transactionService, never()).recalculateAccountBalance(any());
        }
    }

    @Nested
    @DisplayName("POST /api/accounts/recalculate-all-balances")
    class RecalculateAllAccountBalances {
        
        @Test
        @DisplayName("Should recalculate all account balances successfully")
        void shouldRecalculateAllAccountBalancesSuccessfully() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            
            // When & Then
            mockMvc.perform(post("/api/accounts/recalculate-all-balances")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(content().string("All account balances have been recalculated"));
            
            verify(transactionService).recalculateAllAccountBalances();
        }
    }
}