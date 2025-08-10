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
class CsvRealFileTest {
    
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
    void shouldParseRealBankCsvData() throws IOException {
        // This is actual data from the CSV file in test/resources
        String realCsvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Swile FR Montpellier;Swile FR Montpellier;A6NKPSX;080825 CB****3846-0,05EUR 1 EURO = 1,000000;Carte bancaire;Shopping et services;High-Tech/Electromenager;-0,05;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Revolut 9309 FR Paris;Revolut 9309 FR Paris;A5013LI;070825 CB****3846-35,00EUR 1 EURO = 1,000000;Carte bancaire;Banque et assurances;Banque et assurance - autre;-35,00;;08/08/2025;08/08/2025;0\n" +
            "08/08/2025;BAGGA FR TOULOUSE;BAGGA FR TOULOUSE;A5013LH;070825 CB****3846-20,00EUR 1 EURO = 1,000000;Carte bancaire;Alimentation;Restauration rapide;-20,00;;08/08/2025;08/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "real_bank.csv", "text/csv", realCsvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== Real Bank CSV Import Results ===");
        for (int i = 0; i < result.size(); i++) {
            Transaction t = result.get(i);
            System.out.println("Transaction " + (i+1) + ":");
            System.out.println("  Description: " + t.getDescription());
            System.out.println("  Merchant: " + t.getMerchantName());
            System.out.println("  Amount: " + t.getAmount());
            System.out.println("  Type: " + t.getTransactionType());
            System.out.println();
        }
        
        // Verify counts
        long incomeCount = result.stream().filter(t -> t.getTransactionType() == Transaction.TransactionType.INCOME).count();
        long expenseCount = result.stream().filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE).count();
        
        System.out.println("Total transactions: " + result.size());
        System.out.println("Income transactions: " + incomeCount);
        System.out.println("Expense transactions: " + expenseCount);
        
        assertThat(result).hasSize(3);
        assertThat(expenseCount).isEqualTo(3);  // All should be expenses (negative amounts)
        assertThat(incomeCount).isEqualTo(0);   // No income transactions
        
        // Verify specific transaction details
        Transaction swileTransaction = result.get(0);
        assertThat(swileTransaction.getDescription()).isEqualTo("Swile FR Montpellier");
        assertThat(swileTransaction.getAmount()).isEqualTo(new BigDecimal("0.05"));
        assertThat(swileTransaction.getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        
        Transaction revolutTransaction = result.get(1);
        assertThat(revolutTransaction.getAmount()).isEqualTo(new BigDecimal("35.00"));
        assertThat(revolutTransaction.getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        
        Transaction baggaTransaction = result.get(2);
        assertThat(baggaTransaction.getDescription()).isEqualTo("BAGGA FR TOULOUSE");
        assertThat(baggaTransaction.getAmount()).isEqualTo(new BigDecimal("20.00"));
        assertThat(baggaTransaction.getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
    }
}