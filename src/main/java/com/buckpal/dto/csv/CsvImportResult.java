package com.buckpal.dto.csv;

import java.util.List;

public class CsvImportResult {
    private String sessionId;
    private int totalProcessed;
    private int successfulImports;
    private int skippedRows;
    private int failedImports;
    private List<String> errors;
    private List<Long> importedTransactionIds;
    
    public CsvImportResult() {}
    
    public CsvImportResult(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
    
    public int getSuccessfulImports() { return successfulImports; }
    public void setSuccessfulImports(int successfulImports) { this.successfulImports = successfulImports; }
    
    public int getSkippedRows() { return skippedRows; }
    public void setSkippedRows(int skippedRows) { this.skippedRows = skippedRows; }
    
    public int getFailedImports() { return failedImports; }
    public void setFailedImports(int failedImports) { this.failedImports = failedImports; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public List<Long> getImportedTransactionIds() { return importedTransactionIds; }
    public void setImportedTransactionIds(List<Long> importedTransactionIds) { this.importedTransactionIds = importedTransactionIds; }
}