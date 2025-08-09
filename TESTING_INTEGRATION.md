# Guide de Test - Intégration React + Spring Boot

## Setup pour les tests

### 1. Démarrer le backend Spring Boot
```bash
# Dans le répertoire principal
mvn spring-boot:run
```
Backend accessible sur `http://localhost:8080`

### 2. Démarrer le frontend React
```bash
# Dans react-app/
cd react-app
npm install
npm run dev
```
Frontend accessible sur `http://localhost:3000`

## Tests d'intégration complets

### Étape 1 : Créer un compte utilisateur

1. **Aller sur** `http://localhost:3000`
2. **Cliquer** sur "Créer un compte"
3. **Remplir le formulaire** :
   - Prénom : Jean
   - Nom : Dupont
   - Email : jean.dupont@test.com
   - Mot de passe : test123
   - Confirmer : test123
4. **Cliquer** "Créer le compte"
5. **Vérifier** : Redirection automatique vers le dashboard

### Étape 2 : Explorer le dashboard

1. **Vérifier l'affichage** :
   - Navbar avec nom d'utilisateur
   - Cartes de statistiques (solde, revenus, dépenses)
   - Section "Mes comptes" (vide au début)
   - Section "Transactions récentes" (vide au début)

2. **Tester la navigation** :
   - Cliquer sur les liens dans la navbar
   - Vérifier les pages placeholder

### Étape 3 : Créer un compte bancaire (via API directe)

Comme l'interface de gestion des comptes n'est pas encore implémentée, utilisons l'API directement :

```bash
# Récupérer le token JWT depuis le localStorage du navigateur
# Puis créer un compte :

curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "Compte Courant",
    "accountType": "CHECKING",
    "bankName": "BNP Paribas",
    "balance": 1500.00
  }'
```

**Ou via le dashboard (solution temporaire) :**
- Ouvrir les DevTools (F12)
- Console → Exécuter :
```javascript
// Créer un compte via JavaScript
fetch('/api/accounts', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
  },
  body: JSON.stringify({
    name: 'Compte Courant',
    accountType: 'CHECKING',
    bankName: 'BNP Paribas',
    balance: 1500.00
  })
}).then(r => r.json()).then(console.log)
```

3. **Actualiser** le dashboard - le compte devrait apparaître

### Étape 4 : Tester l'import CSV complet

1. **Aller** sur "Import CSV" dans la navbar

#### Test avec votre fichier réel
2. **Upload** votre fichier `09082025_659915.csv` :
   - Sélectionner le fichier
   - Cliquer "Continuer"
   - Vérifier l'aperçu avec les 13 colonnes

3. **Configuration du mapping** :
   - Sélectionner le compte créé précédemment
   - **Date** : Colonne 0 (Date de comptabilisation)
   - **Description** : Colonne 1 (Libelle simplifie)
   - **Débit** : Colonne 8 (Debit)
   - **Crédit** : Colonne 9 (Credit)
   - **Catégorie** : Colonne 7 (Sous categorie)
   - **Cocher** "Sauvegarder ce mapping"
   - **Nom banque** : "BNP Paribas"
   - Cliquer "Prévisualiser"

4. **Prévisualisation** :
   - Vérifier le parsing des transactions
   - Voir les statistiques (valides, erreurs, doublons)
   - Tester l'approbation/rejet de transactions
   - Cliquer "Importer X transactions"

5. **Résultats** :
   - Vérifier les statistiques d'import
   - Noter les éventuelles erreurs
   - Retourner au dashboard

#### Test avec fichier simple
Créer un fichier `test.csv` :
```csv
Date;Description;Debit;Credit
09/08/2025;Test Transaction 1;10,50;
08/08/2025;Test Transaction 2;;25,00
07/08/2025;Test Transaction 3;5,75;
```

Répéter le processus d'import avec ce fichier plus simple.

### Étape 5 : Vérifier les données importées

