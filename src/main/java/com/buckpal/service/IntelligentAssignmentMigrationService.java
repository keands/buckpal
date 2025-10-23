package com.buckpal.service;

import com.buckpal.entity.Category;
import com.buckpal.entity.MerchantPattern;
import com.buckpal.repository.CategoryRepository;
import com.buckpal.repository.MerchantPatternRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
     * Initialise des patterns de base pour les marchands courants si aucun pattern n'existe
     */
    @Transactional
    public void initializeDefaultPatternsIfEmpty() {
        long existingPatterns = merchantPatternRepository.count();
        
        if (existingPatterns > 0) {
            System.out.println("Patterns already exist (" + existingPatterns + "), skipping initialization");
            return;
        }
        
        System.out.println("No merchant patterns found, initializing default patterns...");
        
        // Patterns de base pour démarrer le système
        Map<String, String> defaultPatterns = new HashMap<>();
        
        // Alimentation
        defaultPatterns.put("CARREFOUR", "categories.groceries");
        defaultPatterns.put("AUCHAN", "categories.groceries");
        defaultPatterns.put("LECLERC", "categories.groceries");
        defaultPatterns.put("MONOPRIX", "categories.groceries");
        defaultPatterns.put("FRANPRIX", "categories.groceries");
        defaultPatterns.put("CASINO", "categories.groceries");
        defaultPatterns.put("LIDL", "categories.groceries");
        
        // Transport
        defaultPatterns.put("TOTAL", "categories.transportation");
        defaultPatterns.put("SHELL", "categories.transportation");
        defaultPatterns.put("BP", "categories.transportation");
        defaultPatterns.put("ESSO", "categories.transportation");
        defaultPatterns.put("SNCF", "categories.transportation");
        defaultPatterns.put("RATP", "categories.transportation");
        defaultPatterns.put("UBER", "categories.transportation");
        
        // Restaurants
        defaultPatterns.put("MCDONALDS", "categories.diningOut");
        defaultPatterns.put("KFC", "categories.diningOut");
        defaultPatterns.put("BURGER KING", "categories.diningOut");
        defaultPatterns.put("STARBUCKS", "categories.diningOut");
        defaultPatterns.put("PIZZA", "categories.diningOut");
        
        // Services/Utilities
        defaultPatterns.put("EDF", "categories.utilities");
        defaultPatterns.put("ENGIE", "categories.utilities");
        defaultPatterns.put("ORANGE", "categories.utilities");
        defaultPatterns.put("SFR", "categories.utilities");
        defaultPatterns.put("BOUYGUES", "categories.utilities");
        
        // Shopping
        defaultPatterns.put("AMAZON", "categories.shopping");
        defaultPatterns.put("ZARA", "categories.shopping");
        defaultPatterns.put("H&M", "categories.shopping");
        defaultPatterns.put("IKEA", "categories.shopping");
        
        // Santé
        defaultPatterns.put("PHARMACIE", "categories.healthcare");
        defaultPatterns.put("HOPITAL", "categories.healthcare");
        defaultPatterns.put("CLINIQUE", "categories.healthcare");
        
        int created = 0;
        for (Map.Entry<String, String> entry : defaultPatterns.entrySet()) {
            String pattern = entry.getKey();
            String categoryKey = entry.getValue();
            
            Optional<Category> category = categoryRepository.findByName(categoryKey);
            if (category.isPresent()) {
                MerchantPattern merchantPattern = new MerchantPattern();
                merchantPattern.setPattern(pattern);
                merchantPattern.setCategoryId(category.get().getId());
                merchantPattern.setSpecificityScore(5); // Score moyen
                merchantPattern.setConfidenceScore(new BigDecimal("0.75")); // Confiance modérée
                merchantPatternRepository.save(merchantPattern);
                created++;
            } else {
                System.out.println("Category not found for key: " + categoryKey);
            }
        }
        
        System.out.println("Created " + created + " default merchant patterns");
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