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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvElementsValidationTest {
    
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
    void shouldHandleFrenchDecimalFormat() throws IOException {
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Test Transaction;Test Merchant;REF001;Details;Type;Cat;SubCat;0,05;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Test Credit;Credit Merchant;REF002;Details;Type;Cat;SubCat;;2500,50;08/08/2025;08/08/2025;0\n" +
            "07/08/2025;Large Amount;Large Merchant;REF003;Details;Type;Cat;SubCat;1234,56;;07/08/2025;07/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "decimal_test.csv", "text/csv", csvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== French Decimal Format Test ===");
        for (Transaction t : result) {
            System.out.println("Amount: " + t.getAmount() + ", Type: " + t.getTransactionType());
        }
        
        assertThat(result).hasSize(3);
        
        // Test small decimal: 0,05 -> 0.05
        assertThat(result.get(0).getAmount()).isEqualTo(new BigDecimal("0.05"));
        assertThat(result.get(0).getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        
        // Test credit with decimal: 2500,50 -> 2500.50
        assertThat(result.get(1).getAmount()).isEqualTo(new BigDecimal("2500.50"));
        assertThat(result.get(1).getTransactionType()).isEqualTo(Transaction.TransactionType.INCOME);
        
        // Test large amount: 1234,56 -> 1234.56
        assertThat(result.get(2).getAmount()).isEqualTo(new BigDecimal("1234.56"));
        assertThat(result.get(2).getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
    }
    
    @Test
    void shouldHandleDebitCreditColumnsCorrectly() throws IOException {
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Expense Transaction;Expense Merchant;REF001;Details;Type;Cat;SubCat;50,00;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Income Transaction;Income Merchant;REF002;Details;Type;Cat;SubCat;;1000,00;08/08/2025;08/08/2025;0\n" +
            "07/08/2025;Negative Debit;Negative Merchant;REF003;Details;Type;Cat;SubCat;-25,75;;07/08/2025;07/08/2025;0\n" +
            "06/08/2025;Zero Amount;Zero Merchant;REF004;Details;Type;Cat;SubCat;0,00;;06/08/2025;06/08/2025;0\n" +
            "05/08/2025;Empty Debit;Empty Merchant;REF005;Details;Type;Cat;SubCat;;;05/08/2025;05/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "debit_credit_test.csv", "text/csv", csvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== Debit/Credit Column Test ===");
        for (Transaction t : result) {
            System.out.println("Description: " + t.getDescription() + 
                             ", Amount: " + t.getAmount() + 
                             ", Type: " + t.getTransactionType());
        }
        
        // Should only import transactions with valid amounts (3 out of 5)
        assertThat(result).hasSize(3);
        
        // Positive debit -> EXPENSE
        assertThat(result.get(0).getDescription()).isEqualTo("Expense Transaction");
        assertThat(result.get(0).getAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.get(0).getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        
        // Credit -> INCOME
        assertThat(result.get(1).getDescription()).isEqualTo("Income Transaction");
        assertThat(result.get(1).getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.get(1).getTransactionType()).isEqualTo(Transaction.TransactionType.INCOME);
        
        // Negative debit (abs value) -> EXPENSE
        assertThat(result.get(2).getDescription()).isEqualTo("Negative Debit");
        assertThat(result.get(2).getAmount()).isEqualTo(new BigDecimal("25.75"));
        assertThat(result.get(2).getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
    }
    
    @Test
    void shouldParseDateFormatsCorrectly() throws IOException {
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;DD/MM/YYYY Format;Merchant1;REF001;Details;Type;Cat;SubCat;10,00;;09/08/2025;09/08/2025;0\n" +
            "2025-08-08;YYYY-MM-DD Format;Merchant2;REF002;Details;Type;Cat;SubCat;20,00;;2025-08-08;2025-08-08;0\n" +
            "8/7/2025;M/D/YYYY Format;Merchant3;REF003;Details;Type;Cat;SubCat;30,00;;8/7/2025;8/7/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "date_format_test.csv", "text/csv", csvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== Date Format Test ===");
        for (Transaction t : result) {
            System.out.println("Description: " + t.getDescription() + 
                             ", Date: " + t.getTransactionDate());
        }
        
        assertThat(result).hasSize(3);
        
        // DD/MM/YYYY format (09/08/2025 = 9th August 2025)
        assertThat(result.get(0).getTransactionDate()).isEqualTo(LocalDate.of(2025, 8, 9));
        
        // YYYY-MM-DD format  
        assertThat(result.get(1).getTransactionDate()).isEqualTo(LocalDate.of(2025, 8, 8));
        
        // M/D/YYYY format
        assertThat(result.get(2).getTransactionDate()).isEqualTo(LocalDate.of(2025, 7, 8));
    }
    
    @Test
    void shouldExtractMerchantNamesCorrectly() throws IOException {
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Simple Description;Simple Merchant Name;REF001;Details;Type;Cat;SubCat;10,00;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Complex Desc;McDonald's Restaurant Location;REF002;Details;Type;Cat;SubCat;25,50;;08/08/2025;08/08/2025;0\n" +
            "07/08/2025;Empty Merchant;;;REF003;Details;Type;Cat;SubCat;15,75;;07/08/2025;07/08/2025;0\n" +
            "06/08/2025;Special Chars;Café & Bar - L'Étoile;REF004;Details;Type;Cat;SubCat;30,00;;06/08/2025;06/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "merchant_test.csv", "text/csv", csvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== Merchant Name Test ===");
        for (Transaction t : result) {
            System.out.println("Description: " + t.getDescription() + 
                             ", Merchant: " + t.getMerchantName());
        }
        
        assertThat(result).hasSize(4);
        
        // Simple merchant name
        assertThat(result.get(0).getDescription()).isEqualTo("Simple Description");
        assertThat(result.get(0).getMerchantName()).isEqualTo("Simple Merchant Name");
        
        // Complex merchant name with special characters
        assertThat(result.get(1).getDescription()).isEqualTo("Complex Desc");
        assertThat(result.get(1).getMerchantName()).isEqualTo("McDonald's Restaurant Location");
        
        // Empty merchant name (should not set merchant)
        assertThat(result.get(2).getDescription()).isEqualTo("Empty Merchant");
        assertThat(result.get(2).getMerchantName()).isNull();
        
        // Special characters and accents
        assertThat(result.get(3).getDescription()).isEqualTo("Special Chars");
        assertThat(result.get(3).getMerchantName()).isEqualTo("Café & Bar - L'Étoile");
    }
    
    @Test
    void shouldHandleRealWorldEdgeCases() throws IOException {
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Quoted Description;Quoted Merchant;REF001;Details;Type;Cat;SubCat;10,50;;09/08/2025;09/08/2025;0\n" +
            "07/08/2025;Very Long Description That Exceeds Normal Length And Contains Many Words;Very Long Merchant Name That Also Exceeds Normal Length;REF003;Details;Type;Cat;SubCat;5,99;;07/08/2025;07/08/2025;0\n" +
            "05/08/2025;Mixed Case Description;UPPERCASE MERCHANT;REF005;Details;Type;Cat;SubCat;100,00;;05/08/2025;05/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "edge_cases_test.csv", "text/csv", csvContent.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== Edge Cases Test ===");
        for (Transaction t : result) {
            System.out.println("Description: '" + t.getDescription() + 
                             "', Merchant: '" + t.getMerchantName() + 
                             "', Amount: " + t.getAmount());
        }
        
        // Should import 3 valid transactions
        assertThat(result).hasSize(3);
        
        // Regular fields
        assertThat(result.get(0).getDescription()).isEqualTo("Quoted Description");
        assertThat(result.get(0).getMerchantName()).isEqualTo("Quoted Merchant");
        assertThat(result.get(0).getAmount()).isEqualTo(new BigDecimal("10.50"));
        
        // Very long descriptions
        assertThat(result.get(1).getDescription()).startsWith("Very Long Description");
        assertThat(result.get(1).getMerchantName()).startsWith("Very Long Merchant");
        
        // Mixed case
        assertThat(result.get(2).getDescription()).isEqualTo("Mixed Case Description");
        assertThat(result.get(2).getMerchantName()).isEqualTo("UPPERCASE MERCHANT");
    }
    
    @Test
    void shouldMaintainBackwardCompatibilityWithStandardCsv() throws IOException {
        String standardCsv = 
            "Date,Description,Amount,Merchant\n" +
            "2025-08-09,Standard Format Test,-25.50,Standard Merchant\n" +
            "2025-08-08,Income Test,1000.00,Income Source\n" +
            "2025-08-07,Parentheses Test,(15.75),Parentheses Merchant\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "standard_csv_test.csv", "text/csv", standardCsv.getBytes());
        
        List<Transaction> result = csvImportService.importTransactionsFromCsv(file, 1L);
        
        System.out.println("\n=== Standard CSV Compatibility Test ===");
        for (Transaction t : result) {
            System.out.println("Description: " + t.getDescription() + 
                             ", Amount: " + t.getAmount() + 
                             ", Type: " + t.getTransactionType() +
                             ", Merchant: " + t.getMerchantName());
        }
        
        assertThat(result).hasSize(3);
        
        // Negative amount -> EXPENSE
        assertThat(result.get(0).getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        assertThat(result.get(0).getAmount()).isEqualTo(new BigDecimal("25.50"));
        
        // Positive amount -> INCOME
        assertThat(result.get(1).getTransactionType()).isEqualTo(Transaction.TransactionType.INCOME);
        assertThat(result.get(1).getAmount()).isEqualTo(new BigDecimal("1000.00"));
        
        // Parentheses amount -> EXPENSE
        assertThat(result.get(2).getTransactionType()).isEqualTo(Transaction.TransactionType.EXPENSE);
        assertThat(result.get(2).getAmount()).isEqualTo(new BigDecimal("15.75"));
    }
}