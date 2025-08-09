package com.buckpal.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour les méthodes de parsing CSV
 * Tests isolés sans dépendances Spring
 */
class CsvParsingUnitTest {
    
    private final CsvImportWizardService service = new CsvImportWizardService();
    
    @Test 
    void parseCsvLine_ShouldDetectSemicolonSeparator() throws Exception {
        // Given
        String line = "09/08/2025;ACHAT CB MONOPRIX;45,67;";
        
        // When
        List<String> result = invokeParseCsvLine(line);
        
        // Then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly("09/08/2025", "ACHAT CB MONOPRIX", "45,67", "");
    }
    
    @Test
    void parseCsvLine_ShouldDetectCommaSeparator() throws Exception {
        // Given
        String line = "08/09/2025,STORE PURCHASE,45.67,FOOD";
        
        // When
        List<String> result = invokeParseCsvLine(line);
        
        // Then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly("08/09/2025", "STORE PURCHASE", "45.67", "FOOD");
    }
    
    @Test
    void parseCsvLine_ShouldHandleQuotedFieldsWithSemicolon() throws Exception {
        // Given
        String line = "09/08/2025;\"DESCRIPTION; AVEC POINT-VIRGULE\";45,67;";
        
        // When
        List<String> result = invokeParseCsvLine(line);
        
        // Then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly("09/08/2025", "DESCRIPTION; AVEC POINT-VIRGULE", "45,67", "");
    }
    
    @Test
    void parseCsvLine_ShouldHandleQuotedFieldsWithComma() throws Exception {
        // Given  
        String line = "08/09/2025,\"DESCRIPTION, WITH COMMA\",45.67,FOOD";
        
        // When
        List<String> result = invokeParseCsvLine(line);
        
        // Then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactly("08/09/2025", "DESCRIPTION, WITH COMMA", "45.67", "FOOD");
    }
    
    @Test
    void parseAmount_ShouldParseFrenchDecimalFormat() throws Exception {
        // Test différents formats français
        assertThat(invokeParseAmount("-10,50")).isEqualTo(new BigDecimal("-10.50"));
        assertThat(invokeParseAmount("1 234,56")).isEqualTo(new BigDecimal("1234.56"));
        assertThat(invokeParseAmount("(45,67)")).isEqualTo(new BigDecimal("-45.67"));
        assertThat(invokeParseAmount("-25,99€")).isEqualTo(new BigDecimal("-25.99"));
        assertThat(invokeParseAmount("-0,05")).isEqualTo(new BigDecimal("-0.05"));
    }
    
    @Test
    void parseAmount_ShouldParseEnglishDecimalFormat() throws Exception {
        // Test différents formats anglais
        assertThat(invokeParseAmount("-10.50")).isEqualTo(new BigDecimal("-10.50"));
        assertThat(invokeParseAmount("1,234.56")).isEqualTo(new BigDecimal("1234.56"));
        assertThat(invokeParseAmount("(45.67)")).isEqualTo(new BigDecimal("-45.67"));
        assertThat(invokeParseAmount("$25.99")).isEqualTo(new BigDecimal("25.99"));
        assertThat(invokeParseAmount("-0.05")).isEqualTo(new BigDecimal("-0.05"));
    }
    
    @Test
    void parseAmount_ShouldHandleEmptyOrInvalidAmounts() throws Exception {
        // Test valeurs vides et invalides
        assertThat(invokeParseAmount("")).isNull();
        assertThat(invokeParseAmount("   ")).isNull();
        assertThat(invokeParseAmount("invalid")).isNull();
        assertThat(invokeParseAmount("abc,def")).isNull();
    }
    
    @Test
    void parseAmount_ShouldHandleZeroAmounts() throws Exception {
        // Test montants zéro
        assertThat(invokeParseAmount("0")).isEqualTo(BigDecimal.ZERO);
        assertThat(invokeParseAmount("0,00")).isEqualTo(new BigDecimal("0.00"));
        assertThat(invokeParseAmount("0.00")).isEqualTo(new BigDecimal("0.00"));
    }
    
    @Test
    void parseAmount_ShouldHandleComplexFrenchFormats() throws Exception {
        // Test des formats français plus complexes
        assertThat(invokeParseAmount("12 345,67")).isEqualTo(new BigDecimal("12345.67"));
        assertThat(invokeParseAmount("-1 000,00€")).isEqualTo(new BigDecimal("-1000.00"));
        assertThat(invokeParseAmount("(999,99)")).isEqualTo(new BigDecimal("-999.99"));
    }
    
    @Test
    void parseAmount_ShouldHandleComplexEnglishFormats() throws Exception {
        // Test des formats anglais plus complexes
        assertThat(invokeParseAmount("12,345.67")).isEqualTo(new BigDecimal("12345.67"));
        assertThat(invokeParseAmount("-$1,000.00")).isEqualTo(new BigDecimal("-1000.00"));
        assertThat(invokeParseAmount("(999.99)")).isEqualTo(new BigDecimal("-999.99"));
    }
    
    // Méthodes utilitaires pour invoquer les méthodes privées via reflection
    
    private List<String> invokeParseCsvLine(String line) throws Exception {
        Method method = CsvImportWizardService.class.getDeclaredMethod("parseCsvLine", String.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(service, line);
    }
    
    private BigDecimal invokeParseAmount(String amountStr) throws Exception {
        Method method = CsvImportWizardService.class.getDeclaredMethod("parseAmount", String.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, amountStr);
    }
}