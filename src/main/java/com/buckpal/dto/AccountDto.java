package com.buckpal.dto;

import com.buckpal.entity.Account.AccountType;

import java.math.BigDecimal;

public class AccountDto {
    
    private Long id;
    private String name;
    private AccountType accountType;
    private BigDecimal balance;
    private Boolean isActive;
    private String bankName;
    
    public AccountDto() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
}