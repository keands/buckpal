package com.buckpal.service;

import com.buckpal.entity.User;
import com.buckpal.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OnboardingService {

    private final TransactionRepository transactionRepository;

    // Seuils de déverrouillage
    private static final int AI_UNLOCK_THRESHOLD = 20;
    private static final int MATURE_USER_THRESHOLD = 50;
    private static final int LEARNING_START_THRESHOLD = 5;

    @Autowired
    public OnboardingService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public OnboardingStatus getOnboardingStatus(User user) {
        long transactionCount = transactionRepository.countByUser(user);
        long assignedCount = transactionRepository.countByUserAndDetailedCategoryIdIsNotNull(user);

        if (transactionCount == 0) {
            return new OnboardingStatus(
                OnboardingPhase.WELCOME,
                transactionCount,
                assignedCount,
                false,
                getWelcomeMessage()
            );
        }

        if (transactionCount < LEARNING_START_THRESHOLD) {
            return new OnboardingStatus(
                OnboardingPhase.FIRST_STEPS,
                transactionCount,
                assignedCount,
                false,
                getFirstStepsMessage(transactionCount)
            );
        }

        if (transactionCount < AI_UNLOCK_THRESHOLD) {
            return new OnboardingStatus(
                OnboardingPhase.LEARNING,
                transactionCount,
                assignedCount,
                false,
                getLearningMessage(transactionCount, AI_UNLOCK_THRESHOLD)
            );
        }

        if (transactionCount < MATURE_USER_THRESHOLD) {
            return new OnboardingStatus(
                OnboardingPhase.AI_AVAILABLE,
                transactionCount,
                assignedCount,
                true,
                getAiAvailableMessage()
            );
        }

        return new OnboardingStatus(
            OnboardingPhase.MATURE,
            transactionCount,
            assignedCount,
            true,
            getMatureMessage(assignedCount, transactionCount)
        );
    }

    public boolean isIntelligentAssignmentAvailable(User user) {
        long transactionCount = transactionRepository.countByUser(user);
        return transactionCount >= AI_UNLOCK_THRESHOLD;
    }

    public OnboardingProgress getOnboardingProgress(User user) {
        long transactionCount = transactionRepository.countByUser(user);
        long assignedCount = transactionRepository.countByUserAndDetailedCategoryIdIsNotNull(user);
        
        OnboardingPhase phase = getOnboardingStatus(user).getPhase();
        
        return new OnboardingProgress(
            transactionCount,
            assignedCount,
            calculateProgressPercentage(transactionCount),
            getNextMilestone(transactionCount),
            getProgressDescription(transactionCount, phase)
        );
    }

    private int calculateProgressPercentage(long transactionCount) {
        if (transactionCount >= MATURE_USER_THRESHOLD) {
            return 100;
        }
        return (int) ((transactionCount * 100) / MATURE_USER_THRESHOLD);
    }

    private int getNextMilestone(long transactionCount) {
        if (transactionCount < LEARNING_START_THRESHOLD) {
            return LEARNING_START_THRESHOLD;
        }
        if (transactionCount < AI_UNLOCK_THRESHOLD) {
            return AI_UNLOCK_THRESHOLD;
        }
        if (transactionCount < MATURE_USER_THRESHOLD) {
            return MATURE_USER_THRESHOLD;
        }
        return MATURE_USER_THRESHOLD;
    }

    private String getProgressDescription(long transactionCount, OnboardingPhase phase) {
        switch (phase) {
            case WELCOME:
                return "Commencez par importer vos transactions";
            case FIRST_STEPS:
                return String.format("Continuez ! %d transactions pour activer les conseils", 
                    LEARNING_START_THRESHOLD - transactionCount);
            case LEARNING:
                return String.format("Encore %d transactions pour débloquer l'IA", 
                    AI_UNLOCK_THRESHOLD - transactionCount);
            case AI_AVAILABLE:
                return "Attribution intelligente débloquée !";
            case MATURE:
                return "Utilisateur expérimenté - Toutes les fonctionnalités disponibles";
            default:
                return "";
        }
    }

    private String getWelcomeMessage() {
        return "Bienvenue sur BuckPal ! Commencez par importer vos transactions ou connecter votre banque pour découvrir la puissance de la gestion budgétaire intelligente.";
    }

    private String getFirstStepsMessage(long count) {
        return String.format("Parfait ! Vous avez importé %d transaction(s). Commencez à les catégoriser pour que BuckPal comprenne vos habitudes de dépenses.", count);
    }

    private String getLearningMessage(long count, int threshold) {
        int remaining = threshold - (int) count;
        return String.format("Excellent progrès ! %d/%d transactions importées. Plus vous catégorisez, plus BuckPal devient intelligent. Encore %d transactions pour débloquer l'attribution intelligente.", 
            count, threshold, remaining);
    }

    private String getAiAvailableMessage() {
        return "🎉 Félicitations ! BuckPal peut maintenant proposer des catégories intelligentes basées sur vos habitudes. Voulez-vous activer cette fonctionnalité ?";
    }

    private String getMatureMessage(long assignedCount, long totalCount) {
        double assignmentRate = totalCount > 0 ? (double) assignedCount / totalCount * 100 : 0;
        return String.format("Utilisateur expérimenté ! %.0f%% de vos transactions sont catégorisées. L'attribution intelligente fonctionne de manière optimale.", assignmentRate);
    }

    // Classes internes pour les résultats
    public static class OnboardingStatus {
        private final OnboardingPhase phase;
        private final long transactionCount;
        private final long assignedCount;
        private final boolean aiAvailable;
        private final String message;

        public OnboardingStatus(OnboardingPhase phase, long transactionCount, long assignedCount, boolean aiAvailable, String message) {
            this.phase = phase;
            this.transactionCount = transactionCount;
            this.assignedCount = assignedCount;
            this.aiAvailable = aiAvailable;
            this.message = message;
        }

        // Getters
        public OnboardingPhase getPhase() { return phase; }
        public long getTransactionCount() { return transactionCount; }
        public long getAssignedCount() { return assignedCount; }
        public boolean isAiAvailable() { return aiAvailable; }
        public String getMessage() { return message; }
    }

    public static class OnboardingProgress {
        private final long transactionCount;
        private final long assignedCount;
        private final int progressPercentage;
        private final int nextMilestone;
        private final String description;

        public OnboardingProgress(long transactionCount, long assignedCount, int progressPercentage, int nextMilestone, String description) {
            this.transactionCount = transactionCount;
            this.assignedCount = assignedCount;
            this.progressPercentage = progressPercentage;
            this.nextMilestone = nextMilestone;
            this.description = description;
        }

        // Getters
        public long getTransactionCount() { return transactionCount; }
        public long getAssignedCount() { return assignedCount; }
        public int getProgressPercentage() { return progressPercentage; }
        public int getNextMilestone() { return nextMilestone; }
        public String getDescription() { return description; }
    }

    public enum OnboardingPhase {
        WELCOME,        // 0 transactions
        FIRST_STEPS,    // 1-4 transactions
        LEARNING,       // 5-19 transactions
        AI_AVAILABLE,   // 20-49 transactions
        MATURE          // 50+ transactions
    }
}