package com.buckpal.service;

import com.buckpal.dto.csv.*;
import com.buckpal.entity.*;
import com.buckpal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CsvImportWizardServiceIntegrationTest {
    
    @Autowired
    private CsvImportWizardService csvImportWizardService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    private User testUser;
    private Account testAccount;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // Créer un utilisateur de test
        testUser = new User("John", "Doe", "john.doe@test.com", "password123");
        testUser = userRepository.save(testUser);
        
        // Créer un compte de test
        testAccount = new Account("Compte Courant", Account.AccountType.CHECKING, testUser);
        testAccount.setBankName("Banque Test");
        testAccount = accountRepository.save(testAccount);
        
        // Créer une catégorie de test
        testCategory = new Category("Alimentation", "Dépenses alimentaires");
        testCategory = categoryRepository.save(testCategory);
    }
    
    @Test
    void uploadCsv_ShouldProcessRealBankFile_WithFrenchFormat() throws IOException {
        // Given - Utilisation du fichier CSV réel
        ClassPathResource resource = new ClassPathResource("09082025_659915.csv");
        byte[] csvContent = Files.readAllBytes(resource.getFile().toPath());
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent
        );
        
        // When
        CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getHeaders()).hasSize(13);
        assertThat(response.getHeaders().get(0)).isEqualTo("Date de comptabilisation");
        assertThat(response.getHeaders().get(8)).isEqualTo("Debit");
        assertThat(response.getHeaders().get(9)).isEqualTo("Credit");
        assertThat(response.getTotalRows()).isGreaterThan(100); // Le fichier a beaucoup de lignes
        assertThat(response.getPreviewData()).hasSize(10); // Limité à 10 pour aperçu
    }
    
    @Test
    void processMappingAndPreview_ShouldHandleFrenchBankFormat() throws IOException {
        // Given - Upload du fichier réel
        ClassPathResource resource = new ClassPathResource("09082025_659915.csv");
        byte[] csvContent = Files.readAllBytes(resource.getFile().toPath());
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv", 
                "text/csv",
                csvContent
        );
        
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        // Configuration du mapping pour format banque française
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0); // "Date de comptabilisation"
        mappingRequest.setDescriptionColumnIndex(1); // "Libelle simplifie"
        mappingRequest.setDebitColumnIndex(8); // "Debit"
        mappingRequest.setCreditColumnIndex(9); // "Credit"
        mappingRequest.setCategoryColumnIndex(7); // "Sous categorie"
        mappingRequest.setBankName("Banque Française");
        mappingRequest.setSaveMapping(true);
        
        // When
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Then
        assertThat(previewResponse).isNotNull();
        assertThat(previewResponse.getValidTransactions()).isNotEmpty();
        assertThat(previewResponse.getTotalProcessed()).isGreaterThan(0);
        assertThat(previewResponse.getValidCount()).isGreaterThan(0);
        
        // Vérifier qu'au moins une transaction est correctement parsée
        CsvPreviewResponse.TransactionPreview firstTransaction = previewResponse.getValidTransactions().get(0);
        assertThat(firstTransaction.getTransactionDate()).isNotNull();
        assertThat(firstTransaction.getAmount()).isNotNull();
        assertThat(firstTransaction.getDescription()).isNotBlank();
        assertThat(firstTransaction.getTransactionType()).isIn("INCOME", "EXPENSE");
    }
    
    @Test 
    void finalizeImport_ShouldCreateTransactions_FromRealData() throws IOException {
        // Given - Configuration complète du workflow
        ClassPathResource resource = new ClassPathResource("09082025_659915.csv");
        byte[] csvContent = Files.readAllBytes(resource.getFile().toPath());
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv", 
                csvContent
        );
        
        // Étape 1: Upload
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        // Étape 2: Mapping 
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(8);
        mappingRequest.setCreditColumnIndex(9);
        mappingRequest.setBankName("Banque Test");
        mappingRequest.setSaveMapping(false);
        
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Étape 3: Validation - Approuver seulement les 5 premières transactions valides
        CsvValidationRequest validationRequest = new CsvValidationRequest();
        validationRequest.setSessionId(uploadResponse.getSessionId());
        
        // Prendre les 5 premiers numéros de ligne des transactions valides
        validationRequest.setApprovedRows(
                previewResponse.getValidTransactions().stream()
                        .limit(5)
                        .map(CsvPreviewResponse.TransactionPreview::getRowIndex)
                        .toList()
        );
        
        // When - Import final
        CsvImportResult importResult = csvImportWizardService.finalizeImport(validationRequest);
        
        // Then
        assertThat(importResult).isNotNull();
        assertThat(importResult.getSuccessfulImports()).isEqualTo(5);
        assertThat(importResult.getImportedTransactionIds()).hasSize(5);
        
        // Vérifier que les transactions sont bien en base
        assertThat(transactionRepository.findById(importResult.getImportedTransactionIds().get(0))).isPresent();
        
        // Vérifier qu'une transaction a les bonnes données
        Transaction savedTransaction = transactionRepository.findById(importResult.getImportedTransactionIds().get(0)).get();
        assertThat(savedTransaction.getAccount().getId()).isEqualTo(testAccount.getId());
        assertThat(savedTransaction.getTransactionDate()).isNotNull();
        assertThat(savedTransaction.getAmount()).isNotNull();
        assertThat(savedTransaction.getDescription()).isNotBlank();
    }
    
    @Test
    void duplicateDetection_ShouldFindExistingTransactions() throws IOException {
        // Given - Créer une transaction existante
        Transaction existingTransaction = new Transaction(
                new BigDecimal("0.05"),
                "Swile FR Montpellier", 
                LocalDate.of(2025, 8, 9),
                Transaction.TransactionType.EXPENSE,
                testAccount
        );
        transactionRepository.save(existingTransaction);
        
        // Upload du CSV qui contient une transaction similaire
        String csvContent = "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
                           "09/08/2025;Swile FR Montpellier;Swile FR Montpellier;A6NKPSX;080825 CB****3846-0,05EUR 1 EURO = 1,000000;Carte bancaire;Shopping et services;High-Tech/Electromenager;-0,05;;09/08/2025;09/08/2025;0";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicate_test.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(8);
        mappingRequest.setCreditColumnIndex(9);
        
        // When
        CsvPreviewResponse previewResponse = csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Then - Le doublon doit être détecté
        assertThat(previewResponse.getDuplicateWarnings()).hasSize(1);
        assertThat(previewResponse.getDuplicateWarnings().get(0).getExistingTransactionId())
                .isEqualTo(existingTransaction.getId());
    }
    
    @Test
    void mappingTemplate_ShouldBeSavedAndReused() throws IOException {
        // Given - Configuration avec sauvegarde du template
        String csvContent = "Date;Description;Debit;Credit\n" +
                           "09/08/2025;Test Transaction;10,50;";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "template_test.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        CsvUploadResponse uploadResponse = csvImportWizardService.uploadCsv(file);
        
        CsvColumnMappingRequest mappingRequest = new CsvColumnMappingRequest();
        mappingRequest.setSessionId(uploadResponse.getSessionId());
        mappingRequest.setAccountId(testAccount.getId());
        mappingRequest.setDateColumnIndex(0);
        mappingRequest.setDescriptionColumnIndex(1);
        mappingRequest.setDebitColumnIndex(2);
        mappingRequest.setCreditColumnIndex(3);
        mappingRequest.setBankName("Ma Banque Template");
        mappingRequest.setSaveMapping(true);
        
        // When - Traiter le mapping (cela sauvegarde le template)
        csvImportWizardService.processMappingAndPreview(mappingRequest);
        
        // Then - Le template doit être sauvegardé et récupérable
        var savedTemplates = csvImportWizardService.getMappingTemplates(testUser.getId());
        assertThat(savedTemplates).hasSize(1);
        assertThat(savedTemplates.get(0).getBankName()).isEqualTo("Ma Banque Template");
        
        // Test de réutilisation du template
        String sessionId2 = "new-session";
        CsvColumnMappingRequest reusedMapping = csvImportWizardService
                .applySavedMapping(sessionId2, "Ma Banque Template", testUser.getId());
        
        assertThat(reusedMapping.getSessionId()).isEqualTo(sessionId2);
        assertThat(reusedMapping.getDateColumnIndex()).isEqualTo(0);
        assertThat(reusedMapping.getDescriptionColumnIndex()).isEqualTo(1);
        assertThat(reusedMapping.getDebitColumnIndex()).isEqualTo(2);
        assertThat(reusedMapping.getCreditColumnIndex()).isEqualTo(3);
    }
}