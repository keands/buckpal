package com.buckpal.service;

import com.buckpal.dto.csv.CsvColumnMappingRequest;
import com.buckpal.dto.csv.CsvPreviewResponse;
import com.buckpal.dto.csv.CsvUploadResponse;
import com.buckpal.entity.Account;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.CategoryRepository;
import com.buckpal.repository.CsvMappingTemplateRepository;
import com.buckpal.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvBalancedPreviewTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @Mock
    private CsvMappingTemplateRepository csvMappingTemplateRepository;
    
    @InjectMocks
    private CsvImportWizardService csvImportWizardService;
    
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
    void shouldShowBalancedPreview_With2Income2Expense() throws IOException {
        // Given - CSV with mixed income and expense transactions
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Expense 1;Merchant 1;REF001;Details;Type;Cat;SubCat;10,50;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Expense 2;Merchant 2;REF002;Details;Type;Cat;SubCat;25,75;;08/08/2025;08/08/2025;0\n" +
            "07/08/2025;Income 1;Company 1;REF003;Details;Type;Cat;SubCat;;1000,00;07/08/2025;07/08/2025;0\n" +
            "06/08/2025;Expense 3;Merchant 3;REF004;Details;Type;Cat;SubCat;50,00;;06/08/2025;06/08/2025;0\n" +
            "05/08/2025;Income 2;Company 2;REF005;Details;Type;Cat;SubCat;;2500,00;05/08/2025;05/08/2025;0\n" +
            "04/08/2025;Expense 4;Merchant 4;REF006;Details;Type;Cat;SubCat;15,25;;04/08/2025;04/08/2025;0\n" +
            "03/08/2025;Income 3;Company 3;REF007;Details;Type;Cat;SubCat;;500,00;03/08/2025;03/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes());
        
        // When - Upload and process mapping
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(8);
        mappingRequest.setCreditColumnIndex(9);
        
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Then - Should show exactly 4 transactions in preview
        assertThat(previewResponse.getValidTransactions()).hasSize(4);
        
        // Count income and expense in preview
        long previewIncomeCount = previewResponse.getValidTransactions().stream()
                .filter(t -> "INCOME".equals(t.getTransactionType()))
                .count();
        
        long previewExpenseCount = previewResponse.getValidTransactions().stream()
                .filter(t -> "EXPENSE".equals(t.getTransactionType()))
                .count();
        
        System.out.println("\n=== Balanced Preview Test Results ===");
        System.out.println("Total valid transactions found: " + previewResponse.getValidCount());
        System.out.println("Preview transactions shown: " + previewResponse.getValidTransactions().size());
        System.out.println("Preview income count: " + previewIncomeCount);
        System.out.println("Preview expense count: " + previewExpenseCount);
        
        for (int i = 0; i < previewResponse.getValidTransactions().size(); i++) {
            var t = previewResponse.getValidTransactions().get(i);
            System.out.println("Preview " + (i+1) + ": " + t.getDescription() + 
                             " - " + t.getAmount() + " - " + t.getTransactionType());
        }
        
        // Should have 2 income and 2 expense transactions in preview
        assertThat(previewIncomeCount).isEqualTo(2);
        assertThat(previewExpenseCount).isEqualTo(2);
        
        // But total count should still reflect all valid transactions
        assertThat(previewResponse.getValidCount()).isEqualTo(7); // All 7 transactions are valid
    }
    
    @Test
    void shouldHandleImbalancedData_MoreExpensesThanIncome() throws IOException {
        // Given - CSV with mostly expenses (5 expenses, 1 income)
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Expense 1;Merchant 1;REF001;Details;Type;Cat;SubCat;10,50;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Expense 2;Merchant 2;REF002;Details;Type;Cat;SubCat;25,75;;08/08/2025;08/08/2025;0\n" +
            "07/08/2025;Income 1;Company 1;REF003;Details;Type;Cat;SubCat;;1000,00;07/08/2025;07/08/2025;0\n" +
            "06/08/2025;Expense 3;Merchant 3;REF004;Details;Type;Cat;SubCat;50,00;;06/08/2025;06/08/2025;0\n" +
            "05/08/2025;Expense 4;Merchant 4;REF005;Details;Type;Cat;SubCat;15,25;;05/08/2025;05/08/2025;0\n" +
            "04/08/2025;Expense 5;Merchant 5;REF006;Details;Type;Cat;SubCat;30,00;;04/08/2025;04/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes());
        
        // When
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(8);
        mappingRequest.setCreditColumnIndex(9);
        
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Then - Should still show 4 transactions, with available balance
        assertThat(previewResponse.getValidTransactions()).hasSize(4);
        
        long previewIncomeCount = previewResponse.getValidTransactions().stream()
                .filter(t -> "INCOME".equals(t.getTransactionType()))
                .count();
        
        long previewExpenseCount = previewResponse.getValidTransactions().stream()
                .filter(t -> "EXPENSE".equals(t.getTransactionType()))
                .count();
        
        System.out.println("\n=== Imbalanced Data Test Results ===");
        System.out.println("Preview income count: " + previewIncomeCount);
        System.out.println("Preview expense count: " + previewExpenseCount);
        
        // Should have 1 income and 3 expense (since we only have 1 income total)
        assertThat(previewIncomeCount).isEqualTo(1);
        assertThat(previewExpenseCount).isEqualTo(3);
        assertThat(previewResponse.getValidCount()).isEqualTo(6); // All transactions are valid
    }
    
    @Test
    void shouldHandleSmallDataset_LessThan4Transactions() throws IOException {
        // Given - CSV with only 2 transactions
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Expense 1;Merchant 1;REF001;Details;Type;Cat;SubCat;10,50;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Income 1;Company 1;REF002;Details;Type;Cat;SubCat;;1000,00;08/08/2025;08/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes());
        
        // When
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(8);
        mappingRequest.setCreditColumnIndex(9);
        
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Then - Should show all available transactions (2)
        assertThat(previewResponse.getValidTransactions()).hasSize(2);
        assertThat(previewResponse.getValidCount()).isEqualTo(2);
        
        System.out.println("\n=== Small Dataset Test Results ===");
        System.out.println("Preview size: " + previewResponse.getValidTransactions().size());
        System.out.println("Total valid: " + previewResponse.getValidCount());
    }
}