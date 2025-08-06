package com.buckpal.repository;

import com.buckpal.entity.Account;
import com.buckpal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    List<Account> findByUserAndIsActive(User user, Boolean isActive);
    
    List<Account> findByUser(User user);
    
    Optional<Account> findByPlaidAccountId(String plaidAccountId);
    
    List<Account> findByPlaidItemId(String plaidItemId);
}