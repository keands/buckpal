import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/contexts/auth-context';
import { CategoryManagement } from '@/components/budget/category-management';
import { 
  User, 
  Tags, 
  Bell, 
  Shield, 
  Palette,
  Globe,
  Database
} from 'lucide-react';

export default function SettingsPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState('profile');

  // Handle URL tab parameter
  useEffect(() => {
    const tabFromUrl = searchParams.get('tab');
    if (tabFromUrl && ['profile', 'categories', 'notifications', 'security', 'appearance', 'data'].includes(tabFromUrl)) {
      setActiveTab(tabFromUrl);
    }
  }, [searchParams]);

  // Update URL when tab changes
  const handleTabChange = (value: string) => {
    setActiveTab(value);
    if (value !== 'profile') {
      setSearchParams({ tab: value });
    } else {
      setSearchParams({});
    }
  };

  return (
    <div className="max-w-6xl mx-auto py-6 px-4">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Paramètres</h1>
        <p className="text-gray-600 mt-2">
          Gérez vos préférences et la configuration de votre compte
        </p>
      </div>

      <Tabs value={activeTab} onValueChange={handleTabChange} className="space-y-6">
        <TabsList className="grid w-full grid-cols-6">
          <TabsTrigger value="profile" className="flex items-center gap-2">
            <User className="w-4 h-4" />
            <span className="hidden sm:inline">Profil</span>
          </TabsTrigger>
          <TabsTrigger value="categories" className="flex items-center gap-2">
            <Tags className="w-4 h-4" />
            <span className="hidden sm:inline">Catégories</span>
          </TabsTrigger>
          <TabsTrigger value="notifications" className="flex items-center gap-2">
            <Bell className="w-4 h-4" />
            <span className="hidden sm:inline">Notifications</span>
          </TabsTrigger>
          <TabsTrigger value="security" className="flex items-center gap-2">
            <Shield className="w-4 h-4" />
            <span className="hidden sm:inline">Sécurité</span>
          </TabsTrigger>
          <TabsTrigger value="appearance" className="flex items-center gap-2">
            <Palette className="w-4 h-4" />
            <span className="hidden sm:inline">Apparence</span>
          </TabsTrigger>
          <TabsTrigger value="data" className="flex items-center gap-2">
            <Database className="w-4 h-4" />
            <span className="hidden sm:inline">Données</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="profile" className="space-y-6">
          <ProfileSettings user={user} />
        </TabsContent>

        <TabsContent value="categories" className="space-y-6">
          <div className="mb-4">
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Gestion des Catégories</h2>
            <p className="text-gray-600">
              Créez et gérez vos catégories personnalisées pour organiser vos transactions
            </p>
          </div>
          <CategoryManagement />
        </TabsContent>

        <TabsContent value="notifications" className="space-y-6">
          <NotificationSettings />
        </TabsContent>

        <TabsContent value="security" className="space-y-6">
          <SecuritySettings />
        </TabsContent>

        <TabsContent value="appearance" className="space-y-6">
          <AppearanceSettings />
        </TabsContent>

        <TabsContent value="data" className="space-y-6">
          <DataSettings />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function ProfileSettings({ user }: { user: any }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <User className="w-5 h-5" />
          Informations du Profil
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="text-sm font-medium text-gray-700">Prénom</label>
            <div className="mt-1 p-2 border border-gray-200 rounded-md bg-gray-50">
              {user?.firstName || 'Non renseigné'}
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-gray-700">Nom</label>
            <div className="mt-1 p-2 border border-gray-200 rounded-md bg-gray-50">
              {user?.lastName || 'Non renseigné'}
            </div>
          </div>
          <div className="md:col-span-2">
            <label className="text-sm font-medium text-gray-700">Email</label>
            <div className="mt-1 p-2 border border-gray-200 rounded-md bg-gray-50">
              {user?.email || 'Non renseigné'}
            </div>
          </div>
        </div>
        
        <div className="pt-4 border-t">
          <Button variant="outline" disabled>
            <User className="w-4 h-4 mr-2" />
            Modifier le Profil (Bientôt disponible)
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function NotificationSettings() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bell className="w-5 h-5" />
          Préférences de Notification
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Notifications par email</h4>
              <p className="text-sm text-gray-500">Recevoir des alertes par email</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Configurer
            </Button>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Notifications de budget</h4>
              <p className="text-sm text-gray-500">Alertes de dépassement de budget</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Configurer
            </Button>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Rappels mensuels</h4>
              <p className="text-sm text-gray-500">Rappels de création de budget</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Configurer
            </Button>
          </div>
        </div>
        
        <p className="text-sm text-gray-500 italic">
          Les notifications seront disponibles dans une future version
        </p>
      </CardContent>
    </Card>
  );
}

function SecuritySettings() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Shield className="w-5 h-5" />
          Sécurité et Confidentialité
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Changer le mot de passe</h4>
              <p className="text-sm text-gray-500">Mettre à jour votre mot de passe</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Modifier
            </Button>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Authentification à deux facteurs</h4>
              <p className="text-sm text-gray-500">Sécurisez davantage votre compte</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Activer
            </Button>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Sessions actives</h4>
              <p className="text-sm text-gray-500">Gérer vos sessions de connexion</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Voir
            </Button>
          </div>
        </div>
        
        <p className="text-sm text-gray-500 italic">
          Les options de sécurité avancées seront disponibles dans une future version
        </p>
      </CardContent>
    </Card>
  );
}

function AppearanceSettings() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Palette className="w-5 h-5" />
          Apparence et Interface
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Thème</h4>
              <p className="text-sm text-gray-500">Choisir entre mode clair et sombre</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Clair
            </Button>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Langue</h4>
              <p className="text-sm text-gray-500">Changer la langue de l'interface</p>
            </div>
            <div className="flex items-center gap-2">
              <Globe className="w-4 h-4 text-gray-500" />
              <span className="text-sm">Français</span>
            </div>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Format des nombres</h4>
              <p className="text-sm text-gray-500">Format d'affichage des montants</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              €1,234.56
            </Button>
          </div>
        </div>
        
        <p className="text-sm text-gray-500 italic">
          Les options d'apparence personnalisées seront disponibles dans une future version
        </p>
      </CardContent>
    </Card>
  );
}

function DataSettings() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Database className="w-5 h-5" />
          Gestion des Données
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Exporter mes données</h4>
              <p className="text-sm text-gray-500">Télécharger toutes vos données</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Exporter
            </Button>
          </div>
          
          <div className="flex items-center justify-between">
            <div>
              <h4 className="text-sm font-medium">Sauvegarde automatique</h4>
              <p className="text-sm text-gray-500">Configuration des sauvegardes</p>
            </div>
            <Button variant="outline" size="sm" disabled>
              Configurer
            </Button>
          </div>
          
          <div className="border-t pt-4">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-sm font-medium text-red-600">Supprimer mon compte</h4>
                <p className="text-sm text-gray-500">Supprimer définitivement votre compte</p>
              </div>
              <Button variant="destructive" size="sm" disabled>
                Supprimer
              </Button>
            </div>
          </div>
        </div>
        
        <p className="text-sm text-gray-500 italic">
          Les options de gestion des données seront disponibles dans une future version
        </p>
      </CardContent>
    </Card>
  );
}