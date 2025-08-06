package com.buckpal.config;

import com.buckpal.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private CategoryService categoryService;
    
    @Override
    public void run(String... args) throws Exception {
        categoryService.initializeDefaultCategories();
    }
}