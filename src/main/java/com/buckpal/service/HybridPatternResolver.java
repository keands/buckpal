package com.buckpal.service;

import com.buckpal.entity.MerchantPattern;
import com.buckpal.entity.User;
import com.buckpal.entity.UserMerchantPattern;
import com.buckpal.entity.Category;
import com.buckpal.repository.MerchantPatternRepository;
import com.buckpal.repository.UserMerchantPatternRepository;
import com.buckpal.repository.CategoryRepository;
import static com.buckpal.service.SmartTransactionAssignmentService.SmartAssignmentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service hybride qui résout les patterns de marchands avec priorité :
 * 1. Patterns personnels de l'utilisateur (priorité absolue)
 * 2. Patterns globaux de base (fallback)
 */
@Service
@Transactional
public class HybridPatternResolver {
    
    private final UserMerchantPatternRepository userPatternRepository;
    private final MerchantPatternRepository globalPatternRepository;
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public HybridPatternResolver(UserMerchantPatternRepository userPatternRepository,
                                MerchantPatternRepository globalPatternRepository,
                                CategoryRepository categoryRepository) {
        this.userPatternRepository = userPatternRepository;
        this.globalPatternRepository = globalPatternRepository;
        this.categoryRepository = categoryRepository;
    }
    
    /**
     * Résout un pattern pour un marchand donné avec logique hybride
     */
    public SmartTransactionAssignmentService.SmartAssignmentResult resolvePattern(String merchantText, User user) {
        if (merchantText == null || merchantText.trim().isEmpty()) {
            return new SmartTransactionAssignmentService.SmartAssignmentResult("", new BigDecimal("0"), "NO_PATTERN_MATCH", List.of());
        }
        
        String cleanMerchantText = merchantText.toUpperCase().trim();
        
        // ÉTAPE 1: Chercher dans les patterns personnels (PRIORITÉ ABSOLUE)
        Optional<PersonalPatternMatch> personalMatch = findBestPersonalPattern(cleanMerchantText, user);
        if (personalMatch.isPresent()) {
            return createPersonalResult(personalMatch.get());
        }
        
        // ÉTAPE 2: Fallback sur les patterns globaux
        Optional<GlobalPatternMatch> globalMatch = findBestGlobalPattern(cleanMerchantText);
        if (globalMatch.isPresent()) {
            return createGlobalResult(globalMatch.get());
        }
        
        // ÉTAPE 3: Aucun pattern trouvé
        return new SmartTransactionAssignmentService.SmartAssignmentResult("", new BigDecimal("0"), "NO_PATTERN_MATCH", List.of());
    }
    
    /**
     * Recherche le meilleur pattern personnel pour l'utilisateur
     */
    private Optional<PersonalPatternMatch> findBestPersonalPattern(String merchantText, User user) {
        List<UserMerchantPattern> matchingPatterns = 
            userPatternRepository.findMatchingPatterns(user, merchantText);
        
        if (matchingPatterns.isEmpty()) {
            return Optional.empty();
        }
        
        // Prendre le pattern avec la meilleure confiance et usage
        UserMerchantPattern bestPattern = matchingPatterns.get(0);
        
        // Charger la catégorie associée
        Optional<Category> category = categoryRepository.findById(bestPattern.getCategoryId());
        if (category.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(new PersonalPatternMatch(bestPattern, category.get()));
    }
    
    /**
     * Recherche le meilleur pattern global (fallback)
     */
    private Optional<GlobalPatternMatch> findBestGlobalPattern(String merchantText) {
        // Utiliser la méthode existante pour trouver les patterns globaux
        List<MerchantPattern> matchingPatterns = globalPatternRepository.findMatchingPatternsWithMinConfidence(
            merchantText, new BigDecimal("0.5"));
        
        if (matchingPatterns.isEmpty()) {
            return Optional.empty();
        }
        
        // Trier par confiance et spécificité
        MerchantPattern bestPattern = matchingPatterns.stream()
            .filter(p -> p.getConfidenceScore().compareTo(new BigDecimal("0.5")) >= 0) // Minimum 50% de confiance
            .max((p1, p2) -> {
                // Priorité à la confiance, puis spécificité
                int confidenceCompare = p1.getConfidenceScore().compareTo(p2.getConfidenceScore());
                if (confidenceCompare != 0) return confidenceCompare;
                return p1.getSpecificityScore().compareTo(p2.getSpecificityScore());
            })
            .orElse(null);
        
        if (bestPattern == null) {
            return Optional.empty();
        }
        
        // Charger la catégorie associée
        Optional<Category> category = Optional.empty();
        if (bestPattern.getCategoryId() != null) {
            category = categoryRepository.findById(bestPattern.getCategoryId());
        } else if (bestPattern.getCategoryName() != null) {
            category = categoryRepository.findByName(bestPattern.getCategoryName());
        }
        
        if (category.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(new GlobalPatternMatch(bestPattern, category.get()));
    }
    
    /**
     * Crée un résultat basé sur un pattern personnel
     */
    private SmartTransactionAssignmentService.SmartAssignmentResult createPersonalResult(PersonalPatternMatch match) {
        UserMerchantPattern pattern = match.pattern;
        Category category = match.category;
        
        return new SmartTransactionAssignmentService.SmartAssignmentResult(
            category.getName(),
            pattern.getConfidenceScore(), // Confiance en BigDecimal (0.0-1.0)
            "PERSONAL_PATTERN",
            List.of() // Pas d'alternatives pour patterns personnels
        );
    }
    
    /**
     * Crée un résultat basé sur un pattern global
     */
    private SmartTransactionAssignmentService.SmartAssignmentResult createGlobalResult(GlobalPatternMatch match) {
        MerchantPattern pattern = match.pattern;
        Category category = match.category;
        
        return new SmartTransactionAssignmentService.SmartAssignmentResult(
            category.getName(),
            pattern.getConfidenceScore(), // Confiance en BigDecimal (0.0-1.0)
            "GLOBAL_PATTERN",
            List.of() // TODO: Implémenter alternatives
        );
    }
    
    /**
     * Enregistre l'utilisation d'un pattern (apprentissage)
     */
    @Transactional
    public void recordPatternUsage(User user, String merchantText, Long categoryId, boolean wasSuccessful, String strategy) {
        if ("PERSONAL_PATTERN".equals(strategy)) {
            recordPersonalPatternUsage(user, merchantText, categoryId, wasSuccessful);
        }
        // Les patterns globaux restent inchangés (pas de modification par l'usage individuel)
    }
    
    private void recordPersonalPatternUsage(User user, String merchantText, Long categoryId, boolean wasSuccessful) {
        List<UserMerchantPattern> patterns = userPatternRepository.findMatchingPatterns(user, merchantText);
        
        for (UserMerchantPattern pattern : patterns) {
            if (pattern.getCategoryId().equals(categoryId)) {
                pattern.recordUsage(wasSuccessful);
                userPatternRepository.save(pattern);
                break;
            }
        }
    }
    
    // Classes internes pour les résultats de matching
    private static class PersonalPatternMatch {
        final UserMerchantPattern pattern;
        final Category category;
        
        PersonalPatternMatch(UserMerchantPattern pattern, Category category) {
            this.pattern = pattern;
            this.category = category;
        }
    }
    
    private static class GlobalPatternMatch {
        final MerchantPattern pattern;
        final Category category;
        
        GlobalPatternMatch(MerchantPattern pattern, Category category) {
            this.pattern = pattern;
            this.category = category;
        }
    }
}