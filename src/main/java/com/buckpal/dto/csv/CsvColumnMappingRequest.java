package com.buckpal.dto.csv;

public class CsvColumnMappingRequest {
    private String sessionId;
    private Long accountId;
    private Integer dateColumnIndex;
    private Integer amountColumnIndex;
    private Integer debitColumnIndex;  // Optional: for separate debit column
    private Integer creditColumnIndex; // Optional: for separate credit column
    private Integer descriptionColumnIndex;
    private Integer categoryColumnIndex; // Optional
    private String bankName; // For saving mapping template
    private boolean saveMapping; // Whether to save this mapping for future use
    
    public CsvColumnMappingRequest() {}
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    
    public Integer getDateColumnIndex() { return dateColumnIndex; }
    public void setDateColumnIndex(Integer dateColumnIndex) { this.dateColumnIndex = dateColumnIndex; }
    
    public Integer getAmountColumnIndex() { return amountColumnIndex; }
    public void setAmountColumnIndex(Integer amountColumnIndex) { this.amountColumnIndex = amountColumnIndex; }
    
    public Integer getDebitColumnIndex() { return debitColumnIndex; }
    public void setDebitColumnIndex(Integer debitColumnIndex) { this.debitColumnIndex = debitColumnIndex; }
    
    public Integer getCreditColumnIndex() { return creditColumnIndex; }
    public void setCreditColumnIndex(Integer creditColumnIndex) { this.creditColumnIndex = creditColumnIndex; }
    
    public Integer getDescriptionColumnIndex() { return descriptionColumnIndex; }
    public void setDescriptionColumnIndex(Integer descriptionColumnIndex) { this.descriptionColumnIndex = descriptionColumnIndex; }
    
    public Integer getCategoryColumnIndex() { return categoryColumnIndex; }
    public void setCategoryColumnIndex(Integer categoryColumnIndex) { this.categoryColumnIndex = categoryColumnIndex; }
    
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    
    public boolean isSaveMapping() { return saveMapping; }
    public void setSaveMapping(boolean saveMapping) { this.saveMapping = saveMapping; }
}