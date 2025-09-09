package com.buckpal.controller;

import com.buckpal.entity.RecurringPayment;
import com.buckpal.entity.RecurringPaymentHistory;
import com.buckpal.entity.User;
import com.buckpal.service.RecurringPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/recurring-payments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RecurringPaymentController {
    
    @Autowired
    private RecurringPaymentService recurringPaymentService;
    
    /**
     * Get all recurring payments for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<RecurringPayment>> getUserRecurringPayments(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<RecurringPayment> payments = recurringPaymentService.getUserRecurringPayments(user);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get active recurring payments only
     */
    @GetMapping("/active")
    public ResponseEntity<List<RecurringPayment>> getActiveRecurringPayments(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<RecurringPayment> payments = recurringPaymentService.getActiveRecurringPayments(user);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recurring payments by type
     */
    @GetMapping("/by-type/{paymentType}")
    public ResponseEntity<List<RecurringPayment>> getRecurringPaymentsByType(
            @PathVariable RecurringPayment.PaymentType paymentType,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<RecurringPayment> payments = recurringPaymentService.getRecurringPaymentsByType(user, paymentType);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get paginated recurring payments
     */
    @GetMapping("/paged")
    public ResponseEntity<Page<RecurringPayment>> getRecurringPaymentsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Pageable pageable = PageRequest.of(page, size);
            Page<RecurringPayment> payments = recurringPaymentService.getRecurringPaymentsPaged(user, pageable);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get a specific recurring payment by ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<RecurringPayment> getRecurringPaymentById(
            @PathVariable Long paymentId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Optional<RecurringPayment> payment = recurringPaymentService.getRecurringPaymentById(paymentId, user);
            
            if (payment.isPresent()) {
                return ResponseEntity.ok(payment.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create a new recurring payment
     */
    @PostMapping
    public ResponseEntity<RecurringPayment> createRecurringPayment(
            @Valid @RequestBody RecurringPaymentCreateRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            RecurringPayment payment = new RecurringPayment();
            payment.setName(request.getName());
            payment.setDescription(request.getDescription());
            payment.setAmount(request.getAmount());
            payment.setPaymentType(request.getPaymentType());
            payment.setFrequency(request.getFrequency());
            payment.setStartDate(request.getStartDate());
            payment.setEndDate(request.getEndDate());
            payment.setRemainingPayments(request.getRemainingPayments());
            payment.setEscalationRate(request.getEscalationRate());
            payment.setColor(request.getColor());
            payment.setIcon(request.getIcon());
            // Note: Category setting would require CategoryService integration
            
            RecurringPayment savedPayment = recurringPaymentService.createRecurringPayment(payment, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPayment);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update an existing recurring payment
     */
    @PutMapping("/{paymentId}")
    public ResponseEntity<RecurringPayment> updateRecurringPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody RecurringPaymentCreateRequest request,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            
            RecurringPayment payment = new RecurringPayment();
            payment.setName(request.getName());
            payment.setDescription(request.getDescription());
            payment.setAmount(request.getAmount());
            payment.setPaymentType(request.getPaymentType());
            payment.setFrequency(request.getFrequency());
            payment.setStartDate(request.getStartDate());
            payment.setEndDate(request.getEndDate());
            payment.setRemainingPayments(request.getRemainingPayments());
            payment.setEscalationRate(request.getEscalationRate());
            payment.setColor(request.getColor());
            payment.setIcon(request.getIcon());
            
            RecurringPayment updatedPayment = recurringPaymentService.updateRecurringPayment(paymentId, payment, user);
            return ResponseEntity.ok(updatedPayment);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete a recurring payment
     */
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deleteRecurringPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            recurringPaymentService.deleteRecurringPayment(paymentId, user);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Deactivate a recurring payment (soft delete)
     */
    @PostMapping("/{paymentId}/deactivate")
    public ResponseEntity<Void> deactivateRecurringPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            recurringPaymentService.deactivateRecurringPayment(paymentId, user);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get budget projection for multiple months
     */
    @GetMapping("/budget-projection")
    public ResponseEntity<Map<String, Object>> getBudgetProjection(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "12") int monthsAhead,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Map<String, Object> projection = recurringPaymentService.getBudgetProjection(user, startDate, monthsAhead);
            return ResponseEntity.ok(projection);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get monthly amounts by payment type
     */
    @GetMapping("/monthly-amounts")
    public ResponseEntity<Map<RecurringPayment.PaymentType, java.math.BigDecimal>> getMonthlyAmountsByType(
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Map<RecurringPayment.PaymentType, java.math.BigDecimal> amounts = 
                recurringPaymentService.getMonthlyAmountsByType(user);
            return ResponseEntity.ok(amounts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get payments ending soon (within 3 months)
     */
    @GetMapping("/ending-soon")
    public ResponseEntity<List<RecurringPayment>> getPaymentsEndingSoon(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<RecurringPayment> payments = recurringPaymentService.getPaymentsEndingSoon(user);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get upcoming payments (next N days)
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<RecurringPaymentHistory>> getUpcomingPayments(
            @RequestParam(defaultValue = "30") int daysAhead,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<RecurringPaymentHistory> payments = recurringPaymentService.getUpcomingPayments(user, daysAhead);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get overdue payments
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<RecurringPaymentHistory>> getOverduePayments(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            List<RecurringPaymentHistory> payments = recurringPaymentService.getOverduePayments(user);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get payment statistics and analytics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            Map<String, Object> stats = recurringPaymentService.getPaymentStatistics(user);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get available payment types with their metadata
     */
    @GetMapping("/payment-types")
    public ResponseEntity<Map<String, Object>> getPaymentTypes() {
        Map<String, Object> types = new java.util.HashMap<>();
        
        for (RecurringPayment.PaymentType type : RecurringPayment.PaymentType.values()) {
            Map<String, Object> typeInfo = new java.util.HashMap<>();
            typeInfo.put("name", type.name());
            typeInfo.put("displayName", type.getDisplayName());
            typeInfo.put("description", type.getDescription());
            typeInfo.put("defaultColor", type.getDefaultColor());
            typeInfo.put("defaultIcon", type.getDefaultIcon());
            types.put(type.name(), typeInfo);
        }
        
        return ResponseEntity.ok(types);
    }
    
    /**
     * Get available payment frequencies
     */
    @GetMapping("/frequencies")
    public ResponseEntity<Map<String, Object>> getPaymentFrequencies() {
        Map<String, Object> frequencies = new java.util.HashMap<>();
        
        for (RecurringPayment.PaymentFrequency freq : RecurringPayment.PaymentFrequency.values()) {
            Map<String, Object> freqInfo = new java.util.HashMap<>();
            freqInfo.put("name", freq.name());
            freqInfo.put("displayName", freq.getDisplayName());
            freqInfo.put("description", freq.getDescription());
            freqInfo.put("monthsInterval", freq.getMonthsInterval());
            freqInfo.put("isWeekly", freq.isWeekly());
            frequencies.put(freq.name(), freqInfo);
        }
        
        return ResponseEntity.ok(frequencies);
    }
    
    /**
     * Request DTO for creating/updating recurring payments
     */
    public static class RecurringPaymentCreateRequest {
        private String name;
        private String description;
        private java.math.BigDecimal amount;
        private RecurringPayment.PaymentType paymentType;
        private RecurringPayment.PaymentFrequency frequency;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer remainingPayments;
        private java.math.BigDecimal escalationRate = java.math.BigDecimal.ZERO;
        private String color;
        private String icon;
        
        // Constructors
        public RecurringPaymentCreateRequest() {}
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
        
        public RecurringPayment.PaymentType getPaymentType() { return paymentType; }
        public void setPaymentType(RecurringPayment.PaymentType paymentType) { this.paymentType = paymentType; }
        
        public RecurringPayment.PaymentFrequency getFrequency() { return frequency; }
        public void setFrequency(RecurringPayment.PaymentFrequency frequency) { this.frequency = frequency; }
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        
        public Integer getRemainingPayments() { return remainingPayments; }
        public void setRemainingPayments(Integer remainingPayments) { this.remainingPayments = remainingPayments; }
        
        public java.math.BigDecimal getEscalationRate() { return escalationRate; }
        public void setEscalationRate(java.math.BigDecimal escalationRate) { this.escalationRate = escalationRate; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }
}