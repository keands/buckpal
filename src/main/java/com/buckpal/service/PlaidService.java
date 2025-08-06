package com.buckpal.service;

import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.Transaction.TransactionType;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;
import com.plaid.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlaidService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Value("${spring.plaid.client-id}")
    private String clientId;
    
    @Value("${spring.plaid.secret}")
    private String secret;
    
    @Value("${spring.plaid.environment}")
    private String environment;
    
    private PlaidApi plaidApi;
    
    public PlaidApi getPlaidApi() {
        if (plaidApi == null) {
            ApiClient apiClient = new ApiClient();
            
            // Configure environment
            if ("development".equals(environment)) {
                apiClient.setPlaidAdapter("https://development.plaid.com");
            } else if ("production".equals(environment)) {
                apiClient.setPlaidAdapter("https://production.plaid.com");
            } else {
                apiClient.setPlaidAdapter("https://sandbox.plaid.com");
            }
            
            plaidApi = apiClient.createService(PlaidApi.class);
        }
        return plaidApi;
    }
    
    public LinkTokenCreateResponse createLinkToken(User user) {
        LinkTokenCreateRequest request = new LinkTokenCreateRequest()
            .clientId(clientId)
            .secret(secret)
            .clientName("BuckPal")
            .countryCodes(List.of(CountryCode.US))
            .language("en")
            .user(new LinkTokenCreateRequestUser().clientUserId(user.getId().toString()))
            .products(List.of(Products.TRANSACTIONS));
        
        try {
            return getPlaidApi().linkTokenCreate(request).execute().body();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create link token: " + e.getMessage(), e);
        }
    }
    
    public ItemPublicTokenExchangeResponse exchangePublicToken(String publicToken) {
        ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest()
            .clientId(clientId)
            .secret(secret)
            .publicToken(publicToken);
        
        try {
            return getPlaidApi().itemPublicTokenExchange(request).execute().body();
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange public token: " + e.getMessage(), e);
        }
    }
    
    public List<Account> syncAccounts(String accessToken, User user) {
        AccountsGetRequest request = new AccountsGetRequest()
            .clientId(clientId)
            .secret(secret)
            .accessToken(accessToken);
        
        try {
            AccountsGetResponse response = getPlaidApi().accountsGet(request).execute().body();
            List<Account> accounts = new ArrayList<>();
            
            for (AccountBase plaidAccount : response.getAccounts()) {
                Account account = createOrUpdateAccount(plaidAccount, user, response.getItem().getItemId());
                accounts.add(account);
            }
            
            return accountRepository.saveAll(accounts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync accounts: " + e.getMessage(), e);
        }
    }
    
    public List<Transaction> syncTransactions(String accessToken, LocalDate startDate, LocalDate endDate) {
        TransactionsGetRequest request = new TransactionsGetRequest()
            .clientId(clientId)
            .secret(secret)
            .accessToken(accessToken)
            .startDate(startDate)
            .endDate(endDate);
        
        try {
            TransactionsGetResponse response = getPlaidApi().transactionsGet(request).execute().body();
            List<Transaction> transactions = new ArrayList<>();
            
            for (com.plaid.client.model.Transaction plaidTransaction : response.getTransactions()) {
                Optional<Transaction> existingTransaction = transactionRepository
                    .findByPlaidTransactionId(plaidTransaction.getTransactionId());
                
                if (existingTransaction.isEmpty()) {
                    Transaction transaction = createTransactionFromPlaid(plaidTransaction);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                }
            }
            
            return transactionRepository.saveAll(transactions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync transactions: " + e.getMessage(), e);
        }
    }
    
    private Account createOrUpdateAccount(AccountBase plaidAccount, User user, String itemId) {
        Optional<Account> existingAccount = accountRepository.findByPlaidAccountId(plaidAccount.getAccountId());
        
        Account account = existingAccount.orElse(new Account());
        account.setName(plaidAccount.getName());
        account.setPlaidAccountId(plaidAccount.getAccountId());
        account.setPlaidItemId(itemId);
        account.setUser(user);
        
        // Map Plaid account type to our enum
        Account.AccountType accountType = mapPlaidAccountType(plaidAccount.getType());
        account.setAccountType(accountType);
        
        // Set balance if available
        if (plaidAccount.getBalances() != null && plaidAccount.getBalances().getCurrent() != null) {
            account.setBalance(BigDecimal.valueOf(plaidAccount.getBalances().getCurrent()));
        }
        
        return account;
    }
    
    private Transaction createTransactionFromPlaid(com.plaid.client.model.Transaction plaidTransaction) {
        Optional<Account> accountOpt = accountRepository.findByPlaidAccountId(plaidTransaction.getAccountId());
        
        if (accountOpt.isEmpty()) {
            return null;
        }
        
        Transaction transaction = new Transaction();
        
        try {
            transaction.setPlaidTransactionId(plaidTransaction.getTransactionId());
            transaction.setAccount(accountOpt.get());
            transaction.setAmount(BigDecimal.valueOf(Math.abs(plaidTransaction.getAmount())));
            transaction.setDescription(plaidTransaction.getName() != null ? plaidTransaction.getName() : "Unknown");
            transaction.setTransactionDate(plaidTransaction.getDate());
            transaction.setIsPending(plaidTransaction.getPending() != null ? plaidTransaction.getPending() : false);
            
            // Try to get merchant name - be defensive about API changes
            try {
                if (plaidTransaction.getMerchantName() != null) {
                    transaction.setMerchantName(plaidTransaction.getMerchantName());
                }
            } catch (Exception e) {
                // getMerchantName might not exist in this version
                transaction.setMerchantName("Unknown Merchant");
            }
            
            // Determine transaction type based on amount
            transaction.setTransactionType(plaidTransaction.getAmount() > 0 ? 
                TransactionType.EXPENSE : TransactionType.INCOME);
                
        } catch (Exception e) {
            // Log and return null if we can't create the transaction
            System.err.println("Error creating transaction from Plaid data: " + e.getMessage());
            return null;
        }
        
        return transaction;
    }
    
    private Account.AccountType mapPlaidAccountType(AccountType plaidType) {
        if (plaidType == null) {
            return Account.AccountType.OTHER;
        }
        
        return switch (plaidType) {
            case DEPOSITORY -> Account.AccountType.CHECKING;
            case CREDIT -> Account.AccountType.CREDIT_CARD;
            case LOAN -> Account.AccountType.LOAN;
            case INVESTMENT -> Account.AccountType.INVESTMENT;
            default -> Account.AccountType.OTHER;
        };
    }
}