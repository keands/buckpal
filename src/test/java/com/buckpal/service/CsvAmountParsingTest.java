package com.buckpal.service;

import com.buckpal.dto.csv.CsvColumnMappingRequest;
import com.buckpal.dto.csv.CsvUploadResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests spécifiques pour le parsing des montants dans différents formats
 */
@ExtendWith(MockitoExtension.class) 
class CsvAmountParsingTest {
    
    @InjectMocks
    private CsvImportWizardService csvImportWizardService;
    
    @Test
    void parseCsvLine_ShouldHandleSemicolonSeparator() throws IOException {
        // Given - CSV avec séparateur point-virgule (format français)
        String csvContent = "Date;Description;Debit;Credit\n" +
                           "09/08/2025;Test Transaction;10,50;";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "french_format.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        // When
        CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
        
        // Then
        assertThat(response.getHeaders()).hasSize(4);
        assertThat(response.getHeaders()).containsExactly("Date", "Description", "Debit", "Credit");
        assertThat(response.getPreviewData().get(0)).containsExactly("09/08/2025", "Test Transaction", "10,50", "");
    }
    
    @Test
    void parseCsvLine_ShouldHandleCommaSeparator() throws IOException {
        // Given - CSV avec séparateur virgule (format anglais)
        String csvContent = "Date,Description,Amount\n" +
                           "08/09/2025,Test Transaction,10.50";
        
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "english_format.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        // When
        CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
        
        // Then
        assertThat(response.getHeaders()).hasSize(3);
        assertThat(response.getHeaders()).containsExactly("Date", "Description", "Amount");
        assertThat(response.getPreviewData().get(0)).containsExactly("08/09/2025", "Test Transaction", "10.50");
    }
    
    @Test
    void parseAmount_ShouldHandleFrenchDecimals() throws IOException {
        // Given - Tests de différents formats de montants français
        String csvContent = "Date;Description;Amount\n" +
                           "09/08/2025;Decimal avec virgule;-10,50\n" +
                           "08/08/2025;Milliers avec espace;1 234,56\n" +
                           "07/08/2025;Parenthèses négatives;(45,67)\n" +
                           "06/08/2025;Avec devise;-25,99€\n" +
                           "05/08/2025;Montant négatif simple;-0,05";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "amount_formats.csv", 
                "text/csv",
                csvContent.getBytes()
        );
        
        // When
        CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
        
        // Then - Vérifier que tous les formats sont correctement parsés
        assertThat(response.getPreviewData()).hasSize(5);
        
        // Les montants devraient être correctement parsés par le service
        // (test indirect via l'aperçu - les valeurs resteront en format texte jusqu'au mapping)
        assertThat(response.getPreviewData().get(0).get(2)).isEqualTo("-10,50");
        assertThat(response.getPreviewData().get(1).get(2)).isEqualTo("1 234,56");
        assertThat(response.getPreviewData().get(2).get(2)).isEqualTo("(45,67)");
        assertThat(response.getPreviewData().get(3).get(2)).isEqualTo("-25,99€");
        assertThat(response.getPreviewData().get(4).get(2)).isEqualTo("-0,05");
    }
    
    @Test
    void parseAmount_ShouldHandleEnglishDecimals() throws IOException {
        // Given - Tests de différents formats de montants anglais
        String csvContent = "Date,Description,Amount\n" +
                           "08/09/2025,Decimal with dot,-10.50\n" +
                           "08/08/2025,Thousands with comma,\"1,234.56\"\n" +
                           "08/07/2025,Negative parentheses,(45.67)\n" +
                           "08/06/2025,With currency,$25.99\n" +
                           "08/05/2025,Simple negative,-0.05";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "english_amounts.csv",
                "text/csv", 
                csvContent.getBytes()
        );
        
        // When
        CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
        
        // Then - Vérifier que tous les formats sont correctement parsés
        assertThat(response.getPreviewData()).hasSize(5);
        assertThat(response.getPreviewData().get(0).get(2)).isEqualTo("-10.50");
        assertThat(response.getPreviewData().get(1).get(2)).isEqualTo("1,234.56");
        assertThat(response.getPreviewData().get(2).get(2)).isEqualTo("(45.67)");
        assertThat(response.getPreviewData().get(3).get(2)).isEqualTo("$25.99");
        assertThat(response.getPreviewData().get(4).get(2)).isEqualTo("-0.05");
    }
    
    @Test
    void parseCsvLine_ShouldHandleQuotedFields() throws IOException {
        // Given - CSV avec des champs entre guillemets contenant des séparateurs
        String csvContent = "Date;Description;Amount\n" +
                           "09/08/2025;\"Description, avec virgule\";-10,50\n" +
                           "08/08/2025;\"Description; avec point-virgule\";25,99";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "quoted_fields.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        // When
        CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
        
        // Then - Les guillemets doivent être supprimés mais le contenu préservé
        assertThat(response.getPreviewData()).hasSize(2);
        assertThat(response.getPreviewData().get(0).get(1)).isEqualTo("Description, avec virgule");
        assertThat(response.getPreviewData().get(1).get(1)).isEqualTo("Description; avec point-virgule");
    }
    
    @Test
    void parseCsvLine_ShouldHandleBankStatementFormat() throws IOException {
        // Given - Format réel d'une banque française (simplifié)
        String csvContent = "Date de comptabilisation;Libelle simplifie;Debit;Credit\n" +
                           "09/08/2025;ACHAT CB MONOPRIX;45,67;\n" +
                           "08/08/2025;VIR SALAIRE;;2 500,00\n" +
                           "07/08/2025;FRAIS BANCAIRES;1,40;";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bank_statement.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        // When
        CsvUploadResponse response = csvImportWizardService.uploadCsv(file);
        
        // Then
        assertThat(response.getHeaders()).containsExactly(
                "Date de comptabilisation", "Libelle simplifie", "Debit", "Credit"
        );
        assertThat(response.getPreviewData()).hasSize(3);
        
        // Première ligne - débit seulement
        assertThat(response.getPreviewData().get(0)).containsExactly(
                "09/08/2025", "ACHAT CB MONOPRIX", "45,67", ""
        );
        
        // Deuxième ligne - crédit seulement 
        assertThat(response.getPreviewData().get(1)).containsExactly(
                "08/08/2025", "VIR SALAIRE", "", "2 500,00"
        );
        
        // Troisième ligne - débit seulement
        assertThat(response.getPreviewData().get(2)).containsExactly(
                "07/08/2025", "FRAIS BANCAIRES", "1,40", ""
        );
    }
}