import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/advanced-select';
import { Plus, Edit, Trash2, Settings } from 'lucide-react';
import { apiClient } from '@/lib/api';
import { translateDetailedCategoryName } from '@/lib/budget-utils';
import type { Category } from '@/types/api';

type BudgetCategoryKey = 'NEEDS' | 'WANTS' | 'SAVINGS' | 'PROJECTS';

interface CreateCategoryFormData {
  name: string;
  description: string;
  budgetCategoryKey: BudgetCategoryKey;
  iconName: string;
  colorCode: string;
}

interface EditCategoryFormData extends CreateCategoryFormData {
  id: number;
}

export function CategoryManagement() {
  const { t } = useTranslation();
  const [categoryGroups, setCategoryGroups] = useState<Record<string, Category[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedBudgetCategory, setSelectedBudgetCategory] = useState<BudgetCategoryKey | null>(null);
  const [createFormData, setCreateFormData] = useState<CreateCategoryFormData>({
    name: '',
    description: '',
    budgetCategoryKey: 'NEEDS',
    iconName: '',
    colorCode: '#ef4444'
  });
  const [editFormData, setEditFormData] = useState<EditCategoryFormData>({
    id: 0,
    name: '',
    description: '',
    budgetCategoryKey: 'NEEDS',
    iconName: '',
    colorCode: '#ef4444'
  });

  const budgetCategoryLabels = {
    NEEDS: { label: 'Besoins', icon: '🎯', color: '#ef4444' },
    WANTS: { label: 'Plaisirs', icon: '🎮', color: '#3b82f6' },
    SAVINGS: { label: 'Épargne', icon: '💰', color: '#10b981' },
    PROJECTS: { label: 'Projets Perso', icon: '🚀', color: '#8b5cf6' }
  };

  const predefinedIcons = [
    // Alimentation et restaurants
    { emoji: '🍔', name: 'Hamburger' },
    { emoji: '🍕', name: 'Pizza' },
    { emoji: '🍝', name: 'Pâtes' },
    { emoji: '🍽️', name: 'Restaurant' },
    { emoji: '☕', name: 'Café' },
    { emoji: '🍷', name: 'Boissons' },
    { emoji: '🛒', name: 'Courses' },
    // Transport
    { emoji: '🚗', name: 'Voiture' },
    { emoji: '⛽', name: 'Essence' },
    { emoji: '🚌', name: 'Transport public' },
    { emoji: '✈️', name: 'Avion' },
    { emoji: '🚲', name: 'Vélo' },
    // Logement et services
    { emoji: '🏠', name: 'Logement' },
    { emoji: '⚡', name: 'Électricité' },
    { emoji: '💧', name: 'Eau' },
    { emoji: '📱', name: 'Téléphone' },
    { emoji: '💻', name: 'Internet' },
    // Santé et soins
    { emoji: '🏥', name: 'Santé' },
    { emoji: '💊', name: 'Médicaments' },
    { emoji: '💄', name: 'Cosmétiques' },
    { emoji: '💇', name: 'Coiffeur' },
    // Loisirs et divertissement
    { emoji: '🎬', name: 'Cinéma' },
    { emoji: '🎮', name: 'Jeux vidéo' },
    { emoji: '🏋️', name: 'Sport' },
    { emoji: '📚', name: 'Livres' },
    { emoji: '🎵', name: 'Musique' },
    { emoji: '🎨', name: 'Art/Créatif' },
    // Shopping et vêtements
    { emoji: '👕', name: 'Vêtements' },
    { emoji: '👠', name: 'Chaussures' },
    { emoji: '💍', name: 'Bijoux' },
    { emoji: '🛍️', name: 'Shopping' },
    // Épargne et finances
    { emoji: '💰', name: 'Épargne' },
    { emoji: '💎', name: 'Investissements' },
    { emoji: '🏦', name: 'Banque' },
    { emoji: '💳', name: 'Cartes' },
    // Projets et éducation
    { emoji: '📖', name: 'Éducation' },
    { emoji: '🏗️', name: 'Travaux' },
    { emoji: '🔧', name: 'Outils' },
    { emoji: '🎁', name: 'Cadeaux' },
    // Divers
    { emoji: '❓', name: 'Divers' },
    { emoji: '📊', name: 'Business' },
    { emoji: '🎯', name: 'Objectifs' }
  ];

  const predefinedColors = [
    { name: 'Rouge', value: '#ef4444' },
    { name: 'Orange', value: '#f97316' },
    { name: 'Ambre', value: '#f59e0b' },
    { name: 'Jaune', value: '#eab308' },
    { name: 'Lime', value: '#84cc16' },
    { name: 'Vert', value: '#22c55e' },
    { name: 'Émeraude', value: '#10b981' },
    { name: 'Sarcelle', value: '#14b8a6' },
    { name: 'Cyan', value: '#06b6d4' },
    { name: 'Bleu ciel', value: '#0ea5e9' },
    { name: 'Bleu', value: '#3b82f6' },
    { name: 'Indigo', value: '#6366f1' },
    { name: 'Violet', value: '#8b5cf6' },
    { name: 'Pourpre', value: '#a855f7' },
    { name: 'Fuchsia', value: '#d946ef' },
    { name: 'Rose', value: '#ec4899' },
    { name: 'Ardoise', value: '#64748b' },
    { name: 'Gris', value: '#6b7280' },
    { name: 'Zinc', value: '#71717a' },
    { name: 'Pierre', value: '#78716c' }
  ];

  useEffect(() => {
    loadCategoryGroups();
  }, []);

  const loadCategoryGroups = async () => {
    try {
      setLoading(true);
      const groups = await apiClient.getCategoriesGroupedByBudgetCategory();
      setCategoryGroups(groups);
    } catch (err) {
      setError('Erreur lors du chargement des catégories');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateCategory = async () => {
    try {
      // Fermer la modal immédiatement pour une meilleure UX
      setShowCreateModal(false);
      
      // Appel API
      const newCategory = await apiClient.createCustomCategory({
        name: createFormData.name,
        description: createFormData.description,
        budgetCategoryKey: createFormData.budgetCategoryKey,
        iconName: createFormData.iconName,
        colorCode: createFormData.colorCode
      });

      // Mise à jour optimiste de l'interface
      const updatedGroups = { ...categoryGroups };
      if (!updatedGroups[createFormData.budgetCategoryKey]) {
        updatedGroups[createFormData.budgetCategoryKey] = [];
      }
      updatedGroups[createFormData.budgetCategoryKey] = [...updatedGroups[createFormData.budgetCategoryKey], newCategory];
      setCategoryGroups(updatedGroups);

      // Reset form
      setCreateFormData({
        name: '',
        description: '',
        budgetCategoryKey: 'NEEDS',
        iconName: '',
        colorCode: '#ef4444'
      });
      setSelectedBudgetCategory(null);
      
    } catch (err) {
      setError('Erreur lors de la création de la catégorie');
      // En cas d'erreur, recharger pour être sûr
      await loadCategoryGroups();
    }
  };

  const handleUpdateCategoryMapping = async (categoryId: number, newBudgetKey: BudgetCategoryKey) => {
    // Sauvegarde de l'état actuel pour rollback en cas d'erreur
    const previousGroups = { ...categoryGroups };
    
    try {
      // Mise à jour optimiste de l'interface
      const updatedGroups = { ...categoryGroups };
      
      // Trouver et déplacer la catégorie
      let categoryToMove: Category | null = null;
      let oldBudgetKey: string | null = null;
      
      for (const [budgetKey, categories] of Object.entries(updatedGroups)) {
        const categoryIndex = categories.findIndex(cat => cat.id === categoryId);
        if (categoryIndex !== -1) {
          categoryToMove = { ...categories[categoryIndex], budgetCategoryKey: newBudgetKey };
          oldBudgetKey = budgetKey;
          updatedGroups[budgetKey] = categories.filter(cat => cat.id !== categoryId);
          break;
        }
      }
      
      if (categoryToMove && oldBudgetKey !== newBudgetKey) {
        // Ajouter à la nouvelle catégorie
        if (!updatedGroups[newBudgetKey]) {
          updatedGroups[newBudgetKey] = [];
        }
        updatedGroups[newBudgetKey] = [...updatedGroups[newBudgetKey], categoryToMove];
        
        // Mettre à jour l'interface immédiatement
        setCategoryGroups(updatedGroups);
        
        // Appel API en arrière-plan
        await apiClient.updateCategoryMapping(categoryId, newBudgetKey);
      }
    } catch (err) {
      // Rollback en cas d'erreur
      setCategoryGroups(previousGroups);
      setError('Erreur lors de la mise à jour du mapping');
    }
  };

  const handleDeleteCategory = async (categoryId: number) => {
    // Sauvegarde de l'état actuel pour rollback en cas d'erreur
    const previousGroups = { ...categoryGroups };
    
    try {
      // Mise à jour optimiste de l'interface
      const updatedGroups = { ...categoryGroups };
      
      // Supprimer la catégorie de l'interface immédiatement
      for (const [budgetKey, categories] of Object.entries(updatedGroups)) {
        updatedGroups[budgetKey] = categories.filter(cat => cat.id !== categoryId);
      }
      
      setCategoryGroups(updatedGroups);
      
      // Appel API en arrière-plan
      await apiClient.deleteCustomCategory(categoryId);
    } catch (err) {
      // Rollback en cas d'erreur
      setCategoryGroups(previousGroups);
      setError('Erreur lors de la suppression de la catégorie');
    }
  };

  const handleEditCategory = async () => {
    // Sauvegarde de l'état actuel pour rollback en cas d'erreur
    const previousGroups = { ...categoryGroups };
    
    try {
      // Mise à jour optimiste de l'interface
      const updatedGroups = { ...categoryGroups };
      let categoryMoved = false;
      
      // Trouver et mettre à jour la catégorie
      for (const [budgetKey, categories] of Object.entries(updatedGroups)) {
        const categoryIndex = categories.findIndex(cat => cat.id === editFormData.id);
        if (categoryIndex !== -1) {
          const updatedCategory = {
            ...categories[categoryIndex],
            name: editFormData.name,
            description: editFormData.description,
            budgetCategoryKey: editFormData.budgetCategoryKey,
            iconName: editFormData.iconName,
            colorCode: editFormData.colorCode
          };
          
          // Si la catégorie de budget a changé, la déplacer
          if (budgetKey !== editFormData.budgetCategoryKey) {
            // Retirer de l'ancienne catégorie
            updatedGroups[budgetKey] = categories.filter(cat => cat.id !== editFormData.id);
            
            // Ajouter à la nouvelle catégorie
            if (!updatedGroups[editFormData.budgetCategoryKey]) {
              updatedGroups[editFormData.budgetCategoryKey] = [];
            }
            updatedGroups[editFormData.budgetCategoryKey] = [...updatedGroups[editFormData.budgetCategoryKey], updatedCategory];
            categoryMoved = true;
          } else {
            // Juste mettre à jour dans la même catégorie
            updatedGroups[budgetKey][categoryIndex] = updatedCategory;
          }
          break;
        }
      }
      
      // Mettre à jour l'interface immédiatement
      setCategoryGroups(updatedGroups);
      
      // Fermer la modal immédiatement
      setShowEditModal(false);
      
      // Reset form
      setEditFormData({
        id: 0,
        name: '',
        description: '',
        budgetCategoryKey: 'NEEDS',
        iconName: '',
        colorCode: '#ef4444'
      });
      
      // Appel API en arrière-plan
      await apiClient.updateCustomCategory(editFormData.id, {
        name: editFormData.name,
        description: editFormData.description,
        budgetCategoryKey: editFormData.budgetCategoryKey,
        iconName: editFormData.iconName,
        colorCode: editFormData.colorCode
      });
      
    } catch (err) {
      // Rollback en cas d'erreur
      setCategoryGroups(previousGroups);
      setError('Erreur lors de la modification de la catégorie');
    }
  };

  const openEditModal = (category: Category) => {
    setEditFormData({
      id: category.id,
      name: category.name,
      description: category.description || '',
      budgetCategoryKey: category.budgetCategoryKey || 'NEEDS',
      iconName: category.iconName || '',
      colorCode: category.colorCode || '#ef4444'
    });
    setShowEditModal(true);
  };

  const openCreateModal = (budgetKey: BudgetCategoryKey) => {
    setSelectedBudgetCategory(budgetKey);
    setCreateFormData(prev => ({ 
      ...prev, 
      budgetCategoryKey: budgetKey,
      colorCode: budgetCategoryLabels[budgetKey].color
    }));
    setShowCreateModal(true);
  };

  if (loading) {
    return (
      <div className="p-4">
        <div className="animate-pulse space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-32 bg-gray-200 rounded-lg" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4">
        <div className="text-red-500 text-center">{error}</div>
      </div>
    );
  }

  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold flex items-center gap-2">
          <Settings className="w-6 h-6" />
          Gestion des catégories
        </h2>
        <Button
          onClick={() => apiClient.initializeDefaultMappings()}
          variant="outline"
          size="sm"
        >
          Initialiser les mappings par défaut
        </Button>
      </div>

      {Object.entries(budgetCategoryLabels).map(([budgetKey, config]) => (
        <Card key={budgetKey} className="border-l-4" style={{ borderLeftColor: config.color }}>
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-2xl">{config.icon}</span>
                <span>{config.label}</span>
                <Badge variant="outline" className="ml-2">
                  {categoryGroups[budgetKey]?.length || 0} catégories
                </Badge>
              </div>
              <Button
                onClick={() => openCreateModal(budgetKey as BudgetCategoryKey)}
                size="sm"
                variant="outline"
                className="flex items-center gap-2"
              >
                <Plus className="w-4 h-4" />
                Ajouter
              </Button>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {categoryGroups[budgetKey]?.map((category) => (
                <div
                  key={category.id}
                  className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50"
                >
                  <div className="flex items-center gap-3">
                    {category.iconName && (
                      <span className="text-lg">{category.iconName}</span>
                    )}
                    <div>
                      <div className="font-medium">
                        {translateDetailedCategoryName(category.name, t)}
                      </div>
                      {category.description && (
                        <div className="text-sm text-gray-500">{category.description}</div>
                      )}
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-2">
                    <Badge variant={category.isDefault ? "secondary" : "outline"}>
                      {category.isDefault ? "Auto" : "Manuel"}
                    </Badge>
                    
                    <Select
                      value={budgetKey}
                      onValueChange={(newKey) => handleUpdateCategoryMapping(category.id, newKey as BudgetCategoryKey)}
                    >
                      <SelectTrigger className="w-40">
                        <SelectValue placeholder="Déplacer vers..." />
                      </SelectTrigger>
                      <SelectContent>
                        <div className="px-2 py-1 text-xs font-medium text-gray-500 border-b">
                          Déplacer cette catégorie vers :
                        </div>
                        {Object.entries(budgetCategoryLabels).map(([key, label]) => (
                          <SelectItem 
                            key={key} 
                            value={key}
                            className={key === budgetKey ? "bg-blue-50 font-medium" : ""}
                          >
                            <div className="flex items-center gap-2">
                              <span>{label.icon}</span>
                              <span>{label.label}</span>
                              {key === budgetKey && (
                                <span className="text-xs text-blue-600 ml-auto">(actuel)</span>
                              )}
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    
                    {!category.isDefault && (
                      <>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-blue-500 hover:text-blue-700 hover:bg-blue-50"
                          onClick={() => openEditModal(category)}
                        >
                          <Edit className="w-4 h-4" />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-red-500 hover:text-red-700 hover:bg-red-50"
                          onClick={() => handleDeleteCategory(category.id)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              )) || (
                <div className="text-center text-gray-500 py-4">
                  Aucune catégorie dans ce groupe
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ))}

      {/* Create Category Modal */}
      <Dialog open={showCreateModal} onOpenChange={setShowCreateModal}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Plus className="w-5 h-5" />
              Nouvelle catégorie
            </DialogTitle>
          </DialogHeader>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Nom</label>
              <Input
                value={createFormData.name}
                onChange={(e) => setCreateFormData(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Nom de la catégorie"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Description (optionnel)</label>
              <Input
                value={createFormData.description}
                onChange={(e) => setCreateFormData(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Description"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Associer à</label>
              <Select
                value={createFormData.budgetCategoryKey}
                onValueChange={(value) => setCreateFormData(prev => ({ ...prev, budgetCategoryKey: value as BudgetCategoryKey }))}
              >
                <SelectTrigger>
                  <div className="flex items-center gap-2">
                    <span>{budgetCategoryLabels[createFormData.budgetCategoryKey].icon}</span>
                    <span>{budgetCategoryLabels[createFormData.budgetCategoryKey].label}</span>
                  </div>
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(budgetCategoryLabels).map(([key, label]) => (
                    <SelectItem key={key} value={key}>
                      {label.icon} {label.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Icône</label>
              <div className="space-y-3">
                {/* Aperçu de l'icône sélectionnée */}
                {createFormData.iconName && (
                  <div className="flex items-center gap-2 p-2 bg-gray-50 rounded-md">
                    <span className="text-2xl">{createFormData.iconName}</span>
                    <span className="text-sm text-gray-600">
                      {predefinedIcons.find(icon => icon.emoji === createFormData.iconName)?.name}
                    </span>
                  </div>
                )}
                
                {/* Grille d'icônes */}
                <div className="grid grid-cols-8 gap-2 p-3 border rounded-lg max-h-48 overflow-y-auto bg-gray-50">
                  {predefinedIcons.map((icon) => (
                    <button
                      key={icon.emoji}
                      type="button"
                      className={`w-10 h-10 rounded-lg flex items-center justify-center text-xl transition-all hover:scale-110 hover:bg-white hover:shadow-sm ${
                        createFormData.iconName === icon.emoji 
                          ? 'bg-blue-100 ring-2 ring-blue-500 shadow-md' 
                          : 'bg-white hover:bg-gray-100'
                      }`}
                      onClick={() => setCreateFormData(prev => ({ ...prev, iconName: icon.emoji }))}
                      title={icon.name}
                    >
                      {icon.emoji}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Couleur</label>
              <div className="space-y-3">
                {/* Couleurs prédéfinies */}
                <div className="grid grid-cols-8 gap-2">
                  {predefinedColors.map((color) => (
                    <button
                      key={color.value}
                      type="button"
                      className={`w-8 h-8 rounded-full border-2 transition-all hover:scale-110 ${
                        createFormData.colorCode === color.value 
                          ? 'border-gray-800 ring-2 ring-gray-300' 
                          : 'border-gray-200 hover:border-gray-400'
                      }`}
                      style={{ backgroundColor: color.value }}
                      onClick={() => setCreateFormData(prev => ({ ...prev, colorCode: color.value }))}
                      title={color.name}
                    />
                  ))}
                </div>
                
                {/* Sélecteur de couleur personnalisé */}
                <div className="flex items-center gap-3">
                  <Input
                    type="color"
                    value={createFormData.colorCode}
                    onChange={(e) => setCreateFormData(prev => ({ ...prev, colorCode: e.target.value }))}
                    className="w-16 h-10 p-1 rounded cursor-pointer"
                  />
                  <Input
                    value={createFormData.colorCode}
                    onChange={(e) => setCreateFormData(prev => ({ ...prev, colorCode: e.target.value }))}
                    placeholder="#3b82f6"
                    className="flex-1"
                  />
                </div>
              </div>
            </div>
            
            <div className="flex justify-end gap-2 pt-4">
              <Button
                variant="outline"
                onClick={() => setShowCreateModal(false)}
              >
                Annuler
              </Button>
              <Button
                onClick={handleCreateCategory}
                disabled={!createFormData.name.trim()}
              >
                Créer
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Edit Category Modal */}
      <Dialog open={showEditModal} onOpenChange={setShowEditModal}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Edit className="w-5 h-5" />
              Modifier la catégorie
            </DialogTitle>
          </DialogHeader>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Nom</label>
              <Input
                value={editFormData.name}
                onChange={(e) => setEditFormData(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Nom de la catégorie"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Description (optionnel)</label>
              <Input
                value={editFormData.description}
                onChange={(e) => setEditFormData(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Description"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Associer à</label>
              <Select
                value={editFormData.budgetCategoryKey}
                onValueChange={(value) => setEditFormData(prev => ({ ...prev, budgetCategoryKey: value as BudgetCategoryKey }))}
              >
                <SelectTrigger>
                  <div className="flex items-center gap-2">
                    <span>{budgetCategoryLabels[editFormData.budgetCategoryKey].icon}</span>
                    <span>{budgetCategoryLabels[editFormData.budgetCategoryKey].label}</span>
                  </div>
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(budgetCategoryLabels).map(([key, label]) => (
                    <SelectItem key={key} value={key}>
                      {label.icon} {label.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Icône</label>
              <div className="space-y-3">
                {/* Aperçu de l'icône sélectionnée */}
                {editFormData.iconName && (
                  <div className="flex items-center gap-2 p-2 bg-gray-50 rounded-md">
                    <span className="text-2xl">{editFormData.iconName}</span>
                    <span className="text-sm text-gray-600">
                      {predefinedIcons.find(icon => icon.emoji === editFormData.iconName)?.name}
                    </span>
                  </div>
                )}
                
                {/* Grille d'icônes */}
                <div className="grid grid-cols-8 gap-2 p-3 border rounded-lg max-h-48 overflow-y-auto bg-gray-50">
                  {predefinedIcons.map((icon) => (
                    <button
                      key={icon.emoji}
                      type="button"
                      className={`w-10 h-10 rounded-lg flex items-center justify-center text-xl transition-all hover:scale-110 hover:bg-white hover:shadow-sm ${
                        editFormData.iconName === icon.emoji 
                          ? 'bg-blue-100 ring-2 ring-blue-500 shadow-md' 
                          : 'bg-white hover:bg-gray-100'
                      }`}
                      onClick={() => setEditFormData(prev => ({ ...prev, iconName: icon.emoji }))}
                      title={icon.name}
                    >
                      {icon.emoji}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-1">Couleur</label>
              <div className="space-y-3">
                {/* Couleurs prédéfinies */}
                <div className="grid grid-cols-8 gap-2">
                  {predefinedColors.map((color) => (
                    <button
                      key={color.value}
                      type="button"
                      className={`w-8 h-8 rounded-full border-2 transition-all hover:scale-110 ${
                        editFormData.colorCode === color.value 
                          ? 'border-gray-800 ring-2 ring-gray-300' 
                          : 'border-gray-200 hover:border-gray-400'
                      }`}
                      style={{ backgroundColor: color.value }}
                      onClick={() => setEditFormData(prev => ({ ...prev, colorCode: color.value }))}
                      title={color.name}
                    />
                  ))}
                </div>
                
                {/* Sélecteur de couleur personnalisé */}
                <div className="flex items-center gap-3">
                  <Input
                    type="color"
                    value={editFormData.colorCode}
                    onChange={(e) => setEditFormData(prev => ({ ...prev, colorCode: e.target.value }))}
                    className="w-16 h-10 p-1 rounded cursor-pointer"
                  />
                  <Input
                    value={editFormData.colorCode}
                    onChange={(e) => setEditFormData(prev => ({ ...prev, colorCode: e.target.value }))}
                    placeholder="#3b82f6"
                    className="flex-1"
                  />
                </div>
              </div>
            </div>
            
            <div className="flex justify-end gap-2 pt-4">
              <Button
                variant="outline"
                onClick={() => setShowEditModal(false)}
              >
                Annuler
              </Button>
              <Button
                onClick={handleEditCategory}
                disabled={!editFormData.name.trim()}
              >
                Modifier
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}