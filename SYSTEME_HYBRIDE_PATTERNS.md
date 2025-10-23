# Système Hybride de Patterns de Marchands - Documentation Complète

## Vue d'ensemble

Le système hybride d'attribution intelligente de catégories aux transactions utilise une approche à double niveau :
1. **Patterns personnels utilisateur** (priorité absolue) - spécifiques à chaque utilisateur
2. **Patterns globaux** (fallback) - partagés entre tous les utilisateurs

## Architecture du Système

### Principe de Fonctionnement

```
Transaction → HybridPatternResolver → 
    1. Chercher patterns personnels utilisateur
       ↓ (trouvé)
       Retourner suggestion personnalisée
    
    2. (si aucun pattern personnel)
       Chercher patterns globaux
       ↓ (trouvé)
       Retourner suggestion globale
    
    3. (si aucun pattern)
       Retourner "NO_PATTERN_MATCH"
```

### Composants Principaux

#### 1. UserMerchantPattern (Entity)
**Fichier**: `src/main/java/com/buckpal/entity/UserMerchantPattern.java`

Stocke les patterns personnels de chaque utilisateur :
- `user` : Utilisateur propriétaire du pattern
- `pattern` : Texte de pattern (ex: "CARREFOUR", "MCDONALDS")
- `categoryId` : ID de la catégorie associée
- `usageCount` : Nombre d'utilisations du pattern
- `successCount` : Nombre de fois que le pattern a été correct
- `confidenceScore` : Score de confiance (0.0 à 1.0)
- `source` : Source du pattern (MANUAL, CONFIRMED, LEARNED)

#### 2. HybridPatternResolver (Service)
**Fichier**: `src/main/java/com/buckpal/service/HybridPatternResolver.java`

Service principal qui :
- Résout les patterns avec logique hybride (personnel > global)
- Enregistre l'utilisation des patterns pour l'apprentissage
- Gère la confiance et les statistiques d'usage

#### 3. PersonalPatternLearningService (Service)
**Fichier**: `src/main/java/com/buckpal/service/PersonalPatternLearningService.java`

Service d'apprentissage automatique qui :
- Apprend des feedbacks utilisateur (acceptation/rejet de suggestions)
- Détecte automatiquement les patterns répétitifs (≥3 assignations identiques)
- Améliore les patterns existants en fonction de leur précision

#### 4. UserMerchantPatternRepository (Repository)
**Fichier**: `src/main/java/com/buckpal/repository/UserMerchantPatternRepository.java`

Repository optimisé avec requêtes personnalisées :
```java
@Query("SELECT ump FROM UserMerchantPattern ump WHERE ump.user = :user " +
       "AND UPPER(:merchantText) LIKE CONCAT('%', ump.pattern, '%') " +
       "ORDER BY ump.confidenceScore DESC, ump.usageCount DESC")
List<UserMerchantPattern> findMatchingPatterns(@Param("user") User user, 
                                              @Param("merchantText") String merchantText);
```

## Flux d'Attribution Intelligente

### 1. Attribution Automatique
```java
SmartTransactionAssignmentService.assignCategoryToTransaction(transaction, user)
    ↓
HybridPatternResolver.resolvePattern(merchantText, user)
    ↓
1. findBestPersonalPattern() → patterns personnels
2. findBestGlobalPattern() → patterns globaux (fallback)
3. Retour "NO_PATTERN_MATCH" si aucun
```

### 2. Apprentissage Automatique

#### A. Depuis le Feedback Utilisateur
```java
PersonalPatternLearningService.learnFromFeedback(user, transactionId, 
                                                suggestedCategory, chosenCategory, wasAccepted)
```
- Si accepté → renforce le pattern existant
- Si rejeté → crée un nouveau pattern personnel avec la catégorie choisie

#### B. Depuis les Assignations Manuelles
```java
PersonalPatternLearningService.learnFromManualAssignments(user)
```
- Analyse les 100 dernières assignations manuelles
- Groupe par marchands similaires et catégories
- Crée automatiquement des patterns pour ≥3 assignations identiques

## Types de Patterns

