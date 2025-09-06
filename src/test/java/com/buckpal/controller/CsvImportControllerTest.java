package com.buckpal.controller;

import com.buckpal.service.CsvImportWizardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CsvImportController Unit Tests")
class CsvImportControllerTest {

    @Mock
    private CsvImportWizardService csvImportWizardService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CsvImportController csvImportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(csvImportController).build();
    }

    @Test
    @DisplayName("Should upload CSV file successfully")
    void shouldUploadCsvFileSuccessfully() throws Exception {
        // Given
        MockMultipartFile csvFile = new MockMultipartFile(
            "file", 
            "transactions.csv", 
            "text/csv", 
            "date,amount,description\n2024-01-01,100.00,Test".getBytes()
        );
        
        Map<String, Object> uploadResult = Map.of(
            "sessionId", "session123",
            "rowCount", 1,
            "headers", List.of("date", "amount", "description")
        );
        
        when(csvImportWizardService.uploadAndParseFile(any(), any()))
            .thenReturn(uploadResult);
        
        // When & Then
        mockMvc.perform(multipart("/api/csv-import/upload")
                .file(csvFile)
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session123"))
                .andExpect(jsonPath("$.rowCount").value(1));
        
        verify(csvImportWizardService).uploadAndParseFile(any(), any());
    }

    @Test
    @DisplayName("Should get CSV templates")
    void shouldGetCsvTemplates() throws Exception {
        // Given
        List<Map<String, Object>> templates = List.of(
            Map.of("bankName", "Chase", "columns", List.of("date", "amount", "description"))
        );
        
        when(csvImportWizardService.getMappingTemplates(any()))
            .thenReturn(templates);
        
        // When & Then
        mockMvc.perform(get("/api/csv-import/templates")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bankName").value("Chase"));
        
        verify(csvImportWizardService).getMappingTemplates(any());
    }

    @Test
    @DisplayName("Should get CSV template for download")
    void shouldGetCsvTemplateForDownload() throws Exception {
        // Given
        String templateCsv = "date,amount,description,category\n";
        
        when(csvImportWizardService.generateCsvTemplate()).thenReturn(templateCsv);
        
        // When & Then
        mockMvc.perform(get("/api/csv-import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(content().string(templateCsv));
        
        verify(csvImportWizardService).generateCsvTemplate();
    }
}