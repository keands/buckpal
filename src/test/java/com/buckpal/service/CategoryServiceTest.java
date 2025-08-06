package com.buckpal.service;

import com.buckpal.entity.Account;
import com.buckpal.entity.Category;
import com.buckpal.entity.Transaction;
import com.buckpal.entity.Transaction.TransactionType;
import com.buckpal.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @InjectMocks
    private CategoryService categoryService;
    
    private Transaction testTransaction;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food & Dining");
        testCategory.setIsDefault(true);
        
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setAmount(new BigDecimal("25.50"));
        testTransaction.setDescription("McDonald's Restaurant");
        testTransaction.setMerchantName("McDonald's");
        testTransaction.setTransactionDate(LocalDate.now());
        testTransaction.setTransactionType(TransactionType.EXPENSE);
        testTransaction.setAccount(new Account());
    }
    
    @Test
    void shouldCategorizeTransactionByDescription() {
        when(categoryRepository.findByName("Food & Dining")).thenReturn(Optional.of(testCategory));
        
        Category result = categoryService.categorizeTransaction(testTransaction);
        
        assertThat(result).isEqualTo(testCategory);
        assertThat(result.getName()).isEqualTo("Food & Dining");
        
        verify(categoryRepository).findByName("Food & Dining");
    }
    
    @Test
    void shouldCategorizeTransactionByMerchantName() {
        testTransaction.setDescription("Purchase");
        
        when(categoryRepository.findByName("Food & Dining")).thenReturn(Optional.of(testCategory));
        
        Category result = categoryService.categorizeTransaction(testTransaction);
        
        assertThat(result).isEqualTo(testCategory);
    }
    
    @Test
    void shouldReturnOtherCategoryWhenNoMatch() {
        testTransaction.setDescription("Unknown transaction");
        testTransaction.setMerchantName("Unknown merchant");
        
        Category otherCategory = new Category();
        otherCategory.setName("Other");
        
        when(categoryRepository.findByName("Other")).thenReturn(Optional.of(otherCategory));
        
        Category result = categoryService.categorizeTransaction(testTransaction);
        
        assertThat(result.getName()).isEqualTo("Other");
    }
    
    @Test
    void shouldCreateOtherCategoryWhenNotExists() {
        testTransaction.setDescription("Unknown transaction");
        testTransaction.setMerchantName("Unknown merchant");
        
        Category newOtherCategory = new Category();
        newOtherCategory.setName("Other");
        newOtherCategory.setDescription("Uncategorized transactions");
        newOtherCategory.setIsDefault(true);
        
        when(categoryRepository.findByName("Other")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(newOtherCategory);
        
        Category result = categoryService.categorizeTransaction(testTransaction);
        
        assertThat(result.getName()).isEqualTo("Other");
        verify(categoryRepository).save(any(Category.class));
    }
    
    @Test
    void shouldInitializeDefaultCategories() {
        when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category());
        
        categoryService.initializeDefaultCategories();
        
        verify(categoryRepository, atLeast(8)).save(any(Category.class));
    }
    
    @Test
    void shouldNotCreateCategoryIfAlreadyExists() {
        when(categoryRepository.findByName("Food & Dining")).thenReturn(Optional.of(testCategory));
        when(categoryRepository.findByName("Transportation")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category());
        
        categoryService.initializeDefaultCategories();
        
        verify(categoryRepository, never()).save(argThat(cat -> "Food & Dining".equals(cat.getName())));
        verify(categoryRepository, atLeastOnce()).save(argThat(cat -> "Transportation".equals(cat.getName())));
    }
}