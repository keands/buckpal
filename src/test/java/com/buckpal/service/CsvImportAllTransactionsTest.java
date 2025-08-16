package com.buckpal.service;

import com.buckpal.dto.csv.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvImportAllTransactionsTest {
    
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
    void shouldImportAllValidTransactions_WhenApprovedRowsIsEmpty() throws IOException {
        // Given - CSV with 6 transactions (preview will show 4, but all 6 should be imported)
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Expense 1;Merchant 1;REF001;Details;Type;Cat;SubCat;10,50;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Expense 2;Merchant 2;REF002;Details;Type;Cat;SubCat;25,75;;08/08/2025;08/08/2025;0\n" +
            "07/08/2025;Income 1;Company 1;REF003;Details;Type;Cat;SubCat;;1000,00;07/08/2025;07/08/2025;0\n" +
            "06/08/2025;Expense 3;Merchant 3;REF004;Details;Type;Cat;SubCat;50,00;;06/08/2025;06/08/2025;0\n" +
            "05/08/2025;Income 2;Company 2;REF005;Details;Type;Cat;SubCat;;2500,00;05/08/2025;05/08/2025;0\n" +
            "04/08/2025;Expense 4;Merchant 4;REF006;Details;Type;Cat;SubCat;15,25;;04/08/2025;04/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            com.buckpal.entity.Transaction t = invocation.getArgument(0);
            t.setId(System.currentTimeMillis()); // Set a fake ID
            return t;
        });
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes());
        
        // When - Upload, process mapping (shows 4-transaction preview), then import all
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(8);
        mappingRequest.setCreditColumnIndex(9);
        
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Simulate frontend behavior: empty approvedRows means import all
        CsvValidationRequest validationRequest = new CsvValidationRequest();
        validationRequest.setSessionId(uploadResponse.getSessionId());
        validationRequest.setApprovedRows(new ArrayList<>()); // Empty list = import all
        validationRequest.setRejectedRows(new ArrayList<>());
        
        CsvImportResult importResult = csvImportWizardService.finalizeImport(validationRequest);
        
        // Then - Verify results
        System.out.println("\n=== Import All Transactions Test Results ===");
        System.out.println("Preview showed: " + previewResponse.getValidTransactions().size() + " transactions");
        System.out.println("Total valid found: " + previewResponse.getValidCount());
        System.out.println("Successfully imported: " + importResult.getSuccessfulImports());
        System.out.println("Failed imports: " + importResult.getFailedImports());
        System.out.println("Skipped rows: " + importResult.getSkippedRows());
        
        // Preview shows 4 balanced transactions
        assertThat(previewResponse.getValidTransactions()).hasSize(4);
        
        // But all 6 valid transactions are found
        assertThat(previewResponse.getValidCount()).isEqualTo(6);
        
        // And all 6 transactions are imported when approvedRows is empty
        assertThat(importResult.getSuccessfulImports()).isEqualTo(6);
        assertThat(importResult.getFailedImports()).isEqualTo(0);
        assertThat(importResult.getSkippedRows()).isEqualTo(0);
    }
    
    @Test
    void shouldImportOnlySpecificTransactions_WhenApprovedRowsProvided() throws IOException {
        // Given - Same CSV as above
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Expense 1;Merchant 1;REF001;Details;Type;Cat;SubCat;10,50;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Expense 2;Merchant 2;REF002;Details;Type;Cat;SubCat;25,75;;08/08/2025;08/08/2025;0\n" +
            "07/08/2025;Income 1;Company 1;REF003;Details;Type;Cat;SubCat;;1000,00;07/08/2025;07/08/2025;0\n" +
            "06/08/2025;Expense 3;Merchant 3;REF004;Details;Type;Cat;SubCat;50,00;;06/08/2025;06/08/2025;0\n" +
            "05/08/2025;Income 2;Company 2;REF005;Details;Type;Cat;SubCat;;2500,00;05/08/2025;05/08/2025;0\n" +
            "04/08/2025;Expense 4;Merchant 4;REF006;Details;Type;Cat;SubCat;15,25;;04/08/2025;04/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            com.buckpal.entity.Transaction t = invocation.getArgument(0);
            t.setId(System.currentTimeMillis()); // Set a fake ID
            return t;
        });
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes());
        
        // When - Upload, process mapping, then import only first 2 transactions from preview
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(8);
        mappingRequest.setCreditColumnIndex(9);
        
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Simulate user selecting only first 2 transactions from preview
        CsvValidationRequest validationRequest = new CsvValidationRequest();
        validationRequest.setSessionId(uploadResponse.getSessionId());
        validationRequest.setApprovedRows(previewResponse.getValidTransactions().stream()
                .limit(2)
                .map(CsvPreviewResponse.TransactionPreview::getRowIndex)
                .toList());
        validationRequest.setRejectedRows(new ArrayList<>());
        
        CsvImportResult importResult = csvImportWizardService.finalizeImport(validationRequest);
        
        // Then - Only selected transactions are imported
        System.out.println("\n=== Import Selected Transactions Test Results ===");
        System.out.println("Approved rows: " + validationRequest.getApprovedRows().size());
        System.out.println("Successfully imported: " + importResult.getSuccessfulImports());
        System.out.println("Skipped rows: " + importResult.getSkippedRows());
        
        assertThat(importResult.getSuccessfulImports()).isEqualTo(2);
        assertThat(importResult.getSkippedRows()).isEqualTo(4); // 6 total - 2 imported = 4 skipped
    }
    
    @Test
    void shouldSkipRejectedTransactions_EvenWhenApprovedRowsEmpty() throws IOException {
        // Given
        String csvContent = 
            "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
            "09/08/2025;Expense 1;Merchant 1;REF001;Details;Type;Cat;SubCat;10,50;;09/08/2025;09/08/2025;0\n" +
            "08/08/2025;Expense 2;Merchant 2;REF002;Details;Type;Cat;SubCat;25,75;;08/08/2025;08/08/2025;0\n" +
            "07/08/2025;Income 1;Company 1;REF003;Details;Type;Cat;SubCat;;1000,00;07/08/2025;07/08/2025;0\n";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            com.buckpal.entity.Transaction t = invocation.getArgument(0);
            t.setId(System.currentTimeMillis()); // Set a fake ID
            return t;
        });
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes());
        
        // When - Reject one transaction from preview
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(8);
        mappingRequest.setCreditColumnIndex(9);
        
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Reject the first transaction (row 2)
        CsvValidationRequest validationRequest = new CsvValidationRequest();
        validationRequest.setSessionId(uploadResponse.getSessionId());
        validationRequest.setApprovedRows(new ArrayList<>()); // Empty = import all EXCEPT rejected
        validationRequest.setRejectedRows(Collections.singletonList(2)); // Reject row 2
        
        CsvImportResult importResult = csvImportWizardService.finalizeImport(validationRequest);
        
        // Then - All except rejected are imported
        System.out.println("\n=== Import With Rejections Test Results ===");
        System.out.println("Total valid: " + previewResponse.getValidCount());
        System.out.println("Rejected rows: " + validationRequest.getRejectedRows().size());
        System.out.println("Successfully imported: " + importResult.getSuccessfulImports());
        System.out.println("Skipped rows: " + importResult.getSkippedRows());
        
        assertThat(importResult.getSuccessfulImports()).isEqualTo(2); // 3 total - 1 rejected = 2 imported
        assertThat(importResult.getSkippedRows()).isEqualTo(1); // 1 rejected
    }
}