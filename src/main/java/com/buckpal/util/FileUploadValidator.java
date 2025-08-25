package com.buckpal.util;

import com.buckpal.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FileUploadValidator {
    
    private static final List<String> ALLOWED_CSV_CONTENT_TYPES = Arrays.asList(
        "text/csv",
        "application/csv",
        "text/plain"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    private static final List<String> CSV_MAGIC_NUMBERS = Arrays.asList(
        "Date,", "date,", "DATE,", // Common CSV headers
        "\ufeff" // UTF-8 BOM
    );
    
    public static void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("INVALID_FILE", "File cannot be empty");
        }
        
        validateFileSize(file);
        validateFileName(file);
        validateContentType(file);
        validateFileContent(file);
    }
    
    private static void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", 
                "File size cannot exceed " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        }
    }
    
    private static void validateFileName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new BusinessException("INVALID_FILE_TYPE", "Only CSV files are allowed");
        }
        
        // Check for path traversal attempts
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException("INVALID_FILE_NAME", "Invalid file name");
        }
    }
    
    private static void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CSV_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            // Allow if content type is not set (some browsers don't set it correctly for CSV)
            if (contentType != null) {
                throw new BusinessException("INVALID_CONTENT_TYPE", "Invalid file content type");
            }
        }
    }
    
    private static void validateFileContent(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new BusinessException("EMPTY_FILE", "File content is empty");
            }
            
            // Check first few bytes for CSV-like content
            String header = new String(bytes, 0, Math.min(1024, bytes.length));
            
            // Basic CSV content validation
            if (!header.contains(",") && !header.contains(";")) {
                throw new BusinessException("INVALID_CSV_FORMAT", "File does not appear to be a valid CSV");
            }
            
            // Check for potentially malicious content
            String upperHeader = header.toUpperCase();
            if (upperHeader.contains("=CMD") || upperHeader.contains("=EXEC") || 
                upperHeader.contains("@SUM") || upperHeader.contains("=SUM(")) {
                throw new BusinessException("MALICIOUS_CONTENT", "File contains potentially malicious formulas");
            }
            
        } catch (IOException e) {
            throw new BusinessException("FILE_READ_ERROR", "Could not read file content");
        }
    }
    
    public static String sanitizeCsvCell(String cell) {
        if (cell == null) {
            return null;
        }
        
        // Remove potential formula injection
        String sanitized = cell.trim();
        
        // Remove leading = @ + - characters that could trigger formula execution
        if (sanitized.length() > 0) {
            char firstChar = sanitized.charAt(0);
            if (firstChar == '=' || firstChar == '@' || firstChar == '+' || firstChar == '-') {
                sanitized = "'" + sanitized; // Prefix with quote to force text interpretation
            }
        }
        
        return sanitized;
    }
}