package com.buckpal.service;

import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.Transaction.TransactionType;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private CsvImportService csvImportService;
    
    private Account testAccount;
    private String csvContent;
    
    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setUser(testUser);
        
        csvContent = "Date,Description,Amount,Merchant\n" +
                    "2023-12-01,McDonald's Purchase,-15.50,McDonald's\n" +
                    "2023-12-02,Salary Deposit,2500.00,Company Inc\n" +
                    "12/03/2023,Gas Station,-45.20,Shell\n";
    }
    
    @Test
    void shouldImportTransactionsFromValidCsv() throws IOException {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "transactions.csv", "text/csv", csvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        assertThat(result).hasSize(3);
        
        Transaction firstTransaction = result.get(0);
        assertThat(firstTransaction.getDescription()).isEqualTo("McDonald's Purchase");
        assertThat(firstTransaction.getAmount()).isEqualTo(new BigDecimal("15.50"));
        assertThat(firstTransaction.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(firstTransaction.getMerchantName()).isEqualTo("McDonald's");
        assertThat(firstTransaction.getTransactionDate()).isEqualTo(LocalDate.of(2023, 12, 1));
        assertThat(firstTransaction.getAccount()).isEqualTo(testAccount);
        
        Transaction secondTransaction = result.get(1);
        assertThat(secondTransaction.getTransactionType()).isEqualTo(TransactionType.INCOME);
        assertThat(secondTransaction.getAmount()).isEqualTo(new BigDecimal("2500.00"));
        
        verify(accountRepository).findById(1L);
        verify(transactionRepository).saveAll(anyList());
    }
    
    @Test
    void shouldThrowExceptionWhenAccountNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "transactions.csv", "text/csv", csvContent.getBytes());
        
        assertThatThrownBy(() -> csvImportService.importTransactionsFromCsv(file, 1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Account not found");
    }
    
    @Test
    void shouldHandleInvalidCsvLines() throws IOException {
        String invalidCsvContent = "Date,Description,Amount,Merchant\n" +
                                  "invalid-date,Test,-50.00,Test Merchant\n" +
                                  "2023-12-01,Valid Transaction,-25.00,Valid Merchant\n" +
                                  "incomplete-line\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "transactions.csv", "text/csv", invalidCsvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        assertThat(result).hasSize(2);
        
        Transaction validTransaction = result.stream()
            .filter(t -> "Valid Transaction".equals(t.getDescription()))
            .findFirst()
            .orElse(null);
        
        assertThat(validTransaction).isNotNull();
        assertThat(validTransaction.getAmount()).isEqualTo(new BigDecimal("25.00"));
    }
    
    @Test
    void shouldHandleDifferentDateFormats() throws IOException {
        String multiDateFormatCsv = "Date,Description,Amount,Merchant\n" +
                                   "2023-12-01,ISO Format,-10.00,Store1\n" +
                                   "12/02/2023,US Format,-20.00,Store2\n" +
                                   "02/12/2023,EU Format,-30.00,Store3\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "transactions.csv", "text/csv", multiDateFormatCsv.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        assertThat(result).hasSize(3);
        
        assertThat(result.get(0).getTransactionDate()).isEqualTo(LocalDate.of(2023, 12, 1));
        assertThat(result.get(1).getTransactionDate()).isEqualTo(LocalDate.of(2023, 12, 2));
    }
    
    @Test
    void shouldGenerateCsvTemplate() {
        String template = csvImportService.generateCsvTemplate();
        
        assertThat(template).contains("Date,Description,Amount,Merchant");
        assertThat(template).contains("Sample Transaction");
        assertThat(template).contains("Salary");
    }
    
    @Test
    void shouldHandleAmountWithCurrencySymbols() throws IOException {
        String csvWithCurrency = "Date,Description,Amount,Merchant\n" +
                                "2023-12-01,Test Transaction,\"$-50.00\",Test Store\n" +
                                "2023-12-02,Another Transaction,\"€25.50\",Another Store\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "transactions.csv", "text/csv", csvWithCurrency.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.get(1).getAmount()).isEqualTo(new BigDecimal("25.50"));
    }
}