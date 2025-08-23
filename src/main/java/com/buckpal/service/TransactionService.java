package com.buckpal.service;

import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        Transaction savedTransaction = transactionRepository.save(transaction);
        updateAccountBalance(transaction.getAccount());
        return savedTransaction;
    }
    
    @Transactional
    public Transaction updateTransaction(Long transactionId, Transaction updatedTransaction) {
        Optional<Transaction> existingOpt = transactionRepository.findById(transactionId);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found");
        }
        
        Transaction existing = existingOpt.get();
        Account oldAccount = existing.getAccount();
        Account newAccount = updatedTransaction.getAccount();
        
        // Update transaction fields
        existing.setAmount(updatedTransaction.getAmount());
        existing.setDescription(updatedTransaction.getDescription());
        existing.setMerchantName(updatedTransaction.getMerchantName());
        existing.setTransactionDate(updatedTransaction.getTransactionDate());
        existing.setTransactionType(updatedTransaction.getTransactionType());
        existing.setCategory(updatedTransaction.getCategory());
        
        // Handle account change
        if (newAccount != null && !oldAccount.getId().equals(newAccount.getId())) {
            existing.setAccount(newAccount);
        }
        
        Transaction savedTransaction = transactionRepository.save(existing);
        
        // Update balances for affected accounts
        updateAccountBalance(oldAccount);
        if (newAccount != null && !oldAccount.getId().equals(newAccount.getId())) {
            updateAccountBalance(newAccount);
        }
        
        return savedTransaction;
    }
    
    @Transactional
    public void deleteTransaction(Long transactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found");
        }
        
        Transaction transaction = transactionOpt.get();
        Account account = transaction.getAccount();
        
        transactionRepository.deleteById(transactionId);
        updateAccountBalance(account);
    }
    
    @Transactional
    public void deleteAllTransactionsByAccount(Account account) {
        List<Transaction> transactions = transactionRepository.findByAccount(account);
        transactionRepository.deleteAll(transactions);
        updateAccountBalance(account);
    }
    
    @Transactional
    public void recalculateAccountBalance(Account account) {
        updateAccountBalance(account);
    }
    
    @Transactional
    public void recalculateAllAccountBalances() {
        List<Account> accounts = accountRepository.findAll();
        for (Account account : accounts) {
            updateAccountBalance(account);
        }
    }
    
    private void updateAccountBalance(Account account) {
        BigDecimal calculatedBalance = transactionRepository.calculateBalanceByAccount(account);
        account.setBalance(calculatedBalance);
        accountRepository.save(account);
    }
    
    // Read-only methods
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }
    
    public List<Transaction> findByAccount(Account account) {
        return transactionRepository.findByAccount(account);
    }
    
    public Long countByAccount(Account account) {
        return transactionRepository.countByAccount(account);
    }
}