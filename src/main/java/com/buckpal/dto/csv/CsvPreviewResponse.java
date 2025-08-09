package com.buckpal.dto.csv;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CsvPreviewResponse {
    private String sessionId;
    private List<TransactionPreview> validTransactions;
    private List<ValidationError> validationErrors;
    private List<DuplicateDetection> duplicateWarnings;
    private int totalProcessed;
    private int validCount;
    private int errorCount;
    private int duplicateCount;
    
    public CsvPreviewResponse() {}
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public List<TransactionPreview> getValidTransactions() { return validTransactions; }
    public void setValidTransactions(List<TransactionPreview> validTransactions) { this.validTransactions = validTransactions; }
    
    public List<ValidationError> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<ValidationError> validationErrors) { this.validationErrors = validationErrors; }
    
    public List<DuplicateDetection> getDuplicateWarnings() { return duplicateWarnings; }
    public void setDuplicateWarnings(List<DuplicateDetection> duplicateWarnings) { this.duplicateWarnings = duplicateWarnings; }
    
    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
    
    public int getValidCount() { return validCount; }
    public void setValidCount(int validCount) { this.validCount = validCount; }
    
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    
    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
    
    public static class TransactionPreview {
        private int rowIndex;
        private LocalDate transactionDate;
        private BigDecimal amount;
        private String description;
        private String category;
        private String transactionType;
        
        public TransactionPreview() {}
        
        public TransactionPreview(int rowIndex, LocalDate transactionDate, BigDecimal amount, 
                                String description, String category, String transactionType) {
            this.rowIndex = rowIndex;
            this.transactionDate = transactionDate;
            this.amount = amount;
            this.description = description;
            this.category = category;
            this.transactionType = transactionType;
        }
        
        public int getRowIndex() { return rowIndex; }
        public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }
        
        public LocalDate getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    }
    
    public static class ValidationError {
        private int rowIndex;
        private String error;
        private String rawData;
        private String field;
        
        public ValidationError() {}
        
        public ValidationError(int rowIndex, String error, String rawData, String field) {
            this.rowIndex = rowIndex;
            this.error = error;
            this.rawData = rawData;
            this.field = field;
        }
        
        public int getRowIndex() { return rowIndex; }
        public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getRawData() { return rawData; }
        public void setRawData(String rawData) { this.rawData = rawData; }
        
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
    }
    
    public static class DuplicateDetection {
        private int rowIndex;
        private LocalDate transactionDate;
        private BigDecimal amount;
        private String description;
        private Long existingTransactionId;
        
        public DuplicateDetection() {}
        
        public DuplicateDetection(int rowIndex, LocalDate transactionDate, BigDecimal amount, 
                                String description, Long existingTransactionId) {
            this.rowIndex = rowIndex;
            this.transactionDate = transactionDate;
            this.amount = amount;
            this.description = description;
            this.existingTransactionId = existingTransactionId;
        }
        
        public int getRowIndex() { return rowIndex; }
        public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }
        
        public LocalDate getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Long getExistingTransactionId() { return existingTransactionId; }
        public void setExistingTransactionId(Long existingTransactionId) { this.existingTransactionId = existingTransactionId; }
    }
}