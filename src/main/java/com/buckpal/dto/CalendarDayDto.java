package com.buckpal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CalendarDayDto {
    
    private LocalDate date;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netAmount;
    private Long transactionCount;
    
    public CalendarDayDto() {}
    
    public CalendarDayDto(LocalDate date, BigDecimal totalIncome, BigDecimal totalExpense, 
                         BigDecimal netAmount, Long transactionCount) {
        this.date = date;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.netAmount = netAmount;
        this.transactionCount = transactionCount;
    }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
    
    public BigDecimal getTotalExpense() { return totalExpense; }
    public void setTotalExpense(BigDecimal totalExpense) { this.totalExpense = totalExpense; }
    
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    
    public Long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Long transactionCount) { this.transactionCount = transactionCount; }
}