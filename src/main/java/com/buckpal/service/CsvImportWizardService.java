package com.buckpal.service;

import com.buckpal.dto.csv.*;
import com.buckpal.entity.Account;
import com.buckpal.entity.Category;
import com.buckpal.entity.CsvMappingTemplate;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.repository.CategoryRepository;
import com.buckpal.repository.CsvMappingTemplateRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CsvImportWizardService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CsvMappingTemplateRepository csvMappingTemplateRepository;

    //TODO In-memory storage for CSV sessions (in production, use Redis or database)
    private final Map<String, CsvSession> csvSessions = new ConcurrentHashMap<>();

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
    };

    private final Logger logger = LoggerFactory.getLogger(CsvImportWizardService.class);

    /**
     * Step 1: Upload CSV and return preview
     */
    public CsvUploadResponse uploadCsv(MultipartFile file) throws IOException {
        String sessionId = UUID.randomUUID().toString();

        List<List<String>> allRows = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        String detectedSeparator = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    // Detect separator from header line
                    detectedSeparator = detectSeparator(line);
                }

                List<String> row = parseCsvLine(line, detectedSeparator);

                if (isFirstLine) {
                    headers = row;
                    isFirstLine = false;
                } else {
                    allRows.add(row);
                }
            }
        }

        // Store session data
        CsvSession session = new CsvSession();
        session.setSessionId(sessionId);
        session.setHeaders(headers);
        session.setAllRows(allRows);
        session.setOriginalFilename(file.getOriginalFilename());
        csvSessions.put(sessionId, session);

        // Return first 10 rows for preview
        List<List<String>> previewData = allRows.stream()
                .limit(10)
                .collect(Collectors.toList());

        return new CsvUploadResponse(sessionId, headers, previewData, allRows.size());
    }

    /**
     * Step 2: Process mapping and return preview with validation
     */
    public CsvPreviewResponse processMappingAndPreview(CsvColumnMappingRequest request) {
        CsvSession session = csvSessions.get(request.getSessionId());
        if (session == null) {
            throw new RuntimeException("CSV session not found");
        }

        session.setMapping(request);

        List<CsvPreviewResponse.TransactionPreview> validTransactions = new ArrayList<>();
        List<CsvPreviewResponse.ValidationError> validationErrors = new ArrayList<>();
        List<CsvPreviewResponse.DuplicateDetection> duplicateWarnings = new ArrayList<>();

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        for (int i = 0; i < session.getAllRows().size(); i++) {
            List<String> row = session.getAllRows().get(i);

            try {
                TransactionData transactionData = parseTransactionFromRow(row, request, i + 2); // +2 because header is row 1

                if (transactionData.hasErrors()) {
                    validationErrors.addAll(transactionData.getValidationErrors());
                } else {
                    // Check for duplicates
                    Optional<Transaction> duplicate = findDuplicateTransaction(
                            account.getId(),
                            transactionData.getDate(),
                            transactionData.getAmount(),
                            transactionData.getDescription()
                    );

                    if (duplicate.isPresent()) {
                        duplicateWarnings.add(new CsvPreviewResponse.DuplicateDetection(
                                i + 2,
                                transactionData.getDate(),
                                transactionData.getAmount(),
                                transactionData.getDescription(),
                                duplicate.get().getId()
                        ));
                    }

                    validTransactions.add(new CsvPreviewResponse.TransactionPreview(
                            i + 2,
                            transactionData.getDate(),
                            transactionData.getAmount(),
                            transactionData.getDescription(),
                            transactionData.getCategory(),
                            determineTransactionType(transactionData.getAmount())
                    ));
                }

            } catch (Exception e) {
                validationErrors.add(new CsvPreviewResponse.ValidationError(
                        i + 2,
                        "Erreur lors du traitement de la ligne: " + e.getMessage(),
                        String.join(",", row),
                        "general"
                ));
            }
        }

        // Create balanced preview (4 transactions: 2 income + 2 expense)
        List<CsvPreviewResponse.TransactionPreview> balancedPreview = createBalancedPreview(validTransactions);

        CsvPreviewResponse response = new CsvPreviewResponse();
        response.setSessionId(request.getSessionId());
        response.setValidTransactions(balancedPreview); // Show balanced preview instead of all
        response.setValidationErrors(validationErrors);
        response.setDuplicateWarnings(duplicateWarnings);
        response.setTotalProcessed(session.getAllRows().size());
        response.setValidCount(validTransactions.size()); // Keep full count for stats
        response.setErrorCount(validationErrors.size());
        response.setDuplicateCount(duplicateWarnings.size());

        return response;
    }

    /**
     * Create a balanced preview with 4 transactions (2 income + 2 expense)
     */
    private List<CsvPreviewResponse.TransactionPreview> createBalancedPreview(
            List<CsvPreviewResponse.TransactionPreview> allTransactions) {

        List<CsvPreviewResponse.TransactionPreview> incomeTransactions = allTransactions.stream()
                .filter(t -> "INCOME".equals(t.getTransactionType()))
                .toList();

        List<CsvPreviewResponse.TransactionPreview> expenseTransactions = allTransactions.stream()
                .filter(t -> "EXPENSE".equals(t.getTransactionType()))
                .toList();

        List<CsvPreviewResponse.TransactionPreview> preview = new ArrayList<>();

        // Prioritize showing more income transactions (2-3) for better visibility
        if (incomeTransactions.size() >= 3) {
            // Add 3 income transactions if we have enough
            preview.addAll(incomeTransactions.stream().limit(3).toList());
            // Add 1 expense transaction
            preview.addAll(expenseTransactions.stream().limit(1).toList());
        } else if (incomeTransactions.size() >= 2) {
            // Add 2 income transactions
            preview.addAll(incomeTransactions.stream().limit(2).toList());
            // Add up to 2 expense transactions
            preview.addAll(expenseTransactions.stream().limit(2).toList());
        } else {
            // Fallback: add available income transactions
            preview.addAll(incomeTransactions.stream().limit(4).toList());
            // Fill remaining slots with expense transactions
            int remainingSlots = 4 - preview.size();
            if (remainingSlots > 0) {
                preview.addAll(expenseTransactions.stream().limit(remainingSlots).toList());
            }
        }

        // Ensure we have exactly 4 transactions if possible
        if (preview.size() < 4) {
            int needed = 4 - preview.size();

            // Add more transactions from whichever type has more available
            if (expenseTransactions.size() > preview.stream()
                    .mapToInt(t -> "EXPENSE".equals(t.getTransactionType()) ? 1 : 0).sum()) {
                // Add more expenses
                long currentExpenseCount = preview.stream()
                        .filter(t -> "EXPENSE".equals(t.getTransactionType())).count();
                preview.addAll(expenseTransactions.stream()
                        .skip(currentExpenseCount)
                        .limit(needed)
                        .toList());
            } else if (incomeTransactions.size() > preview.stream()
                    .mapToInt(t -> "INCOME".equals(t.getTransactionType()) ? 1 : 0).sum()) {
                // Add more income
                long currentIncomeCount = preview.stream()
                        .filter(t -> "INCOME".equals(t.getTransactionType())).count();
                preview.addAll(incomeTransactions.stream()
                        .skip(currentIncomeCount)
                        .limit(needed)
                        .toList());
            }
        }

        // Sort by row index to maintain original order
        return preview.stream()
                .sorted(Comparator.comparingInt(CsvPreviewResponse.TransactionPreview::getRowIndex))
                .collect(Collectors.toList());
    }

    /**
     * Step 3: Final import with user validation choices
     */
    public CsvImportResult finalizeImport(CsvValidationRequest request) {
        CsvSession session = csvSessions.get(request.getSessionId());
        if (session == null) {
            throw new RuntimeException("CSV session not found");
        }

        CsvImportResult result = new CsvImportResult(request.getSessionId());
        List<String> errors = new ArrayList<>();
        List<Long> importedTransactionIds = new ArrayList<>();

        Account account = accountRepository.findById(session.getMapping().getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (int i = 0; i < session.getAllRows().size(); i++) {
            int rowIndex = i + 2; // +2 for header

            // Skip rejected rows
            if (request.getRejectedRows() != null && request.getRejectedRows().contains(rowIndex)) {
                skippedCount++;
                continue;
            }

            // Skip if not in approved rows (when approved list is provided and not empty)
            // If approvedRows is empty, import all valid transactions (preview workflow)
            if (request.getApprovedRows() != null &&
                    !request.getApprovedRows().isEmpty() &&
                    !request.getApprovedRows().contains(rowIndex)) {
                skippedCount++;
                continue;
            }

            try {
                List<String> row = session.getAllRows().get(i);
                TransactionData transactionData;

                // Apply manual corrections if provided
                if (request.getManualCorrections() != null &&
                        request.getManualCorrections().containsKey(rowIndex)) {
                    transactionData = applyManualCorrection(row, session.getMapping(), rowIndex,
                            request.getManualCorrections().get(rowIndex));
                } else {
                    transactionData = parseTransactionFromRow(row, session.getMapping(), rowIndex);
                }

                if (transactionData.hasErrors()) {
                    failedCount++;
                    errors.add("Ligne " + rowIndex + ": " +
                            transactionData.getValidationErrors().stream()
                                    .map(CsvPreviewResponse.ValidationError::getError)
                                    .collect(Collectors.joining(", ")));
                    continue;
                }

                // Create and save transaction
                Transaction transaction = createTransactionFromData(transactionData, account);
                transaction = transactionRepository.save(transaction);
                importedTransactionIds.add(transaction.getId());
                successCount++;

            } catch (Exception e) {
                failedCount++;
                errors.add("Ligne " + rowIndex + ": " + e.getMessage());
            }
        }

        // Save mapping template if requested
        if (session.getMapping().isSaveMapping() && session.getMapping().getBankName() != null) {
            saveMappingTemplate(session.getMapping(), account.getUser());
        }

        // Clean up session
        csvSessions.remove(request.getSessionId());

        result.setTotalProcessed(session.getAllRows().size());
        result.setSuccessfulImports(successCount);
        result.setSkippedRows(skippedCount);
        result.setFailedImports(failedCount);
        result.setErrors(errors);
        result.setImportedTransactionIds(importedTransactionIds);

        return result;
    }

    /**
     * Get saved mapping templates for a user
     */
    public List<CsvMappingTemplate> getMappingTemplates(Long userId) {
        return csvMappingTemplateRepository.findByUserId(userId);
    }

    /**
     * Apply a saved mapping template
     */
    public CsvColumnMappingRequest applySavedMapping(String sessionId, String bankName, Long userId) {
        Optional<CsvMappingTemplate> template = csvMappingTemplateRepository
                .findByUserIdAndBankName(userId, bankName);

        if (template.isEmpty()) {
            throw new RuntimeException("Template de mapping non trouvé");
        }

        CsvMappingTemplate t = template.get();
        CsvColumnMappingRequest request = new CsvColumnMappingRequest();
        request.setSessionId(sessionId);
        request.setDateColumnIndex(t.getDateColumnIndex());
        request.setAmountColumnIndex(t.getAmountColumnIndex());
        request.setDebitColumnIndex(t.getDebitColumnIndex());
        request.setCreditColumnIndex(t.getCreditColumnIndex());
        request.setDescriptionColumnIndex(t.getDescriptionColumnIndex());
        request.setCategoryColumnIndex(t.getCategoryColumnIndex());
        request.setBankName(t.getBankName());

        return request;
    }

    // Private helper methods

    /**
     * Detect CSV separator from the first line (headers)
     */
    private String detectSeparator(String headerLine) {
        // Count occurrences of potential separators
        int semicolonCount = (int) headerLine.chars().filter(ch -> ch == ';').count();
        int commaCount = (int) headerLine.chars().filter(ch -> ch == ',').count();
        int tabCount = (int) headerLine.chars().filter(ch -> ch == '\t').count();

        // Log for debugging
        logger.debug("CSV Separator detection - Line: " + headerLine);
        logger.debug("Semicolon count: " + semicolonCount + ", Comma count: " + commaCount + ", Tab count: " + tabCount);

        // Prioritize semicolon (French format), then comma, then tab
        if (semicolonCount > 0) {
            logger.debug("Detected separator: semicolon (;)");
            return ";";
        } else if (commaCount > 0) {
            logger.debug("Detected separator: comma (,)");
            return ",";
        } else if (tabCount > 0) {
            logger.debug("Detected separator: tab");
            return "\t";
        } else {
            logger.debug("No separator detected, defaulting to comma");
            return ",";
        }
    }

    private List<String> parseCsvLine(String line, String separator) {
        if (separator == null) separator = ",";

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char sep = separator.charAt(0);

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == sep && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());

        // Clean quotes
        return result.stream()
                .map(s -> s.replaceAll("^\"|\"$", ""))
                .collect(Collectors.toList());
    }

    private TransactionData parseTransactionFromRow(List<String> row, CsvColumnMappingRequest mapping, int rowIndex) {
        TransactionData data = new TransactionData();
        List<CsvPreviewResponse.ValidationError> errors = new ArrayList<>();

        // Parse date
        if (mapping.getDateColumnIndex() != null && mapping.getDateColumnIndex() < row.size()) {
            String dateStr = row.get(mapping.getDateColumnIndex());
            LocalDate date;
            if (dateStr.contains(" ")) { // Avoid time in transaction
                date = parseDate(dateStr.substring(0, dateStr.indexOf(' ')));
            } else
                date = parseDate(dateStr);

            if (date == null) {
                throw new DateTimeParseException("Format de date invalide", dateStr, mapping.getDateColumnIndex());
            } else {
                data.setDate(date);
            }
        }

        // Parse amount
        BigDecimal amount = parseAmountFromMapping(row, mapping, rowIndex, errors);
        data.setAmount(amount);

        // Parse description
        if (mapping.getDescriptionColumnIndex() != null && mapping.getDescriptionColumnIndex() < row.size()) {
            data.setDescription(row.get(mapping.getDescriptionColumnIndex()));
        }

        // Parse category
        if (mapping.getCategoryColumnIndex() != null && mapping.getCategoryColumnIndex() < row.size()) {
            data.setCategory(row.get(mapping.getCategoryColumnIndex()));
        }

        data.setValidationErrors(errors);
        return data;
    }

    private BigDecimal parseAmountFromMapping(List<String> row, CsvColumnMappingRequest mapping,
                                              int rowIndex, List<CsvPreviewResponse.ValidationError> errors) {
        BigDecimal amount = BigDecimal.ZERO;
        boolean hasAmount = false;
        boolean hasDebit = false;
        boolean hasCredit = false;

        // Check single amount column
        if (mapping.getAmountColumnIndex() != null && mapping.getAmountColumnIndex() < row.size()) {
            String amountStr = row.get(mapping.getAmountColumnIndex());
            amount = parseAmount(amountStr);
            if (amount != null) {
                hasAmount = true;
            }
        }

        // Check debit/credit columns
        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        if (mapping.getDebitColumnIndex() != null && mapping.getDebitColumnIndex() < row.size()) {
            String debitStr = row.get(mapping.getDebitColumnIndex());
            if (!debitStr.trim().isEmpty()) {
                debitAmount = parseAmount(debitStr);
                if (debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) != 0) {
                    hasDebit = true;
                    // Don't negate - French bank CSV already has negative values for debits
                    // debitAmount = debitAmount.negate(); // Removed: debit amounts are already negative
                }
            }
        }

        if (mapping.getCreditColumnIndex() != null && mapping.getCreditColumnIndex() < row.size()) {
            String creditStr = row.get(mapping.getCreditColumnIndex());
            if (!creditStr.trim().isEmpty()) {
                creditAmount = parseAmount(creditStr);
                if (creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) != 0) {
                    hasCredit = true;
                }
            }
        }

        // Determine final amount
        if (hasAmount && (hasDebit || hasCredit)) {
            // Both single amount and debit/credit - needs manual validation
            errors.add(new CsvPreviewResponse.ValidationError(rowIndex,
                    "Montant présent dans colonnes séparées et unique - validation manuelle requise",
                    String.join(",", row), "amount"));
            return BigDecimal.ZERO;
        } else if (hasAmount) {
            return amount;
        } else if (hasDebit && hasCredit) {
            // Both debit and credit - needs manual validation
            errors.add(new CsvPreviewResponse.ValidationError(rowIndex,
                    "Montants présents dans débit ET crédit - validation manuelle requise",
                    String.join(",", row), "amount"));
            return BigDecimal.ZERO;
        } else if (hasDebit) {
            return debitAmount;
        } else if (hasCredit) {
            return creditAmount;
        } else {
            errors.add(new CsvPreviewResponse.ValidationError(rowIndex,
                    "Aucun montant valide trouvé", String.join(",", row), "amount"));
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }

        try {
            String cleanAmount = amountStr.trim();

            // Handle parentheses for negative amounts
            if (cleanAmount.startsWith("(") && cleanAmount.endsWith(")")) {
                cleanAmount = "-" + cleanAmount.substring(1, cleanAmount.length() - 1);
            }

            // Remove currency symbols and spaces
            cleanAmount = cleanAmount.replaceAll("[\\$€£¥\\s]", "");

            // Handle French decimal format (comma as decimal separator)
            // First check if it's French format: has comma as decimal separator
            if (cleanAmount.matches(".*\\d+,\\d{1,2}$")) {
                // French format: replace comma with dot for decimal
                cleanAmount = cleanAmount.replaceAll(",", ".");
                // Remove any thousand separators that might be dots or spaces
                if (cleanAmount.indexOf('.') != cleanAmount.lastIndexOf('.')) {
                    // Multiple dots - keep only the last one as decimal separator
                    int lastDot = cleanAmount.lastIndexOf('.');
                    cleanAmount = cleanAmount.substring(0, lastDot).replaceAll("\\.", "") +
                            cleanAmount.substring(lastDot);
                }
            } else {
                // English format: remove thousand separators (commas)
                cleanAmount = cleanAmount.replaceAll(",", "");
            }

            // Handle negative sign anywhere in the string
            boolean isNegative = cleanAmount.contains("-");
            cleanAmount = cleanAmount.replaceAll("-", "");

            BigDecimal result = new BigDecimal(cleanAmount);
            return isNegative ? result.negate() : result;

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Optional<Transaction> findDuplicateTransaction(Long accountId, LocalDate date,
                                                           BigDecimal amount, String description) {
        return transactionRepository.findByAccountIdAndTransactionDateAndAmountAndDescription(
                accountId, date, amount, description);
    }

    private String determineTransactionType(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) >= 0 ? "INCOME" : "EXPENSE";
    }

    private TransactionData applyManualCorrection(List<String> row, CsvColumnMappingRequest mapping,
                                                  int rowIndex, CsvValidationRequest.ManualCorrection correction) {
        TransactionData data = new TransactionData();
        List<CsvPreviewResponse.ValidationError> errors = new ArrayList<>();

        // Apply corrected date
        if (correction.getCorrectedDate() != null) {
            LocalDate date = parseDate(correction.getCorrectedDate());
            if (date == null) {
                errors.add(new CsvPreviewResponse.ValidationError(rowIndex,
                        "Date corrigée invalide", correction.getCorrectedDate(), "date"));
            }
            data.setDate(date);
        }

        // Apply corrected amount
        if (correction.getCorrectedAmount() != null) {
            BigDecimal amount = parseAmount(correction.getCorrectedAmount());
            if (amount == null) {
                errors.add(new CsvPreviewResponse.ValidationError(rowIndex,
                        "Montant corrigé invalide", correction.getCorrectedAmount(), "amount"));
            }
            data.setAmount(amount);
        }

        // Apply corrected description
        if (correction.getCorrectedDescription() != null) {
            data.setDescription(correction.getCorrectedDescription());
        }

        data.setValidationErrors(errors);
        return data;
    }

    private Transaction createTransactionFromData(TransactionData data, Account account) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionDate(data.getDate());
        transaction.setAmount(data.getAmount().abs());
        transaction.setDescription(data.getDescription());
        transaction.setTransactionType(data.getAmount().compareTo(BigDecimal.ZERO) >= 0 ?
                Transaction.TransactionType.INCOME : Transaction.TransactionType.EXPENSE);
        transaction.setIsPending(false);

        // Set category if available
        if (data.getCategory() != null && !data.getCategory().trim().isEmpty()) {
            // Find category by name (you might want to implement category matching logic)
            // For now, we'll leave it null and let user assign categories later
        }

        return transaction;
    }

    private void saveMappingTemplate(CsvColumnMappingRequest mapping, User user) {
        // Delete existing template for this bank
        csvMappingTemplateRepository.deleteByUserIdAndBankName(user.getId(), mapping.getBankName());

        // Create new template
        CsvMappingTemplate template = new CsvMappingTemplate(mapping.getBankName(), user);
        template.setDateColumnIndex(mapping.getDateColumnIndex());
        template.setAmountColumnIndex(mapping.getAmountColumnIndex());
        template.setDebitColumnIndex(mapping.getDebitColumnIndex());
        template.setCreditColumnIndex(mapping.getCreditColumnIndex());
        template.setDescriptionColumnIndex(mapping.getDescriptionColumnIndex());
        template.setCategoryColumnIndex(mapping.getCategoryColumnIndex());

        csvMappingTemplateRepository.save(template);
    }

    // Inner classes for data handling
    private static class CsvSession {
        private String sessionId;
        private String originalFilename;
        private List<String> headers;
        private List<List<String>> allRows;
        private CsvColumnMappingRequest mapping;

        // Getters and setters
        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public void setHeaders(List<String> headers) {
            this.headers = headers;
        }

        public List<List<String>> getAllRows() {
            return allRows;
        }

        public void setAllRows(List<List<String>> allRows) {
            this.allRows = allRows;
        }

        public CsvColumnMappingRequest getMapping() {
            return mapping;
        }

        public void setMapping(CsvColumnMappingRequest mapping) {
            this.mapping = mapping;
        }
    }

    private static class TransactionData {
        private LocalDate date;
        private BigDecimal amount;
        private String description;
        private String category;
        private List<CsvPreviewResponse.ValidationError> validationErrors = new ArrayList<>();

        public boolean hasErrors() {
            return !validationErrors.isEmpty();
        }

        // Getters and setters
        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public List<CsvPreviewResponse.ValidationError> getValidationErrors() {
            return validationErrors;
        }

        public void setValidationErrors(List<CsvPreviewResponse.ValidationError> validationErrors) {
            this.validationErrors = validationErrors;
        }
    }
}