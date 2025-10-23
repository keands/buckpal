package com.buckpal.service;

import com.buckpal.entity.Transaction;
import com.buckpal.entity.User;
import com.buckpal.entity.UserMerchantPattern;
import com.buckpal.entity.UserAssignmentFeedback;
import com.buckpal.repository.UserMerchantPatternRepository;
import com.buckpal.repository.UserAssignmentFeedbackRepository;
import com.buckpal.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsable de l'apprentissage automatique des patterns personnels
 * basé sur les comportements et feedbacks des utilisateurs
 */
@Service
@Transactional
public class PersonalPatternLearningService {
    
    private final UserMerchantPatternRepository userPatternRepository;
    private final UserAssignmentFeedbackRepository feedbackRepository;
    private final TransactionRepository transactionRepository;
    
    @Autowired
    public PersonalPatternLearningService(UserMerchantPatternRepository userPatternRepository,
                                        UserAssignmentFeedbackRepository feedbackRepository,
                                        TransactionRepository transactionRepository) {
        this.userPatternRepository = userPatternRepository;
        this.feedbackRepository = feedbackRepository;
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Apprendre depuis les feedbacks de l'utilisateur
     * Appelé quand l'utilisateur accepte ou rejette une suggestion
     */
    @Transactional
    public void learnFromFeedback(User user, Long transactionId, String suggestedCategoryName, 
                                 String chosenCategoryName, boolean wasAccepted) {
        
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            return;
        }
        
        Transaction transaction = transactionOpt.get();
        String merchantText = buildMerchantText(transaction);
        
        if (wasAccepted && suggestedCategoryName.equals(chosenCategoryName)) {
            // Utilisateur a accepté la suggestion -> Renforcer le pattern existant ou en créer un
            reinforcePattern(user, merchantText, suggestedCategoryName);
        } else {
            // Utilisateur a rejeté ou choisi une catégorie différente -> Créer pattern personnel
            createPersonalPattern(user, merchantText, chosenCategoryName, UserMerchantPattern.PatternSource.CONFIRMED);
        }
    }
    
