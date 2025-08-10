package com.buckpal.dto;

public class TransactionCountResponse {
    
    private Long count;
    
    public TransactionCountResponse() {}
    
    public TransactionCountResponse(Long count) {
        this.count = count;
    }
    
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}