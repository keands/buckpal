package com.buckpal.service;

import com.buckpal.entity.Account;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.Transaction.TransactionType;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),  // French format first
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),  // US format second
        DateTimeFormatter.ofPattern("d/M/yyyy"),    // Single digit French first
        DateTimeFormatter.ofPattern("M/d/yyyy")     // Single digit US second
    };
    
    public List<Transaction> importTransactionsFromCsv(MultipartFile file, Long accountId) throws IOException {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        List<Transaction> transactions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                Transaction transaction = parseCsvLine(line, account);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        }
        
        return transactionRepository.saveAll(transactions);
    }
    
    private Transaction parseCsvLine(String line, Account account) {
        try {
            // Detect CSV format: comma or semicolon separated
            String[] fields;
            boolean isFrenchFormat = line.contains(";") && line.contains(",") && 
                                   line.split(";").length > line.split(",").length;
            
            if (isFrenchFormat) {
                // French bank CSV format with semicolons
                fields = line.split(";");
            } else {
                // Standard CSV format with commas
                fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            }
            
            if (fields.length < 3) {
                return null;
            }
            
            // Clean fields from quotes
            for (int i = 0; i < fields.length; i++) {
                fields[i] = fields[i].trim().replaceAll("^\"|\"$", "");
            }
            
            Transaction transaction = new Transaction();
            transaction.setAccount(account);
            
            if (isFrenchFormat && fields.length >= 10) {
                // French bank format: Date de comptabilisation;Libelle simplifie;...;Debit;Credit;...
                // Parse date (first column)
                LocalDate date = parseDate(fields[0]);
                transaction.setTransactionDate(date);
                
                // Parse description (second column - "Libelle simplifie")
                transaction.setDescription(fields[1]);
                
                // Parse merchant name from detailed description (third column - "Libelle operation")
                if (!fields[2].isEmpty()) {
                    transaction.setMerchantName(fields[2]);
                }
                
                // Parse amounts from Debit (index 8) and Credit (index 9) columns
                BigDecimal debitAmount = parseFrenchAmount(fields[8]);
                BigDecimal creditAmount = parseFrenchAmount(fields[9]);
                
                BigDecimal finalAmount;
                TransactionType transactionType;
                
                if (debitAmount.compareTo(BigDecimal.ZERO) != 0) {
                    // Debit transaction (expense) - take absolute value since debits can be negative
                    finalAmount = debitAmount.abs();
                    transactionType = TransactionType.EXPENSE;
                } else if (creditAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // Credit transaction (income)
                    finalAmount = creditAmount;
                    transactionType = TransactionType.INCOME;
                } else {
                    // No amount found, skip
                    return null;
                }
                
                transaction.setAmount(finalAmount);
                transaction.setTransactionType(transactionType);
                
            } else {
                // Standard format: Date,Description,Amount,Merchant
                // Parse date (assuming first column)
                LocalDate date = parseDate(fields[0]);
                transaction.setTransactionDate(date);
                
                // Parse description (assuming second column)
                transaction.setDescription(fields[1]);
                
                // Parse amount (assuming third column)
                BigDecimal amount = parseAmount(fields[2]);
                transaction.setAmount(amount.abs());
                
                // Determine transaction type based on amount sign
                transaction.setTransactionType(amount.compareTo(BigDecimal.ZERO) >= 0 ? 
                    TransactionType.INCOME : TransactionType.EXPENSE);
                
                // Parse merchant name if available (fourth column)
                if (fields.length > 3 && !fields[3].isEmpty()) {
                    transaction.setMerchantName(fields[3]);
                }
            }
            
            transaction.setIsPending(false);
            
            return transaction;
            
        } catch (Exception e) {
            System.err.println("Error parsing CSV line: " + line + " - " + e.getMessage());
            return null;
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        
        // Default to today if parsing fails
        return LocalDate.now();
    }
    
    private BigDecimal parseAmount(String amountStr) {
        try {
            // Remove currency symbols and spaces
            String cleanAmount = amountStr.replaceAll("[\\$€£¥,\\s]", "");
            
            // Handle parentheses for negative amounts
            if (cleanAmount.startsWith("(") && cleanAmount.endsWith(")")) {
                cleanAmount = "-" + cleanAmount.substring(1, cleanAmount.length() - 1);
            }
            
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal parseFrenchAmount(String amountStr) {
        try {
            if (amountStr == null || amountStr.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // Remove currency symbols and spaces
            String cleanAmount = amountStr.replaceAll("[\\$€£¥\\s]", "").trim();
            
            // Handle empty amounts
            if (cleanAmount.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // Handle French decimal format: replace comma with dot
            cleanAmount = cleanAmount.replace(",", ".");
            
            // Handle negative amounts (remove leading minus for debit amounts)
            boolean isNegative = cleanAmount.startsWith("-");
            if (isNegative) {
                cleanAmount = cleanAmount.substring(1);
            }
            
            // Handle parentheses for negative amounts
            if (cleanAmount.startsWith("(") && cleanAmount.endsWith(")")) {
                cleanAmount = cleanAmount.substring(1, cleanAmount.length() - 1);
                isNegative = true;
            }
            
            if (cleanAmount.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal amount = new BigDecimal(cleanAmount);
            return isNegative ? amount.negate() : amount;
            
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    public String generateCsvTemplate() {
        return "Date,Description,Amount,Merchant\n" +
               "2023-12-01,Sample Transaction,-50.00,Sample Store\n" +
               "2023-12-02,Salary,2500.00,Company Inc\n";
    }
}