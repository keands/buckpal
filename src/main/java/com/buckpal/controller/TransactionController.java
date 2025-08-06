package com.buckpal.controller;

import com.buckpal.dto.TransactionDto;
import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import com.buckpal.service.CategoryService;
import com.buckpal.service.CsvImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransactionController {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private CsvImportService csvImportService;
    
    @Autowired
    private CategoryService categoryService;
    
    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User user = (User) authentication.getPrincipal();
        List<Account> userAccounts = accountRepository.findByUser(user);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionRepository
            .findByAccountsOrderByTransactionDateDesc(userAccounts, pageable);
        
        Page<TransactionDto> transactionDtos = transactions.map(this::convertToDto);
        
        return ResponseEntity.ok(transactionDtos);
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByAccount(
            Authentication authentication,
            @PathVariable Long accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        User user = (User) authentication.getPrincipal();
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        List<Transaction> transactions;
        if (startDate != null && endDate != null) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            transactions = transactionRepository.findByAccountAndTransactionDateBetween(account, start, end);
        } else {
            Pageable pageable = PageRequest.of(0, 100);
            transactions = transactionRepository.findByAccount(account, pageable).getContent();
        }
        
        List<TransactionDto> transactionDtos = transactions.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactionDtos);
    }
    
    @PostMapping("/import-csv/{accountId}")
    public ResponseEntity<?> importCsvTransactions(
            Authentication authentication,
            @PathVariable Long accountId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        User user = (User) authentication.getPrincipal();
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        List<Transaction> importedTransactions = csvImportService.importTransactionsFromCsv(file, accountId);
        
        // Auto-categorize imported transactions
        for (Transaction transaction : importedTransactions) {
            transaction.setCategory(categoryService.categorizeTransaction(transaction));
            transactionRepository.save(transaction);
        }
        
        return ResponseEntity.ok().body("Imported " + importedTransactions.size() + " transactions");
    }
    
    @GetMapping("/csv-template")
    public ResponseEntity<String> getCsvTemplate() {
        String template = csvImportService.generateCsvTemplate();
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=\"transaction_template.csv\"")
            .body(template);
    }
    
    @PutMapping("/{transactionId}/category")
    public ResponseEntity<TransactionDto> updateTransactionCategory(
            Authentication authentication,
            @PathVariable Long transactionId,
            @RequestParam Long categoryId) {
        
        User user = (User) authentication.getPrincipal();
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!transaction.getAccount().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        categoryService.getCategoryById(categoryId).ifPresent(transaction::setCategory);
        Transaction updatedTransaction = transactionRepository.save(transaction);
        
        return ResponseEntity.ok(convertToDto(updatedTransaction));
    }
    
    private TransactionDto convertToDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setMerchantName(transaction.getMerchantName());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setIsPending(transaction.getIsPending());
        dto.setAccountId(transaction.getAccount().getId());
        dto.setAccountName(transaction.getAccount().getName());
        
        if (transaction.getCategory() != null) {
            dto.setCategoryId(transaction.getCategory().getId());
            dto.setCategoryName(transaction.getCategory().getName());
        }
        
        return dto;
    }
}