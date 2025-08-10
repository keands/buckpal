package com.buckpal.service;

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
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvDebugTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private CsvImportService csvImportService;
    
    private Account testAccount;
    
    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Test Account");
        testAccount.setUser(testUser);
    }
    
    @Test
    void debugCsvImport() throws IOException {
        String csvContent = "Date,Description,Amount,Merchant\n" +
                           "2024-01-01,Negative Amount,-50.00,Test Store\n" +
                           "2024-01-02,Positive Amount,100.00,Test Company\n" +
                           "2024-01-03,Zero Amount,0.00,Test Merchant\n" +
                           "2024-01-04,Parentheses Amount,(25.75),Test Store2\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "debug.csv", "text/csv", csvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== CSV Import Debug Results ===");
        for (int i = 0; i < result.size(); i++) {
            Transaction t = result.get(i);
            System.out.println("Transaction " + (i+1) + ":");
            System.out.println("  Description: " + t.getDescription());
            System.out.println("  Amount: " + t.getAmount());
            System.out.println("  Type: " + t.getTransactionType());
            System.out.println("  Expected Type: " + getExpectedType(t.getDescription()));
            System.out.println();
        }
        
        // Verify counts
        long incomeCount = result.stream().filter(t -> t.getTransactionType() == Transaction.TransactionType.INCOME).count();
        long expenseCount = result.stream().filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE).count();
        
        System.out.println("Total transactions: " + result.size());
        System.out.println("Income transactions: " + incomeCount);
        System.out.println("Expense transactions: " + expenseCount);
        
        assertThat(result).hasSize(4);
    }
    
    private String getExpectedType(String description) {
        if (description.contains("Negative") || description.contains("Parentheses")) {
            return "EXPENSE (negative amount)";
        } else if (description.contains("Positive")) {
            return "INCOME (positive amount)";
        } else {
            return "INCOME (zero amount, >= 0)";
        }
    }
}