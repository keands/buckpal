package com.buckpal.dto;

public class DeleteAllTransactionsResponse {
    
    private String message;
    private Integer deletedCount;
    
    public DeleteAllTransactionsResponse() {}
    
    public DeleteAllTransactionsResponse(String message, Integer deletedCount) {
        this.message = message;
        this.deletedCount = deletedCount;
    }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Integer getDeletedCount() { return deletedCount; }
    public void setDeletedCount(Integer deletedCount) { this.deletedCount = deletedCount; }
}