    /**
     * Apprendre depuis les assignations manuelles répétées
     * Détecte automatiquement les patterns quand un utilisateur assigne manuellement la même catégorie
     */
    @Transactional
    public void learnFromManualAssignments(User user) {
        // Récupérer les transactions assignées manuellement
        List<Transaction> recentManualAssignments = transactionRepository
            .findByUser(user)
            .stream()
            .filter(t -> t.getDetailedCategoryId() != null)
            .filter(t -> t.getAssignmentConfidence() == null || t.getAssignmentConfidence().compareTo(BigDecimal.ZERO) == 0)
            .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate())) // Trier par date décroissante
            .limit(100) // Analyser les 100 dernières assignations manuelles
            .collect(Collectors.toList());
        
        // Grouper par marchands similaires et catégories
        Map<String, Map<Long, List<Transaction>>> merchantCategoryGroups = new HashMap<>();
        
        for (Transaction transaction : recentManualAssignments) {
            String merchantKey = extractMerchantKey(transaction);
            Long categoryId = transaction.getDetailedCategoryId();
            
            merchantCategoryGroups
                .computeIfAbsent(merchantKey, k -> new HashMap<>())
                .computeIfAbsent(categoryId, k -> new ArrayList<>())
                .add(transaction);
        }
        
        // Créer des patterns pour les marchands qui ont ≥ 3 assignations identiques
        for (Map.Entry<String, Map<Long, List<Transaction>>> merchantEntry : merchantCategoryGroups.entrySet()) {
            String merchantKey = merchantEntry.getKey();
            
            for (Map.Entry<Long, List<Transaction>> categoryEntry : merchantEntry.getValue().entrySet()) {
                Long categoryId = categoryEntry.getKey();
                List<Transaction> transactions = categoryEntry.getValue();
                
                if (transactions.size() >= 3) { // Seuil d'apprentissage automatique
                    // Vérifier si un pattern personnel n'existe pas déjà
                    Optional<UserMerchantPattern> existingPattern = 
                        userPatternRepository.findByUserAndPatternAndCategoryId(user, merchantKey, categoryId);
                    
                    if (existingPattern.isEmpty()) {
                        // Créer un nouveau pattern appris automatiquement
                        UserMerchantPattern newPattern = new UserMerchantPattern(
                            user, merchantKey, categoryId, UserMerchantPattern.PatternSource.LEARNED
                        );
                        newPattern.setUsageCount(transactions.size());
                        newPattern.setSuccessCount(transactions.size());
                        newPattern.setConfidenceScore(calculateConfidenceFromFrequency(transactions.size()));
                        
                        userPatternRepository.save(newPattern);
                    }
                }
            }
        }
    }
    
    /**
     * Renforce un pattern existant ou en crée un nouveau
     */
    private void reinforcePattern(User user, String merchantText, String categoryName) {
        // TODO: Convertir categoryName en categoryId
        // Pour l'instant, on skip cette fonctionnalité
    }
    
    /**
     * Crée un pattern personnel basé sur le feedback utilisateur
     */
    private void createPersonalPattern(User user, String merchantText, String categoryName, 
                                     UserMerchantPattern.PatternSource source) {
        // TODO: Convertir categoryName en categoryId
        // Pour l'instant, on skip cette fonctionnalité
    }
    
    /**
     * Extrait la clé de marchand pour le groupement
     */
    private String extractMerchantKey(Transaction transaction) {
        String merchant = transaction.getMerchantName();
        if (merchant == null || merchant.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        // Nettoyer et normaliser le nom du marchand
        String cleaned = merchant.toUpperCase().trim();
        
        // Extraire les mots-clés principaux (enlever dates, numéros, etc.)
        String[] words = cleaned.split("\\s+");
        
        // Prendre les 1-2 premiers mots significatifs
        List<String> significantWords = Arrays.stream(words)
            .filter(word -> word.length() > 2) // Mots > 2 caractères
            .filter(word -> !word.matches("\\d+")) // Pas de nombres purs
            .filter(word -> !word.matches("\\d{2}/\\d{2}")) // Pas de dates
            .limit(2)
            .collect(Collectors.toList());
        
        if (significantWords.isEmpty()) {
            return cleaned;
        }
        
        return String.join(" ", significantWords);
    }
    
    /**
     * Construit le texte marchand pour matching
     */
    private String buildMerchantText(Transaction transaction) {
        StringBuilder sb = new StringBuilder();
        
        if (transaction.getMerchantName() != null && !transaction.getMerchantName().trim().isEmpty()) {
            sb.append(transaction.getMerchantName().trim());
        }
        
        if (transaction.getDescription() != null && !transaction.getDescription().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(transaction.getDescription().trim());
        }
        
        return sb.toString();
    }
    
    /**
     * Calcule la confiance basée sur la fréquence d'utilisation
     */
    private BigDecimal calculateConfidenceFromFrequency(int frequency) {
        // Plus l'utilisateur a répété le pattern, plus on a confiance
        if (frequency >= 10) return new BigDecimal("0.95");
        if (frequency >= 7) return new BigDecimal("0.90");
        if (frequency >= 5) return new BigDecimal("0.85");
        if (frequency >= 3) return new BigDecimal("0.80");
        return new BigDecimal("0.75");
    }
    
    /**
     * Analyse et améliore les patterns existants d'un utilisateur
     */
    @Transactional
    public PatternImprovementReport improveExistingPatterns(User user) {
        List<UserMerchantPattern> patterns = userPatternRepository.findByUserOrderByUsageCountDescLastUsedAtDesc(user);
        
        int improved = 0;
        int removed = 0;
        
        for (UserMerchantPattern pattern : patterns) {
            BigDecimal accuracy = pattern.getAccuracyRate();
            
            if (accuracy.compareTo(new BigDecimal("0.3")) < 0 && pattern.getUsageCount() > 5) {
                // Pattern avec très faible précision et beaucoup d'usage -> supprimer
                userPatternRepository.delete(pattern);
                removed++;
            } else if (accuracy.compareTo(new BigDecimal("0.6")) < 0) {
                // Pattern avec précision moyenne -> réduire la confiance
                pattern.setConfidenceScore(pattern.getConfidenceScore().multiply(new BigDecimal("0.8")));
                userPatternRepository.save(pattern);
                improved++;
            }
        }
        
        return new PatternImprovementReport(improved, removed);
    }
    
    /**
     * Rapport d'amélioration des patterns
     */
    public static class PatternImprovementReport {
        public final int patternsImproved;
        public final int patternsRemoved;
        
        public PatternImprovementReport(int improved, int removed) {
            this.patternsImproved = improved;
            this.patternsRemoved = removed;
        }
    }
}