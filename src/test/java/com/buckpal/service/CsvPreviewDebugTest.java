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
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvPreviewDebugTest {
    
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
    void debugRealCsvPreview() throws IOException {
        // Utiliser le vrai fichier CSV
        ClassPathResource resource = new ClassPathResource("09082025_659915.csv");
        byte[] csvContent = Files.readAllBytes(resource.getFile().toPath());
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            com.buckpal.entity.Transaction t = invocation.getArgument(0);
            t.setId(System.currentTimeMillis());
            return t;
        });
        
        MockMultipartFile file = new MockMultipartFile(
            "file", "09082025_659915.csv", "text/csv", csvContent);
        
        // Upload du fichier
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        System.out.println("\n=== Analyse du fichier CSV ===");
        System.out.println("Nombre total de lignes: " + uploadResponse.getTotalRows());
        System.out.println("En-têtes détectées: " + uploadResponse.getHeaders().size());
        System.out.println("Aperçu des données brutes: " + uploadResponse.getPreviewData().size() + " lignes");
        
        // Configuration du mapping (format banque française)
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0); // Date de comptabilisation
        mappingRequest.setDescriptionColumnIndex(1); // Libelle simplifie
        mappingRequest.setDebitColumnIndex(8); // Debit
        mappingRequest.setCreditColumnIndex(9); // Credit
        
        // Traitement du mapping et création de l'aperçu
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        System.out.println("\n=== Résultats du traitement ===");
        System.out.println("Transactions totales traitées: " + previewResponse.getTotalProcessed());
        System.out.println("Transactions valides: " + previewResponse.getValidCount());
        System.out.println("Erreurs: " + previewResponse.getErrorCount());
        System.out.println("Doublons: " + previewResponse.getDuplicateCount());
        System.out.println("Aperçu affiché: " + previewResponse.getValidTransactions().size() + " transactions");
        
        // Analyser les types de transactions dans l'aperçu
        long incomeCount = previewResponse.getValidTransactions().stream()
                .filter(t -> "INCOME".equals(t.getTransactionType()))
                .count();
        
        long expenseCount = previewResponse.getValidTransactions().stream()
                .filter(t -> "EXPENSE".equals(t.getTransactionType()))
                .count();
        
        System.out.println("\n=== Composition de l'aperçu ===");
        System.out.println("Revenus (INCOME): " + incomeCount);
        System.out.println("Dépenses (EXPENSE): " + expenseCount);
        
        System.out.println("\n=== Détail des transactions de l'aperçu ===");
        for (int i = 0; i < previewResponse.getValidTransactions().size(); i++) {
            var transaction = previewResponse.getValidTransactions().get(i);
            System.out.println("Transaction " + (i+1) + ":");
            System.out.println("  Ligne: " + transaction.getRowIndex());
            System.out.println("  Description: " + transaction.getDescription());
            System.out.println("  Montant: " + transaction.getAmount());
            System.out.println("  Type: " + transaction.getTransactionType());
            System.out.println("  Date: " + transaction.getTransactionDate());
        }
        
        // Afficher les erreurs s'il y en a
        if (previewResponse.getErrorCount() > 0) {
            System.out.println("\n=== Erreurs détectées ===");
            for (var error : previewResponse.getValidationErrors()) {
                System.out.println("Ligne " + error.getRowIndex() + ": " + error.getError());
            }
        }
        
        // On s'attend à avoir des transactions
        assertThat(previewResponse.getValidCount()).isGreaterThan(0);
    }
}