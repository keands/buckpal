# 🧪 Bruno API Integration Testing - BuckPal

## 🎯 Vue d'Ensemble

Cette documentation couvre la **Phase 2** du système de tests BuckPal : les **tests d'intégration API avec Bruno** intégrés dans votre **pipeline GitLab CI**.

## 📋 Table des Matières

- [Installation et Configuration](#installation-et-configuration)
- [Structure des Tests](#structure-des-tests) 
- [Exécution Locale](#exécution-locale)
- [Intégration GitLab CI](#intégration-gitlab-ci)
- [Environnements](#environnements)
- [Maintenance](#maintenance)

## 🚀 Installation et Configuration

### Prérequis

- **Node.js 18+** installé
- **Bruno CLI** installé globalement
- **Application Spring Boot** démarrée sur `localhost:8080`
- **Base de données PostgreSQL** configurée

### Installation de Bruno CLI

```bash
npm install -g @usebruno/cli
```

### Vérification de l'Installation

```bash
bru --version
```

## 📁 Structure des Tests

```
bruno-tests/BuckPal-API/
├── bruno.json                    # Configuration de collection
├── environments/
│   ├── Local.bru                 # Variables environnement local
│   └── CI.bru                    # Variables environnement CI
├── 01-Authentication/
│   ├── 01-Signup.bru            # Création utilisateur
│   └── 02-Signin.bru            # Authentification + token
├── 02-Accounts/
│   ├── 01-Create-Account.bru    # Création compte
│   └── 02-Get-Accounts.bru      # Liste des comptes
├── 03-Transactions/
│   ├── 01-Create-Transaction.bru # Création transaction
│   └── 02-Get-Transactions.bru   # Liste transactions paginée
├── 04-Budgets/
│   ├── 01-Create-Budget.bru     # Création budget
│   └── 02-Get-Budget-Models.bru  # Modèles de budget
├── 05-Categories/
│   └── 01-Get-Category-Mapping.bru # Mapping catégories
└── 08-CSV-Import/
    └── 01-Get-CSV-Template.bru   # Template CSV
```

## 🧪 Types de Tests

### **Tests d'Authentification**
- ✅ Création d'utilisateur (signup)
- ✅ Connexion et récupération de token JWT
- ✅ Stockage automatique du token pour tests suivants

### **Tests de Business Logic**
- ✅ **Gestion des Comptes** : CRUD comptes bancaires
- ✅ **Gestion des Transactions** : Création, liste paginée 
- ✅ **Gestion des Budgets** : Création, modèles disponibles
- ✅ **Système de Catégories** : Mapping des catégories
- ✅ **Import CSV** : Récupération template

### **Validations Incluses**
- ✅ **Status HTTP** corrects (200, 201, 404, etc.)
- ✅ **Structure des réponses JSON**
- ✅ **Présence des champs obligatoires**
- ✅ **Types et valeurs des données**
- ✅ **Headers HTTP** appropriés
- ✅ **Chaînage des tests** (utilisation des IDs créés)

## 🏃‍♂️ Exécution Locale

### Méthode 1 : Script Automatisé (Recommandée)

```bash
# Démarrer Spring Boot
mvn spring-boot:run

# Dans un autre terminal
./scripts/run-bruno-tests.sh local
```

### Méthode 2 : Commandes Directes

```bash
# Vérifier que l'app tourne
curl http://localhost:8080/actuator/health

# Exécuter les tests Bruno
cd bruno-tests/BuckPal-API
bru run --env Local
```

### Méthode 3 : Tests Spécifiques

```bash
# Tester seulement l'authentification
bru run --env Local 01-Authentication/

# Tester un endpoint spécifique  
bru run --env Local 02-Accounts/01-Create-Account.bru
```

## 🔧 Intégration GitLab CI

### Configuration Pipeline

Ajoutez à votre `.gitlab-ci.yml` :

```yaml
include:
  - local: '.gitlab-ci-bruno.yml'
```

### Variables CI/CD Nécessaires

Configurez dans GitLab Project Settings > CI/CD > Variables :

```bash
# Base de données de test
DATABASE_URL=jdbc:postgresql://postgres:5432/buckpal_test
DATABASE_USERNAME=buckpal  
DATABASE_PASSWORD=buckpal

# Profil Spring Boot pour CI
SPRING_PROFILES_ACTIVE=ci
```

### Étapes du Pipeline CI

1. **Build** : Compilation Maven + tests unitaires
2. **Build App** : Package de l'application JAR
3. **API Tests** : 
   - Démarrage PostgreSQL
   - Lancement Spring Boot en arrière-plan
   - Attente de la disponibilité de l'API
   - Exécution des tests Bruno
   - Génération rapport JUnit XML

### Résultats des Tests

- **Rapports JUnit** intégrés dans GitLab
- **Artifacts** conservés pour debugging
- **Échec du pipeline** si tests API échouent

## 🌍 Environnements

### Environment Local

```javascript
vars {
  baseUrl: http://localhost:8080
  apiPath: /api  
  testEmail: test@buckpal.com
  testPassword: Test123!
}
```

### Environment CI

```javascript
vars {
  baseUrl: http://localhost:8080
  apiPath: /api
  testEmail: ci-test@buckpal.com  
  testPassword: CiTest123!
  dbResetUrl: http://localhost:8080/api/admin/reset-test-db
}
```

### Variables Dynamiques

Les tests stockent automatiquement :
- `authToken` : Token JWT Bearer
- `userId` : ID de l'utilisateur connecté
- `testAccountId` : ID du compte de test
- `testBudgetId` : ID du budget de test
- `testTransactionId` : ID de la transaction de test

## 📊 Monitoring et Rapports

### Format de Sortie

- **Console** : Résultats en temps réel
- **HTML** : Rapport visuel (local)
- **JUnit XML** : Intégration CI/CD
- **JSON** : Analyse programmatique

### Métriques Surveillées

- ✅ **Taux de succès** des endpoints
- ✅ **Temps de réponse** API
- ✅ **Couverture fonctionnelle**
- ✅ **Régression** des features

## 🛠️ Maintenance

### Ajouter de Nouveaux Tests

1. **Créer le fichier `.bru`** dans le bon dossier
2. **Définir la metadata** (nom, séquence)
3. **Configurer la requête** HTTP
4. **Écrire les assertions** de test
5. **Tester localement**

#### Exemple de Nouveau Test

```javascript
meta {
  name: Update Account
  type: http
  seq: 3
}

put {
  url: {{baseUrl}}{{apiPath}}/accounts/{{testAccountId}}
  body: json
  auth: bearer
}

auth:bearer {
  token: {{authToken}}
}

body:json {
  {
    "name": "Updated Account Name",
    "balance": 1500.00
  }
}

tests {
  test("Status should be 200", function() {
    expect(res.getStatus()).to.equal(200);
  });
  
  test("Account should be updated", function() {
    const data = res.getBody();
    expect(data.name).to.equal('Updated Account Name');
    expect(data.balance).to.equal(1500.00);
  });
}
```

### Debugging des Tests

```bash
# Verbeux avec détails
bru run --env Local --verbose

# Test spécifique en mode debug
bru run --env Local --reporter-html-template 01-Authentication/02-Signin.bru
```

### Mise à Jour des Variables

Modifiez les fichiers d'environnement :
- `environments/Local.bru` pour développement
- `environments/CI.bru` pour intégration continue

## 🚀 Avantages

### **Qualité**
- ✅ Tests réels contre l'API complète
- ✅ Validation du comportement end-to-end
- ✅ Détection des régressions rapidement

### **Intégration**
- ✅ Pipeline GitLab CI automatisé
- ✅ Rapports intégrés
- ✅ Blocage des déploiements défaillants

### **Maintenance**
- ✅ Tests lisibles et maintenables
- ✅ Environnements configurables
- ✅ Exécution locale simple

### **Performance**
- ✅ Tests rapides (~2-5 minutes)
- ✅ Parallélisation possible
- ✅ Feedback immédiat

## 🎯 Prochaines Étapes

### Extensions Possibles

1. **Plus d'Endpoints** : Couvrir les 85 endpoints REST
2. **Tests de Charge** : Performance sous stress
3. **Tests de Sécurité** : Validation des permissions
4. **Tests de Régression** : Suite complète automatisée
5. **Monitoring** : Métriques de performance API

---

## 📞 Support

Pour toute question sur les tests Bruno :
1. Vérifiez les logs GitLab CI
2. Testez localement avec `./scripts/run-bruno-tests.sh`
3. Consultez la documentation Bruno : https://docs.usebruno.com/

**Les tests Bruno complètent parfaitement la Phase 1 (tests unitaires) pour une couverture de test complète et robuste de votre API BuckPal !** 🎉