### 1. Patterns Personnels (UserMerchantPattern)
- **MANUAL** : Créé manuellement par l'utilisateur
- **CONFIRMED** : Créé suite à feedback positif utilisateur
- **LEARNED** : Détecté automatiquement par l'analyse comportementale

### 2. Patterns Globaux (MerchantPattern)
- Patterns de base partagés entre tous les utilisateurs
- Utilisés comme fallback quand aucun pattern personnel n'existe
- Initialisés automatiquement avec des marchands courants

## Exemples d'Utilisation

### Scénario 1 : Première Attribution
1. Transaction "CARREFOUR VILLENEUVE" arrive
2. Aucun pattern personnel → utilise pattern global "CARREFOUR" → "categories.groceries"
3. Utilisateur accepte → crée pattern personnel CONFIRMED

### Scénario 2 : Apprentissage Automatique
1. Utilisateur assigne manuellement 3 transactions "BOULANGERIE PAUL" à "categories.diningOut"
2. PersonalPatternLearningService détecte le pattern répétitif
3. Crée automatiquement pattern personnel LEARNED "BOULANGERIE PAUL" → "categories.diningOut"

### Scénario 3 : Correction d'Erreur
1. Système suggère "categories.groceries" pour "STARBUCKS"
2. Utilisateur rejette et choisit "categories.diningOut"
3. Crée pattern personnel CONFIRMED "STARBUCKS" → "categories.diningOut"
4. Prochaine transaction Starbucks utilisera le pattern personnel

## Configuration et Migration

### Initialisation des Patterns de Base
```java
IntelligentAssignmentMigrationService.initializeDefaultPatternsIfEmpty()
```
Crée automatiquement des patterns globaux pour des marchands courants si la base est vide.

### Migration Legacy
```java
IntelligentAssignmentMigrationService.migrateAllPatterns()
```
Migre les anciens patterns basés sur categoryName vers categoryId.

## Avantages du Système Hybride

### 1. Personnalisation
- Chaque utilisateur a ses propres patterns
- Apprentissage basé sur le comportement individuel
- Pas de conflit entre utilisateurs aux habitudes différentes

### 2. Performance
- Requêtes optimisées avec index sur user_id + pattern
- Priorité aux patterns personnels (plus précis)
- Fallback intelligent sur patterns globaux

### 3. Apprentissage Continu
- Amélioration automatique des suggestions
- Détection de nouveaux patterns comportementaux
- Scoring de confiance évolutif

## Tests et Validation

### Pour Tester le Système
1. **Démarrer l'application** : `mvn spring-boot:run`
2. **Naviguer vers les transactions** non assignées (September budget)
3. **Cliquer "Attribution intelligente"** sur les 20 transactions
4. **Vérifier les suggestions** retournées
5. **Accepter/rejeter** quelques suggestions pour tester l'apprentissage

### Endpoints API Pertinents
- `POST /api/transactions/{id}/assign-category` : Attribution manuelle
- `POST /api/transactions/{id}/smart-assign` : Attribution intelligente
- `POST /api/transactions/{id}/feedback` : Feedback sur suggestion

### Monitoring des Patterns
```sql
-- Voir les patterns personnels d'un utilisateur
SELECT * FROM user_merchant_patterns WHERE user_id = [USER_ID] ORDER BY confidence_score DESC;

-- Statistiques d'usage
SELECT source, COUNT(*), AVG(confidence_score) 
FROM user_merchant_patterns 
WHERE user_id = [USER_ID] 
GROUP BY source;
```

## Points d'Attention

### 1. Gestion de la Confiance
- Patterns personnels commencent avec confiance élevée (0.90)
- Patterns globaux ont confiance variable selon leur historique
- Patterns avec précision <30% sont supprimés automatiquement

### 2. Performance
- Index sur (user_id, pattern) pour requêtes rapides
- Limite de 100 transactions analysées pour l'apprentissage automatique
- Patterns triés par confiance puis usage

### 3. Évolution Future
- Possibilité d'ajouter apprentissage par machine learning
- Analyse sémantique des descriptions de transactions
- Patterns basés sur montants et dates

---

**Version**: 1.0 - Implémenté le 10/09/2025  
**Statut**: Prêt pour test avec les 20 transactions de septembre