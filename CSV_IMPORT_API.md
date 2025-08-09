# API Import CSV - Documentation

## Vue d'ensemble

L'API d'import CSV permet aux utilisateurs d'importer des transactions bancaires via un processus wizard multi-étapes avec mapping des colonnes personnalisable, validation des données, détection de doublons et sauvegarde des templates de mapping.

## Workflow du Wizard

### Étape 1: Upload du fichier CSV
`POST /api/csv-import/upload`

### Étape 2: Configuration du mapping des colonnes
`POST /api/csv-import/mapping`

### Étape 3: Validation et import final
`POST /api/csv-import/validate`

---

## Endpoints détaillés

### 1. Upload du fichier CSV

**Endpoint:** `POST /api/csv-import/upload`

**Description:** Upload d'un fichier CSV et récupération d'un aperçu avec les en-têtes.

**Headers:**
```
Content-Type: multipart/form-data
Authorization: Bearer <jwt_token>
```

**Paramètres:**
- `file` (form-data): Fichier CSV à uploader

**Réponse de succès (200):**
```json
{
  "sessionId": "uuid-string",
  "headers": ["Date", "Description", "Montant", "Catégorie"],
  "previewData": [
    ["01/12/2023", "Achat supermarché", "-45.67", "Alimentation"],
    ["02/12/2023", "Salaire", "2500.00", "Revenus"]
  ],
  "totalRows": 150
}
```

**Erreurs:**
- `400 Bad Request`: Fichier vide ou format invalide
- `500 Internal Server Error`: Erreur lors du traitement du fichier

---

### 2. Configuration du mapping des colonnes

**Endpoint:** `POST /api/csv-import/mapping`

**Description:** Configuration du mapping des colonnes et prévisualisation avec validation.

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

**Corps de la requête:**
```json
{
  "sessionId": "uuid-string",
  "accountId": 1,
  "dateColumnIndex": 0,
  "amountColumnIndex": 2,
  "debitColumnIndex": null,
  "creditColumnIndex": null,
  "descriptionColumnIndex": 1,
  "categoryColumnIndex": 3,
  "bankName": "BNP Paribas",
  "saveMapping": true
}
```

**Paramètres:**
- `sessionId` (string, requis): ID de session de l'upload
- `accountId` (number, requis): ID du compte de destination
- `dateColumnIndex` (number, requis): Index de la colonne de date
- `amountColumnIndex` (number): Index de la colonne de montant unique
- `debitColumnIndex` (number): Index de la colonne de débit (alternatif)
- `creditColumnIndex` (number): Index de la colonne de crédit (alternatif)
- `descriptionColumnIndex` (number): Index de la colonne de description
- `categoryColumnIndex` (number): Index de la colonne de catégorie (optionnel)
- `bankName` (string): Nom de la banque pour sauvegarder le template
- `saveMapping` (boolean): Sauvegarder ce mapping pour usage futur

**Réponse de succès (200):**
```json
{
  "sessionId": "uuid-string",
  "validTransactions": [
    {
      "rowIndex": 2,
      "transactionDate": "2023-12-01",
      "amount": -45.67,
      "description": "Achat supermarché",
      "category": "Alimentation",
      "transactionType": "EXPENSE"
    }
  ],
  "validationErrors": [
    {
      "rowIndex": 3,
      "error": "Format de date invalide: 32/13/2023",
      "rawData": "32/13/2023,Description,100.00",
      "field": "date"
    }
  ],
  "duplicateWarnings": [
    {
      "rowIndex": 4,
      "transactionDate": "2023-12-01",
      "amount": -45.67,
      "description": "Achat supermarché",
      "existingTransactionId": 123
    }
  ],
  "totalProcessed": 150,
  "validCount": 140,
  "errorCount": 5,
  "duplicateCount": 5
}
```

---

### 3. Validation et import final

**Endpoint:** `POST /api/csv-import/validate`

**Description:** Import final avec choix de validation de l'utilisateur.

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

**Corps de la requête:**
```json
{
  "sessionId": "uuid-string",
  "approvedRows": [2, 5, 6, 7],
  "rejectedRows": [3, 4],
  "manualCorrections": {
    "8": {
      "correctedDate": "01/12/2023",
      "correctedAmount": "-45.67",
      "correctedDescription": "Achat corrigé",
      "categoryId": 1
    }
  }
}
```

**Paramètres:**
- `sessionId` (string, requis): ID de session
- `approvedRows` (array): Numéros de lignes à importer
- `rejectedRows` (array): Numéros de lignes à ignorer
- `manualCorrections` (object): Corrections manuelles par numéro de ligne

