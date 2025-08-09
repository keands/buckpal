package com.buckpal.dto.csv;

import java.util.List;
import java.util.Map;

public class CsvValidationRequest {
    private String sessionId;
    private List<Integer> approvedRows; // Row indexes to import
    private List<Integer> rejectedRows; // Row indexes to skip
    private Map<Integer, ManualCorrection> manualCorrections; // Row index -> corrections
    
    public CsvValidationRequest() {}
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public List<Integer> getApprovedRows() { return approvedRows; }
    public void setApprovedRows(List<Integer> approvedRows) { this.approvedRows = approvedRows; }
    
    public List<Integer> getRejectedRows() { return rejectedRows; }
    public void setRejectedRows(List<Integer> rejectedRows) { this.rejectedRows = rejectedRows; }
    
    public Map<Integer, ManualCorrection> getManualCorrections() { return manualCorrections; }
    public void setManualCorrections(Map<Integer, ManualCorrection> manualCorrections) { this.manualCorrections = manualCorrections; }
    
    public static class ManualCorrection {
        private String correctedDate;
        private String correctedAmount;
        private String correctedDescription;
        private Long categoryId;
        
        public ManualCorrection() {}
        
        public String getCorrectedDate() { return correctedDate; }
        public void setCorrectedDate(String correctedDate) { this.correctedDate = correctedDate; }
        
        public String getCorrectedAmount() { return correctedAmount; }
        public void setCorrectedAmount(String correctedAmount) { this.correctedAmount = correctedAmount; }
        
        public String getCorrectedDescription() { return correctedDescription; }
        public void setCorrectedDescription(String correctedDescription) { this.correctedDescription = correctedDescription; }
        
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    }
}