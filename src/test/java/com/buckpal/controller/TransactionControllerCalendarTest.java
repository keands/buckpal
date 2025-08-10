package com.buckpal.controller;

import com.buckpal.dto.CalendarDayDto;
import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerCalendarTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Account testAccount;

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
    }

    @Test
    void getCalendarData_ShouldReturnCalendarDayDtos() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        List<Account> userAccounts = List.of(testAccount);
        List<Object[]> expectedCalendarData = List.of(
            new Object[]{
                LocalDate.of(2024, 1, 15),
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                3L
            },
            new Object[]{
                LocalDate.of(2024, 1, 16),
                new BigDecimal("0.00"),
                new BigDecimal("250.00"),
                new BigDecimal("-250.00"),
                2L
            }
        );

        when(authentication.getPrincipal()).thenReturn(testUser);
        when(accountRepository.findByUser(testUser)).thenReturn(userAccounts);
        when(transactionRepository.findCalendarDataRawByAccountsAndDateRange(
            eq(userAccounts), 
            eq(startDate), 
            eq(endDate)
        )).thenReturn(expectedCalendarData);

        // When & Then
        mockMvc.perform(get("/api/transactions/calendar")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2024-01-15"))
                .andExpect(jsonPath("$[0].totalIncome").value(1000.00))
                .andExpect(jsonPath("$[0].totalExpense").value(500.00))
                .andExpect(jsonPath("$[0].netAmount").value(500.00))
                .andExpect(jsonPath("$[0].transactionCount").value(3))
                .andExpect(jsonPath("$[1].date").value("2024-01-16"))
                .andExpect(jsonPath("$[1].totalIncome").value(0.00))
                .andExpect(jsonPath("$[1].totalExpense").value(250.00))
                .andExpect(jsonPath("$[1].netAmount").value(-250.00))
                .andExpect(jsonPath("$[1].transactionCount").value(2));

        verify(accountRepository).findByUser(testUser);
        verify(transactionRepository).findCalendarDataRawByAccountsAndDateRange(
            eq(userAccounts), eq(startDate), eq(endDate)
        );
    }

    @Test
    void getCalendarData_WithInvalidDateFormat_ShouldReturn400() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/transactions/calendar")
                .param("startDate", "invalid-date")
                .param("endDate", "2024-01-31")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCalendarData_WithEmptyResult_ShouldReturnEmptyArray() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        
        List<Account> userAccounts = List.of(testAccount);
        List<Object[]> emptyCalendarData = List.of();

        when(authentication.getPrincipal()).thenReturn(testUser);
        when(accountRepository.findByUser(testUser)).thenReturn(userAccounts);
        when(transactionRepository.findCalendarDataRawByAccountsAndDateRange(
            eq(userAccounts), 
            eq(startDate), 
            eq(endDate)
        )).thenReturn(emptyCalendarData);

        // When & Then
        mockMvc.perform(get("/api/transactions/calendar")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-01-31")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }
}