**Réponse de succès (200):**
```json
{
  "sessionId": "uuid-string",
  "totalProcessed": 150,
  "successfulImports": 140,
  "skippedRows": 5,
  "failedImports": 5,
  "errors": [
    "Ligne 15: Montant invalide après correction"
  ],
  "importedTransactionIds": [201, 202, 203, 204]
}
```

---

### 4. Récupération des templates sauvegardés

**Endpoint:** `GET /api/csv-import/templates`

**Description:** Récupération des templates de mapping sauvegardés pour l'utilisateur.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Réponse de succès (200):**
```json
[
  {
    "id": 1,
    "bankName": "BNP Paribas",
    "dateColumnIndex": 0,
    "amountColumnIndex": 2,
    "descriptionColumnIndex": 1,
    "categoryColumnIndex": 3,
    "dateFormat": "dd/MM/yyyy"
  }
]
```

---

### 5. Application d'un template sauvegardé

**Endpoint:** `POST /api/csv-import/templates/{bankName}/apply`

**Description:** Application d'un template de mapping sauvegardé.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Paramètres d'URL:**
- `bankName` (string): Nom de la banque du template

**Paramètres de requête:**
- `sessionId` (string): ID de session de l'upload

**Réponse de succès (200):**
```json
{
  "sessionId": "uuid-string",
  "dateColumnIndex": 0,
  "amountColumnIndex": 2,
  "descriptionColumnIndex": 1,
  "categoryColumnIndex": 3,
  "bankName": "BNP Paribas"
}
```

---

### 6. Template CSV exemple

**Endpoint:** `GET /api/csv-import/template`

**Description:** Téléchargement d'un template CSV exemple.

**Réponse de succès (200):**
```
Content-Type: text/csv
Content-Disposition: attachment; filename=template.csv
```

---

## Formats de données supportés

### Formats de date
- `DD/MM/YYYY` (01/12/2023)
- `MM/DD/YYYY` (12/01/2023)
- `YYYY-MM-DD` (2023-12-01)
- `D/M/YYYY` (1/12/2023)

### Formats de montant
- Décimaux simples: `45.67`, `-45.67`
- Avec symboles monétaires: `€45.67`, `$45.67`
- Avec séparateurs: `1,234.56`
- Parenthèses pour négatifs: `(45.67)`
- Colonnes séparées débit/crédit

### Structure CSV
- Ligne d'en-têtes obligatoire
- Séparateur virgule `,`
- Guillemets pour échapper: `"Description, avec virgule"`
- Encodage UTF-8 recommandé

---

## Gestion des erreurs

### Codes d'erreur HTTP
- `400 Bad Request`: Paramètres invalides ou données malformées
- `401 Unauthorized`: Token JWT manquant ou invalide
- `404 Not Found`: Session ou ressource non trouvée
- `500 Internal Server Error`: Erreur serveur

### Types d'erreurs de validation
- **Date invalide**: Format de date non reconnu
- **Montant invalide**: Valeur non numérique
- **Colonnes multiples**: Montant présent dans plusieurs colonnes
- **Données manquantes**: Champs obligatoires vides
- **Doublon détecté**: Transaction identique existante

---

## Cas d'usage

### 1. Import standard
1. Upload du fichier CSV
2. Configuration du mapping (colonnes standard)
3. Validation et import de toutes les transactions valides

### 2. Import avec validation manuelle
1. Upload du fichier CSV
2. Configuration du mapping
3. Révision des erreurs et doublons
4. Correction manuelle des données problématiques
5. Import sélectif

### 3. Réutilisation de template
1. Upload du fichier CSV du même format
2. Application du template sauvegardé
3. Validation et import

---

## Exemples de fichiers CSV supportés

### Format banque française
```csv
Date;Libellé;Débit;Crédit;Solde
01/12/2023;ACHAT CB MONOPRIX;45,67;;1234,56
02/12/2023;VIR SALAIRE;;2500,00;3734,56
```

### Format international
```csv
Date,Description,Amount,Category
2023-12-01,Grocery Store,-45.67,Food
2023-12-02,Salary,2500.00,Income
```

### Format avec colonnes étendues
```csv
Transaction Date,Merchant,Debit,Credit,Category,Reference
01/12/2023,MONOPRIX,45.67,,Food,CB1234
02/12/2023,COMPANY INC,,2500.00,Salary,VIR5678
```

---

## Sécurité

- **Authentification JWT**: Toutes les requêtes nécessitent un token valide
- **Isolation des données**: Les sessions sont isolées par utilisateur
- **Validation stricte**: Tous les paramètres sont validés côté serveur
- **Nettoyage des sessions**: Les sessions temporaires sont supprimées après import
- **Pas de stockage permanent**: Les fichiers CSV ne sont pas stockés sur le serveur