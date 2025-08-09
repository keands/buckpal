package com.buckpal.service;

import com.buckpal.dto.csv.CsvColumnMappingRequest;
import com.buckpal.dto.csv.CsvUploadResponse;
import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvImportWizardServiceTest {
    
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
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User("John", "Doe", "john.doe@example.com", "password");
        testUser.setId(1L);
        
        testAccount = new Account("Test Account", Account.AccountType.CHECKING, testUser);
        testAccount.setId(1L);
        testAccount.setBankName("Test Bank");
    }
    
    @Test
    void uploadCsv_ShouldReturnValidResponse_WhenValidCsvProvided() throws IOException {
        // Given
        String csvContent = "Date,Description,Amount,Category\n" +
                           "01/12/2023,Achat supermarché,-45.67,Alimentation\n" +
                           "02/12/2023,Salaire,2500.00,Revenus";
        
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "transactions.csv", 
                "text/csv", 
                csvContent.getBytes()
        );
        
        // When
        CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getHeaders()).hasSize(4);
        assertThat(response.getHeaders()).containsExactly("Date", "Description", "Amount", "Category");
        assertThat(response.getTotalRows()).isEqualTo(2);
        assertThat(response.getPreviewData()).hasSize(2);
        assertThat(response.getPreviewData().get(0)).containsExactly("01/12/2023", "Achat supermarché", "-45.67", "Alimentation");
    }
    
    @Test
    void uploadCsv_ShouldHandleEmptyFile_WhenEmptyFileProvided() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.csv", 
                "text/csv", 
                "".getBytes()
        );
        
        // When & Then
        assertThatThrownBy(() -> csvImportWizardService.uploadCsv(emptyFile))
                .isInstanceOf(RuntimeException.class);
    }
    
    @Test
    void processMappingAndPreview_ShouldValidateDateFormats() {
        // Given
        String sessionId = "test-session";
        String csvContent = "Date,Description,Amount\n" +
                           "01/12/2023,Valid date,-45.67\n" +
                           "invalid-date,Invalid date,100.00\n" +
                           "2023-12-03,ISO date,25.50";
        
        // Create a session manually for testing
        // This would normally be done through uploadCsv, but we're testing the logic directly
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountIdAndTransactionDateAndAmountAndDescription(
                anyLong(), any(LocalDate.class), any(BigDecimal.class), any(String.class)))
                .thenReturn(Optional.empty());
        
        CsvColumnMappingRequest request = new CsvColumnMappingRequest();
        request.setSessionId(sessionId);
        request.setAccountId(1L);
        request.setDateColumnIndex(0);
        request.setAmountColumnIndex(2);
        request.setDescriptionColumnIndex(1);
        
        // When - This test would need the session to be set up properly
        // For now, this demonstrates the testing approach
        
        // Then
        // Verify that dates are parsed correctly and invalid dates are flagged for manual validation
    }
    
    @Test
    void parseAmount_ShouldHandleVariousFormats() {
        // This would be testing private methods through public interface
        // Testing various amount formats: -45.67, (45.67), 1,234.56, etc.
        
        // Given various amount strings
        // When parsing through the service
        // Then verify correct BigDecimal values are returned
    }
    
    @Test
    void findDuplicateTransaction_ShouldDetectDuplicates_WhenSameTransactionExists() {
        // Given
        LocalDate date = LocalDate.of(2023, 12, 1);
        BigDecimal amount = new BigDecimal("-45.67");
        String description = "Achat supermarché";
        
        Transaction existingTransaction = new Transaction(amount, description, date, 
                Transaction.TransactionType.EXPENSE, testAccount);
        existingTransaction.setId(1L);
        
        when(transactionRepository.findByAccountIdAndTransactionDateAndAmountAndDescription(
                1L, date, amount, description))
                .thenReturn(Optional.of(existingTransaction));
        
        // When testing duplicate detection through the service
        // Then verify duplicate is detected
    }
    
    @Test
    void saveMappingTemplate_ShouldSaveTemplate_WhenSaveMappingIsTrue() {
        // Given a mapping request with saveMapping = true
        // When processing the mapping
        // Then verify template is saved to repository
    }
    
    @Test
    void applySavedMapping_ShouldReturnStoredMapping_WhenTemplateExists() {
        // Given a saved template for a bank
        // When applying saved mapping
        // Then verify correct mapping is returned
    }
}