package com.buckpal.controller;

import com.buckpal.entity.User;
import com.buckpal.repository.UserRepository;
import com.buckpal.service.OnboardingService;
import com.buckpal.service.OnboardingService.OnboardingStatus;
import com.buckpal.service.OnboardingService.OnboardingProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    @Autowired
    private OnboardingService onboardingService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Get current onboarding status for the authenticated user
     */
    @GetMapping("/status")
    public ResponseEntity<OnboardingStatus> getOnboardingStatus(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }

            OnboardingStatus status = onboardingService.getOnboardingStatus(user);
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get detailed onboarding progress for the authenticated user
     */
    @GetMapping("/progress")
    public ResponseEntity<OnboardingProgress> getOnboardingProgress(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }

            OnboardingProgress progress = onboardingService.getOnboardingProgress(user);
            return ResponseEntity.ok(progress);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if intelligent assignment is available for the user
     */
    @GetMapping("/ai-availability")
    public ResponseEntity<Map<String, Object>> checkAiAvailability(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }

            boolean aiAvailable = onboardingService.isIntelligentAssignmentAvailable(user);
            OnboardingStatus status = onboardingService.getOnboardingStatus(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("aiAvailable", aiAvailable);
            response.put("phase", status.getPhase().name());
            response.put("transactionCount", status.getTransactionCount());
            response.put("message", status.getMessage());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get onboarding configuration and thresholds
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getOnboardingConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("aiUnlockThreshold", 20);
            config.put("matureUserThreshold", 50);
            config.put("learningStartThreshold", 5);
            
            Map<String, String> phaseDescriptions = new HashMap<>();
            phaseDescriptions.put("WELCOME", "Découvrez BuckPal");
            phaseDescriptions.put("FIRST_STEPS", "Premiers pas");
            phaseDescriptions.put("LEARNING", "Apprentissage en cours");
            phaseDescriptions.put("AI_AVAILABLE", "IA débloquée");
            phaseDescriptions.put("MATURE", "Utilisateur expert");
            
            config.put("phases", phaseDescriptions);
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Mark AI feature as acknowledged by user (for UI state management)
     */
    @PostMapping("/acknowledge-ai")
    public ResponseEntity<Map<String, Object>> acknowledgeAiFeature(
            @RequestParam(required = false, defaultValue = "false") boolean accepted,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }

            // Here you could store user preferences about AI feature
            // For now, just return success with the user's choice
            
            Map<String, Object> response = new HashMap<>();
            response.put("acknowledged", true);
            response.put("aiAccepted", accepted);
            response.put("message", accepted ? 
                "Attribution intelligente activée !" : 
                "Vous pourrez activer l'attribution intelligente plus tard dans les paramètres."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get onboarding tips and recommendations based on current phase
     */
    @GetMapping("/tips")
    public ResponseEntity<Map<String, Object>> getOnboardingTips(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }

            OnboardingStatus status = onboardingService.getOnboardingStatus(user);
            
            Map<String, Object> tips = new HashMap<>();
            tips.put("phase", status.getPhase().name());
            tips.put("tips", getTipsForPhase(status.getPhase()));
            tips.put("nextSteps", getNextStepsForPhase(status.getPhase()));
            
            return ResponseEntity.ok(tips);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String[] getTipsForPhase(OnboardingService.OnboardingPhase phase) {
        switch (phase) {
            case WELCOME:
                return new String[]{
                    "Importez vos transactions via un fichier CSV pour commencer",
                    "Connectez votre banque pour une synchronisation automatique",
                    "Explorez les différentes sections : Dashboard, Comptes, Budget"
                };
            case FIRST_STEPS:
                return new String[]{
                    "Catégorisez vos premières transactions manuellement",
                    "Découvrez les catégories prédéfinies",
                    "Créez vos propres catégories personnalisées si nécessaire"
                };
            case LEARNING:
                return new String[]{
                    "Continuez à catégoriser pour enseigner à BuckPal vos habitudes",
                    "Plus vous catégorisez, plus l'IA sera précise",
                    "Vérifiez votre progression vers le déverrouillage de l'IA"
                };
            case AI_AVAILABLE:
                return new String[]{
                    "L'attribution intelligente est maintenant disponible !",
                    "Activez-la pour gagner du temps sur la catégorisation",
                    "Vous pourrez toujours corriger les suggestions de l'IA"
                };
            case MATURE:
                return new String[]{
                    "Utilisez les statistiques avancées pour analyser vos dépenses",
                    "Configurez des budgets et objectifs d'épargne",
                    "Explorez les tendances et patterns de vos dépenses"
                };
            default:
                return new String[]{};
        }
    }

    private String[] getNextStepsForPhase(OnboardingService.OnboardingPhase phase) {
        switch (phase) {
            case WELCOME:
                return new String[]{
                    "Importer vos premières transactions",
                    "Découvrir l'interface"
                };
            case FIRST_STEPS:
                return new String[]{
                    "Catégoriser vos transactions",
                    "Explorer les fonctionnalités de base"
                };
            case LEARNING:
                return new String[]{
                    "Continuer la catégorisation pour débloquer l'IA",
                    "Atteindre 20 transactions catégorisées"
                };
            case AI_AVAILABLE:
                return new String[]{
                    "Activer l'attribution intelligente",
                    "Tester les suggestions automatiques"
                };
            case MATURE:
                return new String[]{
                    "Optimiser vos budgets",
                    "Analyser vos tendances de dépenses"
                };
            default:
                return new String[]{};
        }
    }
}