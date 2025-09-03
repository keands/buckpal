package com.buckpal.service;

import com.buckpal.entity.Category;
import com.buckpal.entity.MerchantPattern;
import com.buckpal.repository.CategoryRepository;
import com.buckpal.repository.MerchantPatternRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service pour migrer et maintenir la compatibilité entre l'ancien système 
 * de catégories (noms) et le nouveau système (IDs avec mapping).
 */
@Service
@Transactional
public class IntelligentAssignmentMigrationService {
    
    private final MerchantPatternRepository merchantPatternRepository;
    private final CategoryRepository categoryRepository;
    
    // Mapping de compatibilité pour les anciens noms de catégories
    private static final Map<String, String> LEGACY_CATEGORY_MAPPING = new HashMap<>();
    
    static {
        // Mapping des anciens noms vers les nouvelles clés i18n
        LEGACY_CATEGORY_MAPPING.put("Alimentation", "categories.groceries");
        LEGACY_CATEGORY_MAPPING.put("Transport", "categories.transportation");
        LEGACY_CATEGORY_MAPPING.put("Divertissement", "categories.entertainment");
        LEGACY_CATEGORY_MAPPING.put("Shopping", "categories.shopping");
        LEGACY_CATEGORY_MAPPING.put("Restaurants", "categories.diningOut");
        LEGACY_CATEGORY_MAPPING.put("Santé", "categories.healthcare");
        LEGACY_CATEGORY_MAPPING.put("Logement", "categories.housing");
        LEGACY_CATEGORY_MAPPING.put("Services", "categories.utilities");
        LEGACY_CATEGORY_MAPPING.put("Loisirs", "categories.hobbies");
        LEGACY_CATEGORY_MAPPING.put("Voyages", "categories.travel");
        LEGACY_CATEGORY_MAPPING.put("Essence", "categories.transportation");
        LEGACY_CATEGORY_MAPPING.put("Carburant", "categories.transportation");
        LEGACY_CATEGORY_MAPPING.put("Courses", "categories.groceries");
        LEGACY_CATEGORY_MAPPING.put("Supermarché", "categories.groceries");
        LEGACY_CATEGORY_MAPPING.put("Pharmacie", "categories.healthcare");
        LEGACY_CATEGORY_MAPPING.put("Hôpital", "categories.healthcare");
        LEGACY_CATEGORY_MAPPING.put("Médecin", "categories.healthcare");
        LEGACY_CATEGORY_MAPPING.put("Électricité", "categories.utilities");
        LEGACY_CATEGORY_MAPPING.put("Eau", "categories.utilities");
        LEGACY_CATEGORY_MAPPING.put("Internet", "categories.utilities");
        LEGACY_CATEGORY_MAPPING.put("Téléphone", "categories.utilities");
        LEGACY_CATEGORY_MAPPING.put("Assurance", "categories.insurance");
        LEGACY_CATEGORY_MAPPING.put("Vêtements", "categories.shopping");
        LEGACY_CATEGORY_MAPPING.put("Chaussures", "categories.shopping");
        LEGACY_CATEGORY_MAPPING.put("Cinéma", "categories.entertainment");
        LEGACY_CATEGORY_MAPPING.put("Musique", "categories.entertainment");
        LEGACY_CATEGORY_MAPPING.put("Sport", "categories.hobbies");
        LEGACY_CATEGORY_MAPPING.put("Gym", "categories.hobbies");
        LEGACY_CATEGORY_MAPPING.put("Fitness", "categories.hobbies");
        LEGACY_CATEGORY_MAPPING.put("Livres", "categories.education");
        LEGACY_CATEGORY_MAPPING.put("Formation", "categories.education");
        LEGACY_CATEGORY_MAPPING.put("École", "categories.education");
        LEGACY_CATEGORY_MAPPING.put("Université", "categories.education");
        LEGACY_CATEGORY_MAPPING.put("Épargne", "categories.savings");
        LEGACY_CATEGORY_MAPPING.put("Investissement", "categories.investments");
        LEGACY_CATEGORY_MAPPING.put("Retraite", "categories.retirement");
        LEGACY_CATEGORY_MAPPING.put("Urgence", "categories.emergencyFund");
        LEGACY_CATEGORY_MAPPING.put("Travaux", "categories.homeImprovement");
        LEGACY_CATEGORY_MAPPING.put("Rénovation", "categories.homeImprovement");
        LEGACY_CATEGORY_MAPPING.put("Cadeaux", "categories.giftsDonations");
        LEGACY_CATEGORY_MAPPING.put("Dons", "categories.giftsDonations");
        LEGACY_CATEGORY_MAPPING.put("Charity", "categories.giftsDonations");
    }
    
