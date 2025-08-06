package com.buckpal.controller;

import com.buckpal.dto.AccountDto;
import com.buckpal.entity.Account;
import com.buckpal.entity.User;
import com.buckpal.repository.AccountRepository;
import com.buckpal.service.PlaidService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AccountController {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private PlaidService plaidService;
    
    @GetMapping
    public ResponseEntity<List<AccountDto>> getUserAccounts(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Account> accounts = accountRepository.findByUserAndIsActive(user, true);
        
        List<AccountDto> accountDtos = accounts.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(accountDtos);
    }
    
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDto> getAccount(Authentication authentication, @PathVariable Long accountId) {
        User user = (User) authentication.getPrincipal();
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return ResponseEntity.ok(convertToDto(account));
    }
    
    @PostMapping
    public ResponseEntity<AccountDto> createAccount(
            Authentication authentication, 
            @Valid @RequestBody AccountDto accountDto) {
        
        User user = (User) authentication.getPrincipal();
        
        Account account = new Account();
        account.setName(accountDto.getName());
        account.setAccountType(accountDto.getAccountType());
        account.setBalance(accountDto.getBalance());
        account.setBankName(accountDto.getBankName());
        account.setUser(user);
        
        Account savedAccount = accountRepository.save(account);
        
        return ResponseEntity.ok(convertToDto(savedAccount));
    }
    
    @PutMapping("/{accountId}")
    public ResponseEntity<AccountDto> updateAccount(
            Authentication authentication,
            @PathVariable Long accountId,
            @Valid @RequestBody AccountDto accountDto) {
        
        User user = (User) authentication.getPrincipal();
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        account.setName(accountDto.getName());
        account.setAccountType(accountDto.getAccountType());
        account.setBalance(accountDto.getBalance());
        account.setBankName(accountDto.getBankName());
        
        Account updatedAccount = accountRepository.save(account);
        
        return ResponseEntity.ok(convertToDto(updatedAccount));
    }
    
    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> deleteAccount(Authentication authentication, @PathVariable Long accountId) {
        User user = (User) authentication.getPrincipal();
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        account.setIsActive(false);
        accountRepository.save(account);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sync-plaid")
    public ResponseEntity<?> syncPlaidAccounts(
            Authentication authentication,
            @RequestParam String accessToken) throws IOException {
        
        User user = (User) authentication.getPrincipal();
        List<Account> syncedAccounts = plaidService.syncAccounts(accessToken, user);
        
        return ResponseEntity.ok().body("Synced " + syncedAccounts.size() + " accounts from Plaid");
    }
    
    private AccountDto convertToDto(Account account) {
        AccountDto dto = new AccountDto();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setAccountType(account.getAccountType());
        dto.setBalance(account.getBalance());
        dto.setIsActive(account.getIsActive());
        dto.setBankName(account.getBankName());
        return dto;
    }
}