1. **Dashboard** : Voir les nouvelles transactions dans "Transactions récentes"
2. **Solde** : Vérifier que le solde du compte a été mis à jour

**Via API (pour validation) :**
```bash
curl -X GET "http://localhost:8080/api/transactions?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Étape 6 : Tester la réutilisation du template

1. **Nouvel import CSV** avec un fichier du même format
2. **Étape mapping** : Cliquer sur le bouton "BNP Paribas" dans les templates
3. **Vérifier** que le mapping est pré-rempli
4. **Continuer** l'import

### Étape 7 : Tests d'erreur et edge cases

#### Test avec fichier invalide
- Essayer d'uploader un fichier .txt → Erreur
- Essayer un CSV mal formaté → Gérer les erreurs

#### Test avec données problématiques
Créer `test_errors.csv` :
```csv
Date;Description;Debit;Credit
32/13/2025;Invalid Date;10,50;
08/08/2025;Both Columns;15,25;20,00
invalid-date;Another Error;abc,def;
```

Vérifier que les erreurs sont bien détectées et proposées en validation manuelle.

#### Test de doublons
- Importer le même fichier deux fois
- Vérifier la détection de doublons
- Tester les choix d'approbation/rejet

### Étape 8 : Tests d'authentification

1. **Déconnexion** : Cliquer sur "Déconnexion"
2. **Vérification** : Redirection vers /login
3. **Route protégée** : Essayer d'accéder à `/dashboard` → Redirection login
4. **Reconnexion** : Se connecter avec les mêmes identifiants
5. **Token persistance** : Actualiser la page → Rester connecté

### Étape 9 : Tests de responsive design

1. **Tester** sur différentes tailles d'écran
2. **Mobile** : Vérifier que l'interface s'adapte
3. **Tablet** : Vérifier la disposition des cartes

## Vérifications techniques

### Logs backend à surveiller
```bash
# Logs d'authentification
logging.level.com.buckpal.security=DEBUG

# Logs d'import CSV
logging.level.com.buckpal.service.CsvImportWizardService=DEBUG

# Logs SQL
logging.level.org.hibernate.SQL=DEBUG
```

### DevTools frontend
- **Network** : Vérifier les appels API (codes 200, 401, etc.)
- **Console** : Pas d'erreurs JavaScript
- **Application > Storage** : Token JWT présent

### Base de données
Vérifier les données en base :
```sql
SELECT * FROM users;
SELECT * FROM accounts;
SELECT * FROM transactions ORDER BY created_at DESC LIMIT 10;
SELECT * FROM csv_mapping_templates;
```

## Problèmes courants et solutions

### Erreur CORS
**Symptôme :** Erreurs CORS dans les DevTools
**Solution :** Vérifier la configuration CORS du backend Spring Boot

### Token JWT invalide
**Symptôme :** Redirections constantes vers /login
**Solution :** 
- Vérifier la clé JWT dans `application.yml`
- Supprimer le token du localStorage et se reconnecter

### Proxy ne fonctionne pas
**Symptôme :** Erreurs 404 sur les appels API
**Solution :** 
- Vérifier que le backend tourne sur port 8080
- Redémarrer le dev server React (`npm run dev`)

### Import CSV échoue
**Symptôme :** Erreurs lors du mapping
**Solution :**
- Vérifier le format du CSV (encodage UTF-8)
- Tester avec le fichier exemple fourni
- Vérifier les logs backend

## Métriques de réussite

✅ **Authentification** : Inscription, connexion, déconnexion fonctionnelles
✅ **Dashboard** : Affichage des données, navigation fluide  
✅ **Import CSV** : Workflow complet fonctionnel avec votre fichier réel
✅ **Templates** : Sauvegarde et réutilisation des mappings
✅ **Validation** : Gestion des erreurs et doublons
✅ **API** : Toutes les requêtes HTTP retournent les bonnes réponses
✅ **UX** : Interface intuitive et responsive

L'intégration est réussie si tous ces points fonctionnent correctement !