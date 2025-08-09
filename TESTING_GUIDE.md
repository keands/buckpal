# Guide de Test - Import CSV

## Tests Backend

### Exécuter les tests

Si Maven est installé :
```bash
# Tous les tests
mvn test

# Tests spécifiques
mvn test -Dtest=CsvParsingUnitTest
mvn test -Dtest=CsvAmountParsingTest
mvn test -Dtest=CsvImportWizardServiceIntegrationTest
```

### Tests créés

**1. `CsvParsingUnitTest`** - Tests unitaires isolés
- Parsing des lignes CSV (`;` vs `,`)
- Parsing des montants (format français vs anglais)
- Gestion des guillemets et caractères spéciaux

**2. `CsvAmountParsingTest`** - Tests d'upload et aperçu
- Upload de fichiers CSV avec différents formats
- Validation des en-têtes et aperçus
- Tests avec votre fichier réel `09082025_659915.csv`

**3. `CsvImportWizardServiceIntegrationTest`** - Tests complets
- Workflow complet du wizard
- Tests avec base de données H2 en mémoire
- Détection de doublons
- Sauvegarde et réutilisation des templates

## Tests manuels avec Postman/Insomnia

### 1. Upload du fichier CSV
```
POST http://localhost:8080/api/csv-import/upload
Headers: Authorization: Bearer <your_jwt_token>
Body: form-data
  - file: [votre fichier CSV]
```

Réponse attendue :
```json
{
  "sessionId": "uuid-string",
  "headers": ["Date de comptabilisation", "Libelle simplifie", ...],
  "previewData": [
    ["09/08/2025", "Swile FR Montpellier", "Shopping et services", ...]
  ],
  "totalRows": 150
}
```

### 2. Configuration du mapping
```
POST http://localhost:8080/api/csv-import/mapping
Headers: 
  - Authorization: Bearer <your_jwt_token>
  - Content-Type: application/json
  
Body:
{
  "sessionId": "uuid-from-step-1",
  "accountId": 1,
  "dateColumnIndex": 0,
  "descriptionColumnIndex": 1,
  "debitColumnIndex": 8,
  "creditColumnIndex": 9,
  "categoryColumnIndex": 7,
  "bankName": "Ma Banque",
  "saveMapping": true
}
```

Réponse attendue :
```json
{
  "sessionId": "uuid-string",
  "validTransactions": [...],
  "validationErrors": [...],
  "duplicateWarnings": [...],
  "totalProcessed": 150,
  "validCount": 145,
  "errorCount": 3,
  "duplicateCount": 2
}
```

### 3. Import final
```
POST http://localhost:8080/api/csv-import/validate
Headers:
  - Authorization: Bearer <your_jwt_token>
  - Content-Type: application/json

Body:
{
  "sessionId": "uuid-from-step-1",
  "approvedRows": [2, 3, 4, 5, 6],
  "rejectedRows": [7, 8],
  "manualCorrections": {
    "9": {
      "correctedDate": "09/08/2025",
      "correctedAmount": "-45.67",
      "correctedDescription": "Description corrigée"
    }
  }
}
```

## Fichiers de test disponibles

### Format français (votre fichier réel)
- Fichier : `src/test/resources/09082025_659915.csv`
- Séparateur : `;`
- Format décimal : `10,50`
- Format de date : `DD/MM/YYYY`
- Colonnes débit/crédit séparées

### Format anglais (créer pour test)
```csv
Date,Description,Amount,Category
08/09/2025,Grocery Store,-45.67,Food
08/08/2025,Salary,2500.00,Income
08/07/2025,Gas Station,-73.00,Transport
```

### Format mixte débit/crédit (créer pour test)
```csv
Date;Description;Debit;Credit
09/08/2025;Achat magasin;45,67;
08/08/2025;Salaire;;2500,00
07/08/2025;Frais bancaires;1,40;
```

## Cas de test importants

### ✅ Formats de date supportés
- `09/08/2025` (DD/MM/YYYY)
- `08/09/2025` (MM/DD/YYYY) 
- `2025-08-09` (YYYY-MM-DD)
- `9/8/2025` (D/M/YYYY)

### ✅ Formats de montant supportés
**Français :**
- `-10,50` (décimale virgule)
- `1 234,56` (espace milliers)
- `(45,67)` (parenthèses négatives)
- `-25,99€` (avec devise)

**Anglais :**
- `-10.50` (décimale point)
- `1,234.56` (virgule milliers)
- `(45.67)` (parenthèses négatives)
- `$25.99` (avec devise)

### ✅ Gestion des erreurs
- Dates invalides → validation manuelle
- Montants non numériques → erreur
- Colonnes débit+crédit remplies → validation manuelle
- Doublons détectés → avertissement

### ✅ Templates de mapping
- Sauvegarde par banque et utilisateur
- Réutilisation automatique
- Gestion des conflits (écrasement)

## Intégration Frontend Flutter

### Étapes d'implémentation suggérées

1. **Écran d'upload**
   - Widget `file_picker` pour sélectionner le CSV
   - Appel API `/upload` 
   - Affichage aperçu des premières lignes

2. **Écran de mapping**
   - Interface pour sélectionner les colonnes
   - Dropdowns pour chaque type de colonne
   - Prévisualisation en temps réel
   - Bouton "Utiliser template existant"

3. **Écran de validation**
   - Liste des transactions valides
   - Liste des erreurs avec correction manuelle
   - Liste des doublons avec choix
   - Boutons Approuver/Rejeter par transaction

4. **Écran de résultats**
   - Statistiques d'import
   - Liste des transactions créées
   - Bouton "Voir les transactions"

### Modèles Dart suggérés

```dart
class CsvUploadResponse {
  final String sessionId;
  final List<String> headers;
  final List<List<String>> previewData;
  final int totalRows;
}

class CsvColumnMappingRequest {
  final String sessionId;
  final int accountId;
  final int? dateColumnIndex;
  final int? amountColumnIndex;
  final int? debitColumnIndex;
  final int? creditColumnIndex;
  final int? descriptionColumnIndex;
  final String? bankName;
  final bool saveMapping;
}
```

## Dépannage

### Erreurs communes

**1. "Session not found"**
- Vérifier que le `sessionId` est correct
- Les sessions expirent après un certain temps

**2. "Account not found"**
- Vérifier que l'`accountId` existe et appartient à l'utilisateur

**3. "Format de date invalide"**
- Vérifier les formats supportés
- Utiliser la validation manuelle si nécessaire

**4. "JWT token required"**
- S'assurer que l'utilisateur est authentifié
- Vérifier l'en-tête `Authorization`

### Logs utiles

```
# Application logs
logging.level.com.buckpal.service.CsvImportWizardService=DEBUG

# SQL queries (test environment)
logging.level.org.hibernate.SQL=DEBUG
```

Cette infrastructure de test vous permet de valider complètement la fonctionnalité avant l'implémentation frontend !