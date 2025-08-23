package com.buckpal.controller;

import com.buckpal.dto.CalendarDayDto;
import com.buckpal.dto.DeleteAllTransactionsResponse;
import com.buckpal.dto.TransactionCountResponse;
import com.buckpal.dto.TransactionDto;
import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import com.buckpal.service.CategoryService;
import com.buckpal.service.CsvImportService;
import com.buckpal.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransactionController {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private CsvImportService csvImportService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private TransactionService transactionService;
    
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
        
        // Auto-categorize imported transactions and save with balance updates
        for (Transaction transaction : importedTransactions) {
            transaction.setCategory(categoryService.categorizeTransaction(transaction));
            transactionService.createTransaction(transaction);
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
    
    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(
            Authentication authentication,
            @Valid @RequestBody TransactionDto transactionDto) {
        
        User user = (User) authentication.getPrincipal();
        Account account = accountRepository.findById(transactionDto.getAccountId())
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDto.getAmount());
        transaction.setDescription(transactionDto.getDescription());
        transaction.setMerchantName(transactionDto.getMerchantName());
        transaction.setTransactionDate(transactionDto.getTransactionDate());
        transaction.setTransactionType(transactionDto.getTransactionType());
        transaction.setAccount(account);
        
        if (transactionDto.getCategoryId() != null) {
            categoryService.getCategoryById(transactionDto.getCategoryId())
                .ifPresent(transaction::setCategory);
        }
        
        Transaction savedTransaction = transactionService.createTransaction(transaction);
        return ResponseEntity.ok(convertToDto(savedTransaction));
    }
    
    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionDto> updateTransaction(
            Authentication authentication,
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionDto transactionDto) {
        
        User user = (User) authentication.getPrincipal();
        Transaction existingTransaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!existingTransaction.getAccount().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        Account account = accountRepository.findById(transactionDto.getAccountId())
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        Transaction updatedTransaction = new Transaction();
        updatedTransaction.setAmount(transactionDto.getAmount());
        updatedTransaction.setDescription(transactionDto.getDescription());
        updatedTransaction.setMerchantName(transactionDto.getMerchantName());
        updatedTransaction.setTransactionDate(transactionDto.getTransactionDate());
        updatedTransaction.setTransactionType(transactionDto.getTransactionType());
        updatedTransaction.setAccount(account);
        
        if (transactionDto.getCategoryId() != null) {
            categoryService.getCategoryById(transactionDto.getCategoryId())
                .ifPresent(updatedTransaction::setCategory);
        }
        
        Transaction savedTransaction = transactionService.updateTransaction(transactionId, updatedTransaction);
        return ResponseEntity.ok(convertToDto(savedTransaction));
    }
    
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<?> deleteTransaction(
            Authentication authentication,
            @PathVariable Long transactionId) {
        
        User user = (User) authentication.getPrincipal();
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!transaction.getAccount().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        transactionService.deleteTransaction(transactionId);
        return ResponseEntity.ok().build();
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
    
    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarDayDto>> getCalendarData(
            Authentication authentication,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        User user = (User) authentication.getPrincipal();
        List<Account> userAccounts = accountRepository.findByUser(user);
        
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        List<Object[]> rawData = transactionRepository
            .findCalendarDataRawByAccountsAndDateRange(userAccounts, start, end);
        
        List<CalendarDayDto> calendarData = rawData.stream().map(row -> {
            LocalDate date = (LocalDate) row[0];
            BigDecimal totalIncome = (BigDecimal) row[1];
            BigDecimal totalExpense = (BigDecimal) row[2];
            BigDecimal netAmount = (BigDecimal) row[3];
            Long transactionCount = (Long) row[4];
            
            return new CalendarDayDto(
                date,
                totalIncome != null ? totalIncome : BigDecimal.ZERO,
                totalExpense != null ? totalExpense : BigDecimal.ZERO,
                netAmount != null ? netAmount : BigDecimal.ZERO,
                transactionCount != null ? transactionCount : 0L
            );
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(calendarData);
    }
    
    @DeleteMapping("/account/{accountId}/all")
    public ResponseEntity<?> deleteAllTransactionsByAccount(
            Authentication authentication,
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "false") Boolean forceDelete) {
        
        User user = (User) authentication.getPrincipal();
        
        logger.info("User {} attempting to delete all transactions for account {}", 
                   user.getEmail(), accountId);
        
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Account not found");
        }
        Account account = accountOpt.get();
        
        if (!account.getUser().getId().equals(user.getId())) {
            logger.warn("User {} attempted to delete transactions for account {} they don't own", 
                       user.getEmail(), accountId);
            return ResponseEntity.status(403).body("Access denied");
        }
        
        List<Transaction> transactions = transactionRepository.findByAccount(account);
        int transactionCount = transactions.size();
        
        if (transactionCount == 0) {
            logger.info("No transactions found for account {} (user: {})", accountId, user.getEmail());
            return ResponseEntity.ok(new DeleteAllTransactionsResponse(
                "All transactions deleted successfully", 
                0
            ));
        }
        
        // Log the deletion action with details
        logger.warn("PERMANENT DELETION: User {} is deleting {} transactions from account '{}' (ID: {}). " +
                   "Force delete: {}. This action is irreversible.", 
                   user.getEmail(), transactionCount, account.getName(), accountId, forceDelete);
        
        // Perform hard delete using TransactionService to update balance
        try {
            transactionService.deleteAllTransactionsByAccount(account);
            
            logger.error("DELETION COMPLETED: {} transactions permanently deleted from account '{}' " +
                        "(ID: {}) by user {}. This action cannot be undone.", 
                        transactionCount, account.getName(), accountId, user.getEmail());
            
            return ResponseEntity.ok(new DeleteAllTransactionsResponse(
                "All transactions permanently deleted. This action cannot be undone.", 
                transactionCount
            ));
            
        } catch (Exception e) {
            logger.error("DELETION FAILED: Error deleting transactions for account {} (user: {}): {}", 
                        accountId, user.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(500)
                .body("Failed to delete transactions: " + e.getMessage());
        }
    }
    
    @GetMapping("/account/{accountId}/count")
    public ResponseEntity<?> getTransactionCountByAccount(
            Authentication authentication,
            @PathVariable Long accountId) {
        
        User user = (User) authentication.getPrincipal();
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Account not found");
        }
        Account account = accountOpt.get();
        
        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        Long count = transactionRepository.countByAccount(account);
        
        return ResponseEntity.ok(new TransactionCountResponse(count));
    }
    
    @GetMapping("/date/{date}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByDate(
            Authentication authentication,
            @PathVariable String date) {
        
        User user = (User) authentication.getPrincipal();
        
        try {
            LocalDate transactionDate = LocalDate.parse(date);
            
            // Get all user's accounts
            List<Account> userAccounts = accountRepository.findByUser(user);
            
            // Get all transactions for that date across all user accounts
            List<Transaction> transactions = transactionRepository
                    .findByAccountInAndTransactionDateOrderByTransactionDateDesc(userAccounts, transactionDate);
            
            List<TransactionDto> transactionDtos = transactions.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(transactionDtos);
            
        } catch (Exception e) {
            logger.error("Error retrieving transactions for date {}: {}", date, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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