package com.buckpal.repository;

import com.buckpal.entity.CsvMappingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CsvMappingTemplateRepository extends JpaRepository<CsvMappingTemplate, Long> {
    
    @Query("SELECT cmt FROM CsvMappingTemplate cmt WHERE cmt.user.id = :userId")
    List<CsvMappingTemplate> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT cmt FROM CsvMappingTemplate cmt WHERE cmt.user.id = :userId AND cmt.bankName = :bankName")
    Optional<CsvMappingTemplate> findByUserIdAndBankName(@Param("userId") Long userId, @Param("bankName") String bankName);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CsvMappingTemplate cmt WHERE cmt.user.id = :userId AND cmt.bankName = :bankName")
    void deleteByUserIdAndBankName(@Param("userId") Long userId, @Param("bankName") String bankName);
}