package com.buckpal.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests pour la détection automatique du séparateur CSV
 */
class CsvSeparatorDetectionTest {
    
    private final CsvImportWizardService service = new CsvImportWizardService();
    
    @Test
    void detectSeparator_ShouldDetectSemicolon_ForFrenchFormat() throws Exception {
        // Given
        String headerLine = "Date de comptabilisation;Libelle simplifie;Debit;Credit";
        
        // When
        String separator = invokeDetectSeparator(headerLine);
        
        // Then
        assertThat(separator).isEqualTo(";");
    }
    
    @Test
    void detectSeparator_ShouldDetectComma_ForEnglishFormat() throws Exception {
        // Given
        String headerLine = "Date,Description,Amount,Category";
        
        // When
        String separator = invokeDetectSeparator(headerLine);
        
        // Then
        assertThat(separator).isEqualTo(",");
    }
    
    @Test
    void parseCsvLine_ShouldParseFrenchBankFormat() throws Exception {
        // Given
        String line = "09/08/2025;Swile FR Montpellier;-0,05;;09/08/2025;09/08/2025;0";
        String separator = ";";
        
        // When
        List<String> result = invokeParseCsvLine(line, separator);
        
        // Then
        assertThat(result).hasSize(7);
        assertThat(result.get(0)).isEqualTo("09/08/2025");
        assertThat(result.get(1)).isEqualTo("Swile FR Montpellier");
        assertThat(result.get(2)).isEqualTo("-0,05");
        assertThat(result.get(3)).isEqualTo(""); // Empty credit column
        assertThat(result.get(4)).isEqualTo("09/08/2025");
    }
    
    @Test
    void uploadCsv_ShouldCorrectlyParseFrenchBankFile() throws IOException {
        // Given - Simuler le contenu de votre fichier bancaire
        String csvContent = "Date de comptabilisation;Libelle simplifie;Libelle operation;Reference;Informations complementaires;Type operation;Categorie;Sous categorie;Debit;Credit;Date operation;Date de valeur;Pointage operation\n" +
                           "09/08/2025;Swile FR Montpellier;Swile FR Montpellier;A6NKPSX;080825 CB****3846-0,05EUR 1 EURO = 1,000000;Carte bancaire;Shopping et services;High-Tech/Electromenager;-0,05;;09/08/2025;09/08/2025;0\n" +
                           "08/08/2025;Revolut 9309 FR Paris;Revolut 9309 FR Paris;A5013LI;070825 CB****3846-35,00EUR 1 EURO = 1,000000;Carte bancaire;Banque et assurances;Banque et assurance - autre;-35,00;;08/08/2025;08/08/2025;0";
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bank_transactions.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        // When
        var response = service.uploadCsv(file);
        
        // Then
        assertThat(response.getHeaders()).hasSize(13);
        assertThat(response.getHeaders().get(0)).isEqualTo("Date de comptabilisation");
        assertThat(response.getHeaders().get(1)).isEqualTo("Libelle simplifie");
        assertThat(response.getHeaders().get(8)).isEqualTo("Debit");
        assertThat(response.getHeaders().get(9)).isEqualTo("Credit");
        
        assertThat(response.getPreviewData()).hasSize(2);
        assertThat(response.getPreviewData().get(0)).hasSize(13);
        assertThat(response.getPreviewData().get(0).get(0)).isEqualTo("09/08/2025");
        assertThat(response.getPreviewData().get(0).get(1)).isEqualTo("Swile FR Montpellier");
        assertThat(response.getPreviewData().get(0).get(8)).isEqualTo("-0,05");
        assertThat(response.getPreviewData().get(0).get(9)).isEqualTo(""); // Empty credit
    }
    
    // Helper methods to invoke private methods via reflection
    
    private String invokeDetectSeparator(String headerLine) throws Exception {
        Method method = CsvImportWizardService.class.getDeclaredMethod("detectSeparator", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, headerLine);
    }
    
    @SuppressWarnings("unchecked")
    private List<String> invokeParseCsvLine(String line, String separator) throws Exception {
        Method method = CsvImportWizardService.class.getDeclaredMethod("parseCsvLine", String.class, String.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(service, line, separator);
    }
}