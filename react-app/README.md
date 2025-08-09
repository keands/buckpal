# BuckPal Web - Frontend React

Interface web moderne pour l'application de gestion de budget BuckPal.

## Stack Technique

- **React 18** avec TypeScript
- **Vite** pour le build et le dev server
- **Tailwind CSS** pour le styling
- **React Router** pour la navigation
- **Axios** pour les appels API
- **React Hook Form** pour les formulaires
- **Zod** pour la validation

## Installation et lancement

### Prérequis
- Node.js 16+ 
- npm ou yarn
- Backend Spring Boot en cours d'exécution sur `http://localhost:8080`

### Installation
```bash
cd react-app
npm install
```

### Développement
```bash
npm run dev
```
L'application sera accessible sur `http://localhost:3000`

### Build de production
```bash
npm run build
```

## Structure du projet

```
src/
├── components/          # Composants UI réutilisables
│   ├── ui/             # Composants de base (Button, Input, Card...)
│   └── layout/         # Composants de layout (Navbar...)
├── contexts/           # Contextes React (auth, etc.)
├── lib/               # Utilitaires et configuration
│   ├── api.ts         # Client API TypeScript
│   └── utils.ts       # Fonctions utilitaires
├── pages/             # Pages de l'application
│   ├── login.tsx      # Page de connexion
│   ├── register.tsx   # Page d'inscription
│   ├── dashboard.tsx  # Tableau de bord
│   └── csv-import.tsx # Wizard d'import CSV
├── types/             # Types TypeScript
│   └── api.ts         # Types pour l'API
├── App.tsx            # Composant racine
└── main.tsx          # Point d'entrée
```

## Fonctionnalités implémentées

### ✅ Authentification
- Connexion avec email/mot de passe
- Inscription de nouveaux utilisateurs
- Gestion JWT token avec localStorage
- Routes protégées

### ✅ Dashboard
- Vue d'ensemble des comptes et soldes
- Statistiques mensuelles (revenus/dépenses)
- Transactions récentes
- Navigation rapide vers les fonctionnalités

### ✅ Import CSV - Wizard complet
- **Étape 1 :** Upload de fichier avec validation
- **Étape 2 :** Mapping des colonnes avec templates réutilisables
- **Étape 3 :** Prévisualisation des données transformées
- **Étape 4 :** Validation manuelle des erreurs et doublons
- **Étape 5 :** Résultats d'import avec statistiques

#### Spécificités de l'import CSV :
- Support formats français et anglais
- Colonnes séparées débit/crédit ou montant unique
- Templates de mapping par banque
- Détection automatique des doublons
- Validation des formats de date/montant
- Correction manuelle des données problématiques

## Configuration API

L'application utilise un proxy Vite pour rediriger les appels API vers le backend Spring Boot :

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

## Types TypeScript

Tous les types sont définis dans `src/types/api.ts` et correspondent exactement aux DTOs Java du backend Spring Boot.

## Authentification

Le token JWT est stocké dans le localStorage et automatiquement ajouté aux en-têtes des requêtes API. La déconnexion automatique se fait en cas de token expiré (401).

## Design System

L'interface utilise un design system basé sur shadcn/ui avec Tailwind CSS :
- Palette de couleurs cohérente
- Composants réutilisables
- Mode sombre supporté (variables CSS)
- Design responsive

## Intégration Backend

L'application est entièrement intégrée avec l'API Spring Boot :
- Endpoints d'authentification (`/api/auth/*`)
- Gestion des comptes (`/api/accounts/*`)
- Transactions (`/api/transactions/*`)
- Import CSV complet (`/api/csv-import/*`)

## Prochaines étapes

- [ ] Page de gestion des comptes
- [ ] Page de gestion des transactions
- [ ] Graphiques et visualisations
- [ ] Paramètres utilisateur
- [ ] Tests unitaires (Jest + React Testing Library)
- [ ] PWA (Progressive Web App)

## Scripts disponibles

```bash
npm run dev          # Serveur de développement
npm run build        # Build de production
npm run preview      # Aperçu du build
npm run lint         # Linting du code
```