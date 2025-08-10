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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvFrenchFormatTest {
    
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
    void shouldParseFrenchBankCsvFormat() throws IOException {
        String frenchCsvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Swile FR Montpellier;Swile FR Montpellier;A6NKPSX;080825 CB****3846-0,05EUR 1 EURO = 1,000000;Carte bancaire;Shopping et services;High-Tech/Electromenager;0,05;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Revolut 9309 FR Paris;Revolut 9309 FR Paris;A5013LI;070825 CB****3846-35,00EUR 1 EURO = 1,000000;Carte bancaire;Banque et assurances;Banque et assurance - autre;35,00;;08/08/2025;08/08/2025;0\n" +
            "08/08/2025;Salary Deposit;Company Salary Transfer;SAL001;Monthly salary payment;Virement;Income;Salary;;2500,00;08/08/2025;08/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "french_bank.csv", "text/csv", frenchCsvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== French CSV Import Results ===");
        for (int i = 0; i < result.size(); i++) {
            Transaction t = result.get(i);
            System.out.println("Transaction " + (i+1) + ":");
            System.out.println("  Description: " + t.getDescription());
            System.out.println("  Merchant: " + t.getMerchantName());
            System.out.println("  Amount: " + t.getAmount());
            System.out.println("  Type: " + t.getTransactionType());
            System.out.println();
        }
        
        assertThat(result).hasSize(3);
        
        // First transaction - Debit (Expense)
        Transaction swileTransaction = result.get(0);
        assertThat(swileTransaction.getDescription()).isEqualTo("Swile FR Montpellier");
        assertThat(swileTransaction.getMerchantName()).isEqualTo("Swile FR Montpellier");
        assertThat(swileTransaction.getAmount()).isEqualTo(new BigDecimal("0.05"));
        assertThat(swileTransaction.getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        
        // Second transaction - Debit (Expense) 
        Transaction revolutTransaction = result.get(1);
        assertThat(revolutTransaction.getDescription()).isEqualTo("Revolut 9309 FR Paris");
        assertThat(revolutTransaction.getAmount()).isEqualTo(new BigDecimal("35.00"));
        assertThat(revolutTransaction.getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        
        // Third transaction - Credit (Income)
        Transaction salaryTransaction = result.get(2);
        assertThat(salaryTransaction.getDescription()).isEqualTo("Salary Deposit");
        assertThat(salaryTransaction.getAmount()).isEqualTo(new BigDecimal("2500.00"));
        assertThat(salaryTransaction.getTransactionType()).isEqualTo(Transaction.TransactionType.INCOME);
        
        // Verify counts
        long incomeCount = result.stream().filter(t -> t.getTransactionType() == Transaction.TransactionType.INCOME).count();
        long expenseCount = result.stream().filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE).count();
        
        System.out.println("Income transactions: " + incomeCount);
        System.out.println("Expense transactions: " + expenseCount);
        
        assertThat(incomeCount).isEqualTo(1);
        assertThat(expenseCount).isEqualTo(2);
    }
    
    @Test
    void shouldStillHandleStandardCsvFormat() throws IOException {
        String standardCsvContent = "Date,Description,Amount,Merchant\n" +
                                   "2024-01-01,McDonald's,-15.50,McDonald's\n" +
                                   "2024-01-02,Salary,2500.00,Company Inc\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "standard.csv", "text/csv", standardCsvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        assertThat(result.get(1).getTransactionType()).isEqualTo(Transaction.TransactionType.INCOME);
    }
}