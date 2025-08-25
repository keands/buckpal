package com.buckpal.repository;

import com.buckpal.entity.Category;
import com.buckpal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByParentCategoryIsNull();
    
    List<Category> findByParentCategory(Category parentCategory);
    
    Optional<Category> findByName(String name);
    
    List<Category> findByIsDefault(Boolean isDefault);
    
    List<Category> findByUser(User user);
    
    Long countByUser(User user);
    
    List<Category> findByUserAndParentCategoryIsNull(User user);
    
    List<Category> findByUserAndIsDefault(User user, Boolean isDefault);
}