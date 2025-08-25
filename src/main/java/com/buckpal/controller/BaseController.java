package com.buckpal.controller;

import com.buckpal.entity.User;
import com.buckpal.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class BaseController {
    
    /**
     * Extract the current authenticated user from the security context
     */
    protected User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("AUTHENTICATION_REQUIRED", "User must be authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new BusinessException("INVALID_AUTHENTICATION", "Invalid authentication principal");
        }
        
        return (User) principal;
    }
    
    /**
     * Extract user from Authentication parameter (alternative method)
     */
    protected User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("AUTHENTICATION_REQUIRED", "User must be authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new BusinessException("INVALID_AUTHENTICATION", "Invalid authentication principal");
        }
        
        return (User) principal;
    }
    
    /**
     * Get current user ID for convenience
     */
    protected Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    /**
     * Get current user ID from Authentication parameter
     */
    protected Long getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }
}