    @Autowired
    public IntelligentAssignmentMigrationService(MerchantPatternRepository merchantPatternRepository,
                                               CategoryRepository categoryRepository) {
        this.merchantPatternRepository = merchantPatternRepository;
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Migre tous les patterns qui utilisent encore des noms de catégories 
     * vers le nouveau système basé sur les IDs.
     */
    @Transactional
    public MigrationResult migrateAllPatterns() {
        List<MerchantPattern> patternsToMigrate = merchantPatternRepository.findByCategoryIdIsNull();
        
        int migrated = 0;
        int failed = 0;
        
        for (MerchantPattern pattern : patternsToMigrate) {
            if (migratePattern(pattern)) {
                migrated++;
            } else {
                failed++;
            }
        }
        
        return new MigrationResult(migrated, failed, patternsToMigrate.size());
    }
    
    /**
     * Migre un pattern spécifique depuis categoryName vers categoryId
     */
    private boolean migratePattern(MerchantPattern pattern) {
        if (pattern.getCategoryId() != null) {
            return true; // Déjà migré
        }
        
        String categoryName = pattern.getCategoryName();
        if (categoryName == null) {
            return false;
        }
        
        // Essayer le mapping direct d'abord
        Optional<Category> category = categoryRepository.findByName(categoryName);
        
        // Si pas trouvé, essayer le mapping de compatibilité
        if (category.isEmpty() && LEGACY_CATEGORY_MAPPING.containsKey(categoryName)) {
            String mappedName = LEGACY_CATEGORY_MAPPING.get(categoryName);
            category = categoryRepository.findByName(mappedName);
        }
        
        if (category.isPresent()) {
            pattern.setCategoryId(category.get().getId());
            merchantPatternRepository.save(pattern);
            return true;
        }
        
        return false;
    }
    
    /**
     * Trouve la catégorie ID pour un nom de catégorie (avec compatibilité legacy)
     */
    public Optional<Long> findCategoryIdByName(String categoryName) {
        if (categoryName == null) {
            return Optional.empty();
        }
        
        // Essayer le nom direct
        Optional<Category> category = categoryRepository.findByName(categoryName);
        
        // Si pas trouvé, essayer le mapping de compatibilité
        if (category.isEmpty() && LEGACY_CATEGORY_MAPPING.containsKey(categoryName)) {
            String mappedName = LEGACY_CATEGORY_MAPPING.get(categoryName);
            category = categoryRepository.findByName(mappedName);
        }
        
        return category.map(Category::getId);
    }
    
    /**
     * Classe pour les résultats de migration
     */
    public static class MigrationResult {
        private final int migrated;
        private final int failed;
        private final int total;
        
        public MigrationResult(int migrated, int failed, int total) {
            this.migrated = migrated;
            this.failed = failed;
            this.total = total;
        }
        
        public int getMigrated() { return migrated; }
        public int getFailed() { return failed; }
        public int getTotal() { return total; }
        public double getSuccessRate() { 
            return total > 0 ? (double) migrated / total : 0.0; 
        }
        
        @Override
        public String toString() {
            return String.format("Migration: %d/%d réussis (%.1f%%)", 
                migrated, total, getSuccessRate() * 100);
        }
    }
}